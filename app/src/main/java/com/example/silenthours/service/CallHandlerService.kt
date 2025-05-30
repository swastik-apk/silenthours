package com.example.silenthours.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.util.Log
import com.example.silenthours.data.db.AppDatabase
import com.example.silenthours.data.preferences.SettingsRepository
import com.example.silenthours.model.Group
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

class CallHandlerService : Service() {

    private val TAG = "CallHandlerService"
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob) // IO for DB/network

    companion object {
        const val EXTRA_INCOMING_NUMBER = "incoming_number"
        // Removed static originalRingerMode, will use SharedPreferences
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CallHandlerService started.")
        val incomingNumber = intent?.getStringExtra(EXTRA_INCOMING_NUMBER)

        if (incomingNumber == null) {
            Log.e(TAG, "No incoming number provided to service.")
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            handleCall(applicationContext, incomingNumber)
        }

        return START_NOT_STICKY
    }

    private suspend fun handleCall(context: Context, incomingNumber: String) {
        val settingsRepository = SettingsRepository(context)
        val appDatabase = AppDatabase.getDatabase(context)
        val groupDao = appDatabase.groupDao()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val isSilenceEnabled = settingsRepository.isSilenceModeEnabled().first()
        val startTimeStr = settingsRepository.getStartTime().first()
        val endTimeStr = settingsRepository.getEndTime().first()

        Log.d(TAG, "Silence Mode Enabled: $isSilenceEnabled, Start: $startTimeStr, End: $endTimeStr")

        if (!isSilenceEnabled) {
            Log.d(TAG, "Silence mode is not enabled. Ignoring call.")
            stopSelf()
            return
        }

        if (!isCurrentTimeInSilenceRange(startTimeStr, endTimeStr)) {
            Log.d(TAG, "Current time is outside silence range. Ignoring call.")
            stopSelf()
            return
        }

        val contactId = getContactIdFromNumber(context, incomingNumber)
        if (contactId == null) {
            Log.d(TAG, "Incoming number $incomingNumber not found in contacts. Ignoring call.")
            stopSelf()
            return
        }
        Log.d(TAG, "Contact ID for $incomingNumber is $contactId")

        val allGroups = groupDao.getAllGroupsSuspend()
        val isContactInAnyGroup = allGroups.any { group ->
            Log.d(TAG, "Checking group ${group.name} with contacts: ${group.contactIds}")
            group.contactIds.contains(contactId)
        }

        if (!isContactInAnyGroup) {
            Log.d(TAG, "Contact $contactId ($incomingNumber) is not in any allowed group. Ignoring call.")
            stopSelf()
            return
        }

        Log.i(TAG, "Silencing call from $incomingNumber (Contact ID: $contactId) as it's in a group and within time.")

        // Silence Ringer
        try {
            val currentRingerMode = audioManager.ringerMode
            // Only store if not already silent (to avoid overwriting if multiple calls or already in silent mode)
            // And only if we haven't stored one yet for *this app's* silencing action.
            // This check `settingsRepository.getOriginalRingerMode() == null` ensures we only store it once.
            if (currentRingerMode != AudioManager.RINGER_MODE_SILENT && settingsRepository.getOriginalRingerMode() == null) {
                settingsRepository.setOriginalRingerMode(currentRingerMode)
                Log.d(TAG, "Original ringer mode $currentRingerMode saved.")
            } else if (currentRingerMode == AudioManager.RINGER_MODE_SILENT) {
                Log.d(TAG, "Ringer mode is already silent. Not saving original mode again unless cleared.")
            } else {
                 Log.d(TAG, "Original ringer mode was already saved: ${settingsRepository.getOriginalRingerMode()}. Not overwriting.")
            }

            if (currentRingerMode != AudioManager.RINGER_MODE_SILENT) { // Avoid redundant set if already silent
                 audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                 Log.d(TAG, "Ringer mode set to SILENT.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while setting/getting ringer mode.", e)
            // Need DND access or other permissions potentially
        } catch (e: Exception) {
            Log.e(TAG, "Exception while setting ringer mode to silent.", e)
        }


        // Attempt to End Call (Requires API 28+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                @SuppressLint("MissingPermission") // Permission check is done at MainActivity
                val callEnded = telecomManager.endCall() // This is the correct method
                Log.d(TAG, "Attempted to end call via TelecomManager. Success: $callEnded")
                if (!callEnded) {
                     Log.w(TAG, "telecomManager.endCall() returned false. Call might not have been active or another issue.")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while trying to end call. ANSWER_PHONE_CALLS might not be granted or not sufficient for this device/OS version.", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while trying to end call.", e)
            }
        } else {
            Log.w(TAG, "Cannot attempt to end call, API level ${android.os.Build.VERSION.SDK_INT} is below 28.")
        }

        // For now, the service stops itself after processing one call.
        // Ringer mode restoration would need to be handled, e.g., when call ends or after a timeout.
        // This is a simplified first pass.
        stopSelf()
    }

    private fun isCurrentTimeInSilenceRange(startTimeStr: String, endTimeStr: String): Boolean {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            val startParts = startTimeStr.split(":").map { it.toInt() }
            val startHour = startParts[0]
            val startMinute = startParts[1]
            val startTimeInMinutes = startHour * 60 + startMinute

            val endParts = endTimeStr.split(":").map { it.toInt() }
            val endHour = endParts[0]
            val endMinute = endParts[1]
            var endTimeInMinutes = endHour * 60 + endMinute

            Log.d(TAG, "Current: $currentTimeInMinutes, Start: $startTimeInMinutes, End: $endTimeInMinutes")

            // Handles overnight range (e.g., 22:00 - 07:00)
            if (endTimeInMinutes < startTimeInMinutes) {
                // If current time is after start OR before end (on the next day)
                return currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutes
            } else {
                // If current time is after start AND before end (on the same day)
                return currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time strings: $startTimeStr, $endTimeStr", e)
            return false // Default to not in range if parsing fails
        }
    }

    @SuppressLint("Range")
    private fun getContactIdFromNumber(context: Context, phoneNumber: String): String? {
        var contactId: String? = null
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(phoneNumber)
                .build()
            val projection = arrayOf(ContactsContract.PhoneLookup._ID) // Contact ID

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact ID for number: $phoneNumber", e)
        }
        return contactId
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "CallHandlerService destroyed.")
        // Ringer mode restoration is now handled by SettingsViewModel or CallStateReceiver
    }
}
