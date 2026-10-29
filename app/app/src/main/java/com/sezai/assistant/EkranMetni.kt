package com.sezai.assistant

import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

// Ekran metni yalnızca bellekte tutulur; parola ve taslak alt ağaçlarına girilmez.
internal object EkranMetni {
    fun otomatikHedefMi(acik: Boolean, secili: String?, gelen: String?): Boolean =
        acik && !secili.isNullOrBlank() && secili == gelen
    fun alinabilirMi(gorunur: Boolean, parola: Boolean, duzenlenir: Boolean, sinif: String): Boolean =
        gorunur && !parola && !duzenlenir && !sinif.endsWith("EditText")

    fun topla(kok: AccessibilityNodeInfo, paket: String): List<String> {
        val bulunan = mutableListOf<String>()
        var adet = 0
        var uzunluk = 0
        fun gez(dugum: AccessibilityNodeInfo, derinlik: Int) {
            if (++adet > 400 || derinlik > 30 || uzunluk >= 12000) return
            if (dugum.packageName?.toString() != paket) return
            val sinif = dugum.className?.toString().orEmpty()
            if (!alinabilirMi(dugum.isVisibleToUser, dugum.isPassword, dugum.isEditable, sinif)) return
            if (!sinif.endsWith("Button") && !sinif.endsWith("Switch") && !sinif.endsWith("CheckBox")) {
                val metin = (dugum.text ?: if (sinif.endsWith("TerminalView")) dugum.contentDescription else null)
                    ?.toString()?.trim().orEmpty().take(12000 - uzunluk)
                if (metin.isNotBlank()) {
                    metin.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { bulunan.add(it) }
                    uzunluk += metin.length
                }
            }
            for (i in 0 until minOf(dugum.childCount, 100)) {
                val cocuk = dugum.getChild(i) ?: continue
                try { gez(cocuk, derinlik + 1) } finally { cocuk.recycle() }
            }
        }
        gez(kok, 0)
        return bulunan.distinct().takeLast(128)
    }
}

// İlk görünüm okunmaz. Büyüyen yanıtın yalnızca yeni bölümü ve yeni satırlar okunur.
internal class EkranAkisi {
    private var onceki: List<String>? = null
    private val okunan = LinkedHashSet<String>()
    fun sifirla() { onceki = null; okunan.clear() }
    private fun kimlik(metin: String) = MessageDigest.getInstance("SHA-256")
        .digest(metin.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun hatirla(metinler: List<String>) {
        metinler.forEach { okunan.add(kimlik(it)) }
        while (okunan.size > 256) okunan.remove(okunan.first())
    }
    fun yeni(metinler: List<String>): String {
        val simdi = metinler.takeLast(128).map { it.take(12000) }
        val eski = onceki
        onceki = simdi
        if (eski == null || simdi == eski) { hatirla(simdi); return "" }
        var ortak = 0
        while (ortak < minOf(eski.size, simdi.size) && eski[ortak] == simdi[ortak]) ortak++
        val ek = mutableListOf<String>()
        if (ortak > 0 || (eski.size == 1 && simdi.size == 1 && simdi[0].startsWith(eski[0]))) {
            var basla = ortak
            if (ortak == eski.lastIndex && ortak < simdi.size && simdi[ortak].startsWith(eski[ortak])) {
                ek.add(simdi[ortak].removePrefix(eski[ortak])); basla++
            }
            ek.addAll(simdi.drop(basla).filter { kimlik(it) !in okunan })
        } else {
            val ortusen = (minOf(eski.size, simdi.size) downTo 1)
                .firstOrNull { eski.takeLast(it) == simdi.take(it) } ?: 0
            if (ortusen > 0) ek.addAll(simdi.drop(ortusen).filter { kimlik(it) !in okunan })
            // İlişkisiz ekran/konuşma geçişi yeni başlangıçtır; bütün geçmiş seslendirilmez.
        }
        hatirla(simdi)
        return ek.filter { it.isNotBlank() }.joinToString("\n").take(12000)
    }
}

internal object EkranIncelemeBellek {
    var metin: String? = null
}
