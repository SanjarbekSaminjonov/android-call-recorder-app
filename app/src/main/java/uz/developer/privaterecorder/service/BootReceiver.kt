package uz.developer.privaterecorder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.i("SecureRecorder", "[BootReceiver] System boot or package update event received ($action).")
            
            val prefs = context.getSharedPreferences("secure_recorder_prefs", Context.MODE_PRIVATE)
            val monitoringEnabled = prefs.getBoolean("monitoring_enabled", true)
            if (!monitoringEnabled) {
                Log.i("SecureRecorder", "[BootReceiver] Monitoring is disabled in user preferences. Skipping start.")
                return
            }

            val hasPhoneState = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            val hasRecordAudio = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            
            if (hasPhoneState && hasRecordAudio) {
                try {
                    val serviceIntent = Intent(context, RecorderControllerService::class.java).apply {
                        this.action = RecorderControllerService.ACTION_START_MONITORING
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.i("SecureRecorder", "[BootReceiver] Successfully started RecorderControllerService on boot.")
                } catch (e: Exception) {
                    Log.e("SecureRecorder", "[BootReceiver] Failed to start service on boot: ${e.message}", e)
                }
            } else {
                Log.w("SecureRecorder", "[BootReceiver] Permissions missing. Skipping service auto-start.")
            }
        }
    }
}
