# Secure Call Recorder 🎙️🔒

A **100% offline, privacy-first call recording application** engineered specifically for modern Android versions (**Android 15–17 / API 35–37**) and **Samsung One UI 7–9** (Snapdragon SM-S9420 / Galaxy S24+ and modern Galaxy devices).

---

## 🌟 Core Architecture & Privacy Pillars

- **🛡️ Zero-Internet Privacy Guarantee:**
  The `android.permission.INTERNET` permission is completely stripped from `AndroidManifest.xml`. The app cannot communicate with any remote servers, analytics, crash reporters, or external APIs. All data stays strictly on your physical device.
- **⚡ Dual Capture Engine:**
  1. **Enhanced Mode (Dual-Side Audio via Shizuku):** Elevates audio capture privileges to UID 2000 (`com.android.shell`) to bypass Google's modern call recording restrictions and directly tap internal audio streams for crystal-clear caller and receiver audio.
  2. **Standard Mode (Hardware-Aware Native Fallback):** For users without Shizuku, the app automatically switches to an optimized microphone recorder supporting Bluetooth SCO (Galaxy Buds), wired headsets, and USB-C headsets without requiring terminal commands.
- **🔐 Sandboxed Vault (`.pvr`):**
  Recordings are stored inside the app's internal sandbox (`/data/user/0/uz.developer.privaterecorder/files/secure_vault/`). File managers (Samsung My Files, Google Files) and media indexers cannot scan or play these files. Data is safeguarded with custom obfuscation headers and XOR masking.
- **🔋 Samsung One UI Deep Sleep & WakeLock Guard:**
  Integrates a selective `PARTIAL_WAKE_LOCK` and persistent standby Foreground Service (`microphone|phoneCall|specialUse`). When you place the phone to your ear and the proximity sensor turns off the screen, One UI is prevented from throttling the CPU or dropping audio buffer frames. WakeLocks are immediately released the moment the call ends.
- **👆 Biometric & PIN Security:**
  Protects all call archives with Android's native `BiometricPrompt` (Fingerprint, Face Unlock, or Device Lock PIN).
- **🔄 Sudden Reboot & Crash Recovery:**
  If a phone reboots or loses power mid-call, incomplete temporary recordings are automatically detected, finalized, and secured into the vault upon the next reboot or app launch.

---

## 🚀 Key Features & Enhancements

### 1. Advanced Audio Player
- **Variable Playback Speed:** Switch between `1.0x`, `1.25x`, `1.5x`, and `2.0x` speeds dynamically without pitch distortion.
- **Quick Scrubbing:** Dedicated `-10s` rewind and `+10s` fast-forward controls for quick navigation during long calls.
- **Volume Boost (+5dB):** Built-in software amplification toggle to clearly hear quiet callers or low ear-speaker outputs.
- **Interactive Scrubber:** Smooth seekbar slider to instantly jump to any timestamp in the call.
- **Precise Duration Display:** Calculates and displays the exact duration (`MM:SS` or `HH:MM:SS`) derived from 16kHz 16-bit mono PCM stream metrics.

### 2. Starred Protection (Favorites)
- Mark any recording with a **Star (⭐)** directly from the card.
- Filter only your starred recordings using the header chip filter.
- **Guaranteed Safe:** Starred recordings are strictly protected from accidental deletion and are automatically excluded from all retention cleanups.

### 3. Smart Search & Custom Notes
- **Recording Notes (📝):** Attach custom notes or memos to any recording (stored safely without altering vault cryptographic integrity or file timestamps).
- **Note Badge:** Cards with notes display a quick-access badge for one-click reading and editing.
- **3-Way Smart Search:** Real-time search across **Contact Names**, **Phone Numbers**, and **Note Contents**.

### 4. 6-Way Dynamic Sorting
Easily sort your recordings with one tap:
- 📅 **Newest first** / **Oldest first**
- ⏱️ **Longest first** / **Shortest first**
- 💾 **Largest size** / **Smallest size**
- Real-time counter displayed in the toolbar: `Recordings: X of Y`.

### 5. Automated Storage Cleanup (Auto-Retention Policy)
Keep device storage clean automatically without manual effort:
- Retention rules: **Never**, **30 days**, **60 days**, **90 days**, or **180 days**.
- Runs silently in the background immediately upon call termination and on app launch.
- Never touches starred recordings.

---

## 🛠️ Technical Deep Dive

### Dual-Side Audio Capture via Shizuku (UID 2000)
Starting in Android 10, Google restricted third-party apps from capturing two-way call audio (`AudioSource.VOICE_CALL` throws `SecurityException` for standard user apps). 

Secure Call Recorder leverages the **Shizuku IPC Framework** to bind to a background user service executing under UID 2000 (`android.uid.shell`). Using reflection via `LSPosed HiddenApiBypass`, it instantiates the hidden multi-parameter `AudioRecord` constructor:
```kotlin
AudioRecord(
    AudioAttributes(VOICE_CALL, USAGE_VOICE_COMMUNICATION),
    AudioFormat(16000Hz, MONO, PCM_16BIT),
    bufferSize,
    sessionId,
    Context("com.android.shell"),
    AttributionSource(UID 2000),
    trackFlags
)
```
This enables dual-side call recording on modern Android 15–17 devices without requiring root access.

### Audio Pipeline & Math
- **Sample Rate:** 16,000 Hz (16 kHz)
- **Channels:** Mono (1 channel)
- **Bit Depth:** 16-bit PCM (`ENCODING_PCM_16BIT` = 2 bytes per sample)
- **Bitrate:** $16,000 \times 2 = 32,000\text{ bytes/second}$ ($32\text{ bytes/millisecond}$)
- **File Calculation:**
  $$\text{Duration (ms)} = \frac{\text{PCM Byte Size}}{32}$$

### Contact & Call Resolution
When a call disconnects (`CALL_STATE_IDLE`), `RecorderControllerService` queries `CallLog.Calls` and `ContactsContract.PhoneLookup` to resolve the caller's name and phone number. Files are automatically formatted:
```text
REC_20261008_143015_John_Doe_+998901234567.pvr
```

---

## 💡 Samsung One UI Optimization & Notification Setup

Android 14–17 mandates an active notification for Foreground Services using the `microphone` type. If you block the application's notifications globally, Android or Samsung One UI may terminate the recording service.

The app uses **two independent notification channels**:
1. `channel_standby_monitoring` (Low priority / Silent standby)
2. `channel_active_recording` (Active call alert)

### How to Hide the Standby Icon from the Status Bar:
1. Open phone **Settings** > **Apps** > **Secure Call Recorder** > **Notifications**.
2. Tap **Notification categories**.
3. Locate **Call Recording Status** (Standby).
4. Set it to **Silent** or toggle it **OFF**.
5. *Result:* Background call detection remains 100% active and protected against battery managers, while your status bar stays completely clean!

---

## 📱 Application Screens

- **🎙️ Recordings Screen:** Audio player with speed controls, waveforms, volume boost, sorting dropdown, starred filter, search bar, and WAV export.
- **⚡ System & Permissions:** Visual dashboard showing Shizuku status, telephony & contact permissions, and Samsung battery optimization status.
- **🧹 Storage Cleanup:** Storage usage analysis, auto-retention policy selection (30, 60, 90, 180 days), and manual bulk cleanup.
- **🔒 Security & Lock:** Biometric fingerprint/face unlock and device PIN protection toggles.
- **📖 How It Works (Docs):** Interactive documentation with quick-access shortcuts to Samsung notification settings.

---

## 🏗️ Building and Installing

### Prerequisites
- JDK 17+ (or JDK 21 / 25)
- Android SDK Platform 37 (`android-37.0`)
- Gradle 9.6+

### 1. Build Debug APK:
```bash
./gradlew assembleDebug
```

### 2. Install to Device via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Launch App:
```bash
adb shell am start -n uz.developer.privaterecorder/.ui.MainActivity
```

---

## 📄 License & Privacy Disclaimer

This project is developed for personal privacy and security research. Always ensure compliance with your local laws regarding call recording consent before recording conversations.
