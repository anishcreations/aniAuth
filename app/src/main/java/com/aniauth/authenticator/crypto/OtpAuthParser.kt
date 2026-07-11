package com.aniauth.authenticator.crypto

import android.net.Uri
import com.aniauth.authenticator.model.Account

object OtpAuthParser {
    fun parse(uriString: String): Account? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme != "otpauth") return null
            if (uri.host != "totp") return null
            
            // Format: otpauth://totp/Label?secret=SECRET&issuer=Issuer
            val path = uri.path?.removePrefix("/") ?: ""
            val secret = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer")
            
            val label = if (path.contains(":")) {
                path.substringAfter(":")
            } else {
                path
            }
            
            val finalIssuer = issuer ?: if (path.contains(":")) {
                path.substringBefore(":")
            } else {
                null
            }
            
            Account(
                label = finalIssuer ?: label, // Label is now 'Issuer'
                encryptedSecret = secret.trim(),
                issuer = null, // not using this legacy field anymore
                username = if (finalIssuer != null) label else null // Name is now 'Username'
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
