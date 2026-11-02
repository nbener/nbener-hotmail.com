package com.sezai.assistant

import android.content.Context
import android.util.Log
import java.io.File

// Sesli notlar: yalnız uygulamanın özel klasöründe, satır başına bir not (yeni en sonda)
class NotDefteri(context: Context) {
    private val dosya = File(context.filesDir, "notlar.txt")

    fun ekle(metin: String): Boolean = try {
        dosya.appendText(metin.replace('\n', ' ').trim() + "\n"); true
    } catch (e: Exception) { Log.w("Sezai", "Not yazılamadı: $e"); false }

    // En yeni not önce gelir
    fun son(adet: Int): List<String> = try {
        if (dosya.exists()) dosya.readLines().filter { it.isNotBlank() }.takeLast(adet).reversed() else emptyList()
    } catch (e: Exception) { Log.w("Sezai", "Not okunamadı: $e"); emptyList() }
}
