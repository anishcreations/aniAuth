# Add project specific ProGuard rules here.

# ---- aniAuth Phone App ProGuard Rules ----

# Keep Account data class for JSON serialization
-keep class com.aniauth.authenticator.model.Account { *; }

# Keep crypto classes (reflection-based KeyStore access)
-keep class com.aniauth.authenticator.crypto.** { *; }

# ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-keep class androidx.camera.** { *; }

# Wearable API
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.wearable.**

# Compose (R8 compatible)
-dontwarn androidx.compose.**

# Biometrics
-keep class androidx.biometric.** { *; }
