package com.focuslite.core.model

data class BlockedApp(
    val packageName: String,
    val appName: String,
    val platformCategory: ShortFormPlatform,
    val isBlocked: Boolean = true,
    val isReelsBlocked: Boolean = true
)

data class AppLimit(
    val packageName: String,
    val dailyQuotaMinutes: Int,
    val isEnabled: Boolean = true,
    val warningThresholdMinutes: Int = 5
)

data class DailyUsage(
    val packageName: String,
    val dateIsoUtc: String,
    val usageSeconds: Int,
    val openCount: Int,
    val lastUsedTimestamp: Long
)

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val usageMinutes: Int,
    val dailyQuotaMinutes: Int?,
    val isBlocked: Boolean
)

data class DowntimeSchedule(
    val scheduleId: Long = 0,
    val name: String,
    val startTime: String, // HH:mm
    val endTime: String,   // HH:mm
    val daysOfWeekMask: Int, // Bit 0 = Sunday .. Bit 6 = Saturday
    val isActive: Boolean = true,
    val allowEmergencyBreak: Boolean = true
)

enum class ShortFormPlatform(val displayName: String, val defaultPackageName: String) {
    INSTAGRAM("Instagram", "com.instagram.android"),
    YOUTUBE("YouTube", "com.google.android.youtube"),
    TIKTOK("TikTok", "com.zhiliaoapp.musically"),
    FACEBOOK("Facebook", "com.facebook.katana"),
    SNAPCHAT("Snapchat", "com.snapchat.android"),
    OTHER("Other", "")
}

data class ActiveBreak(
    val allowedPackage: String?,
    val endTimeMillis: Long
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= endTimeMillis
}
