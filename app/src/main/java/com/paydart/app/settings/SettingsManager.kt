package com.paydart.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paydart_settings")

class SettingsManager(private val context: Context) {

    private val KEY_CAMERA_RESOLUTION = stringPreferencesKey("camera_resolution")
    private val KEY_PREFERRED_PACKAGE = stringPreferencesKey("preferred_upi_package")
    private val KEY_SCAN_BEHAVIOR = stringPreferencesKey("scan_behavior")
    private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val resName = preferences[KEY_CAMERA_RESOLUTION]
            val pkg = preferences[KEY_PREFERRED_PACKAGE]?.takeIf { it.isNotBlank() }
            val behaviorName = preferences[KEY_SCAN_BEHAVIOR]
            val vibration = preferences[KEY_VIBRATION_ENABLED] ?: true

            AppSettings(
                cameraResolution = CameraResolution.fromName(resName),
                preferredPackage = pkg,
                scanBehavior = ScanBehavior.fromName(behaviorName),
                vibrationEnabled = vibration
            )
        }

    suspend fun updateResolution(resolution: CameraResolution) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CAMERA_RESOLUTION] = resolution.name
        }
    }

    suspend fun updatePreferredPackage(packageName: String?) {
        context.dataStore.edit { preferences ->
            if (packageName.isNullOrBlank()) {
                preferences.remove(KEY_PREFERRED_PACKAGE)
            } else {
                preferences[KEY_PREFERRED_PACKAGE] = packageName
            }
        }
    }

    suspend fun updateScanBehavior(behavior: ScanBehavior) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SCAN_BEHAVIOR] = behavior.name
        }
    }

    suspend fun updateVibration(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VIBRATION_ENABLED] = enabled
        }
    }
}
