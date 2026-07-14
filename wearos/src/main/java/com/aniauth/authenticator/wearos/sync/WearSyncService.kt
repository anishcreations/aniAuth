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
import java.nio.charset.StandardCharsets

class WearSyncService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/sync-accounts") {
            val jsonString = String(messageEvent.data, StandardCharsets.UTF_8)
            try {
                val array = JSONArray(jsonString)
                val newAccounts = mutableListOf<Account>()
                var invalidCount = 0
                
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
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
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        "Failed to sync accounts: corrupted data.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
