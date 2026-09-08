package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptedPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("secure_qris_calc_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
        private const val ITERATIONS = 1000
        private const val KEY_LENGTH = 256
        // Internal salt for seed derivation
        private const val INTERNAL_SALT = "QRIS_SECURE_CALC_SALT_2026"
        private const val SECRET_SEED = "kalkulator_qris_merchant_secret_key"
    }

    private fun getSecretKey(): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(SECRET_SEED.toCharArray(), INTERNAL_SALT.toByteArray(), ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combine IV + ciphertext
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            return plainText
        }
    }

    /**
     * Decrypts ciphertext string using AES-256-GCM.
     */
    fun decrypt(cipherTextBase64: String): String {
        if (cipherTextBase64.isEmpty()) return ""
        try {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH) return cipherTextBase64

            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)

            val cipherText = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmSpec)
            val decrypted = cipher.doFinal(cipherText)

            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            return cipherTextBase64
        }
    }

    fun saveStringEncrypted(key: String, value: String) {
        val encrypted = encrypt(value)
        prefs.edit().putString(key, encrypted).apply()
    }

    fun getStringDecrypted(key: String, defaultValue: String = ""): String {
        val encrypted = prefs.getString(key, null) ?: return defaultValue
        val decrypted = decrypt(encrypted)
        return if (decrypted.isNotBlank()) decrypted else defaultValue
    }

    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }
}
