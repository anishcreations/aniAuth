package com.aniauth.authenticator

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private var isAppLocked by mutableStateOf(false)
    private var isUnlocked by mutableStateOf(false)
    private var themeSetting by mutableStateOf("system")
    private var pendingExportType: String? = null

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
        themeSetting = prefs.getString("theme_setting", "system") ?: "system"

        setupBiometrics()
        setContent { AppContent() }

        if (isAppLocked) {
            authenticateBiometric()
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
                            getSharedPreferences("ani_auth_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("app_lock_enabled", enabled)
                                .apply()
                            isAppLocked = enabled
                            if (enabled) isUnlocked = false
                        },
                        isLocked = isAppLocked,
                        themeSetting = themeSetting,
                        onThemeChange = { newTheme ->
                            getSharedPreferences("ani_auth_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("theme_setting", newTheme)
                                .apply()
                            themeSetting = newTheme
                        }
                    )
                }
            }
        }
    }

    private fun verifyIdentityBeforeAction(title: String, subtitle: String, onSuccess: () -> Unit) {
        val exec = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, exec, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        try {
            prompt.authenticate(info)
        } catch (e: Exception) {
            onSuccess()
        }
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
            createDocumentLauncher.launch(intent)
        }
    }

    private fun triggerImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        openDocumentLauncher.launch(intent)
    }

    private fun setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock aniAuth")
            .setSubtitle("Confirm identity to view codes")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    private fun authenticateBiometric() {
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            isUnlocked = true
        }
    }
}
