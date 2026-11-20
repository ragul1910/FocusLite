package com.focuslite.core.service

import android.util.Log
import com.focuslite.core.common.TimeUtils
import com.focuslite.core.model.AppBlockerRepository
import com.focuslite.core.model.DowntimeRepository
import com.focuslite.core.model.FocusBreakRepository
import com.focuslite.core.model.UsageStatsRepository
import kotlinx.coroutines.flow.first

sealed interface BlockEvaluationResult {
    data object Allowed : BlockEvaluationResult
    data class Blocked(val reason: BlockReason) : BlockEvaluationResult
}

enum class BlockReason {
    QUOTA_EXCEEDED,
    DOWNTIME_ACTIVE,
    APP_BLOCKED
}

class AppBlockEvaluator(
    private val appBlockerRepository: AppBlockerRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val downtimeRepository: DowntimeRepository,
    private val focusBreakRepository: FocusBreakRepository
) {
    companion object {
        private const val TAG = "FocusLite_A11y"
    }

    suspend fun evaluate(
        packageName: String,
        currentTimeHhMm: String? = null,
        dayOfWeek: Int? = null,
        todayIso: String? = null
    ): BlockEvaluationResult {
        // 0. Whitelist bypass: Essential phone, SMS, system settings, and launchers are never blocked
        if (SystemAppWhitelist.isWhitelisted(packageName)) {
            Log.d(TAG, "[$packageName] Whitelisted system application -> Allowed")
            return BlockEvaluationResult.Allowed
        }

        // 1. Temporary mindful break active check
        if (focusBreakRepository.isBreakActive(packageName)) {
            Log.d(TAG, "[$packageName] Active break in progress -> Allowed")
            return BlockEvaluationResult.Allowed
        }

        // 2. Downtime schedule check
        val time = currentTimeHhMm ?: TimeUtils.getCurrentTimeHhMm()
        val day = dayOfWeek ?: TimeUtils.getDayOfWeek()
        if (downtimeRepository.isDowntimeActive(time, day)) {
            Log.i(TAG, "[$packageName] Downtime active ($time, day $day) -> Blocked (DOWNTIME_ACTIVE)")
            return BlockEvaluationResult.Blocked(BlockReason.DOWNTIME_ACTIVE)
        }

        // 3. Daily quota usage limit check
        val limit = appBlockerRepository.getAppLimit(packageName).first()
        if (limit != null && limit.isEnabled) {
            val date = todayIso ?: TimeUtils.getTodayIsoUtc()
            val usedSeconds = usageStatsRepository.getTodayUsageSeconds(packageName, date)
            val quotaSeconds = limit.dailyQuotaMinutes * 60
            if (usedSeconds >= quotaSeconds) {
                Log.i(TAG, "[$packageName] Quota exceeded (${usedSeconds}s >= ${quotaSeconds}s) -> Blocked (QUOTA_EXCEEDED)")
                return BlockEvaluationResult.Blocked(BlockReason.QUOTA_EXCEEDED)
            }
        }

        // 4. Explicit app block toggle check
        val isExplicitlyBlocked = appBlockerRepository.isAppBlocked(packageName)
        if (isExplicitlyBlocked) {
            Log.i(TAG, "[$packageName] Explicit block rule active -> Blocked (APP_BLOCKED)")
            return BlockEvaluationResult.Blocked(BlockReason.APP_BLOCKED)
        }

        Log.d(TAG, "[$packageName] No blocking rules matched -> Allowed")
        return BlockEvaluationResult.Allowed
    }
}
