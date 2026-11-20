package com.focuslite.core.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.focuslite.core.common.TimeUtils
import com.focuslite.core.database.FocusLiteDatabase
import com.focuslite.core.database.repository.AppBlockerRepositoryImpl
import com.focuslite.core.database.repository.DowntimeRepositoryImpl
import com.focuslite.core.database.repository.UsageStatsRepositoryImpl
import com.focuslite.core.datastore.FocusBreakRepositoryImpl
import com.focuslite.core.datastore.ReelsShieldRepositoryImpl
import com.focuslite.core.datastore.dataStore
import com.focuslite.core.service.detector.FacebookReelsDetector
import com.focuslite.core.service.detector.InstagramReelsDetector
import com.focuslite.core.service.detector.ReelsDetector
import com.focuslite.core.service.detector.SnapchatSpotlightDetector
import com.focuslite.core.service.detector.YouTubeShortsDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusLite_A11y"
        private const val REELS_CHECK_DEBOUNCE_MS = 400L
        private const val ACTION_COOLDOWN_MS = 1500L
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private lateinit var appBlockEvaluator: AppBlockEvaluator
    private lateinit var usageStatsRepository: UsageStatsRepositoryImpl
    private lateinit var reelsShieldRepository: ReelsShieldRepositoryImpl
    private lateinit var focusBreakRepository: FocusBreakRepositoryImpl

    private val activeDetectors = mutableMapOf<String, ReelsDetector>()

    private var currentForegroundPackage: String? = null
    private var sessionStartTimeMillis: Long = 0L

    private var lastReelsCheckTimestamp: Long = 0L
    private var lastDismissActionTimestamp: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF -> Finalizing active session")
                    finalizeCurrentSession()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON -> Resetting session timer")
                    sessionStartTimeMillis = SystemClock.elapsedRealtime()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "FocusAccessibilityService onCreate")

        val db = FocusLiteDatabase.getInstance(this)
        val appBlockerRepo = AppBlockerRepositoryImpl(db)
        usageStatsRepository = UsageStatsRepositoryImpl(db)
        val downtimeRepo = DowntimeRepositoryImpl(db)
        focusBreakRepository = FocusBreakRepositoryImpl(dataStore)
        reelsShieldRepository = ReelsShieldRepositoryImpl(dataStore)

        appBlockEvaluator = AppBlockEvaluator(
            appBlockerRepository = appBlockerRepo,
            usageStatsRepository = usageStatsRepository,
            downtimeRepository = downtimeRepo,
            focusBreakRepository = focusBreakRepository
        )

        // Register ONLY validated detectors initially (Instagram & YouTube Shorts per M2 requirements)
        val allDetectors = listOf(
            InstagramReelsDetector(),
            YouTubeShortsDetector(),
            FacebookReelsDetector(),
            SnapchatSpotlightDetector()
        )

        allDetectors.filter { it.isValidated }.forEach { detector ->
            activeDetectors[detector.targetPackage] = detector
            Log.i(TAG, "Registered validated detector for ${detector.platform.displayName} (${detector.targetPackage})")
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg != currentForegroundPackage && !SystemAppWhitelist.isWhitelisted(pkg)) {
                    onPackageChanged(pkg)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastReelsCheckTimestamp >= REELS_CHECK_DEBOUNCE_MS) {
                    lastReelsCheckTimestamp = now
                    val pkg = event.packageName?.toString() ?: currentForegroundPackage
                    if (pkg != null && activeDetectors.containsKey(pkg)) {
                        checkAndBlockReels(pkg)
                    }
                }
            }
        }
    }

    private fun onPackageChanged(newPackage: String) {
        Log.d(TAG, "Foreground package changed: $currentForegroundPackage -> $newPackage")
        finalizeCurrentSession()
        currentForegroundPackage = newPackage
        sessionStartTimeMillis = SystemClock.elapsedRealtime()

        serviceScope.launch {
            val result = appBlockEvaluator.evaluate(newPackage)
            if (result is BlockEvaluationResult.Blocked) {
                Log.w(TAG, "Blocking application $newPackage due to ${result.reason}")
                // Send back to safely exit target app, then launch the overlay
                performGlobalAction(GLOBAL_ACTION_HOME)
                launchBlockingOverlay(newPackage, result.reason)
            }
        }
    }

    private fun finalizeCurrentSession() {
        val pkg = currentForegroundPackage ?: return
        if (sessionStartTimeMillis > 0) {
            val durationSeconds = ((SystemClock.elapsedRealtime() - sessionStartTimeMillis) / 1000).toInt()
            if (durationSeconds > 0) {
                val todayIso = TimeUtils.getTodayIsoUtc()
                Log.d(TAG, "Recording $durationSeconds seconds usage for $pkg on $todayIso")
                serviceScope.launch {
                    usageStatsRepository.recordUsageDelta(pkg, todayIso, durationSeconds)
                }
            }
        }
        sessionStartTimeMillis = 0L
    }

    private fun checkAndBlockReels(packageName: String) {
        val detector = activeDetectors[packageName] ?: return

        serviceScope.launch {
            // Check if user has an active break
            if (focusBreakRepository.isBreakActive(packageName)) {
                return@launch
            }

            // Check if Master Reels Shield or this platform shield is disabled
            if (!reelsShieldRepository.isReelsShieldEnabled().first()) return@launch
            if (!reelsShieldRepository.isPlatformShieldEnabled(detector.platform).first()) return@launch

            // Anti-loop cooldown check
            val now = SystemClock.elapsedRealtime()
            if (now - lastDismissActionTimestamp < ACTION_COOLDOWN_MS) {
                return@launch
            }

            val root = rootInActiveWindow ?: return@launch
            try {
                if (detector.isShortFormVisible(root)) {
                    Log.i(TAG, "Short-form video detected in ${detector.platform.displayName}! Triggering dismissal.")
                    lastDismissActionTimestamp = now
                    val dismissed = detector.performDismissAction(root)
                    if (!dismissed) {
                        Log.d(TAG, "Dismiss action unhandled -> dispatching GLOBAL_ACTION_BACK")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inspecting node hierarchy: ${e.message}")
            }
        }
    }

    private fun launchBlockingOverlay(packageName: String, reason: BlockReason) {
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockedAppActivity.EXTRA_REASON, reason.name)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "FocusAccessibilityService interrupted")
    }

    override fun onDestroy() {
        Log.i(TAG, "FocusAccessibilityService onDestroy")
        finalizeCurrentSession()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
