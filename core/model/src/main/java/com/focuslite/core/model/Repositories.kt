package com.focuslite.core.model

import kotlinx.coroutines.flow.Flow

interface AppBlockerRepository {
    fun getBlockedApps(): Flow<List<BlockedApp>>
    fun getAppLimit(packageName: String): Flow<AppLimit?>
    fun getAllLimits(): Flow<List<AppLimit>>
    suspend fun setAppLimit(packageName: String, quotaMinutes: Int, isEnabled: Boolean)
    suspend fun toggleAppBlock(packageName: String, isBlocked: Boolean)
    suspend fun isAppBlocked(packageName: String): Boolean
    suspend fun insertDefaultAppsIfEmpty()
}

interface UsageStatsRepository {
    fun getDailyUsage(dateIsoUtc: String): Flow<List<DailyUsage>>
    fun getUsageForApp(packageName: String, startDate: String, endDate: String): Flow<List<DailyUsage>>
    suspend fun recordUsageDelta(packageName: String, dateIsoUtc: String, deltaSeconds: Int)
    suspend fun getTodayUsageSeconds(packageName: String, dateIsoUtc: String): Int
}

interface DowntimeRepository {
    fun getActiveSchedules(): Flow<List<DowntimeSchedule>>
    suspend fun getScheduleById(id: Long): DowntimeSchedule?
    suspend fun saveSchedule(schedule: DowntimeSchedule)
    suspend fun deleteSchedule(id: Long)
    suspend fun isDowntimeActive(currentLocalTime: String, dayOfWeek: Int): Boolean
}

interface ReelsShieldRepository {
    fun isReelsShieldEnabled(): Flow<Boolean>
    fun isPlatformShieldEnabled(platform: ShortFormPlatform): Flow<Boolean>
    suspend fun setReelsShieldMasterToggle(enabled: Boolean)
    suspend fun setPlatformShieldToggle(platform: ShortFormPlatform, enabled: Boolean)
}

interface FocusBreakRepository {
    fun getActiveBreak(): Flow<ActiveBreak?>
    suspend fun requestBreak(durationMinutes: Int, allowedPackage: String? = null)
    suspend fun cancelBreak()
    suspend fun isBreakActive(packageName: String?): Boolean
}
