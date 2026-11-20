package com.focuslite.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val platformCategory: String, // "INSTAGRAM", "YOUTUBE", "TIKTOK", "FACEBOOK", "SNAPCHAT", "OTHER"
    val isBlocked: Boolean = true,
    val isReelsBlocked: Boolean = true
)

@Entity(
    tableName = "app_limits",
    foreignKeys = [
        ForeignKey(
            entity = BlockedAppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val dailyQuotaMinutes: Int,
    val isEnabled: Boolean = true,
    val warningThresholdMinutes: Int = 5
)

@Entity(
    tableName = "daily_usage_records",
    indices = [
        Index(value = ["packageName", "dateIsoUtc"], unique = true),
        Index(value = ["dateIsoUtc"])
    ]
)
data class DailyUsageRecordEntity(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0,
    val packageName: String,
    val dateIsoUtc: String, // "YYYY-MM-DD"
    val usageSeconds: Int,
    val openCount: Int = 1,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downtime_schedules")
data class DowntimeScheduleEntity(
    @PrimaryKey(autoGenerate = true) val scheduleId: Long = 0,
    val name: String,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val daysOfWeekMask: Int = 127, // 1111111 = all days
    val isActive: Boolean = true,
    val allowEmergencyBreak: Boolean = true
)
