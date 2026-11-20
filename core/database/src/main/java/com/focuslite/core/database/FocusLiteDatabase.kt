package com.focuslite.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focuslite.core.database.dao.AppLimitDao
import com.focuslite.core.database.dao.BlockedAppDao
import com.focuslite.core.database.dao.DowntimeDao
import com.focuslite.core.database.dao.UsageRecordDao
import com.focuslite.core.database.entity.AppLimitEntity
import com.focuslite.core.database.entity.BlockedAppEntity
import com.focuslite.core.database.entity.DailyUsageRecordEntity
import com.focuslite.core.database.entity.DowntimeScheduleEntity

@Database(
    entities = [
        BlockedAppEntity::class,
        AppLimitEntity::class,
        DailyUsageRecordEntity::class,
        DowntimeScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusLiteDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun downtimeDao(): DowntimeDao

    companion object {
        @Volatile
        private var INSTANCE: FocusLiteDatabase? = null

        fun getInstance(context: Context): FocusLiteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusLiteDatabase::class.java,
                    "focuslite_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
