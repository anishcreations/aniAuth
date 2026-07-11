# Changelog

All notable changes to the **aniAuth** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.0.0] - 2026-07-11

This is the initial release (Version 0.0.0) of aniAuth.

### Added
- **Core TOTP Calculator**: Full implementation of the RFC 6238 time-based OTP standard (HMAC-SHA1, 30-second step size, 6-digit tokens).
- **Hardware-Backed Cryptography**: Device-bound master encryption key generated inside the Android KeyStore (AES-256-GCM). All raw secret keys are encrypted before writing to disk.
- **Secure Local Storage**: Account metadata stored locally on-device. Excluded from default Android cloud backups and device-to-device transfers to prevent credentials leakage.
- **Biometric Security**: Integrated Android `BiometricPrompt` on app startup to prevent unauthorized access to 2FA codes.
- **High-Speed Scanning**: Google ML Kit Barcode Scanning API integrated for rapid QR code detection.
- **Aesthetic Dark Theme**: Premium "Deep Space" visual layout built with Jetpack Compose, including a real-time circular timer matching the TOTP lifecycle.
- **Password-Secured Backups**: Encryption and decryption engine for portable backup exports utilizing PBKDF2 key derivation and AES-GCM encryption.
- **Compatibility & Migration**: Implemented standard Bitwarden vault JSON import alongside a smart generic JSON parser.
- **Interactive Management**: View and modify account details (Issuer/Label and Name/ID), copy codes via clipboard, reveal the raw secret key, and safely remove accounts.
- **Licensing**: Project released under the open-source MIT License.
