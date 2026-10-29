package com.sezai.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Anahtar APK'ya girmez; telefonda Android Keystore ile şifrelenir.
class AnahtarKasasi(context: Context, ad: String = "sohbet") {
    private val tercih = context.getSharedPreferences("${ad}_ayarlari", Context.MODE_PRIVATE)
    private val takmaAd = "sezai-${ad}-anahtari"

    private fun anahtar(): SecretKey {
        val kasa = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (kasa.getKey(takmaAd, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(takmaAd, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    fun kaydet(metin: String) {
        val sifre = Cipher.getInstance("AES/GCM/NoPadding")
        sifre.init(Cipher.ENCRYPT_MODE, anahtar())
        val veri = sifre.doFinal(metin.trim().toByteArray(Charsets.UTF_8))
        check(tercih.edit()
            .putString("iv", Base64.encodeToString(sifre.iv, Base64.NO_WRAP))
            .putString("veri", Base64.encodeToString(veri, Base64.NO_WRAP)).commit())
    }

    fun varMi(): Boolean = tercih.contains("veri") && tercih.contains("iv")

    fun oku(): String? {
        val veri = tercih.getString("veri", null) ?: return null
        val iv = tercih.getString("iv", null) ?: return null
        val sifre = Cipher.getInstance("AES/GCM/NoPadding")
        sifre.init(Cipher.DECRYPT_MODE, anahtar(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return String(sifre.doFinal(Base64.decode(veri, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    fun sil() { check(tercih.edit().remove("iv").remove("veri").commit()) }
}
