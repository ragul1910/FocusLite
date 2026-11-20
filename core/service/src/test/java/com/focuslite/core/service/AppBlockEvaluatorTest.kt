package com.focuslite.core.service

import com.focuslite.core.model.ActiveBreak
import com.focuslite.core.model.AppBlockerRepository
import com.focuslite.core.model.AppLimit
import com.focuslite.core.model.BlockedApp
import com.focuslite.core.model.DailyUsage
import com.focuslite.core.model.DowntimeRepository
import com.focuslite.core.model.DowntimeSchedule
import com.focuslite.core.model.FocusBreakRepository
import com.focuslite.core.model.ShortFormPlatform
import com.focuslite.core.model.UsageStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppBlockEvaluatorTest {

    private lateinit var fakeAppBlockerRepo: FakeAppBlockerRepository
    private lateinit var fakeUsageStatsRepo: FakeUsageStatsRepository
    private lateinit var fakeDowntimeRepo: FakeDowntimeRepository
    private lateinit var fakeFocusBreakRepo: FakeFocusBreakRepository

    private lateinit var evaluator: AppBlockEvaluator

    @Before
    fun setUp() {
        fakeAppBlockerRepo = FakeAppBlockerRepository()
        fakeUsageStatsRepo = FakeUsageStatsRepository()
        fakeDowntimeRepo = FakeDowntimeRepository()
        fakeFocusBreakRepo = FakeFocusBreakRepository()

        evaluator = AppBlockEvaluator(
            appBlockerRepository = fakeAppBlockerRepo,
            usageStatsRepository = fakeUsageStatsRepo,
            downtimeRepository = fakeDowntimeRepo,
            focusBreakRepository = fakeFocusBreakRepo
        )
    }

    @Test
    fun testWhitelistedSystemApp_isAlwaysAllowed() = runTest {
        // Even if downtime is active and the app is explicitly configured as blocked, system dialer must NOT be blocked
        fakeDowntimeRepo.isDowntimeActive = true
        fakeAppBlockerRepo.blockedPackages.add("com.google.android.dialer")

        val resultDialer = evaluator.evaluate("com.google.android.dialer")
        assertEquals(BlockEvaluationResult.Allowed, resultDialer)

        val resultSettings = evaluator.evaluate("com.android.settings")
        assertEquals(BlockEvaluationResult.Allowed, resultSettings)

        val resultFocusLite = evaluator.evaluate("com.focuslite.android")
        assertEquals(BlockEvaluationResult.Allowed, resultFocusLite)
    }

    @Test
    fun testBreakActive_allowsRestrictedApp() = runTest {
        val testPkg = "com.instagram.android"
        fakeAppBlockerRepo.blockedPackages.add(testPkg)

        // Without break -> blocked
        val resultBefore = evaluator.evaluate(testPkg)
        assertTrue(resultBefore is BlockEvaluationResult.Blocked)

        // Activate mindful break
        fakeFocusBreakRepo.activeBreak = ActiveBreak(
            allowedPackage = testPkg,
            endTimeMillis = System.currentTimeMillis() + 300_000L
        )

        // With break -> allowed immediately
        val resultAfter = evaluator.evaluate(testPkg)
        assertEquals(BlockEvaluationResult.Allowed, resultAfter)
    }

    @Test
    fun testDowntimeActive_blocksNonWhitelistedApp() = runTest {
        val testPkg = "com.zhiliaoapp.musically"
        fakeDowntimeRepo.isDowntimeActive = true

        val result = evaluator.evaluate(testPkg, currentTimeHhMm = "23:00", dayOfWeek = 2)
        assertEquals(BlockEvaluationResult.Blocked(BlockReason.DOWNTIME_ACTIVE), result)
    }

    @Test
    fun testDowntimeInactive_allowsUnconstrainedApp() = runTest {
        val testPkg = "com.example.productivity"
        fakeDowntimeRepo.isDowntimeActive = false

        val result = evaluator.evaluate(testPkg, currentTimeHhMm = "14:00", dayOfWeek = 2)
        assertEquals(BlockEvaluationResult.Allowed, result)
    }

    @Test
    fun testExplicitAppBlock_blocksApp() = runTest {
        val testPkg = "com.facebook.katana"
        fakeAppBlockerRepo.blockedPackages.add(testPkg)

        val result = evaluator.evaluate(testPkg)
        assertEquals(BlockEvaluationResult.Blocked(BlockReason.APP_BLOCKED), result)
    }

    @Test
    fun testAppQuotaExceeded_blocksApp() = runTest {
        val testPkg = "com.google.android.youtube"
        val today = "2026-11-20"
        // 30 minute quota = 1800 seconds
        fakeAppBlockerRepo.appLimits[testPkg] = AppLimit(
            packageName = testPkg,
            dailyQuotaMinutes = 30,
            isEnabled = true
        )
        fakeUsageStatsRepo.usageRecords["$testPkg:$today"] = 1801

        val result = evaluator.evaluate(testPkg, todayIso = today)
        assertEquals(BlockEvaluationResult.Blocked(BlockReason.QUOTA_EXCEEDED), result)
    }

    @Test
    fun testAppQuotaWithinLimit_allowsApp() = runTest {
        val testPkg = "com.google.android.youtube"
        val today = "2026-11-20"
        fakeAppBlockerRepo.appLimits[testPkg] = AppLimit(
            packageName = testPkg,
            dailyQuotaMinutes = 30,
            isEnabled = true
        )
        fakeUsageStatsRepo.usageRecords["$testPkg:$today"] = 900 // 15 mins

        val result = evaluator.evaluate(testPkg, todayIso = today)
        assertEquals(BlockEvaluationResult.Allowed, result)
    }

    @Test
    fun testDisabledAppLimit_allowsAppEvenIfUsageExceedsQuota() = runTest {
        val testPkg = "com.custom.app"
        val today = "2026-11-20"
        fakeAppBlockerRepo.appLimits[testPkg] = AppLimit(
            packageName = testPkg,
            dailyQuotaMinutes = 10,
            isEnabled = false // disabled
        )
        fakeUsageStatsRepo.usageRecords["$testPkg:$today"] = 3600 // 1 hr used

        val result = evaluator.evaluate(testPkg, todayIso = today)
        assertEquals(BlockEvaluationResult.Allowed, result)
    }

    @Test
    fun testDisablingEnforcement_immediatelyStopsBlocking() = runTest {
        val testPkg = "com.game.addictive"
        fakeAppBlockerRepo.blockedPackages.add(testPkg)

        // Blocked initially
        val result1 = evaluator.evaluate(testPkg)
        assertEquals(BlockEvaluationResult.Blocked(BlockReason.APP_BLOCKED), result1)

        // User unblocks in settings
        fakeAppBlockerRepo.blockedPackages.remove(testPkg)

        val result2 = evaluator.evaluate(testPkg)
        assertEquals(BlockEvaluationResult.Allowed, result2)
    }

    @Test
    fun testConfigurableCustomTestPackages() = runTest {
        val testPackages = listOf("com.test.app1", "com.test.app2", "com.test.app3")
        val today = "2026-11-20"

        // Configure app1: blocked
        fakeAppBlockerRepo.blockedPackages.add(testPackages[0])
        // Configure app2: limited to 5m, exceeded
        fakeAppBlockerRepo.appLimits[testPackages[1]] = AppLimit(testPackages[1], 5, isEnabled = true)
        fakeUsageStatsRepo.usageRecords["${testPackages[1]}:$today"] = 301
        // Configure app3: free
        // (no restrictions)

        assertEquals(BlockEvaluationResult.Blocked(BlockReason.APP_BLOCKED), evaluator.evaluate(testPackages[0], todayIso = today))
        assertEquals(BlockEvaluationResult.Blocked(BlockReason.QUOTA_EXCEEDED), evaluator.evaluate(testPackages[1], todayIso = today))
        assertEquals(BlockEvaluationResult.Allowed, evaluator.evaluate(testPackages[2], todayIso = today))
    }
}

// --- FAKE REPOSITORIES FOR DETERMINISTIC TESTING ---

class FakeAppBlockerRepository : AppBlockerRepository {
    val blockedPackages = mutableSetOf<String>()
    val appLimits = mutableMapOf<String, AppLimit>()

    override fun getBlockedApps(): Flow<List<BlockedApp>> = flowOf(emptyList())

    override fun getAppLimit(packageName: String): Flow<AppLimit?> = flowOf(appLimits[packageName])

    override fun getAllLimits(): Flow<List<AppLimit>> = flowOf(appLimits.values.toList())

    override suspend fun setAppLimit(packageName: String, quotaMinutes: Int, isEnabled: Boolean) {
        appLimits[packageName] = AppLimit(packageName, quotaMinutes, isEnabled)
    }

    override suspend fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        if (isBlocked) blockedPackages.add(packageName) else blockedPackages.remove(packageName)
    }

    override suspend fun isAppBlocked(packageName: String): Boolean = blockedPackages.contains(packageName)

    override suspend fun insertDefaultAppsIfEmpty() {}
}

class FakeUsageStatsRepository : UsageStatsRepository {
    val usageRecords = mutableMapOf<String, Int>()

    override fun getDailyUsage(dateIsoUtc: String): Flow<List<DailyUsage>> = flowOf(emptyList())

    override fun getUsageForApp(packageName: String, startDate: String, endDate: String): Flow<List<DailyUsage>> = flowOf(emptyList())

    override suspend fun recordUsageDelta(packageName: String, dateIsoUtc: String, deltaSeconds: Int) {
        val key = "$packageName:$dateIsoUtc"
        usageRecords[key] = (usageRecords[key] ?: 0) + deltaSeconds
    }

    override suspend fun getTodayUsageSeconds(packageName: String, dateIsoUtc: String): Int {
        val key = "$packageName:$dateIsoUtc"
        return usageRecords[key] ?: 0
    }
}

class FakeDowntimeRepository : DowntimeRepository {
    var isDowntimeActive: Boolean = false

    override fun getActiveSchedules(): Flow<List<DowntimeSchedule>> = flowOf(emptyList())

    override suspend fun getScheduleById(id: Long): DowntimeSchedule? = null

    override suspend fun saveSchedule(schedule: DowntimeSchedule) {}

    override suspend fun deleteSchedule(id: Long) {}

    override suspend fun isDowntimeActive(currentLocalTime: String, dayOfWeek: Int): Boolean = isDowntimeActive
}

class FakeFocusBreakRepository : FocusBreakRepository {
    var activeBreak: ActiveBreak? = null

    override fun getActiveBreak(): Flow<ActiveBreak?> = flowOf(activeBreak)

    override suspend fun requestBreak(durationMinutes: Int, allowedPackage: String?) {
        activeBreak = ActiveBreak(
            allowedPackage = allowedPackage,
            endTimeMillis = System.currentTimeMillis() + durationMinutes * 60_000L
        )
    }

    override suspend fun cancelBreak() {
        activeBreak = null
    }

    override suspend fun isBreakActive(packageName: String?): Boolean {
        val b = activeBreak ?: return false
        if (b.isExpired) return false
        return b.allowedPackage == null || b.allowedPackage == packageName
    }
}
