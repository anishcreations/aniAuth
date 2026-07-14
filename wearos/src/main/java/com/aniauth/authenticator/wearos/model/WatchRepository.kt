package com.aniauth.authenticator.wearos.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class WatchRepository(context: Context) {
    internal val prefs = context.getSharedPreferences("wear_accounts_pref", Context.MODE_PRIVATE)

    companion object {
        private val decryptionCache = java.util.concurrent.ConcurrentHashMap<String, String>()

        fun decryptCached(ciphertext: String): String {
            if (ciphertext.isEmpty()) return ""
            return decryptionCache.getOrPut(ciphertext) {
                com.aniauth.authenticator.wearos.crypto.KeyStoreHelper.decrypt(ciphertext) ?: ""
            }
        }

        fun clearCache() {
            decryptionCache.clear()
        }
    }

    fun getAccounts(): List<Account> {
        val rawJson = prefs.getString("accounts_json", "[]") ?: "[]"
        val list = mutableListOf<Account>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Account(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        encryptedSecret = obj.getString("encryptedSecret"),
                        issuer = obj.optString("issuer", null).takeIf { it?.isNotEmpty() == true },
                        username = obj.optString("username", null).takeIf { it?.isNotEmpty() == true }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveAccounts(accounts: List<Account>) {
        val array = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("label", acc.label)
            obj.put("encryptedSecret", acc.encryptedSecret)
            obj.put("issuer", acc.issuer ?: "")
            obj.put("username", acc.username ?: "")
            array.put(obj)
        }
        prefs.edit().putString("accounts_json", array.toString()).apply()
    }
}
