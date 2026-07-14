package com.aniauth.authenticator.crypto

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpGenerator {

    fun generateTOTP(
        secret: String,
        timeInterval: Long = 30,
        timestamp: Long = System.currentTimeMillis() / 1000
    ): String? {
        val cleanSecret = secret.replace(Regex("[\\s-]"), "").uppercase()
        if (cleanSecret.isEmpty()) return null
        val keyBytes = decodeBase32(cleanSecret) ?: return null
        if (keyBytes.isEmpty()) return null
        
        val timeStep = timestamp / timeInterval
        
        // Convert timeStep to an 8-byte byte array
        val buffer = ByteBuffer.allocate(8)
        buffer.putLong(timeStep)
        val timeBytes = buffer.array()
        
        return try {
            val mac = Mac.getInstance("HmacSHA1")
            val keySpec = SecretKeySpec(keyBytes, "HmacSHA1")
            mac.init(keySpec)
            val hash = mac.doFinal(timeBytes)
            
            // Dynamic truncation
            val offset = (hash[hash.size - 1].toInt() and 0xf)
            val binary = (
                ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
            )
            
            val otp = binary % 10.0.pow(6).toInt()
            String.format("%06d", otp)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBase32(base32: String): ByteArray? {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.trimEnd('=')
        val byteCount = (clean.length * 5) / 8
        val bytes = ByteArray(byteCount)
        
        var buffer = 0
        var bitsLeft = 0
        var count = 0
        
        for (char in clean) {
            val valIndex = base32Chars.indexOf(char)
            if (valIndex == -1) return null // Invalid Base32 character
            
            // Mask with 0xFFFF to prevent integer overflow and sign extension issues
            buffer = ((buffer shl 5) or valIndex) and 0xFFFF
            bitsLeft += 5
            if (bitsLeft >= 8) {
                if (count < bytes.size) {
                    bytes[count++] = (buffer shr (bitsLeft - 8)).toByte()
                }
                bitsLeft -= 8
            }
        }
        return bytes
    }

    fun isValidSecret(secret: String): Boolean {
        val cleanSecret = secret.replace(Regex("[\\s-]"), "").uppercase()
        if (cleanSecret.isEmpty()) return false
        val keyBytes = decodeBase32(cleanSecret)
        return keyBytes != null && keyBytes.isNotEmpty()
    }
}
