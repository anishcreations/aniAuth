<p align="center">
  <img src="assets/logo.png" alt="aniAuth Logo" width="160" height="160" style="border-radius: 20%;" />
</p>

<h1 align="center">aniAuth</h1>

<p align="center">
  <strong>A minimalist, secure, and aesthetic TOTP authenticator for Android.</strong>
</p>

<p align="center">
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat-square&logo=android" alt="Platform" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat-square&logo=kotlin" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-2024.06.00-4285F4.svg?style=flat-square&logo=jetpackcompose" alt="Compose" /></a>
  <img src="https://img.shields.io/badge/Min_SDK-26-orange.svg?style=flat-square" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target_SDK-35-blue.svg?style=flat-square" alt="Target SDK" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-green.svg?style=flat-square" alt="License" /></a>
</p>

<p align="center">
  aniAuth is fully local-first and privacy-focused, combining a modern dark UI with hardware-backed encryption to keep your 2FA accounts safe and accessible.
</p>

---

> [!NOTE]
> See [CHANGELOG.md](CHANGELOG.md) for detailed version updates and release logs.

## 📖 Table of Contents
- [🚀 Features](#-features)
- [🔒 Security & Threat Model](#-security--threat-model)
- [📂 Codebase Structure](#-codebase-structure)
- [📦 Compatibility & Imports](#-compatibility--imports)
- [💡 How It Works Under the Hood](#-how-it-works-under-the-hood)
- [📄 License](#-license)

---

## 🚀 Features

### ⚡ Seamless Setup
- **High-Speed QR Scanner**: Built using Google ML Kit Vision API for instant QR code scanning.
- **Manual Entry**: Polished form with field validation for adding keys with custom labels and usernames.

### 🔒 Premium Security
- **Hardware-Backed Encryption**: Master encryption keys are stored securely within the device's Secure Element (SE) or Trusted Execution Environment (TEE) via the Android KeyStore.
- **Biometric Access Control**: Optional biometric prompt (fingerprint or face unlock) required on app startup.
- **Zero Network Footprint**: Fully offline. The app requests no internet permission (`android.permission.INTERNET` is omitted from the manifest).

### 🌌 Aesthetic UI/UX
- **Deep Space Theme**: Modern, high-contrast dark theme built with Jetpack Compose Material 3.
- **Dynamic Visual Timer**: A glowing circular progress indicator tracks the 30-second token lifecycle, turning red in the final 5 seconds.
- **One-Tap Copy**: Tap any card to copy the code directly to your clipboard.
- **Adaptive Launcher Icon**: Creative geometric "A" icon designed for modern home screen styling.

---

## 🔒 Security & Threat Model

### 1. Data Encryption (At Rest)
All account data is stored locally in `SharedPreferences`. The raw shared secrets are encrypted using the AES/GCM/NoPadding cipher.
* **Key Generation**: A 256-bit AES master key is generated inside the Android KeyStore using the `KeyGenParameterSpec` builder with GCM block mode and no padding.
* **Key Isolation**: The master key remains isolated in hardware (TEE/SE) and cannot be extracted in plain text by the operating system or other apps.
* **Payload Encryption**: For each account, the secret is encrypted with a unique initialization vector (IV). The IV (12 bytes) and cipher text are combined, Base64-encoded, and saved to disk.

### 2. Encrypted Backups
When exporting backups, data security is maintained through a combination of key derivation and GCM encryption:
* **Key Derivation (KDF)**: A strong 256-bit AES key is derived from the backup password using **PBKDF2WithHmacSHA256** with 10,000 iterations and a cryptographically secure 16-byte salt.
* **Encryption**: The JSON payload (with decrypted secrets) is encrypted with the derived key using **AES/GCM/NoPadding** and a secure 12-byte IV.
* **Export Payload**: The exported file is formatted as a Base64 string containing: `salt (16 bytes) + IV (12 bytes) + encrypted payload`.

---

## 📂 Codebase Structure

```
aniAuth/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aniauth/authenticator/
│   │   │   │   ├── crypto/
│   │   │   │   │   ├── BackupManager.kt      # Encrypted backup & import engine
│   │   │   │   │   ├── KeyStoreHelper.kt     # Hardware-backed AES encryption
│   │   │   │   │   ├── OtpAuthParser.kt      # Parse otpauth:// URIs
│   │   │   │   │   └── TotpGenerator.kt      # RFC 6238 TOTP calculator
│   │   │   │   ├── model/
│   │   │   │   │   ├── Account.kt            # Data model representing a 2FA account
│   │   │   │   │   ├── AccountRepository.kt   # Local storage and CRUD operations
│   │   │   │   │   └── AccountSerializer.kt   # JSON serialization for backup/import
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── AddAccountScreen.kt
│   │   │   │   │   │   ├── BiometricLockScreen.kt
│   │   │   │   │   │   ├── DashboardScreen.kt
│   │   │   │   │   │   ├── ScannerScreen.kt
│   │   │   │   │   │   └── AccountDetailsScreen.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       └── Theme.kt
│   │   │   │   └── MainActivity.kt           # App lifecycle & entry point
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📦 Compatibility & Imports

aniAuth makes migrating from other password managers and authenticators seamless by offering smart imports:
* **Bitwarden Vault Exports**: Import Bitwarden's JSON vaults directly. The app will extract TOTP secrets from the standard `login.totp` field nested inside items.
* **Generic JSON Formats**: The importer automatically parses common properties like `secret`, `key`, `encryptedSecret`, `label`, `name`, `issuer`, and `username` to build the credentials list.
* **Double-Encryption Safety**: On import, plain text secrets are parsed, encrypted via the device's hardware KeyStore immediately, and then written to the database.

---

## 💡 How It Works Under the Hood

### RFC 6238 TOTP Standard
1. The secret key is decoded from its Base32 string representation.
2. The current Unix epoch time is divided by 30 (seconds) to obtain the current time step.
3. The time step is converted into a 64-bit byte buffer and HMAC-SHA1 hashed using the decoded secret key.
4. Dynamic truncation extracts a 4-byte segment from the SHA-1 hash starting at an offset specified by the last 4 bits of the hash.
5. The segment is converted into a 6-digit code using modulo arithmetic (`binary % 1,000,000`).

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
