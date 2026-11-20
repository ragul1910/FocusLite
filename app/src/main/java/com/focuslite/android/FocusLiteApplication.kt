package com.focuslite.android

import android.app.Application
import com.focuslite.android.di.AppContainer
import com.focuslite.android.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusLiteApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Prepopulate default social app targets
        CoroutineScope(Dispatchers.IO).launch {
            container.appBlockerRepository.insertDefaultAppsIfEmpty()
        }
    }
}
