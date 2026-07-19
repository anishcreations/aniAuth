# aniAuth Wear OS Companion 

Welcome to the dedicated documentation page for the **aniAuth Wear OS Companion** app (`v1.1.0`). 

This companion module allows you to securely view your 2FA TOTP codes directly on your wrist (designed primarily for Galaxy Watch and Wear OS devices).

---

## Table of Contents
- [Key Features](#key-features)
- [How It Works: Architecture](#how-it-works-architecture)
- [Security and Threat Model](#security-and-threat-model)
- [Installation and Syncing Guide](#installation-and-syncing-guide)
- [Codebase Structure](#codebase-structure)
- [Changelog](#changelog)
- [Troubleshooting](#troubleshooting)

---

## Key Features
- **Instant Wrist Codes:** View live 6-digit TOTP codes with a clean, circular Wear OS interface optimized for round screens.
- **Obsidian-Violet Styling:** A beautiful, dark AMOLED theme using the same Obsidian-Violet palette as the phone app (`Color(0xFF08070A)` background, `Color(0xFFC084FC)` accents).
- **Fully Offline After Sync:** Once synced, your watch generates TOTP codes independently using its own local clock and local copy of the RFC 6238 algorithm — no phone or internet connection needed.
- **Zero Scroll Lag:** Employs a self-contained `GlobalTimerHeader` composable (with its own isolated `LaunchedEffect` timer at 500ms intervals) so the account list only recomposes once every 30 seconds when TOTP codes actually change.
- **Biometric-Protected Syncing:** Transfer accounts wirelessly from your phone with a single tap, protected by your phone's biometric/device credential locks.
- **Rotary Bezel Support:** Navigate through your accounts smoothly using your watch's physical rotating bezel or digital crown.
- **Instant Lock & Duress Wipe:** Secures your keys with a 4-digit PIN setup featuring iOS-style circular dot indicators. Locks instantly via `onPause()` on screen dim, backgrounding, or side-button presses. Includes a duress limit that silently wipes accounts after a customizable number of incorrect attempts (configured via the phone app).

---

## How It Works: Architecture

The companion app runs as a completely separate Gradle module (`:wearos`) with its own independent copies of all crypto and model classes — it shares zero runtime code with the phone `:app` module. The two apps communicate exclusively over Google's **Wearable Data Layer API**.

### Sync Flow (Phone → Watch)

```
Phone App                              Bluetooth                    Watch App
─────────                              ─────────                    ─────────
1. User taps "Sync to Watch"
   in Settings
           │
2. Biometric/credential
   verification required
           │
3. AccountSerializer.toJson(           ──────────────────►   5. WearSyncService receives
   accounts, decryptSecrets=true)       sendMessage()            message on "/sync-accounts"
   produces JSON with plaintext         via Wearable             path
   Base32 secrets                       MessageClient
           │                                                         │
4. Payload sent over                                          6. JSON parsed, each secret
   "/sync-accounts" path                                         validated via
                                                                 TotpGenerator.isValidSecret()
                                                                     │
                                                              7. Valid secrets encrypted with
                                                                 watch's OWN KeyStore master
                                                                 key (AniAuthWatchMasterKey)
                                                                     │
                                                              8. Encrypted accounts saved to
                                                                 watch SharedPreferences
                                                                 (file: wear_accounts_pref)
                                                                     │
                                                              9. Toast confirmation shown
                                                                 with sync count
```

### Key Technical Details
- **Matching Application ID:** Both apps use `com.aniauth.authenticator` as their `applicationId` and are signed with the same developer key. This is required by Google Play Services for Bluetooth pairing.
- **Watch Presence Detection:** The phone app queries `Wearable.getNodeClient(context).connectedNodes` in the background. The "Sync to Watch" row in Settings only renders when at least one connected watch node is detected.
- **Message Path:** Data is transmitted via `Wearable.getMessageClient().sendMessage(nodeId, "/sync-accounts", jsonBytes)` over the secure Bluetooth data layer.
- **WearableListenerService:** The watch registers `WearSyncService` in its `AndroidManifest.xml` as a receiver bound to the Wearable message intent filter. The service wakes automatically when a message arrives on the `/sync-accounts` path.
- **Re-encryption on Watch:** The phone decrypts secrets from its own KeyStore (`AniAuthMasterKey`) before sending. The watch then encrypts each secret with its own independent KeyStore key (`AniAuthWatchMasterKey`). This means the phone's and watch's encrypted blobs are completely different — neither can decrypt the other's data.

### Offline Operation
After the initial sync, the watch is 100% self-sufficient:
- Accounts are stored locally in `SharedPreferences` on the watch's internal storage.
- TOTP codes are computed locally using the watch's own `TotpGenerator` and its internal system clock.
- You can turn off Bluetooth, power off your phone, or go offline — your watch will continue generating valid codes indefinitely.

---

## Security and Threat Model

### What happens if someone steals my watch?

**Your 2FA keys remain completely secure.** Here's why:

1. **Hardware-Backed Encryption (TEE/SE):**
   All account secrets are encrypted with **AES-256-GCM** using a master key (alias: `AniAuthWatchMasterKey`) generated inside the watch's **Android KeyStore hardware enclave (TEE or Secure Element)**. This master key:
   - Is generated inside the secure hardware and never leaves it.
   - Cannot be exported, copied, dumped, or extracted — even with root/ADB access.
   - Is bound to the specific watch hardware. Moving the encrypted `SharedPreferences` file to another device renders it unreadable.

2. **Per-Secret Unique IV:**
   Each account secret is encrypted with a cryptographically random **12-byte IV** generated by the AES-GCM cipher. Even if two accounts have the same Base32 secret, their encrypted blobs will be completely different.

3. **Instant Auto-Lock:**
   The app hooks into `ComponentActivity.onPause()`. The moment you:
   - Look away (screen timeout)
   - Press the side/home button
   - Switch to another app
   - Tilt your wrist away (Always-On Display triggers `onPause`)
   
   ...the app immediately resets `isUnlocked` to `false`, requiring the 4-digit PIN to re-enter.

4. **In-Memory PIN Caching (Performance, Not Security Bypass):**
   The PIN is decrypted from KeyStore once during `onCreate()` and cached in memory (`cachedCorrectPin`). This eliminates the ~200ms hardware KeyStore decryption delay during PIN entry. The cache is automatically cleared when the process is killed (e.g., by the system or on reboot).

5. **Android App Sandbox:**
   Android enforces strict process isolation. No other app on the watch can read aniAuth's private `SharedPreferences` files or access its KeyStore keys.

6. **No Raw Key Display:**
   Unlike the phone app (which allows viewing decrypted secrets via long-press), the watch UI has zero code paths that reveal or display raw Base32 secret keys. The UI only receives computed 6-digit TOTP codes.

### Threat Summary

| Threat | Mitigated? | How |
|--------|-----------|-----|
| Physical theft of watch | ✅ | PIN lock + hardware-bound KeyStore encryption |
| ADB extraction of SharedPreferences | ✅ | Data is AES-GCM encrypted; master key is non-exportable |
| Rooted watch / custom ROM | ✅ | KeyStore keys are in TEE/SE hardware; cannot be read by software |
| Bluetooth interception during sync | ✅ | Wearable Data Layer uses encrypted BLE; requires paired devices |
| Shoulder-surfing TOTP codes | ⚠️ | 6-digit codes rotate every 30s; limited window of usefulness |

---

## Installation and Syncing Guide

### Watch Installation Guide
You can install the Wear OS companion app using one of the following methods:

#### Method 1: Sideloading Pre-built APK via ADB over Wi-Fi
If you downloaded the pre-built `aniAuth-wear-v1.0.0.apk` from GitHub Releases, you can install it using a terminal/command prompt:

1. **Install Android Platform Tools (ADB)**:
   - **macOS**: Install using Homebrew:
     ```bash
     brew install android-platform-tools
     ```
   - **Windows***: Download the official [SDK Platform-Tools for Windows](https://developer.android.com/tools/releases/platform-tools), extract the zip, and open Command Prompt/PowerShell inside the extracted folder.
   - **Linux***: Install via your package manager:
     ```bash
     # Debian / Ubuntu
     sudo apt install android-tools-adb
     # Fedora / RHEL
     sudo dnf install android-tools
     ```
2. **Enable Developer Options on your Watch**:
   - On your Watch, go to **Settings** > **System** > **About**.
   - Scroll down and tap **Build number** 7 times until you see a prompt: *"Developer options enabled"*.
3. **Enable Wi-Fi/Wireless Debugging**:
   - Go back to **Settings** > **Developer options**.
   - Turn on **ADB debugging** and **Wireless debugging**.
   - Make sure both your Watch and your Mac are connected to the **same Wi-Fi network**.
4. **Connect your Mac to the Watch**:
   - Tap **Wireless debugging** on the watch to view your watch's IP address and Port (e.g. `192.168.1.100:5555`).
   - Open Terminal on your Mac and connect by running:
     ```bash
     adb connect <watch-ip>:<port>
     ```
   - Keep an eye on your watch face and select **Always allow from this computer** when the authorization prompt pops up.
5. **Install the watch APK**:
   - Run the install command with the path to your downloaded watch APK:
     ```bash
     adb install aniAuth-wear-v1.0.0.apk
     ```

#### Method 2: Running via Android Studio (From Source)
1. Open the **aniAuth** project folder in **Android Studio**.
2. Pair/connect your watch via Wireless Debugging in the Android Studio Device Manager.
3. Select `:wearos` in the module build configurations dropdown at the top.
4. Select your watch as the destination device and click the green **Run** (Play) button.

---

### First-Time Watch Setup
1. Open the **aniAuth** app on your Watch.
2. You will be prompted to **create a 4-digit security PIN**.
3. Confirm the PIN. You will see a clean empty dashboard: *"No accounts synced. Tap 'Sync to Watch' on your phone app."*

### Syncing Accounts
4. Open the aniAuth phone app → **Settings** → scroll to *Data & Backup*.
5. Tap **Sync to Watch** (only visible when a watch is connected via Bluetooth).
6. Verify your identity with fingerprint/face/device credentials.
7. Your watch will display a Toast confirmation and instantly populate with your 2FA accounts!

### Re-Syncing
- You can re-sync at any time to update the watch with new accounts added on the phone.
- Each sync **replaces** the entire watch database with the latest phone data.

---

## Codebase Structure

```
wearos/
├── src/main/
│   ├── java/com/aniauth/authenticator/wearos/
│   │   ├── crypto/
│   │   │   ├── KeyStoreHelper.kt        # AES-256-GCM encryption (key: AniAuthWatchMasterKey)
│   │   │   │                            #   - getOrCreateSecretKey(): generates/retrieves TEE key
│   │   │   │                            #   - encrypt(): IV(12) + ciphertext → Base64
│   │   │   │                            #   - decrypt(): Base64 → plaintext (with ConcurrentHashMap cache)
│   │   │   └── TotpGenerator.kt         # Standalone RFC 6238 implementation
│   │   │                                #   - generateTOTP(): Base32 → HMAC-SHA1 → 6-digit code
│   │   │                                #   - isValidSecret(): validates Base32 format
│   │   ├── model/
│   │   │   ├── Account.kt              # data class: id, label, encryptedSecret, issuer?, username?
│   │   │   └── WatchRepository.kt      # SharedPreferences CRUD (file: wear_accounts_pref)
│   │   │                                #   - decryptCached(): ConcurrentHashMap in-memory cache
│   │   ├── sync/
│   │   │   └── WearSyncService.kt      # WearableListenerService on "/sync-accounts" path
│   │   │                                #   - Validates Base32 keys
│   │   │                                #   - Re-encrypts with watch KeyStore
│   │   │                                #   - Saves to local SharedPreferences
│   │   └── MainActivity.kt             # Watch UI entry point
│   │                                    #   - PinSetupScreen: create 4-digit PIN with dot indicators
│   │                                    #   - PinLockScreen: unlock with cached PIN verification
│   │                                    #   - WatchDashboard: ScalingLazyColumn account list
│   │                                    #   - GlobalTimerHeader: isolated 30s countdown ring
│   │                                    #   - WatchKeypad: lightweight Box-based grid buttons
│   │                                    #   - WatchAccountCard: label + live TOTP code display
│   ├── res/
│   │   ├── mipmap-anydpi-v26/          # Adaptive launcher icons
│   │   └── values/colors.xml
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Changelog

### [1.1.0] (2026-07-17)
*(Last updated: 2026-07-20 - docs & sorting sync)*

- **Synchronized sorting order alignment**: Automatically inherits the dynamic account sorting order (Date Added, Alphabetical A-Z, or Alphabetical Z-A) chosen on the phone app during synchronization (Requires Phone companion app v1.5.0 or higher).

- **Rotary Bezel Scrolling**: Added support for physical rotary dial/RSB and bezel rotation on Wear OS devices (like Galaxy Watch) using Jetpack Compose FocusRequester.
- **Duress PIN & Silent Wipe**: Implemented duress protection that silently clears all synced accounts behind the scenes after a customizable number of incorrect PIN attempts, displaying a "Reset PIN" visual option at the bottom to recreate the passcode.
- **Centered Layout**: Centered the setup and lock screens UI.
- **Auto-Sync Reliability**: Moved the SharedPreferences observer to a strong activity-level reference, resolving a garbage collection bug that disrupted real-time dashboard updates.

### [1.0.0] (2026-07-14) — Initial Release

First official release of the aniAuth Wear OS companion app, featuring:
- **AMOLED-Optimized Dashboard**: Displays synced 2FA codes in a clean list with a circular count-down timer.
- **4-Digit PIN Lock**: Secure lock screen featuring glowing passcode dots and instant locking on screen timeout/dim.
- **Lightweight Keypad**: Replaced heavy Material buttons with compact grid keys for snappy entry.
- **Re-encryption Engine**: Re-encrypts incoming secrets with watch-specific hardware-backed KeyStore master key (AES-256-GCM).
- **Offline Code Generation**: Computes TOTP codes locally on your wrist, working fully offline without active Bluetooth.

---

## Troubleshooting

### The "Sync to Watch" option doesn't show up in phone Settings!
- Make sure **Bluetooth** is enabled on both devices.
- *Ensure your watch is connected and paired via the **Galaxy Wearable** or **Wear OS by Google** companion app.
- Both the phone and watch modules must share the exact same **Application ID** (`com.aniauth.authenticator`) and be signed with the **same developer certificate key**. If the signing keys are mismatched, Google Play Services will block pairing communication for security reasons.

### Sync completes but watch shows "No accounts synced"
- Check that you have at least one account on the phone app.
- Look for a Toast message on the watch — if it says "Skipped X invalid keys", some accounts may have corrupted or non-Base32 secrets.

### Codes on watch don't match codes on phone
- Ensure your watch's **system clock is accurate**. TOTP codes are time-based — even a 30-second drift will produce different codes.
- Go to **Settings → General → Date and Time** on your watch and enable **Automatic date and time**.
