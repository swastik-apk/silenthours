package com.example.silenthours.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class SettingsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SILENCE_MODE_ENABLED = "silence_mode_enabled"
        const val KEY_START_TIME = "start_time"
        const val KEY_END_TIME = "end_time"
        const val KEY_ORIGINAL_RINGER_MODE = "original_ringer_mode" // New key

        const val DEFAULT_START_TIME = "22:00"
        const val DEFAULT_END_TIME = "07:00"
    }

    fun setSilenceModeEnabled(enabled: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_SILENCE_MODE_ENABLED, enabled)
            apply()
        }
    }

    fun isSilenceModeEnabled(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SILENCE_MODE_ENABLED) {
                trySend(sharedPreferences.getBoolean(KEY_SILENCE_MODE_ENABLED, false))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        // Emit initial value
        trySend(sharedPreferences.getBoolean(KEY_SILENCE_MODE_ENABLED, false))
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setStartTime(time: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_START_TIME, time)
            apply()
        }
    }

    fun getStartTime(): Flow<String> = callbackFlow<String> {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_START_TIME) {
                trySend(sharedPreferences.getString(KEY_START_TIME, DEFAULT_START_TIME) ?: DEFAULT_START_TIME)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(sharedPreferences.getString(KEY_START_TIME, DEFAULT_START_TIME) ?: DEFAULT_START_TIME)
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }


    fun setEndTime(time: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_END_TIME, time)
            apply()
        }
    }

    fun getEndTime(): Flow<String> = callbackFlow<String> {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_END_TIME) {
                trySend(sharedPreferences.getString(KEY_END_TIME, DEFAULT_END_TIME) ?: DEFAULT_END_TIME)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(sharedPreferences.getString(KEY_END_TIME, DEFAULT_END_TIME) ?: DEFAULT_END_TIME)
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setOriginalRingerMode(mode: Int) {
        with(sharedPreferences.edit()) {
            putInt(KEY_ORIGINAL_RINGER_MODE, mode)
            apply()
        }
    }

    fun getOriginalRingerMode(): Int? {
        return if (sharedPreferences.contains(KEY_ORIGINAL_RINGER_MODE)) {
            sharedPreferences.getInt(KEY_ORIGINAL_RINGER_MODE, -1) // -1 can signify not found if needed, but contains check is better
        } else {
            null
        }
    }

    fun clearOriginalRingerMode() {
        with(sharedPreferences.edit()) {
            remove(KEY_ORIGINAL_RINGER_MODE)
            apply()
        }
    }
}
