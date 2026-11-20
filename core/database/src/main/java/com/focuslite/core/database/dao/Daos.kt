package com.focuslite.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focuslite.core.database.entity.AppLimitEntity
import com.focuslite.core.database.entity.BlockedAppEntity
import com.focuslite.core.database.entity.DailyUsageRecordEntity
import com.focuslite.core.database.entity.DowntimeScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): BlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apps: List<BlockedAppEntity>)

    @Query("SELECT COUNT(*) FROM blocked_apps")
    suspend fun count(): Int
}

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits")
    fun getAll(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    fun getByPackage(packageName: String): Flow<AppLimitEntity?>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageSync(packageName: String): AppLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(limit: AppLimitEntity)
}

@Dao
interface UsageRecordDao {
    @Query("SELECT * FROM daily_usage_records WHERE dateIsoUtc = :dateIsoUtc")
    fun getDailyUsage(dateIsoUtc: String): Flow<List<DailyUsageRecordEntity>>

    @Query("SELECT * FROM daily_usage_records WHERE packageName = :packageName AND dateIsoUtc BETWEEN :startDate AND :endDate ORDER BY dateIsoUtc ASC")
    fun getUsageRange(packageName: String, startDate: String, endDate: String): Flow<List<DailyUsageRecordEntity>>

    @Query("SELECT usageSeconds FROM daily_usage_records WHERE packageName = :packageName AND dateIsoUtc = :dateIsoUtc LIMIT 1")
    suspend fun getUsageSeconds(packageName: String, dateIsoUtc: String): Int?

    @Query("UPDATE daily_usage_records SET usageSeconds = usageSeconds + :deltaSeconds, lastUsedTimestamp = :now WHERE packageName = :packageName AND dateIsoUtc = :dateIsoUtc")
    suspend fun incrementUsage(packageName: String, dateIsoUtc: String, deltaSeconds: Int, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: DailyUsageRecordEntity): Long
}

@Dao
interface DowntimeDao {
    @Query("SELECT * FROM downtime_schedules ORDER BY startTime ASC")
    fun getAll(): Flow<List<DowntimeScheduleEntity>>

    @Query("SELECT * FROM downtime_schedules WHERE isActive = 1")
    fun getActive(): Flow<List<DowntimeScheduleEntity>>

    @Query("SELECT * FROM downtime_schedules WHERE isActive = 1")
    suspend fun getActiveSync(): List<DowntimeScheduleEntity>

    @Query("SELECT * FROM downtime_schedules WHERE scheduleId = :id LIMIT 1")
    suspend fun getById(id: Long): DowntimeScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: DowntimeScheduleEntity): Long

    @Query("DELETE FROM downtime_schedules WHERE scheduleId = :id")
    suspend fun deleteById(id: Long)
}
