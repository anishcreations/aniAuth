package com.aniauth.authenticator.crypto

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val ALGORITHM = "AES/GCM/NoPadding"

    fun encrypt(plainText: String, password: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)
        
        // Derive key from password using PBKDF2
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Output format: Base64(salt + iv + encrypted)
        val combined = ByteArray(SALT_LENGTH + IV_LENGTH + encrypted.size)
        System.arraycopy(salt, 0, combined, 0, SALT_LENGTH)
        System.arraycopy(iv, 0, combined, SALT_LENGTH, IV_LENGTH)
        System.arraycopy(encrypted, 0, combined, SALT_LENGTH + IV_LENGTH, encrypted.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedBase64: String, password: CharArray): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            if (combined.size < SALT_LENGTH + IV_LENGTH) return null
            
            val salt = ByteArray(SALT_LENGTH)
            val iv = ByteArray(IV_LENGTH)
            val encrypted = ByteArray(combined.size - SALT_LENGTH - IV_LENGTH)
            
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)
            System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH)
            System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, encrypted, 0, encrypted.size)
            
            // Derive key from password using PBKDF2
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec: KeySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
            val tmp = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
