package com.example.silenthours.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import android.app.Application
import android.content.Context // For AudioManager
import android.media.AudioManager // For AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.silenthours.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class SettingsViewModel(private val application: Application) : ViewModel() { // Made application private val

    private val settingsRepository = SettingsRepository(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val silenceModeEnabled: StateFlow<Boolean> = settingsRepository.isSilenceModeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val startTime: StateFlow<String> = settingsRepository.getStartTime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SettingsRepository.DEFAULT_START_TIME)

    val endTime: StateFlow<String> = settingsRepository.getEndTime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SettingsRepository.DEFAULT_END_TIME)

    val isCurrentlySilencingActive: StateFlow<Boolean> =
        combine(silenceModeEnabled, startTime, endTime) { enabled, start, end ->
            if (!enabled) {
                false
            } else {
                isCurrentTimeInSilenceRange(start, end)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    init {
        // Check and restore ringer mode if app was terminated while silencing
        viewModelScope.launch {
            val originalMode = settingsRepository.getOriginalRingerMode()
            if (originalMode != null && audioManager.ringerMode != originalMode && !isCurrentlySilencingActive.value) {
                // If silencing is not supposed to be active now, but ringer is still silent from us
                // And we have a stored original mode that is different from current.
                 try {
                    audioManager.ringerMode = originalMode
                    settingsRepository.clearOriginalRingerMode()
                } catch (e: SecurityException) {
                    // Potentially log this, DND access might be needed on some devices
                }
            }
        }
    }

    private fun isCurrentTimeInSilenceRange(startTimeStr: String, endTimeStr: String): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            val startParts = startTimeStr.split(":").map { it.toInt() }
            val startTimeInMinutes = startParts[0] * 60 + startParts[1]

            val endParts = endTimeStr.split(":").map { it.toInt() }
            var endTimeInMinutes = endParts[0] * 60 + endParts[1]

            if (endTimeInMinutes < startTimeInMinutes) { // Overnight range
                currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutes
            } else { // Same day range
                currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
            }
        } catch (e: Exception) {
            false // Default to not in range if parsing fails
        }
    }

    fun toggleSilenceMode() {
        viewModelScope.launch {
            val newModeEnabled = !silenceModeEnabled.value
            settingsRepository.setSilenceModeEnabled(newModeEnabled)
            if (!newModeEnabled) { // If disabling silence mode
                restoreOriginalRingerMode()
            }
        }
    }

    private fun restoreOriginalRingerMode() {
        viewModelScope.launch {
            val originalMode = settingsRepository.getOriginalRingerMode()
            if (originalMode != null) {
                try {
                    audioManager.ringerMode = originalMode
                    settingsRepository.clearOriginalRingerMode()
                } catch (e: SecurityException) {
                     // Log error, DND access might be required for full control on some devices/Android versions
                }
            }
        }
    }

    fun updateStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            settingsRepository.setStartTime(timeString)
        }
    }

    fun updateEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            settingsRepository.setEndTime(timeString)
        }
    }
}

@Suppress("UNCHECKED_CAST")
class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
