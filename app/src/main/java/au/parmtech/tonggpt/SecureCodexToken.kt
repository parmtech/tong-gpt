package au.parmtech.tonggpt

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Android hardware-backed Keystore when available; secrets never enter source control. */
internal class SecureCodexToken(private val context: Context) {
    private val alias = "tong_gpt_codex_remote_v1"
    private val prefs = context.getSharedPreferences("tg_codex_encrypted", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val maker = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        maker.init(KeyGenParameterSpec.Builder(
            alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return maker.generateKey()
    }

    fun save(value: String) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .apply()
        } catch (_: Exception) {
            prefs.edit().clear().apply()
        }
    }

    fun read(): String? = try {
        val ivString = prefs.getString("iv", null) ?: return null
        val ciphertextString = prefs.getString("ciphertext", null) ?: return null
        val iv = Base64.decode(ivString, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(Base64.decode(ciphertextString, Base64.NO_WRAP)), Charsets.UTF_8)
    } catch (_: Exception) { null }

    fun clear() = prefs.edit().clear().apply()
}
