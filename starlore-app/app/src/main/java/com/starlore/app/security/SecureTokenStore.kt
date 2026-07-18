package com.starlore.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.tencent.mmkv.MMKV
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureTokenStore {
    private const val KEY_ALIAS = "starlore-auth-token"
    private const val STORAGE_KEY = "secure_auth_token_v1"
    private const val LEGACY_KEY = "auth_token"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val storage: MMKV
        get() = MMKV.defaultMMKV()

    fun get(): String {
        val encrypted = storage.decodeString(STORAGE_KEY, "").orEmpty()
        if (encrypted.isNotEmpty()) return decrypt(encrypted)

        // One-time migration from the former plaintext MMKV entry.
        val legacy = storage.decodeString(LEGACY_KEY, "").orEmpty()
        if (legacy.isNotEmpty()) {
            put(legacy)
            storage.removeValueForKey(LEGACY_KEY)
        }
        return legacy
    }

    fun put(value: String) {
        if (value.isEmpty()) {
            storage.removeValueForKey(STORAGE_KEY)
            storage.removeValueForKey(LEGACY_KEY)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encoded = listOf(cipher.iv, ciphertext)
            .joinToString(":") { Base64.encodeToString(it, Base64.NO_WRAP) }
        storage.encode(STORAGE_KEY, encoded)
        storage.removeValueForKey(LEGACY_KEY)
    }

    private fun decrypt(encoded: String): String = runCatching {
        val parts = encoded.split(':', limit = 2)
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrElse {
        storage.removeValueForKey(STORAGE_KEY)
        ""
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }
}
