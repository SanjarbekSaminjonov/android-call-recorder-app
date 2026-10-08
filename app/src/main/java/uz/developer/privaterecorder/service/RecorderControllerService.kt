package uz.developer.privaterecorder.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import uz.developer.privaterecorder.IRecorderService
import uz.developer.privaterecorder.shell.ShellRecorderUserService
import uz.developer.privaterecorder.ui.MainActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecorderControllerService : Service() {

    companion object {
        const val TAG = "SecureRecorder"
        const val ACTION_START_MONITORING = "uz.developer.privaterecorder.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "uz.developer.privaterecorder.action.STOP_MONITORING"
        const val ACTION_RECORDING_COMPLETED = "uz.developer.privaterecorder.action.RECORDING_COMPLETED"

        // Two distinct notification channels to allow silencing standby without affecting active call alerts
        private const val CHANNEL_STANDBY = "channel_standby_monitoring"
        private const val CHANNEL_RECORDING = "channel_active_recording"

        // Two separate notification IDs
        private const val NOTIFICATION_ID_STANDBY = 2001
        private const val NOTIFICATION_ID_RECORDING = 2002

        @Volatile
        var isServiceRunning: Boolean = false
            private set
    }

    /**
     * Fallback recorder: captures audio via standard microphone without needing Shizuku.
     * Accurately handles Bluetooth SCO (Galaxy Buds), Wired headsets, and USB headsets.
     */
    private class NativeMicRecorder(
        private val context: Context,
        private val audioManager: AudioManager?
    ) {
        private var audioRecord: AudioRecord? = null
        private var recordingThread: Thread? = null
        @Volatile private var isRecording = false
        private var deviceChangeListener: Any? = null

        private fun resolveInputDevice(target: AudioDeviceInfo?): AudioDeviceInfo? {
            if (target == null || audioManager == null) return null
            if (target.isSource) return target
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                return inputs.firstOrNull { it.type == target.type }
                    ?: inputs.firstOrNull { it.productName == target.productName && it.isSource }
                    ?: inputs.firstOrNull { it.isSource && (
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                    ) }
            }
            return null
        }

        @SuppressLint("MissingPermission")
        fun start(outputFilePath: String) {
            stop()
            isRecording = true
            recordingThread = Thread({
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else 4096

                // VOICE_COMMUNICATION is designed to link with the active telephone communication audio route
                var record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "[NativeMic] VOICE_COMMUNICATION failed, falling back to MIC...")
                    record.release()
                    record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                }

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "[NativeMic] AudioRecord initialization failed completely.")
                    record.release()
                    return@Thread
                }

                // Dynamic headset routing: Bind to active communication device (Bluetooth, Wired, USB)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val commDevice = audioManager?.communicationDevice
                        val inputDevice = resolveInputDevice(commDevice)
                        if (inputDevice != null) {
                            val ok = record.setPreferredDevice(inputDevice)
                            Log.i(TAG, "[NativeMic] Attached preferred input device: ${inputDevice.productName} (type=${inputDevice.type}, success=$ok)")
                        }

                        // Listen for headset plug/unplug or Bluetooth connect/disconnect mid-call
                        val listener = AudioManager.OnCommunicationDeviceChangedListener { newDevice ->
                            if (newDevice != null && isRecording) {
                                val newInput = resolveInputDevice(newDevice)
                                if (newInput != null) {
                                    val ok = record.setPreferredDevice(newInput)
                                    Log.i(TAG, "[NativeMic] Communication device changed mid-call: ${newInput.productName} (success=$ok)")
                                }
                            }
                        }
                        audioManager?.addOnCommunicationDeviceChangedListener(
                            ContextCompat.getMainExecutor(context),
                            listener
                        )
                        deviceChangeListener = listener
                    } catch (e: Exception) {
                        Log.e(TAG, "[NativeMic] Error configuring communication device listener: ${e.message}")
                    }
                }

                audioRecord = record

                try {
                    record.startRecording()
                    Log.i(TAG, "[NativeMic] Started native mic recording to: $outputFilePath")
                    val file = File(outputFilePath)
                    file.parentFile?.mkdirs()
                    val fos = FileOutputStream(file)
                    val buffer = ByteArray(bufferSize)

                    while (isRecording) {
                        val read = record.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            fos.write(buffer, 0, read)
                        }
                    }
                    fos.flush()
                    fos.close()
                    Log.i(TAG, "[NativeMic] Stopped recording. Output: ${file.length()} bytes")
                } catch (e: Exception) {
                    Log.e(TAG, "[NativeMic] Error during recording: ${e.message}", e)
                } finally {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            (deviceChangeListener as? AudioManager.OnCommunicationDeviceChangedListener)?.let {
                                audioManager?.removeOnCommunicationDeviceChangedListener(it)
                            }
                        } catch (_: Exception) {}
                        deviceChangeListener = null
                    }
                    try {
                        record.stop()
                        record.release()
                    } catch (_: Exception) {}
                    audioRecord = null
                }
            }, "NativeMicThread").apply { start() }
        }

        fun stop() {
            isRecording = false
            try {
                audioRecord?.let {
                    if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        it.stop()
                    }
                }
            } catch (_: Exception) {}
            try {
                recordingThread?.interrupt()
                recordingThread?.join(500)
            } catch (_: Exception) {}
            recordingThread = null
        }

        fun isRecording(): Boolean = isRecording
    }

    private lateinit var nativeMicRecorder: NativeMicRecorder
    private var isRecordingSessionActive = false
    private var isBoundToShizuku = false
    private var recorderService: IRecorderService? = null

    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: Any? = null
    private var legacyPhoneStateListener: PhoneStateListener? = null
    private var audioManager: AudioManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(this, ShellRecorderUserService::class.java))
            .daemon(false)
            .processNameSuffix("recorder_shell")
            .debuggable(false)
            .version(1)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "[Controller] Connected to Shizuku ShellRecorderUserService successfully.")
            recorderService = IRecorderService.Stub.asInterface(service)
            isBoundToShizuku = true

            // If call was already in progress before connection completed
            if (isRecordingSessionActive) {
                beginRecordingSession()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "[Controller] Disconnected from Shizuku ShellRecorderUserService.")
            recorderService = null
            isBoundToShizuku = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "[Controller] RecorderControllerService onCreate. Initializing background monitoring...")
        isServiceRunning = true

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        nativeMicRecorder = NativeMicRecorder(this, audioManager)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SecureRecorder:RecordingWakeLock")?.apply {
            setReferenceCounted(false)
        }

        try {
            setupNotificationChannels()
            startForegroundWithNotification("Standby: Monitoring calls...")
            Log.i(TAG, "[Controller] Foreground Service started in standby mode.")
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Failed to startForeground: ${e.message}", e)
        }

        // Recover any unfinalized recordings from previous sudden reboots or crashes
        Thread({
            uz.developer.privaterecorder.util.SecureAudioVault.recoverOrphanRecordings(this)
        }, "VaultRecoveryThread").start()

        registerTelephonyStateListener()
        bindShizukuUserService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_MONITORING
        Log.i(TAG, "[Controller] onStartCommand received action: $action")

        if (action == ACTION_STOP_MONITORING) {
            Log.i(TAG, "[Controller] Stopping persistent monitoring by user request.")
            finishRecordingSession()
            dismissActiveRecordingNotification()
            unbindShizukuUserService()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun registerTelephonyStateListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state)
                    }
                }
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
                Log.i(TAG, "[Controller] TelephonyCallback registered for API 31+.")
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
                Log.i(TAG, "[Controller] Legacy PhoneStateListener registered.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Failed to register telephony listener: ${e.message}", e)
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.i(TAG, "[Controller] CALL_STATE_OFFHOOK detected! Call connected. Activating recording...")
                isRecordingSessionActive = true

                // Promote foreground service type and update notification
                startForegroundWithNotification("Recording call in progress...", isRecording = true)
                showActiveRecordingNotification("Recording active phone call in progress...")

                beginRecordingSession()
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.i(TAG, "[Controller] CALL_STATE_IDLE detected! Call terminated.")
                if (isRecordingSessionActive) {
                    finishRecordingSession()
                }

                // Immediately dismiss the active call alert and return to silent standby
                dismissActiveRecordingNotification()
                startForegroundWithNotification("Standby: Monitoring calls...", isRecording = false)
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.i(TAG, "[Controller] CALL_STATE_RINGING detected. Incoming call ringing...")
            }
        }
    }

    private var currentRecordingFile: File? = null

    private fun beginRecordingSession() {
        try {
            // Guard Samsung One UI from killing CPU or throttling audio buffers during screen-off / ear proximity
            try {
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(2 * 3600 * 1000L) // 2-hour max safety timeout
                    Log.i(TAG, "[Controller] Acquired PARTIAL_WAKE_LOCK (One UI Deep Sleep guard).")
                }
            } catch (wlEx: Exception) {
                Log.w(TAG, "[Controller] WakeLock acquire warning: ${wlEx.message}")
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "REC_${timestamp}.pcm"

            val tempDir = uz.developer.privaterecorder.util.SecureAudioVault.getTempDir(this)
            val targetFile = File(tempDir, fileName)
            currentRecordingFile = targetFile

            if (isBoundToShizuku && recorderService != null) {
                Log.i(TAG, "[Controller] Enhanced Mode: Using Shizuku Shell UserService (dual-side): ${targetFile.absolutePath}")
                recorderService?.startRecording(targetFile.absolutePath)
            } else {
                Log.i(TAG, "[Controller] Standard Mode: Using native mic fallback (headset-aware): ${targetFile.absolutePath}")
                nativeMicRecorder.start(targetFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Error starting recording session: ${e.message}", e)
        }
    }

    private fun finishRecordingSession() {
        isRecordingSessionActive = false
        val fileToRename = currentRecordingFile
        currentRecordingFile = null

        // Release WakeLock immediately to preserve battery
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "[Controller] Released PARTIAL_WAKE_LOCK.")
            }
        } catch (wlEx: Exception) {
            Log.w(TAG, "[Controller] WakeLock release warning: ${wlEx.message}")
        }

        // Stop native mic fallback if active
        if (nativeMicRecorder.isRecording()) {
            nativeMicRecorder.stop()
        }

        // Stop Shizuku recording if active
        try {
            recorderService?.let {
                if (it.isRecording) {
                    Log.i(TAG, "[Controller] Invoking stopRecording on Shizuku UserService...")
                    it.stopRecording()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Error invoking stopRecording: ${e.message}", e)
        } finally {
            vaultFileWithContactInfo(fileToRename)

            try {
                val broadcastIntent = Intent(ACTION_RECORDING_COMPLETED).setPackage(packageName)
                sendBroadcast(broadcastIntent)
                Log.i(TAG, "[Controller] Broadcast ACTION_RECORDING_COMPLETED dispatched.")
            } catch (ex: Exception) {
                Log.e(TAG, "[Controller] Failed to dispatch completed broadcast: ${ex.message}")
            }
        }
    }

    private fun vaultFileWithContactInfo(originalFile: File?) {
        if (originalFile == null || !originalFile.exists() || originalFile.length() == 0L) {
            return
        }

        // If call was aborted/busy in less than 1 second (< 32KB of 16kHz audio), discard junk file
        if (originalFile.length() < 32000L) {
            Log.i(TAG, "[Controller] Discarding short/aborted recording (${originalFile.length()} bytes < 1 sec).")
            originalFile.delete()
            return
        }

        try {
            // Small pause (400ms) to ensure Telecom finished writing latest CallLog row
            Thread.sleep(400)

            val (number, cachedName) = getLatestCallInfo()
            val contactName = if (!cachedName.isNullOrBlank()) {
                cachedName
            } else if (!number.isNullOrBlank()) {
                lookupContactName(number)
            } else {
                null
            }

            val baseName = originalFile.nameWithoutExtension

            val newBaseName = when {
                !contactName.isNullOrBlank() && !number.isNullOrBlank() -> {
                    val cleanName = contactName.replace("[^a-zA-Z0-9а-яА-ЯёЁ_\\- ]".toRegex(), "").trim().replace(" ", "_")
                    val cleanNumber = number.replace("[^0-9+]".toRegex(), "")
                    "${baseName}_${cleanName}_${cleanNumber}"
                }
                !number.isNullOrBlank() -> {
                    val cleanNumber = number.replace("[^0-9+]".toRegex(), "")
                    "${baseName}_${cleanNumber}"
                }
                !contactName.isNullOrBlank() -> {
                    val cleanName = contactName.replace("[^a-zA-Z0-9а-яА-ЯёЁ_\\- ]".toRegex(), "").trim().replace(" ", "_")
                    "${baseName}_${cleanName}"
                }
                else -> baseName
            }

            val securedFile = uz.developer.privaterecorder.util.SecureAudioVault.secureAndVaultFile(this, originalFile, newBaseName)
            Log.i(TAG, "[Controller] Vaulted recording: ${securedFile?.absolutePath}")
            uz.developer.privaterecorder.util.SecureAudioVault.performAutoRetentionCleanup(this)
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Error vaulting file with contact info: ${e.message}", e)
        }
    }

    private fun getLatestCallInfo(): Pair<String?, String?> {
        try {
            val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME)
            val sortOrder = "${CallLog.Calls.DATE} DESC"
            contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val number = if (numberIdx != -1) cursor.getString(numberIdx) else null
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    Log.i(TAG, "[Controller] Latest CallLog resolved: number=$number, name=$name")
                    return Pair(number, name)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] CallLog query error: ${e.message}", e)
        }
        return Pair(null, null)
    }

    private fun lookupContactName(phoneNumber: String): String? {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx != -1) {
                        val name = cursor.getString(idx)
                        Log.i(TAG, "[Controller] ContactsContract resolved name for $phoneNumber: $name")
                        return name
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] ContactsContract lookup error: ${e.message}", e)
        }
        return null
    }

    private fun bindShizukuUserService() {
        if (isBoundToShizuku) return
        try {
            if (Shizuku.pingBinder()) {
                Log.i(TAG, "[Controller] Shizuku binder ping OK. Binding UserService...")
                Shizuku.bindUserService(userServiceArgs, serviceConnection)
            } else {
                Log.w(TAG, "[Controller] Shizuku binder ping failed. Running in native mic mode.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Exception binding Shizuku UserService: ${e.message}", e)
        }
    }

    private fun unbindShizukuUserService() {
        if (!isBoundToShizuku) return
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            isBoundToShizuku = false
            recorderService = null
            Log.i(TAG, "[Controller] Successfully unbound Shizuku UserService.")
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Exception unbinding Shizuku UserService: ${e.message}", e)
        }
    }

    /**
     * Sets up two clean notification channels and cleans up any legacy channels.
     */
    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Delete legacy / orphan channels from previous builds
            try {
                manager.deleteNotificationChannel("private_call_recorder_channel")
                manager.deleteNotificationChannel("channel_call_recorder")
            } catch (_: Exception) {}

            // Channel 1: Standby Service (Background)
            // Users can silence or hide this category in settings without stopping the background FGS
            val standbyChannel = NotificationChannel(
                CHANNEL_STANDBY,
                "Standby Service (Background)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps background monitor alive against Samsung battery freezer. You can turn this category OFF."
                setShowBadge(false)
            }
            manager.createNotificationChannel(standbyChannel)

            // Channel 2: Active Call Recording (Alert)
            // Prominent alert channel: pops up ONLY during an active phone call
            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING,
                "Active Call Recording (Alert)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Visible notification shown exclusively while an active call is being recorded."
                setShowBadge(true)
            }
            manager.createNotificationChannel(recordingChannel)
        }
    }

    private fun buildStandbyNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_STANDBY)
            .setContentTitle("Secure Call Recorder")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundWithNotification(text: String, isRecording: Boolean = false) {
        val notification = buildStandbyNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val fgsType = if (isRecording) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            try {
                startForeground(NOTIFICATION_ID_STANDBY, notification, fgsType)
            } catch (e: Exception) {
                Log.w(TAG, "[Controller] FGS type ($fgsType) start failed, falling back to SPECIAL_USE: ${e.message}")
                try {
                    startForeground(NOTIFICATION_ID_STANDBY, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (ex: Exception) {
                    Log.e(TAG, "[Controller] Failed startForeground fallback: ${ex.message}", ex)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID_STANDBY, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID_STANDBY, notification)
        }
    }

    private fun updateStandbyNotification(text: String) {
        try {
            val notification = buildStandbyNotification(text)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID_STANDBY, notification)
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Error updating standby notification: ${e.message}", e)
        }
    }

    /**
     * Shows an active recording notification on the dedicated alert channel.
     * Guaranteed to appear even if the user silenced the standby category.
     */
    private fun showActiveRecordingNotification(text: String) {
        try {
            val launchIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val activeNotif = NotificationCompat.Builder(this, CHANNEL_RECORDING)
                .setContentTitle("Recording Call")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID_RECORDING, activeNotif)
            Log.i(TAG, "[Controller] Posted active recording notification on alert channel.")
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Error showing active recording alert: ${e.message}", e)
        }
    }

    private fun dismissActiveRecordingNotification() {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.cancel(NOTIFICATION_ID_RECORDING)
            Log.i(TAG, "[Controller] Dismissed active recording notification.")
        } catch (e: Exception) {
            Log.e(TAG, "[Controller] Error dismissing active recording alert: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "[Controller] RecorderControllerService onDestroy.")
        isServiceRunning = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            legacyPhoneStateListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }

        finishRecordingSession()
        dismissActiveRecordingNotification()
        unbindShizukuUserService()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
