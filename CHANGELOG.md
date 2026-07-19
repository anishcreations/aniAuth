# Changelog

All notable changes to the **aniAuth** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---
* 📱 **Latest Released (Phone)**: [v1.3.0](https://github.com/anishcreations/aniAuth/releases/tag/v1.4.0)
* ⌚ **Latest Released (Wear OS)**: [v1.0.0](https://github.com/anishcreations/aniAuth/releases#release-wear-v1.1.0) 
> **To check full Watch Changelog: [Click Here](WEAROS.md#changelog)**

## [1.5.0] (2026-07-20)

### Added
- **Account Sorting**: Persistent, dynamic account sorting options (Default/Date Added, Alphabetical A-Z, Alphabetical Z-A) with a dedicated top bar dropdown menu.
- **Fast Scroll Alphabet Sidebar**: Interactive vertical alphabet scroll slider overlayed on the right screen edge for quickly jumping to accounts by their first letter.
- **Scroll-Aware Dynamic Visibility**: Configure the sidebar to remain at an extremely faint idle opacity (`0.05f`) when the screen is stationary, automatically lighting up to full active opacity (`0.4f`) when scrolling or hovering, then fading out after a short delay.
- **Floating Letter Indicator**: Overlayed glassmorphic floating letter bubble in the center of the dashboard providing real-time visual feedback of the currently active letter during slides.
- **Z-A Slider Reversal**: Automatic reversing of the alphabet slider layout (Z to A) when "Alphabetical (Z-A)" sorting is enabled, ensuring intuitive top-to-bottom scroll direction.
- **Robust Distance-Based Fallback Search**: Dynamic search algorithm that scrolls to the mathematically closest alphabetical character when an exact match for the selected letter doesn't exist, working seamlessly across all sorting modes.

### Changed
- **Symmetric List Layout & Padding**: Transitioned to an overlay layout for the alphabet sidebar, keeping accounts list cards centered and full-width, with a 6dp margin separation.
- **Reduced Footer Empty Space**: Decreased the dashboard list footer's bottom blank padding from 72dp to 36dp for a tighter scroll finish.

## [1.4.0] (2026-07-17)

### Added
- **Wear OS Watch Settings Group**: Extracted all watch-related configurations into a dedicated "Wear OS Watch" settings section, shown dynamically only when a watch connection is active.
- **Watch PIN Trial Limits Setting**: Added a configuration option to set the maximum failed PIN attempts allowed on the watch (3-default, 6, 9, or Unlimited (999)) before data is silently wiped.
- **Jetpack Compose Back Handlers**: Added native back gesture/button interceptors to Settings, Manual, Scanner, Add Account, and Account Details overlays to return cleanly to the dashboard instead of backgrounding the app.
- **Background Autolock**: Configured the app to automatically lock and require biometric re-verification when brought back from the background/minimized state, with smart exclusions when navigating to the system file picker.

### Wear OS Update
- **Watch Companion Versioning**: Upgraded the Wear OS companion module to [`v1.1.0`](WEAROS.md#110-2026-07-17), integrating rotary bezel scroll support, silent duress database wiping, curved-edge layout optimizations, and empty state focus crash fixes.

## [1.3.0] (2026-07-15)
*(Last updated: 2026-07-16)*

### Added
- **Wear OS Companion App Support**: Integrated the brand new `:wearos` watch module (`v1.0.0`) to view live 2FA codes directly on Galaxy/Wear OS (WEAROS.md) watches.
- **Watch Sync Integration**: Added an opt-in "Sync to Watch" action in the phone's Settings screen (visible when a watch node is detected), protected by biometric authentication, to transmit accounts securely via the Google Wearable Data Layer.
- **App Version Footers**: Added clickable version footer links directly on both the phone settings screen and the main dashboard list.
- **Documentation**: Created [WEAROS.md](WEAROS.md) (detailing companion app setup, TEE KeyStore encryption models, offline sync mechanics, and troubleshooting), added prominent release badges to the README, and introduced a detailed Wear OS wireless ADB sideloading guide.

### Optimized
- **APK Size & Packaging**: Switched to Play Services ML Kit to save **~10MB**. Enabled R8 code minification, resource shrinking, and English-only resource filtering to minimize the app footprint.
- **KeyStore Speedups**: Configured `KeyStoreHelper` to cache the `SecretKey` reference in-memory after loading, eliminating JNI overhead and hardware-enclave lookup on subsequent cryptographic calls.
- **Compose Recomposition Reductions**: Cached the search-filtered accounts list via `remember` on the phone app and reduced timer polling to 1000ms, removing redundant layout passes.

## [1.2.0] (2026-07-14)

### Added
- **Export Options**: Added ability to choose between secure Encrypted Backup (AES-256 encrypted with aniAuth's internal key) and Decrypted Backup (plaintext JSON).
- **Export Verification**: Required identity verification (biometrics or device screen lock credentials) before executing the backup export flow to prevent unauthorized extraction of 2FA keys.
- **Device Credential Fallback**: Updated biometric prompt lock screen to support device PIN, pattern, or password lock screen as a native fallback in case biometric authentication fails or biometric hardware is unavailable.
- **Minimal Empty Dashboard State**: Hidden the Search & Timer pill header when the accounts list is completely empty, replacing it with a minimal top placeholder row that includes a direct "Import" action button.
- **Performance Fix**: Fixed scrolling lag via in-memory KeyStore decryption caching and rate-limiting TOTP updates to only trigger on 30-second epoch ticks.

### Fixed
- **Backup Key Serialization**: Fixed backup key serialization issues to ensure exported (this version and onwards) backups can be re-imported and restored.

---

## [1.1.0] (2026-07-13)

A major update introducing proper Light and Dark mode theme support, premium Obsidian-Violet dark mode UI, interactive theme settings dialog, and various visual design refinements.

### Added
- **Changelog Link**: Added a clickable changelog link to the settings footer.
- **Delete Confirmation Dialog**: Added a confirmation popup when deleting an account to prevent accidental deletions.
- **Proper Light & Dark Theme Support**: Dynamically switch theme settings between System Default, Light Mode, and Dark Mode.
- **Obsidian-Violet Dark Mode**: Refined the default dark theme with a warm, deep black-violet backdrop (`0xFF08070A`) and cards (`0xFF120E1A`) for a rich, premium look.
- **Theme Selection Dialog**: Interactive popup dialog in settings with radio buttons for custom theme selection.
- **Adaptive Footer Styling**: The "View User Manual & Tips" scroll footer button automatically adjusts colors (dynamic `SoftFooterColor`) and uses softer text formatting and transparency depending on the active theme.
- **Backup Instruction in Manual**: Appended a new guide item to the user manual detailing how to export and import backups.
- **Import Guidance in Empty State**: Added clear instructions in the dashboard's empty state directing users to Settings to import keys when no accounts are found.

### Changed
- **Unified Dialog Styling**: Standardized the User Manual dialog confirm button to match the rest of the application's clean text-button design.
- **Compacted Spacing**: Improved the layout efficiency of the theme picker dialog by reducing vertical row padding and column gaps.
- **FAB Translucency**: Muted the floating action buttons (`+` and `scan`) with a soft transparency on dark theme to improve visual balance.

---

## [1.0.0] (2026-07-13)

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

## [0.1.0] (2026-07-12)

Pre-release update hardening the TOTP algorithm and introducing adaptive icons.

### Added
- **TOTP Testing Harness**: Integrated JUnit testing with official RFC 6238 test vectors to guarantee algorithm precision.

### Changed
- **Adaptive App Icons**: Configured dual launcher icon assets (main icon without label, and zoomed-out variant for the splash screen banner to resolve text-clipping issues).
- **Hardened TOTP Algorithm**: Fixed potential integer overflow and sign extension edge cases in the Base32 decoding loop via bitwise masking.
- **Robust Key Sanitization**: Upgraded input handling to strip dashes (`-`) and all forms of whitespace (tabs, newlines) from copy-pasted secret keys.

---

## [0.0.0] (2026-07-11)

Initial pre-release development version.

### Added
- **Core TOTP Calculator**: Full implementation of the RFC 6238 time-based OTP standard (HMAC-SHA1, 30-second step size, 6-digit tokens).
- **Hardware-Backed Cryptography**: Device-bound master encryption key generated inside the Android KeyStore (AES-256-GCM) to encrypt all secret keys before writing to disk.
- **Secure Local Storage**: Account metadata stored locally on-device. Excluded from default Android cloud backups and device-to-device transfers to prevent credentials leakage.
- **Biometric Security**: Integrated Android `BiometricPrompt` on app startup to prevent unauthorized access to 2FA codes.
- **High-Speed Scanning & Import**: Google ML Kit Barcode Scanning API integrated for rapid QR code detection, alongside Bitwarden and generic JSON vault imports.
- **Aesthetic Dark Theme**: Premium "Deep Space" visual layout built with Jetpack Compose, including a real-time circular timer matching the TOTP lifecycle.
- **Password-Secured Backups**: Encryption and decryption engine for portable backup exports utilizing PBKDF2 key derivation and AES-GCM encryption.


