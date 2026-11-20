package com.focuslite.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    fun getTodayIsoUtc(): String {
        return isoFormat.format(Date())
    }

    fun getCurrentTimeHhMm(): String {
        return timeFormat.format(Date())
    }

    fun getDayOfWeek(): Int {
        // Calendar: 1 = Sunday, 2 = Monday ... 7 = Saturday
        val cal = Calendar.getInstance()
        return cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday .. 6 = Saturday
    }

    fun isTimeBetween(currentTime: String, startTime: String, endTime: String): Boolean {
        return if (startTime <= endTime) {
            currentTime in startTime..endTime
        } else {
            // Spans midnight, e.g. 22:00 to 07:00
            currentTime >= startTime || currentTime <= endTime
        }
    }
}
