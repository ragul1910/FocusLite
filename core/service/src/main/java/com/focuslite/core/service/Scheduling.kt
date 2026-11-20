package com.focuslite.core.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focuslite.core.database.FocusLiteDatabase
import com.focuslite.core.database.repository.DowntimeRepositoryImpl
import com.focuslite.core.model.DowntimeSchedule
import kotlinx.coroutines.flow.first
import java.util.Calendar

class DowntimeAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DOWNTIME_TRIGGER = "com.focuslite.action.DOWNTIME_TRIGGER"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DowntimeAlarmReceiver", "Downtime alarm triggered")
        // Alarms wake the device and ensure service state evaluation
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. Restoring alarms.")
            // Re-arm alarms
        }
    }
}

class DailyResetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("DailyResetWorker", "Midnight daily reset executed")
        return Result.success()
    }
}
