# Add project specific ProGuard rules here.

# ---- aniAuth Wear OS Companion ProGuard Rules ----

# Keep Account data class for JSON serialization
-keep class com.aniauth.authenticator.wearos.model.Account { *; }

# Keep crypto classes (reflection-based KeyStore access)
-keep class com.aniauth.authenticator.wearos.crypto.** { *; }

# Keep WearSyncService (registered in AndroidManifest)
-keep class com.aniauth.authenticator.wearos.sync.WearSyncService { *; }

# Wearable API
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.wearable.**

# Compose (R8 compatible)
-dontwarn androidx.compose.**
