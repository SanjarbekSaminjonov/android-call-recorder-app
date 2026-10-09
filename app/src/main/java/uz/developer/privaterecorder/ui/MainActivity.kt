package uz.developer.privaterecorder.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import uz.developer.privaterecorder.service.RecorderControllerService
import uz.developer.privaterecorder.util.SecureAudioVault
import java.io.File
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class RecordingSortOption(val label: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    DURATION_DESC("Longest first"),
    DURATION_ASC("Shortest first"),
    SIZE_DESC("Largest size"),
    SIZE_ASC("Smallest size")
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    UNKNOWN
}

data class RecordingItem(
    val file: File,
    val name: String,
    val title: String,
    val subtitle: String,
    val dateFormatted: String,
    val durationMs: Long,
    val durationFormatted: String,
    val durationSeconds: Long,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val lastModified: Long,
    val direction: CallDirection = CallDirection.UNKNOWN,
    val isStarred: Boolean = false,
    val note: String? = null
)

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMinutes = minutes % 60
        String.format(Locale.US, "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

enum class AutoRetentionPolicy(val label: String, val days: Int) {
    NEVER("Keep forever (Never auto-delete)", 0),
    THIRTY_DAYS("Auto-delete older than 30 days", 30),
    SIXTY_DAYS("Auto-delete older than 60 days", 60),
    NINETY_DAYS("Auto-delete older than 90 days", 90),
    ONE_EIGHTY_DAYS("Auto-delete older than 180 days", 180)
}

enum class BulkCleanupType(val label: String, val daysOld: Int) {
    ONE_MONTH("Delete older than 30 days", 30),
    THREE_MONTHS("Delete older than 90 days", 90),
    ALL("Delete all recordings", 0)
}

enum class AppScreen(val title: String) {
    RECORDINGS("Secure Call Recorder"),
    SYSTEM_PERMISSIONS("System & Permissions"),
    STORAGE_CLEANUP("Storage Cleanup"),
    SECURITY("Security & App Lock"),
    DOCS("How It Works")
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF121418),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C22),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF262830),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF383A42)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006399),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF006A60),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFEDF0F7),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFD9DCE3)
)

class VaultAudioPlayer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    @Volatile private var seekRequestedPositionMs: Long? = null
    @Volatile private var currentSpeed: Float = 1.0f
    @Volatile private var isVolumeBoosted: Boolean = false

    var onProgressUpdate: ((positionMs: Long, durationMs: Long) -> Unit)? = null

    fun play(
        file: File,
        initialSpeed: Float = 1.0f,
        initialVolumeBoost: Boolean = false,
        onComplete: () -> Unit,
        scope: kotlinx.coroutines.CoroutineScope
    ) {
        stop()
        currentSpeed = initialSpeed
        isVolumeBoosted = initialVolumeBoost
        seekRequestedPositionMs = null

        val sampleRate = 16000
        val bytesPerMs = (sampleRate * 2) / 1000L // 32 bytes per ms
        val totalBytes = (file.length() - 64).coerceAtLeast(0)
        val totalDurationMs = if (bytesPerMs > 0) totalBytes / bytesPerMs else 0L

        playbackJob = scope.launch(Dispatchers.IO) {
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(if (minBufferSize > 0) minBufferSize * 2 else 4096)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    track.playbackParams = android.media.PlaybackParams().setSpeed(currentSpeed)
                } catch (_: Exception) {}
            }

            audioTrack = track
            track.setVolume(1.0f)
            track.play()

            val buffer = ByteArray(minBufferSize.coerceAtLeast(4096))
            var stream: InputStream? = null
            var currentByteOffset = 0L

            try {
                stream = SecureAudioVault.openVaultAudioStream(file)
                if (stream != null) {
                    var lastReportTime = 0L
                    while (isActive) {
                        val seekPos = seekRequestedPositionMs
                        if (seekPos != null) {
                            seekRequestedPositionMs = null
                            val targetByteOffset = (seekPos * bytesPerMs).coerceIn(0L, totalBytes)
                            try { stream?.close() } catch (_: Exception) {}
                            stream = SecureAudioVault.openVaultAudioStream(file)
                            stream?.let { s ->
                                s.skip(targetByteOffset)
                                currentByteOffset = targetByteOffset
                                try { track.flush() } catch (_: Exception) {}
                            }
                        }

                        val read = stream?.read(buffer) ?: -1
                        if (read <= 0) break

                        if (isVolumeBoosted) {
                            applyGain(buffer, read, 1.8f)
                        }

                        track.write(buffer, 0, read)
                        currentByteOffset += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime >= 100) {
                            lastReportTime = now
                            val posMs = currentByteOffset / bytesPerMs
                            withContext(Dispatchers.Main) {
                                onProgressUpdate?.invoke(posMs, totalDurationMs)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { stream?.close() } catch (_: Exception) {}
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
                audioTrack = null
                withContext(Dispatchers.Main) {
                    onProgressUpdate?.invoke(totalDurationMs, totalDurationMs)
                    onComplete()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        seekRequestedPositionMs = positionMs
    }

    fun skipBy(deltaMs: Long, currentPosMs: Long) {
        seekTo((currentPosMs + deltaMs).coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                audioTrack?.playbackParams = android.media.PlaybackParams().setSpeed(speed)
            } catch (_: Exception) {}
        }
    }

    fun setVolumeBoost(boost: Boolean) {
        isVolumeBoosted = boost
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun applyGain(buffer: ByteArray, length: Int, multiplier: Float) {
        for (i in 0 until length - 1 step 2) {
            var sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            if (sample > 32767) sample -= 65536
            var amplified = (sample * multiplier).toInt()
            if (amplified > 32767) amplified = 32767
            if (amplified < -32768) amplified = -32768
            buffer[i] = (amplified and 0xFF).toByte()
            buffer[i + 1] = ((amplified shr 8) and 0xFF).toByte()
        }
    }
}

class MainActivity : FragmentActivity() {

    companion object {
        const val TAG = "SecureRecorder"
        const val PREFS_NAME = "secure_recorder_prefs"
        const val KEY_THEME_DARK = "theme_is_dark"
        const val KEY_APP_LOCK = "app_lock_enabled"
        const val KEY_ONBOARDING_DONE = "onboarding_completed"
        const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        const val KEY_FAVORITES = "favorite_recordings"
        const val KEY_SORT_OPTION = "sort_option"
        const val KEY_NOTE_PREFIX = "note_"
        const val KEY_AUTO_RETENTION = "auto_retention_days"
        const val SHIZUKU_PERMISSION_CODE = 3001
    }

    private val player = VaultAudioPlayer()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val crashPrefs = getSharedPreferences("secure_recorder_crash_log", Context.MODE_PRIVATE)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = Log.getStackTraceString(throwable)
            Log.e(TAG, "Uncaught Exception: $stackTrace", throwable)
            crashPrefs.edit().putString("last_crash_trace", stackTrace).commit()
            defaultHandler?.uncaughtException(thread, throwable)
        }
        val initialCrashTrace = crashPrefs.getString("last_crash_trace", null)

        // Ensure legacy recordings are migrated into private vault
        SecureAudioVault.migrateLegacyRecordings(this)

        setContent {
            var crashDialogText by remember { mutableStateOf(initialCrashTrace) }
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean(KEY_THEME_DARK, systemDark)) }

            val savedLock = prefs.getBoolean(KEY_APP_LOCK, false)
            var isAppLockEnabled by remember { mutableStateOf(savedLock) }
            var isAppUnlocked by remember { mutableStateOf(!savedLock) }

            MaterialTheme(colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (crashDialogText != null) {
                        AlertDialog(
                            onDismissRequest = {
                                crashDialogText = null
                                crashPrefs.edit().remove("last_crash_trace").apply()
                            },
                            title = { Text("Diagnostic Error Report") },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text("A previous error was caught:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(crashDialogText ?: "", fontSize = 11.sp)
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    crashDialogText = null
                                    crashPrefs.edit().remove("last_crash_trace").apply()
                                }) {
                                    Text("Dismiss")
                                }
                            }
                        )
                    }

                    if (isAppLockEnabled && !isAppUnlocked) {
                        AppLockScreen(
                            onUnlockRequested = {
                                authenticateWithBiometrics(
                                    onSuccess = { isAppUnlocked = true },
                                    onError = { msg ->
                                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    } else {
                        SecureRecorderAppRoot(
                            activity = this,
                            player = player,
                            isDarkMode = isDarkMode,
                            onToggleTheme = {
                                isDarkMode = !isDarkMode
                                prefs.edit().putBoolean(KEY_THEME_DARK, isDarkMode).apply()
                            },
                            isAppLockEnabled = isAppLockEnabled,
                            onAppLockChanged = { enabled ->
                                if (enabled) {
                                    authenticateWithBiometrics(
                                        onSuccess = {
                                            isAppLockEnabled = true
                                            prefs.edit().putBoolean(KEY_APP_LOCK, true).apply()
                                            Toast.makeText(this, "Biometric App Lock enabled", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = {
                                            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    isAppLockEnabled = false
                                    prefs.edit().putBoolean(KEY_APP_LOCK, false).apply()
                                    Toast.makeText(this, "App Lock disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            isFirstLaunch = !prefs.getBoolean(KEY_ONBOARDING_DONE, false),
                            onOnboardingDismissed = {
                                prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                            },
                            onRequestShizuku = { requestShizukuPermission() }
                        )
                    }
                }
            }
        }
    }

    fun authenticateWithBiometrics(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Call Recorder")
            .setSubtitle("Authenticate using fingerprint or device credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Shizuku is not running. Standard Mode (Mic) is active!", Toast.LENGTH_LONG).show()
                Log.i(TAG, "[UI] Shizuku pingBinder failed. Running in standard mic fallback.")
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku permission already granted", Toast.LENGTH_SHORT).show()
            } else {
                Log.i(TAG, "[UI] Requesting Shizuku permission...")
                Shizuku.requestPermission(SHIZUKU_PERMISSION_CODE)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Standard Mode is active.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "[UI] Shizuku request warning: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
    }
}

/**
 * 2 horizontal lines stylish navigation icon
 */
@Composable
fun TwoHorizontalLinesIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(x = 3.dp.toPx(), y = 8.dp.toPx()),
            end = Offset(x = 21.dp.toPx(), y = 8.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(x = 3.dp.toPx(), y = 16.dp.toPx()),
            end = Offset(x = 15.dp.toPx(), y = 16.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun AppLockScreen(onUnlockRequested: () -> Unit) {
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Secure Call Recorder",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Application is protected.\nAuthenticate to access your private recordings.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlockRequested,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.7f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock App", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureRecorderAppRoot(
    activity: MainActivity,
    player: VaultAudioPlayer,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    isAppLockEnabled: Boolean,
    onAppLockChanged: (Boolean) -> Unit,
    isFirstLaunch: Boolean,
    onOnboardingDismissed: () -> Unit,
    onRequestShizuku: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE) }

    val appVersionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentScreen by remember { mutableStateOf(AppScreen.RECORDINGS) }

    var isShizukuAvailable by remember { mutableStateOf(false) }
    var isShizukuGranted by remember { mutableStateOf(false) }
    var hasSystemPermissions by remember { mutableStateOf(false) }
    var hasContactsPermission by remember { mutableStateOf(false) }
    var isBatteryOptimizedIgnored by remember { mutableStateOf(false) }
    var isServiceRunning by remember { mutableStateOf(RecorderControllerService.isServiceRunning) }

    var recordings by remember { mutableStateOf<List<RecordingItem>>(emptyList()) }
    var currentlyPlayingPath by remember { mutableStateOf<String?>(null) }
    var playbackPositionMs by remember { mutableLongStateOf(0L) }
    var playbackDurationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isVolumeBoosted by remember { mutableStateOf(false) }
    var filterOnlyStarred by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val initialSortName = prefs.getString(MainActivity.KEY_SORT_OPTION, RecordingSortOption.DATE_DESC.name)
    var currentSortOption by remember {
        mutableStateOf(
            try {
                RecordingSortOption.valueOf(initialSortName ?: RecordingSortOption.DATE_DESC.name)
            } catch (_: Exception) {
                RecordingSortOption.DATE_DESC
            }
        )
    }

    var autoRetentionDays by remember {
        mutableIntStateOf(prefs.getInt(MainActivity.KEY_AUTO_RETENTION, 0))
    }

    // Search, Date Filter & Pagination
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var pageSize by remember { mutableIntStateOf(50) }

    // Dialog states
    var fileToDelete by remember { mutableStateOf<RecordingItem?>(null) }
    var bulkCleanupDialogType by remember { mutableStateOf<BulkCleanupType?>(null) }
    var itemForNoteDialog by remember { mutableStateOf<RecordingItem?>(null) }
    var noteInputText by remember { mutableStateOf("") }
    var showOnboardingDialog by remember { mutableStateOf(isFirstLaunch) }

    data class ParsedRecordingInfo(
        val title: String,
        val subtitle: String,
        val direction: CallDirection
    )

    fun parseRecordingInfo(fileName: String): ParsedRecordingInfo {
        val clean = fileName.removeSuffix(".pvr").removeSuffix(".pcm")
        val parts = clean.split("_")
        if (parts.size >= 4 && (parts[3].equals("IN", ignoreCase = true) || parts[3].equals("OUT", ignoreCase = true))) {
            val direction = if (parts[3].equals("IN", ignoreCase = true)) CallDirection.INCOMING else CallDirection.OUTGOING
            val remaining = parts.drop(4)
            return when {
                remaining.size >= 2 -> {
                    val number = remaining.last()
                    val name = remaining.subList(0, remaining.size - 1).joinToString(" ")
                    ParsedRecordingInfo(name, number, direction)
                }
                remaining.size == 1 -> {
                    val number = remaining[0]
                    ParsedRecordingInfo(number, "", direction)
                }
                else -> {
                    val defaultTitle = if (direction == CallDirection.INCOMING) "Incoming Call" else "Outgoing Call"
                    ParsedRecordingInfo(defaultTitle, "", direction)
                }
            }
        }

        return when {
            parts.size >= 5 -> {
                val number = parts.last()
                val name = parts.subList(3, parts.size - 1).joinToString(" ")
                ParsedRecordingInfo(name, number, CallDirection.UNKNOWN)
            }
            parts.size == 4 -> {
                val number = parts[3]
                ParsedRecordingInfo(number, "", CallDirection.UNKNOWN)
            }
            else -> {
                ParsedRecordingInfo(clean, "", CallDirection.UNKNOWN)
            }
        }
    }

    fun refreshRecordings() {
        try {
            SecureAudioVault.recoverOrphanRecordings(context)
            SecureAudioVault.performAutoRetentionCleanup(context)
        } catch (_: Exception) {}

        val vaultDir = SecureAudioVault.getVaultDir(context)
        val files = vaultDir.listFiles { file -> file.isFile && (file.name.endsWith(".pvr") || file.name.endsWith(".pcm")) } ?: emptyArray()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        val favoriteSet = prefs.getStringSet(MainActivity.KEY_FAVORITES, emptySet()) ?: emptySet()

        recordings = files.map { f ->
            val size = f.length()
            val formattedSize = if (size < 1024 * 1024) {
                "${size / 1024} KB"
            } else {
                val df = DecimalFormat("#.##")
                "${df.format(size.toDouble() / (1024 * 1024))} MB"
            }
            val parsed = parseRecordingInfo(f.name)
            val rawAudioBytes = if (f.name.endsWith(".pvr")) (size - 64).coerceAtLeast(0) else size
            val durationMs = rawAudioBytes / 32
            val durationFormatted = formatDuration(durationMs)
            val durationSeconds = (durationMs / 1000).coerceAtLeast(0)
            val note = prefs.getString("${MainActivity.KEY_NOTE_PREFIX}${f.name}", null)

            RecordingItem(
                file = f,
                name = f.name,
                title = parsed.title,
                subtitle = parsed.subtitle,
                dateFormatted = dateFormat.format(Date(f.lastModified())),
                durationMs = durationMs,
                durationFormatted = durationFormatted,
                durationSeconds = durationSeconds,
                sizeBytes = size,
                sizeFormatted = formattedSize,
                lastModified = f.lastModified(),
                direction = parsed.direction,
                isStarred = favoriteSet.contains(f.name),
                note = note
            )
        }
    }

    fun checkAllStatuses() {
        try {
            isShizukuAvailable = Shizuku.pingBinder()
            isShizukuGranted = isShizukuAvailable && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            isShizukuAvailable = false
            isShizukuGranted = false
        }

        val phoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val recordAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val readContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        hasContactsPermission = readContacts
        hasSystemPermissions = phoneState && recordAudio && postNotifications

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(PowerManager::class.java)
            isBatteryOptimizedIgnored = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            isBatteryOptimizedIgnored = true
        }

        val monitoringPref = prefs.getBoolean(MainActivity.KEY_MONITORING_ENABLED, true)
        if (hasSystemPermissions && monitoringPref && !RecorderControllerService.isServiceRunning) {
            try {
                val intent = Intent(context, RecorderControllerService::class.java).apply {
                    action = RecorderControllerService.ACTION_START_MONITORING
                }
                ContextCompat.startForegroundService(context, intent)
                isServiceRunning = true
            } catch (e: Throwable) {
                Log.e(MainActivity.TAG, "startForegroundService exception: ${e.message}", e)
                isServiceRunning = RecorderControllerService.isServiceRunning
            }
        } else {
            isServiceRunning = RecorderControllerService.isServiceRunning
        }
    }

    val systemPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        coroutineScope.launch {
            // Delay ensures Activity is fully focused on MIUI/HyperOS window manager before FGS triggers
            delay(300)
            checkAllStatuses()
        }
    }

    val contactsPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkAllStatuses()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAllStatuses()
                refreshRecordings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                refreshRecordings()
            }
        }
        val filter = IntentFilter(RecorderControllerService.ACTION_RECORDING_COMPLETED)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            isShizukuGranted = grantResult == PackageManager.PERMISSION_GRANTED
            checkAllStatuses()
        }
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (_: Exception) {}

        onDispose {
            try {
                Shizuku.removeRequestPermissionResultListener(listener)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        checkAllStatuses()
        refreshRecordings()
    }

    // Filtered recordings
    val filteredRecordings by remember {
        derivedStateOf {
            val selectedDateStr = selectedDateMillis?.let {
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(it))
            }

            val filtered = recordings.filter { item ->
                if (filterOnlyStarred && !item.isStarred) return@filter false

                val matchesDate = if (selectedDateStr != null) {
                    val itemDateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(item.lastModified))
                    itemDateStr == selectedDateStr
                } else true

                val query = searchQuery.trim().lowercase()
                val matchesQuery = query.isEmpty() ||
                        item.title.lowercase().contains(query) ||
                        item.subtitle.lowercase().contains(query) ||
                        item.name.lowercase().contains(query) ||
                        (item.direction == CallDirection.INCOMING && (query.contains("kiruvchi") || query == "in" || query.contains("incom"))) ||
                        (item.direction == CallDirection.OUTGOING && (query.contains("chiquvchi") || query == "out")) ||
                        (item.note?.lowercase()?.contains(query) == true)

                matchesDate && matchesQuery
            }

            when (currentSortOption) {
                RecordingSortOption.DATE_DESC -> filtered.sortedByDescending { it.lastModified }
                RecordingSortOption.DATE_ASC -> filtered.sortedBy { it.lastModified }
                RecordingSortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
                RecordingSortOption.DURATION_ASC -> filtered.sortedBy { it.durationMs }
                RecordingSortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
                RecordingSortOption.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
            }
        }
    }

    val displayedRecordings by remember {
        derivedStateOf {
            filteredRecordings.take(pageSize)
        }
    }

    // Calendar DatePickerDialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete single item confirmation dialog
    fileToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to permanently delete the recording for \"${target.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentlyPlayingPath == target.file.absolutePath) {
                            player.stop()
                            currentlyPlayingPath = null
                        }
                        target.file.delete()
                        fileToDelete = null
                        refreshRecordings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk cleanup confirmation dialog
    bulkCleanupDialogType?.let { cleanupType ->
        val now = System.currentTimeMillis()
        val filesToPurge = remember(cleanupType, recordings) {
            val eligible = if (cleanupType.daysOld == 0) {
                recordings
            } else {
                val threshold = now - (cleanupType.daysOld.toLong() * 24 * 3600 * 1000)
                recordings.filter { it.lastModified < threshold }
            }
            eligible.filter { !it.isStarred }
        }
        val protectedStarredCount = recordings.count { it.isStarred }

        AlertDialog(
            onDismissRequest = { bulkCleanupDialogType = null },
            title = { Text("Storage Cleanup") },
            text = {
                Column {
                    Text(
                        if (filesToPurge.isEmpty())
                            "No eligible recordings found matching this cleanup criteria."
                        else
                            "Warning: ${filesToPurge.size} audio files will be permanently deleted from secure storage. Proceed?"
                    )
                    if (protectedStarredCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "★ $protectedStarredCount starred recording(s) are protected and will NOT be deleted.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                if (filesToPurge.isNotEmpty()) {
                    Button(
                        onClick = {
                            player.stop()
                            currentlyPlayingPath = null
                            filesToPurge.forEach { it.file.delete() }
                            bulkCleanupDialogType = null
                            refreshRecordings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete (${filesToPurge.size})")
                    }
                } else {
                    Button(onClick = { bulkCleanupDialogType = null }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (filesToPurge.isNotEmpty()) {
                    TextButton(onClick = { bulkCleanupDialogType = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Edit/Add Note Dialog
    itemForNoteDialog?.let { target ->
        AlertDialog(
            onDismissRequest = { itemForNoteDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (target.note.isNullOrBlank()) "Add Recording Note" else "Edit Note",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Call with: ${target.title}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = noteInputText,
                        onValueChange = { noteInputText = it },
                        placeholder = { Text("e.g. Contract agreement, payment details...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = noteInputText.trim()
                    if (trimmed.isEmpty()) {
                        prefs.edit().remove("${MainActivity.KEY_NOTE_PREFIX}${target.name}").apply()
                    } else {
                        prefs.edit().putString("${MainActivity.KEY_NOTE_PREFIX}${target.name}", trimmed).apply()
                    }
                    itemForNoteDialog = null
                    refreshRecordings()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!target.note.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                prefs.edit().remove("${MainActivity.KEY_NOTE_PREFIX}${target.name}").apply()
                                itemForNoteDialog = null
                                refreshRecordings()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = { itemForNoteDialog = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Welcome Onboarding Dialog (First Launch)
    if (showOnboardingDialog) {
        AlertDialog(
            onDismissRequest = {
                showOnboardingDialog = false
                onOnboardingDismissed()
            },
            icon = {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Welcome to Secure Call Recorder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "100% Offline, sandboxed call recording built for Android 15–17 (API 35–37) and Samsung One UI.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    OnboardingPoint(
                        icon = Icons.Default.Mic,
                        title = "Dual Capture Modes",
                        desc = "Works out-of-the-box via standard mic, or elevates with Shizuku for crystal-clear dual-side audio."
                    )
                    OnboardingPoint(
                        icon = Icons.Default.Security,
                        title = "100% Offline Privacy",
                        desc = "Zero internet permission. Files are encrypted in private app storage, invisible to file managers."
                    )
                    OnboardingPoint(
                        icon = Icons.Default.NotificationsOff,
                        title = "Notification Tip",
                        desc = "Do not block notifications! Use 'Notification Categories' in Samsung settings to hide the standby icon cleanly."
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showOnboardingDialog = false
                    onOnboardingDismissed()
                }) {
                    Text("Get Started")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOnboardingDialog = false
                    onOnboardingDismissed()
                    currentScreen = AppScreen.DOCS
                }) {
                    Text("Read Full Guide")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Header with Branding & Appearance Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Secure Recorder",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "100% Offline Vault",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Compact Appearance Toggle (Sun/Moon)
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Clean Navigation Destination Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Call, contentDescription = null) },
                        label = { Text("Recordings", fontWeight = FontWeight.Medium) },
                        selected = currentScreen == AppScreen.RECORDINGS,
                        onClick = {
                            currentScreen = AppScreen.RECORDINGS
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("System & Permissions", fontWeight = FontWeight.Medium) },
                        selected = currentScreen == AppScreen.SYSTEM_PERMISSIONS,
                        onClick = {
                            currentScreen = AppScreen.SYSTEM_PERMISSIONS
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                        label = { Text("Storage Cleanup", fontWeight = FontWeight.Medium) },
                        selected = currentScreen == AppScreen.STORAGE_CLEANUP,
                        onClick = {
                            currentScreen = AppScreen.STORAGE_CLEANUP
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        label = { Text("Security & App Lock", fontWeight = FontWeight.Medium) },
                        selected = currentScreen == AppScreen.SECURITY,
                        onClick = {
                            currentScreen = AppScreen.SECURITY
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                        label = { Text("How It Works (Docs)", fontWeight = FontWeight.Medium) },
                        selected = currentScreen == AppScreen.DOCS,
                        onClick = {
                            currentScreen = AppScreen.DOCS
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF2E7D32), CircleShape)
                        )
                        Text(
                            text = "v$appVersionName · 100% Offline Vault",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (currentScreen == AppScreen.RECORDINGS) {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                TwoHorizontalLinesIcon(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            IconButton(onClick = { currentScreen = AppScreen.RECORDINGS }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = currentScreen.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        if (currentScreen == AppScreen.RECORDINGS) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = if (isServiceRunning) "On" else "Off",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Switch(
                                    checked = isServiceRunning,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            if (!hasSystemPermissions) {
                                                Toast.makeText(context, "Grant system permissions first", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.SYSTEM_PERMISSIONS
                                                return@Switch
                                            }

                                            prefs.edit().putBoolean(MainActivity.KEY_MONITORING_ENABLED, true).apply()
                                            val intent = Intent(context, RecorderControllerService::class.java).apply {
                                                action = RecorderControllerService.ACTION_START_MONITORING
                                            }
                                            ContextCompat.startForegroundService(context, intent)
                                            isServiceRunning = true
                                        } else {
                                            prefs.edit().putBoolean(MainActivity.KEY_MONITORING_ENABLED, false).apply()
                                            val intent = Intent(context, RecorderControllerService::class.java).apply {
                                                action = RecorderControllerService.ACTION_STOP_MONITORING
                                            }
                                            context.startService(intent)
                                            isServiceRunning = false
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.RECORDINGS -> {
                        RecordingsPage(
                            recordings = filteredRecordings,
                            displayedRecordings = displayedRecordings,
                            totalRecordingsCount = recordings.size,
                            currentSortOption = currentSortOption,
                            onSortOptionChanged = { option ->
                                currentSortOption = option
                                prefs.edit().putString(MainActivity.KEY_SORT_OPTION, option.name).apply()
                            },
                            pageSize = pageSize,
                            onLoadMore = { pageSize += 50 },
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { searchQuery = it },
                            selectedDateMillis = selectedDateMillis,
                            onClearDateFilter = { selectedDateMillis = null },
                            onOpenDatePicker = { showDatePicker = true },
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    delay(350)
                                    refreshRecordings()
                                    checkAllStatuses()
                                    isRefreshing = false
                                }
                            },
                            currentlyPlayingPath = currentlyPlayingPath,
                            playbackPositionMs = playbackPositionMs,
                            playbackDurationMs = playbackDurationMs,
                            playbackSpeed = playbackSpeed,
                            isVolumeBoosted = isVolumeBoosted,
                            onPlay = { item ->
                                currentlyPlayingPath = item.file.absolutePath
                                try {
                                    val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                                    val currentVol = audioMgr?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1
                                    if (currentVol == 0) {
                                        Toast.makeText(context, "Media volume is muted. Use phone volume buttons to increase sound.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (_: Exception) {}

                                player.onProgressUpdate = { posMs, durMs ->
                                    playbackPositionMs = posMs
                                    playbackDurationMs = durMs
                                }
                                player.play(
                                    file = item.file,
                                    initialSpeed = playbackSpeed,
                                    initialVolumeBoost = isVolumeBoosted,
                                    onComplete = {
                                        currentlyPlayingPath = null
                                        playbackPositionMs = 0L
                                        playbackDurationMs = 0L
                                    },
                                    scope = coroutineScope
                                )
                            },
                            onStop = {
                                player.stop()
                                currentlyPlayingPath = null
                                playbackPositionMs = 0L
                                playbackDurationMs = 0L
                            },
                            onSeek = { targetMs ->
                                player.seekTo(targetMs)
                                playbackPositionMs = targetMs
                            },
                            onSkip = { deltaMs ->
                                player.skipBy(deltaMs, playbackPositionMs)
                                playbackPositionMs = (playbackPositionMs + deltaMs).coerceIn(0L, playbackDurationMs)
                            },
                            onCycleSpeed = {
                                val nextSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                playbackSpeed = nextSpeed
                                player.setSpeed(nextSpeed)
                            },
                            onToggleVolumeBoost = {
                                isVolumeBoosted = !isVolumeBoosted
                                player.setVolumeBoost(isVolumeBoosted)
                            },
                            onToggleStar = { item ->
                                val currentStars = prefs.getStringSet(MainActivity.KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
                                if (currentStars.contains(item.name)) {
                                    currentStars.remove(item.name)
                                } else {
                                    currentStars.add(item.name)
                                }
                                prefs.edit().putStringSet(MainActivity.KEY_FAVORITES, currentStars).apply()
                                refreshRecordings()
                            },
                            filterOnlyStarred = filterOnlyStarred,
                            onToggleFilterStarred = { filterOnlyStarred = !filterOnlyStarred },
                            onShare = { item ->
                                val wav = SecureAudioVault.exportToCacheWav(context, item.file)
                                if (wav != null) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            wav
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "audio/wav"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Call Audio"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to export WAV", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = { item ->
                                fileToDelete = item
                            },
                            onEditNote = { item ->
                                itemForNoteDialog = item
                                noteInputText = item.note ?: ""
                            },
                            isShizukuActive = isShizukuGranted
                        )
                    }

                    AppScreen.SYSTEM_PERMISSIONS -> {
                        SystemPermissionsPage(
                            isShizukuAvailable = isShizukuAvailable,
                            isShizukuGranted = isShizukuGranted,
                            hasSystemPermissions = hasSystemPermissions,
                            hasContactsPermission = hasContactsPermission,
                            isBatteryOptimizedIgnored = isBatteryOptimizedIgnored,
                            onRequestShizuku = onRequestShizuku,
                            onRequestPermissions = {
                                val permissions = mutableListOf(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.RECORD_AUDIO
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                systemPermissionsLauncher.launch(permissions.toTypedArray())
                            },
                            onRequestContactsPermission = {
                                val permissions = arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.READ_CALL_LOG
                                )
                                contactsPermissionsLauncher.launch(permissions)
                            },
                            onOpenBatterySettings = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        @SuppressLint("BatteryLife")
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("SecureRecorder", "Battery intent failed: ${e.message}")
                                    }
                                }
                            }
                        )
                    }

                    AppScreen.STORAGE_CLEANUP -> {
                        StorageCleanupPage(
                            recordingsCount = recordings.size,
                            starredCount = recordings.count { it.isStarred },
                            autoRetentionDays = autoRetentionDays,
                            onAutoRetentionChanged = { days ->
                                autoRetentionDays = days
                                prefs.edit().putInt(MainActivity.KEY_AUTO_RETENTION, days).apply()
                                SecureAudioVault.performAutoRetentionCleanup(context)
                                refreshRecordings()
                            },
                            onCleanupTriggered = { type ->
                                bulkCleanupDialogType = type
                            }
                        )
                    }

                    AppScreen.SECURITY -> {
                        SecurityPage(
                            isAppLockEnabled = isAppLockEnabled,
                            onAppLockChanged = onAppLockChanged
                        )
                    }

                    AppScreen.DOCS -> {
                        DocsPage(
                            appVersionName = appVersionName,
                            onOpenNotificationSettings = {
                                try {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Open Settings -> Apps -> Secure Call Recorder -> Notifications", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsPage(
    recordings: List<RecordingItem>,
    displayedRecordings: List<RecordingItem>,
    totalRecordingsCount: Int,
    currentSortOption: RecordingSortOption,
    onSortOptionChanged: (RecordingSortOption) -> Unit,
    pageSize: Int,
    onLoadMore: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedDateMillis: Long?,
    onClearDateFilter: () -> Unit,
    onOpenDatePicker: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    currentlyPlayingPath: String?,
    playbackPositionMs: Long,
    playbackDurationMs: Long,
    playbackSpeed: Float,
    isVolumeBoosted: Boolean,
    onPlay: (RecordingItem) -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleVolumeBoost: () -> Unit,
    onToggleStar: (RecordingItem) -> Unit,
    filterOnlyStarred: Boolean,
    onToggleFilterStarred: () -> Unit,
    onShare: (RecordingItem) -> Unit,
    onDelete: (RecordingItem) -> Unit,
    onEditNote: (RecordingItem) -> Unit,
    isShizukuActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Integrated Search Input with Calendar inside trailingIcon
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or number...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onOpenDatePicker) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Filter Date",
                            tint = if (selectedDateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        // Filter & Sort Chips Row (Horizontally scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sort Dropdown Menu Chip
            Box {
                var isSortMenuExpanded by remember { mutableStateOf(false) }

                AssistChip(
                    onClick = { isSortMenuExpanded = true },
                    label = { Text(currentSortOption.label, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false }
                ) {
                    RecordingSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == currentSortOption) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == currentSortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                if (option == currentSortOption) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                isSortMenuExpanded = false
                                onSortOptionChanged(option)
                            }
                        )
                    }
                }
            }

            // Starred Filter Chip
            FilterChip(
                selected = filterOnlyStarred,
                onClick = onToggleFilterStarred,
                label = { Text("Starred", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (filterOnlyStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (filterOnlyStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            // Date Filter Chip (if selected)
            selectedDateMillis?.let { dateMillis ->
                val dateDisplay = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(dateMillis))

                AssistChip(
                    onClick = onOpenDatePicker,
                    label = { Text("Date: $dateDisplay", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = onClearDateFilter, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Filter", modifier = Modifier.size(13.dp))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Header Row: Count & Mode Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isFiltered = searchQuery.isNotEmpty() || selectedDateMillis != null || filterOnlyStarred
            val countText = if (isFiltered && recordings.size != totalRecordingsCount) {
                "Recordings: ${recordings.size} of $totalRecordingsCount"
            } else {
                "Recordings (${recordings.size})"
            }

            Text(
                text = countText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isFiltered && recordings.size != totalRecordingsCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Capture Mode Pill Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (isShizukuActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isShizukuActive) Color(0xFF2E7D32) else Color(0xFFF57C00), CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isShizukuActive) "Enhanced (Dual-Side)" else "Standard (Mic)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isShizukuActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pull-to-Refresh Box
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (recordings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedDateMillis != null)
                            "No recordings found for current filter."
                        else
                            "No call recordings yet.\n(Pull down to refresh)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedRecordings, key = { it.file.absolutePath }) { item ->
                        val isPlaying = currentlyPlayingPath == item.file.absolutePath
                        var isItemMenuExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val callGreen = Color(0xFF4CAF50)
                                        val callBlue = Color(0xFF2196F3)

                                        val iconBg = if (isPlaying) {
                                            MaterialTheme.colorScheme.primary
                                        } else when (item.direction) {
                                            CallDirection.INCOMING -> callGreen.copy(alpha = 0.18f)
                                            CallDirection.OUTGOING -> callBlue.copy(alpha = 0.18f)
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }

                                        val iconTint = if (isPlaying) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else when (item.direction) {
                                            CallDirection.INCOMING -> callGreen
                                            CallDirection.OUTGOING -> callBlue
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        val iconVector = when (item.direction) {
                                            CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                                            CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
                                            else -> if (item.subtitle.isNotBlank() && item.subtitle != "Unknown Number") Icons.Default.Call else Icons.Default.Mic
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(iconBg, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = null,
                                                tint = iconTint,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            if (item.subtitle.isNotBlank()) {
                                                Text(
                                                    text = item.subtitle,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1
                                                )
                                            }
                                            val durBadge = "${item.durationSeconds} sek (${item.durationFormatted})"
                                            val detailsText = "${item.dateFormatted} • $durBadge • ${item.sizeFormatted}"

                                            Text(
                                                text = detailsText,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                            if (!item.note.isNullOrBlank()) {
                                                Surface(
                                                    onClick = { onEditNote(item) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Description,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = item.note,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Star Toggle Button
                                        IconButton(onClick = { onToggleStar(item) }) {
                                            Icon(
                                                imageVector = if (item.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = if (item.isStarred) "Unstar" else "Star",
                                                tint = if (item.isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Play / Stop Button
                                        IconButton(onClick = {
                                            if (isPlaying) {
                                                onStop()
                                            } else {
                                                onPlay(item)
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Stop" else "Play",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // More Options Menu
                                        Box {
                                            IconButton(onClick = { isItemMenuExpanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Options",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = isItemMenuExpanded,
                                                onDismissRequest = { isItemMenuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    },
                                                    text = { Text(if (item.note.isNullOrBlank()) "Add Note" else "Edit Note", fontSize = 13.sp) },
                                                    onClick = {
                                                        isItemMenuExpanded = false
                                                        onEditNote(item)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    },
                                                    text = { Text("Share (WAV)", fontSize = 13.sp) },
                                                    onClick = {
                                                        isItemMenuExpanded = false
                                                        onShare(item)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    },
                                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error, fontSize = 13.sp) },
                                                    onClick = {
                                                        isItemMenuExpanded = false
                                                        onDelete(item)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isPlaying) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)
                                    ) {
                                        var isSeeking by remember { mutableStateOf(false) }
                                        var seekSliderPos by remember { mutableFloatStateOf(0f) }

                                        val currentProgress = if (playbackDurationMs > 0) {
                                            (playbackPositionMs.toFloat() / playbackDurationMs.toFloat()).coerceIn(0f, 1f)
                                        } else 0f

                                        Slider(
                                            value = if (isSeeking) seekSliderPos else currentProgress,
                                            onValueChange = {
                                                isSeeking = true
                                                seekSliderPos = it
                                            },
                                            onValueChangeFinished = {
                                                val targetMs = (seekSliderPos * playbackDurationMs).toLong()
                                                onSeek(targetMs)
                                                isSeeking = false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatDuration(if (isSeeking) (seekSliderPos * playbackDurationMs).toLong() else playbackPositionMs),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = formatDuration(playbackDurationMs),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Player Action Controls: -10s, +10s, Speed, Boost
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { onSkip(-10_000L) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Replay10,
                                                        contentDescription = "Back 10s",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onSkip(10_000L) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Forward10,
                                                        contentDescription = "Forward 10s",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AssistChip(
                                                    onClick = onCycleSpeed,
                                                    label = {
                                                        Text(
                                                            text = "${playbackSpeed}x",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.height(28.dp)
                                                )

                                                FilterChip(
                                                    selected = isVolumeBoosted,
                                                    onClick = onToggleVolumeBoost,
                                                    label = {
                                                        Text(
                                                            text = if (isVolumeBoosted) "Boost ON" else "Boost",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.height(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (recordings.size > displayedRecordings.size) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(onClick = onLoadMore) {
                                    Text("Load 50 more (${recordings.size - displayedRecordings.size} remaining)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemPermissionsPage(
    isShizukuAvailable: Boolean,
    isShizukuGranted: Boolean,
    hasSystemPermissions: Boolean,
    hasContactsPermission: Boolean,
    isBatteryOptimizedIgnored: Boolean,
    onRequestShizuku: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Core System Permissions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Core Recording Permissions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Microphone (RECORD_AUDIO) and Phone State (READ_PHONE_STATE). Essential for detecting and capturing phone calls.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                StatusItem(
                    title = "Recording Engine",
                    isOk = hasSystemPermissions,
                    okText = "Active & Ready",
                    errorText = "Missing Core Permissions"
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusItem(
                    title = "Caller Name Resolution",
                    isOk = hasContactsPermission,
                    okText = "Contacts Granted",
                    errorText = "Optional (Number Only)"
                )
                if (!hasSystemPermissions) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant Core Permissions")
                    }
                } else if (!hasContactsPermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRequestContactsPermission,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Allow Contacts & Name Resolution (Optional)")
                    }
                }
            }
        }

        // Card 2: Enhanced Capture Mode (Shizuku)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Enhanced Dual-Side (Shizuku)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Optional. Standard Mode (Mic capture) is currently active and ready out-of-the-box. Shizuku elevates capture to UID 2000 for crystal-clear internal caller & receiver audio without root.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                StatusItem(
                    title = "Capture Mode",
                    isOk = isShizukuGranted,
                    okText = "Enhanced Dual-Side (UID 2000)",
                    errorText = "Standard Mic Mode (Ready)"
                )
                if (!isShizukuGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRequestShizuku,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Connect Shizuku (Optional)")
                    }
                }
            }
        }

        // Card 3: Universal Background Battery Protection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Background Battery Protection", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android battery savers (Poco/Xiaomi MIUI, HyperOS, Samsung, etc.) aggressively sleep or kill background monitors. Excluding the app keeps standby monitoring alive 24/7.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                StatusItem(
                    title = "Battery Protection",
                    isOk = isBatteryOptimizedIgnored,
                    okText = "Unrestricted (Safe)",
                    errorText = "Restricted (May Be Frozen)"
                )
                if (!isBatteryOptimizedIgnored) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenBatterySettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Disable Battery Optimization")
                    }
                }
            }
        }
    }
}

@Composable
fun StorageCleanupPage(
    recordingsCount: Int,
    starredCount: Int,
    autoRetentionDays: Int,
    onAutoRetentionChanged: (Int) -> Unit,
    onCleanupTriggered: (BulkCleanupType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Secure Vault Storage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Recordings: $recordingsCount ($starredCount Starred ★)\nFiles are stored encrypted in private sandboxed internal storage.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Automatic Retention Policy Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Automatic Retention Policy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Automatically purges old unstarred recordings when new calls finish. Starred (★) recordings are permanently protected and will NEVER be deleted.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                AutoRetentionPolicy.entries.forEach { policy ->
                    val isSelected = autoRetentionDays == policy.days
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = policy.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onAutoRetentionChanged(policy.days) }
                        )
                    }
                    if (policy != AutoRetentionPolicy.entries.last()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Manual Storage Cleanup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "Instantly free up device storage. Starred recordings will remain protected.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedButton(
                    onClick = { onCleanupTriggered(BulkCleanupType.ONE_MONTH) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete older than 30 days")
                }

                OutlinedButton(
                    onClick = { onCleanupTriggered(BulkCleanupType.THREE_MONTHS) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete older than 90 days")
                }

                Button(
                    onClick = { onCleanupTriggered(BulkCleanupType.ALL) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Recordings")
                }
            }
        }
    }
}

@Composable
fun SecurityPage(
    isAppLockEnabled: Boolean,
    onAppLockChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Biometric / PIN App Lock", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Require fingerprint, face, or device PIN every time the app opens.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = onAppLockChanged
                    )
                }
            }
        }
    }
}

@Composable
fun DocsPage(
    appVersionName: String,
    onOpenNotificationSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DocsSectionCard(
            icon = Icons.Default.Security,
            title = "1. 100% Offline & Zero-Internet Guarantee",
            body = "Secure Call Recorder does NOT declare the 'android.permission.INTERNET' permission. By operating in an absolute network-free sandbox, your recorded conversations cannot leak, sync, or be uploaded to external servers or clouds. Everything stays strictly on your physical device."
        )

        DocsSectionCard(
            icon = Icons.Default.Mic,
            title = "2. Dual Capture Modes: Standard vs Enhanced",
            body = "• Standard Mode (Out-of-the-Box):\nUses standard Android microphone recording. It requires zero technical setup or ADB commands. Ideal for quick use, recording your voice and audible audio.\n\n• Enhanced Mode (Shizuku UID 2000):\nAndroid 14–17 blocks third-party apps from capturing call audio lines. Shizuku elevates the recorder to UID 2000 (Shell), allowing direct access to AudioSource.VOICE_CALL to capture crystal-clear audio from both caller and receiver simultaneously."
        )

        DocsSectionCard(
            icon = Icons.Default.Notifications,
            title = "3. How to Hide the Standby Notification (Pro-Tip)",
            body = "Android requires an ongoing notification for background monitoring services. If you block the notification entirely, Android or OEM battery managers may kill the service!\n\nTo hide the persistent icon cleanly without stopping background recording:\n1. Tap the button below to open Notification Settings.\n2. Tap 'Notification categories'.\n3. Set 'Call Recording Status' to 'Silent' or toggle it OFF.\n\nThis keeps background monitoring alive while keeping your status bar completely spotless!"
        ) {
            Button(
                onClick = onOpenNotificationSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open Notification Settings")
            }
        }

        DocsSectionCard(
            icon = Icons.Default.BatteryAlert,
            title = "4. Universal Background Battery Protection",
            body = "Modern Android devices (Poco/Xiaomi MIUI, HyperOS, Samsung One UI, Pixel, etc.) employ aggressive power management that freezes background processes. Secure Call Recorder operates a persistent lightweight standby Foreground Service with WakeLock protection, ensuring battery managers never terminate your active call recording."
        )

        DocsSectionCard(
            icon = Icons.Default.Lock,
            title = "5. Sandboxed Encrypted Vault (.pvr)",
            body = "Recordings are saved directly to internal sandbox storage (/data/user/0/.../secure_vault). Third-party file managers (Google Files, Xiaomi File Manager, Samsung My Files) and media players cannot access this directory. Additionally, files are masked with a proprietary XOR header to prevent unauthorized extraction."
        )

        DocsSectionCard(
            icon = Icons.Default.Info,
            title = "6. App Version & Build Information",
            body = "• Installed Version: v$appVersionName\n• Security Architecture: 100% Offline (Zero Internet Permission)\n• Target Platform: Android 11–17 (API 30–37)\n• Device Optimization: Universal (Poco, Xiaomi, Samsung, Pixel, etc.)\n• License: Open Source (GitHub)"
        )
    }
}

@Composable
fun DocsSectionCard(
    icon: ImageVector,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action?.let {
                Spacer(modifier = Modifier.height(10.dp))
                it()
            }
        }
    }
}

@Composable
fun StatusItem(
    title: String,
    isOk: Boolean,
    okText: String,
    errorText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isOk) Color(0xFF2E7D32) else Color(0xFFC62828),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (isOk) okText else errorText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOk) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}

@Composable
fun OnboardingPoint(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}
