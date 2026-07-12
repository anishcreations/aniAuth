# Changelog

All notable changes to the **aniAuth** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0]

A major upgrade introducing a feature-rich settings dashboard, interactive user manual, unified search-and-timer header, and a space-saving side-by-side clean card layout.

### Added
- **Dedicated Settings Screen**: Centralized, feature-rich hub containing help sections, data management(import/export), biometric security, custom link, repository information, disclaimer, and privacy policy dialogs.
- **Dynamic User Manual**: Accessible guide detailing app shortcuts, security mechanisms, and usage instructions.
- **Integrated Search & Timer**: Unified search bar and countdown timer in a sleek pill header. Displays a soft-toned refresh message that collapses into an active search field with automatic keyboard focus on click.
- **Footer Interaction**: A dedicated interactive footer element at the bottom of the list for quick manual access.

### Changed
- **UI/UX Overhaul**: Transitioned to a clean, compact top bar and a condensed dashboard to optimize screen space.
- **Slimmer Card Layout**: Repositioned account metadata and OTP codes side-by-side with fixed card heights and rounded corners, saving vertical space.
- **Text Truncation & Fitting**: Integrated ellipsis overflow handling for issuer and username labels to prevent layout clipping.
- **License**: Transitioned the project license to the **Apache License 2.0**.
- **Settings Migration**: Moved management tools (Import/Export/Lock) to the Settings screen to make the dashboard cleaner and more organized.

### Removed
- **Redundant Copy Icon**: Removed the copy icon button from account cards to simplify and slim down the visual design.

---

## [0.1.0]

Pre-release update hardening the TOTP algorithm and introducing adaptive icons.

### Added
- **TOTP Testing Harness**: Integrated JUnit testing with official RFC 6238 test vectors to guarantee algorithm precision.

### Changed
- **Adaptive App Icons**: Configured dual launcher icon assets (main icon without label, and zoomed-out variant for the splash screen banner to resolve text-clipping issues).
- **Hardened TOTP Algorithm**: Fixed potential integer overflow and sign extension edge cases in the Base32 decoding loop via bitwise masking.
- **Robust Key Sanitization**: Upgraded input handling to strip dashes (`-`) and all forms of whitespace (tabs, newlines) from copy-pasted secret keys.

---

## [0.0.0]

Initial pre-release development version.

### Added
- **Core TOTP Calculator**: Full implementation of the RFC 6238 time-based OTP standard (HMAC-SHA1, 30-second step size, 6-digit tokens).
- **Hardware-Backed Cryptography**: Device-bound master encryption key generated inside the Android KeyStore (AES-256-GCM) to encrypt all secret keys before writing to disk.
- **Secure Local Storage**: Account metadata stored locally on-device. Excluded from default Android cloud backups and device-to-device transfers to prevent credentials leakage.
- **Biometric Security**: Integrated Android `BiometricPrompt` on app startup to prevent unauthorized access to 2FA codes.
- **High-Speed Scanning & Import**: Google ML Kit Barcode Scanning API integrated for rapid QR code detection, alongside Bitwarden and generic JSON vault imports.
- **Aesthetic Dark Theme**: Premium "Deep Space" visual layout built with Jetpack Compose, including a real-time circular timer matching the TOTP lifecycle.
- **Password-Secured Backups**: Encryption and decryption engine for portable backup exports utilizing PBKDF2 key derivation and AES-GCM encryption.


