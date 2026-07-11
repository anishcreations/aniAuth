package com.aniauth.authenticator.model

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object AccountSerializer {
    
    fun toJson(accounts: List<Account>): String {
        val array = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("label", acc.label)
            obj.put("encryptedSecret", acc.encryptedSecret)
            obj.put("username", acc.username ?: "")
            array.put(obj)
        }
        return array.toString()
    }
    
    fun fromJson(json: String): List<Account> {
        val list = mutableListOf<Account>()
        try {
            val trimmed = json.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parseAccountObject(obj)?.let { list.add(it) }
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                
                // Bitwarden Vault uses "items". Others use "accounts", "entries", etc.
                val array = root.optJSONArray("items")
                    ?: root.optJSONArray("accounts")
                    ?: root.optJSONArray("entries")
                
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        parseAccountObject(obj)?.let { list.add(it) }
                    }
                } else {
                    parseAccountObject(root)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseAccountObject(obj: JSONObject): Account? {
        // 1. Bitwarden Vault nested structure check
        val login = obj.optJSONObject("login")
        if (login != null) {
            val totpUri = login.optString("totp", "")
            if (totpUri.startsWith("otpauth://")) {
                val parsed = com.aniauth.authenticator.crypto.OtpAuthParser.parse(totpUri)
                if (parsed != null) return parsed
            }
        }

        // 2. Generic field extraction (covers Bitwarden Authenticator and others)
        val rawName = obj.optString("name", "")
            .ifEmpty { obj.optString("label", "") }
            .ifEmpty { obj.optString("account", "") }

        val rawIssuer = obj.optString("issuer", "")
        
        val secret = obj.optString("secret", "")
            .ifEmpty { obj.optString("encryptedSecret", "") }
            .ifEmpty { obj.optString("key", "") }
            
        val username = obj.optString("username", "")
            .ifEmpty { obj.optString("user", "") }
            .takeIf { it.isNotEmpty() }

        if (secret.isEmpty()) return null

        var finalIssuer = rawIssuer
        var finalLabel = rawName
        
        if (finalIssuer.isEmpty() && finalLabel.contains(":")) {
            finalIssuer = finalLabel.substringBefore(":")
            finalLabel = finalLabel.substringAfter(":")
        }
        
        if (finalIssuer.isEmpty()) {
            finalIssuer = finalLabel
            finalLabel = username ?: ""
        }

        return Account(
            id = obj.optString("id", UUID.randomUUID().toString()),
            label = finalIssuer.trim(),
            encryptedSecret = secret.trim(), // Stored plain if imported, re-encrypted in MainActivity
            issuer = null,
            username = finalLabel.trim().takeIf { it.isNotEmpty() }
        )
    }
}
