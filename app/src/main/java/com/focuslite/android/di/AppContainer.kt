package com.focuslite.android.di

import android.content.Context
import com.focuslite.core.database.FocusLiteDatabase
import com.focuslite.core.database.repository.AppBlockerRepositoryImpl
import com.focuslite.core.database.repository.DowntimeRepositoryImpl
import com.focuslite.core.database.repository.UsageStatsRepositoryImpl
import com.focuslite.core.datastore.FocusBreakRepositoryImpl
import com.focuslite.core.datastore.ReelsShieldRepositoryImpl
import com.focuslite.core.datastore.dataStore
import com.focuslite.core.model.AppBlockerRepository
import com.focuslite.core.model.DowntimeRepository
import com.focuslite.core.model.FocusBreakRepository
import com.focuslite.core.model.ReelsShieldRepository
import com.focuslite.core.model.UsageStatsRepository

interface AppContainer {
    val appBlockerRepository: AppBlockerRepository
    val usageStatsRepository: UsageStatsRepository
    val downtimeRepository: DowntimeRepository
    val reelsShieldRepository: ReelsShieldRepository
    val focusBreakRepository: FocusBreakRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: FocusLiteDatabase by lazy {
        FocusLiteDatabase.getInstance(context)
    }

    override val appBlockerRepository: AppBlockerRepository by lazy {
        AppBlockerRepositoryImpl(database)
    }

    override val usageStatsRepository: UsageStatsRepository by lazy {
        UsageStatsRepositoryImpl(database)
    }

    override val downtimeRepository: DowntimeRepository by lazy {
        DowntimeRepositoryImpl(database)
    }

    override val reelsShieldRepository: ReelsShieldRepository by lazy {
        ReelsShieldRepositoryImpl(context.dataStore)
    }

    override val focusBreakRepository: FocusBreakRepository by lazy {
        FocusBreakRepositoryImpl(context.dataStore)
    }
}
