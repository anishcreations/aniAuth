package com.aniauth.authenticator

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import com.google.android.gms.wearable.Wearable
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.aniauth.authenticator.crypto.BackupManager
import com.aniauth.authenticator.crypto.KeyStoreHelper
import com.aniauth.authenticator.model.AccountRepository
import com.aniauth.authenticator.ui.screens.BiometricLockScreen
import com.aniauth.authenticator.ui.screens.DashboardScreen
import com.aniauth.authenticator.ui.theme.AniAuthTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var repository: AccountRepository
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    private val authenticationSession = AuthenticationSession()

    private var isAppLocked by mutableStateOf(false)
    private var isUnlocked by mutableStateOf(false)
    private var isScreenSecurityEnabled by mutableStateOf(true)
    private var themeSetting by mutableStateOf("system")
    private var pendingExportType: String? = null
    private var isWatchConnected by mutableStateOf(false)
    private var isNavigatingForFile = false

    // Default backup password key
    private val backupPassword = "aniauth_secure_pass".toCharArray()

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val accounts = repository.getAccounts()
                    val exportJson = com.aniauth.authenticator.model.AccountSerializer.toJson(accounts, decryptSecrets = true)
                    val contentToWrite = if (pendingExportType == "decrypted") {
                        exportJson
                    } else {
                        BackupManager.encrypt(exportJson, backupPassword)
                    }
                    outputStream.write(contentToWrite.toByteArray(Charsets.UTF_8))
                    Toast.makeText(this, "Backup exported successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val fileContent = reader.readText()
                    
                    // 1. Try decrypting as an internal aniAuth backup
                    var decryptedJson = BackupManager.decrypt(fileContent, backupPassword)
                    
                    // 2. If decryption fails, treat as one-way plain JSON import (Bitwarden, etc.)
                    if (decryptedJson == null) {
                        if (fileContent.trim().startsWith("{") || fileContent.trim().startsWith("[")) {
                            decryptedJson = fileContent
                        }
                    }

                    if (decryptedJson == null) {
                        Toast.makeText(this, "Failed to parse file. Invalid password or format.", Toast.LENGTH_LONG).show()
                        return@use
                    }
                    
                    val importedAccounts = com.aniauth.authenticator.model.AccountSerializer.fromJson(decryptedJson)
                    if (importedAccounts.isEmpty()) {
                        Toast.makeText(this, "No valid accounts found.", Toast.LENGTH_LONG).show()
                        return@use
                    }
                    
                    // 3. Smart Merge and Re-encryption
                    val currentAccounts = repository.getAccounts().toMutableList()
                    var mergedCount = 0
                    var invalidCount = 0
                    
                    for (imp in importedAccounts) {
                        val importedSecret = imp.encryptedSecret
                        
                        // Smart Check: If the imported secret was encrypted with this device's KeyStore 
                        // (from an older format backup), decrypt it to get the raw secret. Otherwise, 
                        // treat it directly as a plaintext secret.
                        val plainSecret = KeyStoreHelper.decrypt(importedSecret) ?: importedSecret
                        
                        // Security Safeguard: Validate if it is a valid Base32 secret
                        if (!com.aniauth.authenticator.crypto.TotpGenerator.isValidSecret(plainSecret)) {
                            invalidCount++
                            continue
                        }
                        
                        // Prevent duplicates by checking if decrypted secret matches
                        val isDuplicate = currentAccounts.any { curr ->
                            val decryptedCurr = KeyStoreHelper.decrypt(curr.encryptedSecret) ?: ""
                            decryptedCurr == plainSecret && curr.label == imp.label
                        }
                        
                        if (!isDuplicate) {
                            val reEncrypted = KeyStoreHelper.encrypt(plainSecret)
                            currentAccounts.add(imp.copy(encryptedSecret = reEncrypted))
                            mergedCount++
                        }
                    }
                    
                    repository.saveAccounts(currentAccounts)
                    if (invalidCount > 0) {
                        Toast.makeText(this, "Imported $mergedCount accounts. Skipped $invalidCount invalid/device-locked keys.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Successfully imported $mergedCount new accounts!", Toast.LENGTH_LONG).show()
                    }
                    
                    // Refresh UI
                    setContent { AppContent() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AccountRepository(this)
        
        val prefs = getSharedPreferences("ani_auth_prefs", MODE_PRIVATE)
        isAppLocked = prefs.getBoolean("app_lock_enabled", false)
        isScreenSecurityEnabled = prefs.getBoolean("screen_security_enabled", true)
        themeSetting = prefs.getString("theme_setting", "system") ?: "system"

        updateScreenSecurity(isScreenSecurityEnabled)
        setupBiometrics()
        setContent { AppContent() }
    }

    override fun onResume() {
        super.onResume()
        checkWatchConnection()
        val returningFromFile = isNavigatingForFile
        if (returningFromFile) isNavigatingForFile = false
        authenticationSession.onResume()

        if (!returningFromFile && isAppLocked && !isUnlocked && !authenticationSession.isInProgress) {
            authenticateBiometric()
        }
    }

    override fun onPause() {
        super.onPause()
        authenticationSession.onPause()
        // AndroidX owns prompt cancellation. Cancelling here also cancels the system PIN screen.
        if (isAppLocked && !isNavigatingForFile && !authenticationSession.isInProgress) {
            isUnlocked = false
        }
    }

    override fun onDestroy() {
        authenticationSession.onError()
        super.onDestroy()
    }

    private fun checkWatchConnection() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                isWatchConnected = nodes.isNotEmpty()
            }
            .addOnFailureListener {
                isWatchConnected = false
            }
    }

    private fun syncToWatch() {
        verifyIdentityBeforeAction(
            title = "Sync to Watch",
            subtitle = "Verify identity to synchronize accounts"
        ) {
            val accounts = repository.getAccounts()
            val sortOrder = repository.getSortOrder()
            val sortedAccounts = when (sortOrder) {
                "alphabetical" -> accounts.sortedBy { it.label.lowercase() }
                "alphabetical_desc" -> accounts.sortedByDescending { it.label.lowercase() }
                else -> accounts
            }
            val accountsJson = com.aniauth.authenticator.model.AccountSerializer.toJson(sortedAccounts, decryptSecrets = true)
            
            // Retrieve max attempts setting
            val maxAttempts = getSharedPreferences("ani_auth_prefs", MODE_PRIVATE)
                .getInt("watch_max_failed_attempts", 3)
            
            // Build combined JSON payload
            val combinedObj = org.json.JSONObject()
            combinedObj.put("accounts", org.json.JSONArray(accountsJson))
            combinedObj.put("maxFailedAttempts", maxAttempts)
            
            val bytes = combinedObj.toString().toByteArray(StandardCharsets.UTF_8)
            
            Wearable.getNodeClient(this).connectedNodes
                .addOnSuccessListener { nodes ->
                    if (nodes.isEmpty()) {
                        Toast.makeText(this, "No connected watch found.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    
                    var sentCount = 0
                    for (node in nodes) {
                        Wearable.getMessageClient(this)
                            .sendMessage(node.id, "/sync-accounts", bytes)
                            .addOnSuccessListener {
                                sentCount++
                                if (sentCount == nodes.size) {
                                    Toast.makeText(this, "Sync request sent to watch!", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to send to watch: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error finding watch: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @Composable
    private fun AppContent() {
        val darkTheme = when (themeSetting) {
            "light" -> false
            "dark" -> true
            else -> androidx.compose.foundation.isSystemInDarkTheme()
        }
        AniAuthTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (isAppLocked && !isUnlocked) {
                    BiometricLockScreen(onUnlockClick = { authenticateBiometric() })
                } else {
                    DashboardScreen(
                        repository = repository,
                        onExportBackup = { isEncrypted -> triggerExport(isEncrypted) },
                        onImportBackup = { triggerImport() },
                        onToggleLock = { enabled ->
                            if (enabled) {
                                verifyIdentityBeforeAction(
                                    title = "Enable Biometric Lock",
                                    subtitle = "Verify identity to enable app protection"
                                ) {
                                    updateAppLock(true)
                                }
                            } else {
                                updateAppLock(false)
                            }
                        },
                        isLocked = isAppLocked,
                        themeSetting = themeSetting,
                        onThemeChange = { newTheme ->
                            getSharedPreferences("ani_auth_prefs", MODE_PRIVATE).edit {
                                putString("theme_setting", newTheme)
                            }
                            themeSetting = newTheme
                        },
                        isWatchConnected = isWatchConnected,
                        onSyncToWatch = { syncToWatch() },
                        isScreenSecurityEnabled = isScreenSecurityEnabled,
                        onToggleScreenSecurity = { enabled ->
                            getSharedPreferences("ani_auth_prefs", MODE_PRIVATE).edit {
                                putBoolean("screen_security_enabled", enabled)
                            }
                            isScreenSecurityEnabled = enabled
                            updateScreenSecurity(enabled)
                        },
                        onVerifyIdentity = { title, subtitle, onSuccess ->
                            verifyIdentityBeforeAction(title, subtitle, onSuccess)
                        }
                    )
                }
            }
        }
    }

    private fun updateAppLock(enabled: Boolean) {
        getSharedPreferences("ani_auth_prefs", MODE_PRIVATE).edit {
            putBoolean("app_lock_enabled", enabled)
        }
        isAppLocked = enabled
        // Enabling only reaches here after successful authentication; keep this session unlocked.
        isUnlocked = enabled
    }

    private fun updateScreenSecurity(enabled: Boolean) {
        if (enabled) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun verifyIdentityBeforeAction(title: String, subtitle: String, onSuccess: () -> Unit) {
        if (authenticationSession.isInProgress || !checkAuthenticationAvailable()) return
        authenticationSession.authenticate(
            action = {
                // A successful credential prompt also verifies the app lock after an activity pause.
                if (isAppLocked) isUnlocked = true
                onSuccess()
            },
            launchPrompt = {
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .build()
                biometricPrompt.authenticate(info)
            },
            onLaunchError = {
                Toast.makeText(this, "Could not start authentication. Try again or use your device PIN, pattern, or password.", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun checkAuthenticationAvailable(): Boolean {
        val status = BiometricManager.from(this).canAuthenticate(allowedAuthenticators)
        val message = when (status) {
            BiometricManager.BIOMETRIC_SUCCESS,
            // Some older devices cannot determine availability; let the system prompt try safely.
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> return true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Set up a supported face/fingerprint or device PIN, pattern, or password in Android Settings."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Authentication is temporarily unavailable. Try again when your device security is available."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "No supported biometrics or device credentials are available. Set up a PIN, pattern, or password in Android Settings."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "Install your device's security update to use authentication."
            else -> "Authentication is not supported on this device. Check your Android security settings."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        return false
    }

    private fun triggerExport(isEncrypted: Boolean) {
        verifyIdentityBeforeAction(
            title = "Verify Identity",
            subtitle = "Confirm security credentials to export accounts"
        ) {
            pendingExportType = if (isEncrypted) "encrypted" else "decrypted"
            val filename = if (isEncrypted) "aniauth_backup.json" else "aniauth_decrypted_backup.json"
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, filename)
            }
            isNavigatingForFile = true
            createDocumentLauncher.launch(intent)
        }
    }

    private fun triggerImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        isNavigatingForFile = true
        openDocumentLauncher.launch(intent)
    }

    private fun setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authenticationSession.onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (isAppLocked && (authenticationSession.isPaused ||
                            authenticationSession.wasPausedDuringAuthentication)) isUnlocked = false
                    authenticationSession.onError()
                    if (errorCode != BiometricPrompt.ERROR_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(this@MainActivity, errString, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun authenticateBiometric() {
        verifyIdentityBeforeAction("Unlock aniAuth", "Confirm identity to view codes") {
            isUnlocked = true
        }
    }
}
