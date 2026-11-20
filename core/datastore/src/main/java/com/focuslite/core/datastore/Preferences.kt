package com.focuslite.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focuslite.core.model.ActiveBreak
import com.focuslite.core.model.FocusBreakRepository
import com.focuslite.core.model.ReelsShieldRepository
import com.focuslite.core.model.ShortFormPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focuslite_preferences")

object PreferenceKeys {
    val REELS_SHIELD_MASTER = booleanPreferencesKey("reels_shield_master")
    val BREAK_END_TIME_MILLIS = longPreferencesKey("break_end_time_millis")
    val BREAK_ALLOWED_PACKAGE = stringPreferencesKey("break_allowed_package")

    fun platformKey(platform: ShortFormPlatform): Preferences.Key<Boolean> {
        return booleanPreferencesKey("reels_shield_${platform.name.lowercase()}")
    }
}

class ReelsShieldRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ReelsShieldRepository {

    override fun isReelsShieldEnabled(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PreferenceKeys.REELS_SHIELD_MASTER] ?: true // enabled by default
        }
    }

    override fun isPlatformShieldEnabled(platform: ShortFormPlatform): Flow<Boolean> {
        val key = PreferenceKeys.platformKey(platform)
        return dataStore.data.map { prefs ->
            prefs[key] ?: true
        }
    }

    override suspend fun setReelsShieldMasterToggle(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.REELS_SHIELD_MASTER] = enabled
        }
    }

    override suspend fun setPlatformShieldToggle(platform: ShortFormPlatform, enabled: Boolean) {
        val key = PreferenceKeys.platformKey(platform)
        dataStore.edit { prefs ->
            prefs[key] = enabled
        }
    }
}

class FocusBreakRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : FocusBreakRepository {

    override fun getActiveBreak(): Flow<ActiveBreak?> {
        return dataStore.data.map { prefs ->
            val endTime = prefs[PreferenceKeys.BREAK_END_TIME_MILLIS] ?: 0L
            val pkg = prefs[PreferenceKeys.BREAK_ALLOWED_PACKAGE]
            if (endTime > System.currentTimeMillis()) {
                ActiveBreak(allowedPackage = pkg, endTimeMillis = endTime)
            } else {
                null
            }
        }
    }

    override suspend fun requestBreak(durationMinutes: Int, allowedPackage: String?) {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.BREAK_END_TIME_MILLIS] = endTime
            if (allowedPackage != null) {
                prefs[PreferenceKeys.BREAK_ALLOWED_PACKAGE] = allowedPackage
            } else {
                prefs.remove(PreferenceKeys.BREAK_ALLOWED_PACKAGE)
            }
        }
    }

    override suspend fun cancelBreak() {
        dataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.BREAK_END_TIME_MILLIS)
            prefs.remove(PreferenceKeys.BREAK_ALLOWED_PACKAGE)
        }
    }

    override suspend fun isBreakActive(packageName: String?): Boolean {
        val prefs = dataStore.data.first()
        val endTime = prefs[PreferenceKeys.BREAK_END_TIME_MILLIS] ?: 0L
        val allowedPkg = prefs[PreferenceKeys.BREAK_ALLOWED_PACKAGE]

        if (endTime <= System.currentTimeMillis()) {
            return false
        }
        return allowedPkg == null || packageName == null || allowedPkg == packageName
    }
}
