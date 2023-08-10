package com.ogstudiekort

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import java.security.KeyStore

class BiometricLoginHelper(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "myKeyAlias"
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    fun saveUserDataToPreferences(username: String, password: String) {
        val encryptedUsername = encrypt(username)
        val encryptedPassword = encrypt(password)

        with(sharedPreferences.edit()) {
            putString("encryptedUsername", encryptedUsername.first)
            putString("encryptedUsernameIV", encryptedUsername.second)
            putString("encryptedPassword", encryptedPassword.first)
            putString("encryptedPasswordIV", encryptedPassword.second)
            apply()
        }
    }

    fun retrieveUserDataFromPreferences(): Pair<String, String>? {
        val encryptedUsername = sharedPreferences.getString("encryptedUsername", null)
        val encryptedUsernameIV = sharedPreferences.getString("encryptedUsernameIV", null)
        val encryptedPassword = sharedPreferences.getString("encryptedPassword", null)
        val encryptedPasswordIV = sharedPreferences.getString("encryptedPasswordIV", null)

        if (encryptedUsername != null && encryptedUsernameIV != null && encryptedPassword != null && encryptedPasswordIV != null) {
            return Pair(decrypt(encryptedUsername, encryptedUsernameIV), decrypt(encryptedPassword, encryptedPasswordIV))
        }
        return null
    }

    private fun encrypt(input: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
        val encryptedValue = cipher.doFinal(input.toByteArray())

        return Pair(Base64.encodeToString(encryptedValue, Base64.DEFAULT), iv)
    }

    private fun decrypt(encryptedValue: String, iv: String): String {
        val originalValue = Base64.decode(encryptedValue, Base64.DEFAULT)
        val originalIv = Base64.decode(iv, Base64.DEFAULT)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(originalIv))

        return String(cipher.doFinal(originalValue))
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // Hvis nøglen ikke allerede findes, skab den
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
            )
            keyGenerator.generateKey()
        }

        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isUserDataSaved(): Boolean {
        val encryptedUsername = sharedPreferences.getString("encryptedUsername", null)
        val encryptedPassword = sharedPreferences.getString("encryptedPassword", null)
        return encryptedUsername != null && encryptedPassword != null
    }
}
