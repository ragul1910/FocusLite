package com.focuslite.core.database.repository

import com.focuslite.core.common.TimeUtils
import com.focuslite.core.database.FocusLiteDatabase
import com.focuslite.core.database.entity.AppLimitEntity
import com.focuslite.core.database.entity.BlockedAppEntity
import com.focuslite.core.database.entity.DailyUsageRecordEntity
import com.focuslite.core.database.entity.DowntimeScheduleEntity
import com.focuslite.core.model.AppLimit
import com.focuslite.core.model.AppBlockerRepository
import com.focuslite.core.model.BlockedApp
import com.focuslite.core.model.DailyUsage
import com.focuslite.core.model.DowntimeRepository
import com.focuslite.core.model.DowntimeSchedule
import com.focuslite.core.model.ShortFormPlatform
import com.focuslite.core.model.UsageStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppBlockerRepositoryImpl(
    private val database: FocusLiteDatabase
) : AppBlockerRepository {

    private val blockedAppDao = database.blockedAppDao()
    private val appLimitDao = database.appLimitDao()

    override fun getBlockedApps(): Flow<List<BlockedApp>> {
        return blockedAppDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAppLimit(packageName: String): Flow<AppLimit?> {
        return appLimitDao.getByPackage(packageName).map { it?.toDomain() }
    }

    override fun getAllLimits(): Flow<List<AppLimit>> {
        return appLimitDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun setAppLimit(packageName: String, quotaMinutes: Int, isEnabled: Boolean) {
        appLimitDao.insertOrUpdate(
            AppLimitEntity(
                packageName = packageName,
                dailyQuotaMinutes = quotaMinutes,
                isEnabled = isEnabled
            )
        )
    }

    override suspend fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        val existing = blockedAppDao.getByPackage(packageName)
        if (existing != null) {
            blockedAppDao.insertOrUpdate(existing.copy(isBlocked = isBlocked))
        }
    }

    override suspend fun isAppBlocked(packageName: String): Boolean {
        val app = blockedAppDao.getByPackage(packageName) ?: return false
        return app.isBlocked
    }

    override suspend fun insertDefaultAppsIfEmpty() {
        if (blockedAppDao.count() == 0) {
            val defaults = listOf(
                BlockedAppEntity(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    platformCategory = "INSTAGRAM",
                    isBlocked = true,
                    isReelsBlocked = true
                ),
                BlockedAppEntity(
                    packageName = "com.google.android.youtube",
                    appName = "YouTube",
                    platformCategory = "YOUTUBE",
                    isBlocked = false, // Allow normal YouTube, but block Shorts
                    isReelsBlocked = true
                ),
                BlockedAppEntity(
                    packageName = "com.zhiliaoapp.musically",
                    appName = "TikTok",
                    platformCategory = "TIKTOK",
                    isBlocked = true,
                    isReelsBlocked = true
                ),
                BlockedAppEntity(
                    packageName = "com.facebook.katana",
                    appName = "Facebook",
                    platformCategory = "FACEBOOK",
                    isBlocked = true,
                    isReelsBlocked = true
                ),
                BlockedAppEntity(
                    packageName = "com.snapchat.android",
                    appName = "Snapchat",
                    platformCategory = "SNAPCHAT",
                    isBlocked = true,
                    isReelsBlocked = true
                )
            )
            blockedAppDao.insertAll(defaults)
        }
    }

    private fun BlockedAppEntity.toDomain() = BlockedApp(
        packageName = packageName,
        appName = appName,
        platformCategory = try { ShortFormPlatform.valueOf(platformCategory) } catch (_: Exception) { ShortFormPlatform.OTHER },
        isBlocked = isBlocked,
        isReelsBlocked = isReelsBlocked
    )

    private fun AppLimitEntity.toDomain() = AppLimit(
        packageName = packageName,
        dailyQuotaMinutes = dailyQuotaMinutes,
        isEnabled = isEnabled,
        warningThresholdMinutes = warningThresholdMinutes
    )
}

class UsageStatsRepositoryImpl(
    private val database: FocusLiteDatabase
) : UsageStatsRepository {

    private val usageRecordDao = database.usageRecordDao()

    override fun getDailyUsage(dateIsoUtc: String): Flow<List<DailyUsage>> {
        return usageRecordDao.getDailyUsage(dateIsoUtc).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUsageForApp(packageName: String, startDate: String, endDate: String): Flow<List<DailyUsage>> {
        return usageRecordDao.getUsageRange(packageName, startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordUsageDelta(packageName: String, dateIsoUtc: String, deltaSeconds: Int) {
        val now = System.currentTimeMillis()
        val updated = usageRecordDao.incrementUsage(packageName, dateIsoUtc, deltaSeconds, now)
        if (updated == 0) {
            usageRecordDao.insertRecord(
                DailyUsageRecordEntity(
                    packageName = packageName,
                    dateIsoUtc = dateIsoUtc,
                    usageSeconds = deltaSeconds,
                    openCount = 1,
                    lastUsedTimestamp = now
                )
            )
        }
    }

    override suspend fun getTodayUsageSeconds(packageName: String, dateIsoUtc: String): Int {
        return usageRecordDao.getUsageSeconds(packageName, dateIsoUtc) ?: 0
    }

    private fun DailyUsageRecordEntity.toDomain() = DailyUsage(
        packageName = packageName,
        dateIsoUtc = dateIsoUtc,
        usageSeconds = usageSeconds,
        openCount = openCount,
        lastUsedTimestamp = lastUsedTimestamp
    )
}

class DowntimeRepositoryImpl(
    private val database: FocusLiteDatabase
) : DowntimeRepository {

    private val downtimeDao = database.downtimeDao()

    override fun getActiveSchedules(): Flow<List<DowntimeSchedule>> {
        return downtimeDao.getActive().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getScheduleById(id: Long): DowntimeSchedule? {
        return downtimeDao.getById(id)?.toDomain()
    }

    override suspend fun saveSchedule(schedule: DowntimeSchedule) {
        downtimeDao.insertOrUpdate(
            DowntimeScheduleEntity(
                scheduleId = schedule.scheduleId,
                name = schedule.name,
                startTime = schedule.startTime,
                endTime = schedule.endTime,
                daysOfWeekMask = schedule.daysOfWeekMask,
                isActive = schedule.isActive,
                allowEmergencyBreak = schedule.allowEmergencyBreak
            )
        )
    }

    override suspend fun deleteSchedule(id: Long) {
        downtimeDao.deleteById(id)
    }

    override suspend fun isDowntimeActive(currentLocalTime: String, dayOfWeek: Int): Boolean {
        val activeSchedules = downtimeDao.getActiveSync()
        for (sched in activeSchedules) {
            val dayBit = 1 shl dayOfWeek
            if ((sched.daysOfWeekMask and dayBit) != 0) {
                if (TimeUtils.isTimeBetween(currentLocalTime, sched.startTime, sched.endTime)) {
                    return true
                }
            }
        }
        return false
    }

    private fun DowntimeScheduleEntity.toDomain() = DowntimeSchedule(
        scheduleId = scheduleId,
        name = name,
        startTime = startTime,
        endTime = endTime,
        daysOfWeekMask = daysOfWeekMask,
        isActive = isActive,
        allowEmergencyBreak = allowEmergencyBreak
    )
}
