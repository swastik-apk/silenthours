package com.example.silenthours.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.silenthours.data.preferences.SettingsRepository
import com.example.silenthours.ui.theme.SilentHoursTheme
import java.util.Calendar

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val silenceModeEnabled by viewModel.silenceModeEnabled.collectAsStateWithLifecycle()
    val startTime by viewModel.startTime.collectAsStateWithLifecycle()
    val endTime by viewModel.endTime.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Helper to parse "HH:mm" string to Pair(Hour, Minute)
    fun parseTime(timeString: String): Pair<Int, Int> {
        return try {
            val parts = timeString.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(0, 0) // Default to 00:00 if parsing fails
        }
    }

    val showStartTimePicker = rememberTimePickerDialog(
        context = context,
        initialHour = parseTime(startTime).first,
        initialMinute = parseTime(startTime).second,
        onTimeSelected = { hour, minute -> viewModel.updateStartTime(hour, minute) }
    )

    val showEndTimePicker = rememberTimePickerDialog(
        context = context,
        initialHour = parseTime(endTime).first,
        initialMinute = parseTime(endTime).second,
        onTimeSelected = { hour, minute -> viewModel.updateEndTime(hour, minute) }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable Silence Mode", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = silenceModeEnabled,
                onCheckedChange = { viewModel.toggleSilenceMode() }
            )
        }

        Divider()

        SettingTimeRow(
            label = "Start Time",
            time = startTime,
            onClick = showStartTimePicker,
            enabled = silenceModeEnabled // Optionally disable if global silence mode is off
        )

        SettingTimeRow(
            label = "End Time",
            time = endTime,
            onClick = showEndTimePicker,
            enabled = silenceModeEnabled // Optionally disable if global silence mode is off
        )
    }
}

@Composable
fun SettingTimeRow(label: String, time: String, onClick: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun rememberTimePickerDialog(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    is24HourView: Boolean = false // Default to 12-hour format
): () -> Unit {
    // Use remember to ensure the TimePickerDialog is not recreated on each recomposition unnecessarily
    // and that the lambda returned always refers to the correct context and callbacks.
    return remember(context, initialHour, initialMinute, onTimeSelected, is24HourView) {
        {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute -> onTimeSelected(hourOfDay, minute) },
                initialHour,
                initialMinute,
                is24HourView
            ).show()
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    // This preview requires Application context for ViewModel, which is tricky.
    // For a true preview, use a fake ViewModel or run on an emulator/device.
    // Here's a simplified setup that might not fully work in Android Studio Preview:
    SilentHoursTheme {
        val context = LocalContext.current
        val dummyRepo = SettingsRepository(context)
        // This is not ideal for preview as it still needs Application for the real ViewModel.
        // A more robust preview would mock the ViewModel interface if one existed.
        // For now, we can't easily instantiate SettingsViewModel without Application.
        // So, we'll preview with some default values.

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Silence Mode", style = MaterialTheme.typography.titleMedium)
                Switch(checked = true, onCheckedChange = {})
            }
            Divider()
            SettingTimeRow(label = "Start Time", time = "22:00", onClick = {}, enabled = true)
            SettingTimeRow(label = "End Time", time = "07:00", onClick = {}, enabled = true)
        }
    }
}
