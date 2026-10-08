package uz.developer.privaterecorder.shell

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass
import uz.developer.privaterecorder.IRecorderService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class ShellRecorderUserService : IRecorderService.Stub() {

    companion object {
        private const val TAG = "PrivateRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val isRecordingActive = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recordingJob: Job? = null
    private var currentAudioRecord: AudioRecord? = null
    private var shellContext: WrappedShellContext? = null

    init {
        try {
            HiddenApiBypass.addHiddenApiExemptions("L")
            shellContext = WrappedShellContext.create()
            Log.i(TAG, "ShellRecorderUserService started under UID: ${Process.myUid()}, PID: ${Process.myPid()}")
        } catch (e: Throwable) {
            Log.e(TAG, "ShellRecorderUserService initialization error: ${e.message}", e)
        }
    }

    override fun startRecording(outputFilePath: String) {
        if (isRecordingActive.get()) {
            Log.w(TAG, "startRecording requested, but recording is already in progress.")
            return
        }

        Log.i(TAG, "Starting recording. Output path: $outputFilePath")

        val targetFile = File(outputFilePath)
        val parentDir = targetFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            val created = parentDir.mkdirs()
            Log.d(TAG, "Target directory created: $created ($parentDir)")
        }

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = if (minBufSize > 0) minBufSize * 2 else 4096

        val audioRecord = createAudioRecordWithFallback(bufferSize)
        if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Failed to initialize AudioRecord with any audio source.")
            audioRecord?.release()
            return
        }

        currentAudioRecord = audioRecord
        isRecordingActive.set(true)

        recordingJob = serviceScope.launch {
            try {
                audioRecord.startRecording()
                Log.i(TAG, "AudioRecord started successfully. Commencing PCM stream to $outputFilePath")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AudioRecord: ${e.message}", e)
                try { audioRecord.release() } catch (_: Exception) {}
                currentAudioRecord = null
                isRecordingActive.set(false)
                return@launch
            }

            val buffer = ByteArray(bufferSize)
            var fileOutputStream: FileOutputStream? = null

            try {
                fileOutputStream = FileOutputStream(targetFile)
                Log.i(TAG, "Recording loop started. Writing PCM data...")

                while (isActive && isRecordingActive.get()) {
                    val readBytes = audioRecord.read(buffer, 0, buffer.size)
                    if (readBytes > 0) {
                        fileOutputStream.write(buffer, 0, readBytes)
                    } else if (readBytes < 0) {
                        Log.e(TAG, "AudioRecord read error encountered: $readBytes")
                        break
                    }
                }

                fileOutputStream.flush()
                Log.i(TAG, "Recording stream flushed. Total size: ${targetFile.length()} bytes")
            } catch (e: IOException) {
                Log.e(TAG, "IOException while writing audio stream: ${e.message}", e)
            } finally {
                try {
                    fileOutputStream?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to close FileOutputStream: ${e.message}")
                }
            }
        }
    }

    override fun stopRecording() {
        if (!isRecordingActive.getAndSet(false)) {
            Log.w(TAG, "stopRecording called, but service is not actively recording.")
            return
        }

        Log.i(TAG, "Stopping active recording.")

        // First stop AudioRecord to unblock any waiting native read() calls immediately
        try {
            currentAudioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AudioRecord release: ${e.message}", e)
        } finally {
            currentAudioRecord = null
        }

        recordingJob?.cancel()
        recordingJob = null
    }

    override fun isRecording(): Boolean {
        return isRecordingActive.get()
    }

    private fun createAudioRecordWithFallback(bufferSize: Int): AudioRecord? {
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_CALL,          // 4: Direct Modem Uplink + Downlink (when privileged)
            MediaRecorder.AudioSource.VOICE_RECOGNITION,   // 6: Direct speech tuning, avoids modem HAL AEC mute
            MediaRecorder.AudioSource.MIC,                  // 1: Standard Microphone
            MediaRecorder.AudioSource.VOICE_COMMUNICATION  // 7: VoIP / Hardware Acoustic Echo Cancellation
        )

        for (source in sources) {
            Log.i(TAG, "Evaluating AudioRecord creation for source: $source")
            val record = tryCreateAudioRecord(source, bufferSize)
            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                Log.i(TAG, "Successfully initialized AudioRecord with source: $source")
                return record
            } else {
                record?.release()
                Log.w(TAG, "AudioSource $source failed initialization, falling back to next source...")
            }
        }

        return null
    }

    @SuppressLint("PrivateApi")
    private fun tryCreateAudioRecord(source: Int, bufferSize: Int): AudioRecord? {
        // Method 1: Using Android 12+ hidden constructor with WrappedShellContext
        if (shellContext != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val builder = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)

                try {
                    val method = builder.javaClass.getDeclaredMethod("setInternalCapturePreset", Int::class.javaPrimitiveType)
                    method.isAccessible = true
                    method.invoke(builder, source)
                } catch (_: Throwable) {}

                val audioAttributes = builder.build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()

                val constructors = AudioRecord::class.java.declaredConstructors
                for (ctor in constructors) {
                    val paramTypes = ctor.parameterTypes
                    val hasContext = paramTypes.any { it == android.content.Context::class.java }
                    val hasAttribution = paramTypes.any { it.name.contains("AttributionSource") }
                    val hasAttributes = paramTypes.isNotEmpty() && paramTypes[0] == AudioAttributes::class.java

                    if (hasAttributes && (hasContext || hasAttribution)) {
                        try {
                            ctor.isAccessible = true
                            val args = arrayOfNulls<Any>(paramTypes.size)
                            for (i in paramTypes.indices) {
                                val pType = paramTypes[i]
                                when {
                                    pType == AudioAttributes::class.java -> args[i] = audioAttributes
                                    pType == AudioFormat::class.java -> args[i] = audioFormat
                                    pType == android.content.Context::class.java -> args[i] = shellContext
                                    pType.name.contains("AttributionSource") -> args[i] = shellContext?.attributionSource
                                    i == 2 && (pType == Int::class.javaPrimitiveType || pType == java.lang.Integer::class.java) -> args[i] = bufferSize
                                    pType == Int::class.javaPrimitiveType -> args[i] = 0
                                    pType == Long::class.javaPrimitiveType -> args[i] = 0L
                                    pType == Boolean::class.javaPrimitiveType -> args[i] = false
                                    pType == Float::class.javaPrimitiveType -> args[i] = 0f
                                    pType == Double::class.javaPrimitiveType -> args[i] = 0.0
                                    pType == String::class.java -> args[i] = WrappedShellContext.SHELL_PACKAGE_NAME
                                    else -> args[i] = null
                                }
                            }
                            Log.i(TAG, "Evaluating constructor with ${paramTypes.size} params for source $source")
                            val record = ctor.newInstance(*args) as AudioRecord
                            if (record.state == AudioRecord.STATE_INITIALIZED) {
                                Log.i(TAG, "Successfully initialized AudioRecord via reflection (${paramTypes.size} params) for source $source")
                                return record
                            } else {
                                record.release()
                            }
                        } catch (ctorEx: Throwable) {
                            Log.w(TAG, "Constructor (${paramTypes.size} params) failed: ${ctorEx.message}")
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Hidden constructor strategy failed for source $source: ${t.message}")
            }
        }

        // Method 2: AudioRecord.Builder
        try {
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build()

            val record = AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
        } catch (t: Throwable) {
            Log.w(TAG, "AudioRecord.Builder failed for source $source: ${t.message}")
        }

        // Method 3: Standard legacy AudioRecord constructor
        try {
            val record = AudioRecord(
                source,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Legacy AudioRecord constructor failed for source $source: ${t.message}")
        }

        return null
    }

    fun destroy() {
        Log.i(TAG, "ShellRecorderUserService destroy requested. Cleaning up process resources.")
        stopRecording()
        serviceScope.cancel()
        kotlin.system.exitProcess(0)
    }
}
