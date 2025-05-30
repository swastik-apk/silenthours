package com.example.silenthours.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.util.Log
import com.example.silenthours.data.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {

    private val TAG = "CallStateReceiver"
    // Scope for background tasks initiated by the receiver
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d(TAG, "Phone state changed: $state")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d(TAG, "Incoming call from: $incomingNumber")

                if (incomingNumber != null) {
                    val serviceIntent = Intent(context, CallHandlerService::class.java)
                    serviceIntent.putExtra(CallHandlerService.EXTRA_INCOMING_NUMBER, incomingNumber)
                    context.startService(serviceIntent)
                } else {
                    Log.w(TAG, "Incoming number is null for RINGING state.")
                }
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE || state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                // Call ended (IDLE) or answered (OFFHOOK)
                // We restore ringer mode in either case if it was changed by our app.
                Log.d(TAG, "Call is now IDLE or OFFHOOK. Attempting to restore ringer mode.")
                val settingsRepository = SettingsRepository(context.applicationContext)

                receiverScope.launch {
                    val originalMode = settingsRepository.getOriginalRingerMode()
                    if (originalMode != null) {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        try {
                            if (audioManager.ringerMode != originalMode) {
                                audioManager.ringerMode = originalMode
                                Log.i(TAG, "Ringer mode restored to: $originalMode")
                            } else {
                                Log.d(TAG, "Ringer mode is already $originalMode. No change needed.")
                            }
                            settingsRepository.clearOriginalRingerMode()
                            Log.d(TAG, "Cleared original ringer mode from SharedPreferences.")
                        } catch (e: SecurityException) {
                            Log.e(TAG, "SecurityException while restoring ringer mode.", e)
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception while restoring ringer mode.", e)
                        }
                    } else {
                        Log.d(TAG, "No original ringer mode found in SharedPreferences to restore.")
                    }
                }
            }
        }
    }
}
