package com.aniauth.authenticator.wearos.sync

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.aniauth.authenticator.wearos.crypto.KeyStoreHelper
import com.aniauth.authenticator.wearos.crypto.TotpGenerator
import com.aniauth.authenticator.wearos.model.Account
import com.aniauth.authenticator.wearos.model.WatchRepository
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import android.content.Context

class WearSyncService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/sync-accounts") {
            val jsonString = String(messageEvent.data, StandardCharsets.UTF_8)
            try {
                val accountsArray: JSONArray
                var maxAttempts: Int? = null
                
                if (jsonString.trim().startsWith("{")) {
                    val obj = JSONObject(jsonString)
                    accountsArray = obj.getJSONArray("accounts")
                    if (obj.has("maxFailedAttempts")) {
                        maxAttempts = obj.getInt("maxFailedAttempts")
                    }
                } else {
                    accountsArray = JSONArray(jsonString)
                }

                // Update max attempts if sent from phone
                if (maxAttempts != null) {
                    val lockPrefs = getSharedPreferences("watch_lock_pref", Context.MODE_PRIVATE)
                    lockPrefs.edit().putInt("max_failed_attempts", maxAttempts).apply()
                }

                val newAccounts = mutableListOf<Account>()
                var invalidCount = 0
                
                for (i in 0 until accountsArray.length()) {
                    val obj = accountsArray.getJSONObject(i)
                    val label = obj.getString("label")
                    val plainSecret = obj.getString("encryptedSecret").trim()
                    
                    // Validate Base32 secret key format
                    if (!TotpGenerator.isValidSecret(plainSecret)) {
                        invalidCount++
                        continue
                    }
                    
                    // Encrypt with watch's KeyStore master key
                    val watchEncrypted = KeyStoreHelper.encrypt(plainSecret)
                    
                    newAccounts.add(
                        Account(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            label = label,
                            encryptedSecret = watchEncrypted,
                            issuer = obj.optString("issuer", null).takeIf { it?.isNotEmpty() == true },
                            username = obj.optString("username", null).takeIf { it?.isNotEmpty() == true }
                        )
                    )
                }
                
                if (newAccounts.isNotEmpty()) {
                    val repository = WatchRepository(applicationContext)
                    WatchRepository.clearCache()
                    repository.saveAccounts(newAccounts)
                    
                    Handler(Looper.getMainLooper()).post {
                        if (invalidCount > 0) {
                            Toast.makeText(
                                applicationContext,
                                "Synced ${newAccounts.size} accounts. Skipped $invalidCount invalid keys.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Successfully synced ${newAccounts.size} accounts!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else if (invalidCount > 0) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            applicationContext,
                            "Sync failed: All incoming keys were invalid.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = e.message ?: e.toString()
                android.util.Log.e("WearSyncService", "Sync error: ", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        "Sync failed: $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
