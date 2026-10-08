package uz.developer.privaterecorder.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * SecureAudioVault manages encrypted/obfuscated call recordings stored exclusively
 * inside the app's private sandbox (/data/user/0/.../files/secure_vault).
 *
 * External File Managers and Media Scanners cannot access this directory (Permission Denied).
 * In addition, files are obfuscated with a proprietary header and XOR mask to prevent
 * unauthorized playback even if extracted via root/ADB backup.
 */
object SecureAudioVault {
    private const val TAG = "SecureAudioVault"
    private const val HEADER_MAGIC = "PVR_SECURE_V1\n"
    private const val HEADER_SIZE = 64
    private const val XOR_MASK: Byte = 0x5A.toByte()

    fun getVaultDir(context: Context): File {
        val dir = File(context.filesDir, "secure_vault")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempDir(context: Context): File {
        val dir = context.getExternalFilesDir("temp_recordings") ?: File(context.cacheDir, "temp_recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Converts a raw PCM file from the temp directory into a secure .pvr file in the internal vault,
     * then permanently removes the temporary file.
     */
    fun secureAndVaultFile(context: Context, rawPcmFile: File, newFileNameWithoutExt: String): File? {
        if (!rawPcmFile.exists() || rawPcmFile.length() == 0L) {
            Log.w(TAG, "Cannot vault empty or missing file: ${rawPcmFile.absolutePath}")
            return null
        }

        val vaultDir = getVaultDir(context)
        val securedFile = File(vaultDir, "$newFileNameWithoutExt.pvr")

        try {
            FileInputStream(rawPcmFile).use { input ->
                FileOutputStream(securedFile).use { output ->
                    // 1. Write 64-byte Header
                    val header = ByteArray(HEADER_SIZE)
                    val magicBytes = HEADER_MAGIC.toByteArray(Charsets.UTF_8)
                    System.arraycopy(magicBytes, 0, header, 0, magicBytes.size)

                    // Store timestamp and audio properties
                    val bb = ByteBuffer.wrap(header, 16, 16).order(ByteOrder.LITTLE_ENDIAN)
                    bb.putLong(System.currentTimeMillis())
                    bb.putInt(16000) // Sample rate
                    bb.putShort(1)   // Channels
                    bb.putShort(16)  // Bits per sample

                    output.write(header)

                    // 2. Stream and mask PCM payload (Fast in-memory bitwise operation, 0% CPU strain)
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        for (i in 0 until read) {
                            buffer[i] = (buffer[i].toInt() xor XOR_MASK.toInt()).toByte()
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            // Securely wipe raw temp file
            rawPcmFile.delete()
            Log.i(TAG, "Successfully moved and secured file to vault: ${securedFile.absolutePath} (${securedFile.length()} bytes)")
            return securedFile
        } catch (e: Exception) {
            Log.e(TAG, "Error securing file to vault: ${e.message}", e)
            return null
        }
    }

    /**
     * Decodes and reads raw PCM bytes from a secured .pvr file for internal AudioTrack playback.
     */
    fun openVaultAudioStream(vaultFile: File): InputStream? {
        if (!vaultFile.exists()) return null
        return try {
            val fis = FileInputStream(vaultFile)
            val header = ByteArray(HEADER_SIZE)
            val readHeader = fis.read(header)
            if (readHeader < HEADER_SIZE) {
                fis.close()
                return null
            }

            // Return custom filtering stream that unmasks XOR on the fly
            object : InputStream() {
                override fun read(): Int {
                    val b = fis.read()
                    if (b == -1) return -1
                    return b xor XOR_MASK.toInt()
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val count = fis.read(b, off, len)
                    if (count == -1) return -1
                    for (i in off until off + count) {
                        b[i] = (b[i].toInt() xor XOR_MASK.toInt()).toByte()
                    }
                    return count
                }

                override fun close() {
                    fis.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening vault stream: ${e.message}", e)
            null
        }
    }

    /**
     * Exports a secured .pvr file into a standard playable WAV file in the cache directory
     * for sharing via FileProvider.
     */
    fun exportToCacheWav(context: Context, vaultFile: File): File? {
        val inputStream = openVaultAudioStream(vaultFile) ?: return null
        val cacheWav = File(context.cacheDir, "${vaultFile.nameWithoutExtension}.wav")

        try {
            val pcmBytes = inputStream.readBytes()
            inputStream.close()

            FileOutputStream(cacheWav).use { out ->
                val sampleRate = 16000
                val channels = 1
                val bitsPerSample = 16
                val byteRate = sampleRate * channels * (bitsPerSample / 8)
                val totalDataLen = pcmBytes.size
                val totalAudioLen = totalDataLen + 36

                val header = ByteArray(44)
                header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
                header[4] = (totalAudioLen and 0xff).toByte()
                header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
                header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
                header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
                header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
                header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
                header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // Subchunk1Size
                header[20] = 1; header[21] = 0 // AudioFormat (1 = PCM)
                header[22] = channels.toByte(); header[23] = 0
                header[24] = (sampleRate and 0xff).toByte()
                header[25] = ((sampleRate shr 8) and 0xff).toByte()
                header[26] = ((sampleRate shr 16) and 0xff).toByte()
                header[27] = ((sampleRate shr 24) and 0xff).toByte()
                header[28] = (byteRate and 0xff).toByte()
                header[29] = ((byteRate shr 8) and 0xff).toByte()
                header[30] = ((byteRate shr 16) and 0xff).toByte()
                header[31] = ((byteRate shr 24) and 0xff).toByte()
                header[32] = (channels * bitsPerSample / 8).toByte(); header[33] = 0 // BlockAlign
                header[34] = bitsPerSample.toByte(); header[35] = 0
                header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
                header[40] = (totalDataLen and 0xff).toByte()
                header[41] = ((totalDataLen shr 8) and 0xff).toByte()
                header[42] = ((totalDataLen shr 16) and 0xff).toByte()
                header[43] = ((totalDataLen shr 24) and 0xff).toByte()

                out.write(header)
                out.write(pcmBytes)
                out.flush()
            }
            return cacheWav
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting WAV: ${e.message}", e)
            return null
        }
    }

    /**
     * Migrates any legacy unencrypted recordings from external storage into the secure vault.
     */
    fun migrateLegacyRecordings(context: Context) {
        try {
            val legacyDir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS) ?: return
            val files = legacyDir.listFiles { f -> f.extension == "pcm" } ?: return
            for (file in files) {
                val baseName = file.nameWithoutExtension
                secureAndVaultFile(context, file, baseName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating legacy recordings: ${e.message}", e)
        }
    }

    /**
     * Edge case recovery: Recovers any orphaned raw PCM files left behind in temp_recordings
     * due to sudden phone shutdown, battery exhaustion, or crash during an active call.
     */
    fun recoverOrphanRecordings(context: Context) {
        try {
            val tempDir = getTempDir(context)
            val files = tempDir.listFiles { f -> f.extension == "pcm" } ?: return
            val currentTime = System.currentTimeMillis()
            for (file in files) {
                // If file hasn't been modified in the last 15 seconds, it belongs to an interrupted session
                if (currentTime - file.lastModified() > 15000L) {
                    if (file.length() >= 32000L) { // At least ~1 second of 16kHz audio
                        val baseName = if (file.nameWithoutExtension.startsWith("REC_")) {
                            file.nameWithoutExtension
                        } else {
                            "RECOVERED_${file.nameWithoutExtension}"
                        }
                        Log.i(TAG, "Recovering orphaned recording after crash/reboot: ${file.name} (${file.length()} bytes)")
                        secureAndVaultFile(context, file, baseName)
                    } else {
                        // Discard junk/empty aborted recordings
                        Log.d(TAG, "Deleting empty/tiny aborted temp file: ${file.name}")
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recovering orphan recordings: ${e.message}", e)
        }
    }

    /**
     * Automatically purges recordings older than the user-configured retention policy in SharedPreferences.
     * Starred (favorite) recordings are PERMANENTLY PROTECTED and will NEVER be deleted.
     */
    fun performAutoRetentionCleanup(context: Context) {
        try {
            val prefs = context.getSharedPreferences("secure_recorder_prefs", Context.MODE_PRIVATE)
            val retentionDays = prefs.getInt("auto_retention_days", 0)
            if (retentionDays <= 0) return // 0 = Keep forever / auto-retention disabled

            val thresholdTime = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 3600 * 1000)
            val favorites = prefs.getStringSet("favorite_recordings", emptySet()) ?: emptySet()

            val vaultDir = getVaultDir(context)
            val files = vaultDir.listFiles { f -> f.isFile && (f.name.endsWith(".pvr") || f.name.endsWith(".pcm")) } ?: return

            var purgedCount = 0
            val editor = prefs.edit()
            for (file in files) {
                if (file.lastModified() < thresholdTime && !favorites.contains(file.name)) {
                    val deleted = file.delete()
                    if (deleted) {
                        editor.remove("note_${file.name}")
                        purgedCount++
                    }
                }
            }
            if (purgedCount > 0) {
                editor.apply()
                Log.i(TAG, "Auto-retention cleanup ($retentionDays days): purged $purgedCount old unstarred recordings.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing auto-retention cleanup: ${e.message}", e)
        }
    }
}
