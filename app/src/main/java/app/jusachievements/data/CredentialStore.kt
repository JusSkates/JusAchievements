package app.jusachievements.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Credentials(val username: String, val apiKey: String)

/**
 * Stores the RetroAchievements username and Web API key. The key is encrypted with an
 * AES-GCM key that lives in the Android Keystore and never leaves the device.
 */
class CredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    @Volatile
    var cached: Credentials? = null
        private set

    fun load(): Credentials? {
        cached?.let { return it }
        val user = prefs.getString(KEY_USER, null) ?: return null
        val blob = prefs.getString(KEY_API, null) ?: return null
        val apiKey = runCatching { decrypt(blob) }.getOrNull() ?: run { clear(); return null }
        return Credentials(user, apiKey).also { cached = it }
    }

    fun save(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_USER, credentials.username)
            .putString(KEY_API, encrypt(credentials.apiKey))
            .apply()
        cached = credentials
    }

    fun clear() {
        prefs.edit().clear().apply()
        cached = null
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val sealed = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + sealed, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val bytes = Base64.decode(blob, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val sealed = bytes.copyOfRange(IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(sealed), Charsets.UTF_8)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "ja_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val KEY_USER = "user"
        const val KEY_API = "api"
    }
}
