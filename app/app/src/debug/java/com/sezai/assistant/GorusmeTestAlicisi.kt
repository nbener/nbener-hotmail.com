package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

// Gerçek arama, ses veya mikrofon olmadan aynı kesinti akışını çalıştırır.
class GorusmeTestAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rapor = mutableListOf<String>()
        fun kontrol(ad: String, islem: () -> Unit) {
            try { islem(); rapor.add("PASS $ad") }
            catch (_: Exception) { rapor.add("FAIL $ad") }
        }
        kontrol("Çalan telefonda seslendirme başlamıyor") {
            val ortam = SahteOrtam().apply { durum = AudioManager.MODE_RINGTONE }
            val koruma = GorusmeOnceligi(ortam) {}
            check(!koruma.hazirlan(true)); check(ortam.istek == 0)
        }
        kontrol("Aktif telefon ve VoIP görüşmesinde mikrofon açılmıyor") {
            for (mod in listOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)) {
                val ortam = SahteOrtam().apply { durum = mod }
                check(!GorusmeOnceligi(ortam) {}.hazirlan(false))
                check(ortam.istek == 0)
            }
        }
        kontrol("Dinlerken VoIP görüşmesi tek kesinti üretiyor") {
            val ortam = SahteOrtam(); var kesinti = 0
            val koruma = GorusmeOnceligi(ortam) { kesinti++ }
            koruma.izle(); check(koruma.hazirlan(false))
            check(ortam.istek == 0) // Tanıma hizmetinin kendi ses odağıyla çakışmaz.
            ortam.modOlayi?.invoke(AudioManager.MODE_IN_COMMUNICATION)
            ortam.modOlayi?.invoke(AudioManager.MODE_IN_CALL)
            check(kesinti == 1)
        }
        kontrol("Her ses odağı kaybı durduruyor, geri kazanma başlatmıyor") {
            for (kayip in listOf(AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)) {
                val ortam = SahteOrtam(); var kesinti = 0
                val koruma = GorusmeOnceligi(ortam) { kesinti++ }
                check(koruma.hazirlan(true))
                val olay = ortam.odakOlayi!!
                olay(kayip); olay(AudioManager.AUDIOFOCUS_GAIN)
                check(kesinti == 1 && ortam.istek == 1 && ortam.birakma > 0)
            }
        }
        kontrol("Reddedilen ses odağıyla seslendirme başlamıyor") {
            val ortam = SahteOrtam().apply { odakVer = false }
            check(!GorusmeOnceligi(ortam) {}.hazirlan(true))
            check(ortam.istek == 1 && ortam.birakma > 0)
        }
        kontrol("Eski odak olayı yeni dinlemeyi kesmiyor") {
            val ortam = SahteOrtam(); var kesinti = 0
            val koruma = GorusmeOnceligi(ortam) { kesinti++ }
            check(koruma.hazirlan(true)); val eski = ortam.odakOlayi!!
            koruma.durdur(); check(koruma.hazirlan(false))
            eski(AudioManager.AUDIOFOCUS_LOSS)
            check(kesinti == 0)
        }
        kontrol("Görüşme bitişinde otomatik başlama yok, izleme bırakılıyor") {
            val ortam = SahteOrtam(); var kesinti = 0
            val koruma = GorusmeOnceligi(ortam) { kesinti++ }
            koruma.izle(); check(koruma.hazirlan(false))
            ortam.modOlayi?.invoke(AudioManager.MODE_RINGTONE)
            ortam.modOlayi?.invoke(AudioManager.MODE_NORMAL)
            check(kesinti == 1 && ortam.istek == 0)
            koruma.kapat(); check(ortam.modOlayi == null)
        }
        kontrol("Cihazda görüşme modu izleyicisi eklenip kaldırılabiliyor") {
            val koruma = GorusmeOnceligi(context) {}
            try { koruma.izle() } finally { koruma.kapat() }
        }
        context.openFileOutput("gorusme_test_sonuc.txt", Context.MODE_PRIVATE).bufferedWriter().use {
            it.write("TEST_ID ${intent.getStringExtra("test_id")}\n" + rapor.joinToString("\n"))
        }
    }

    private class SahteOrtam : SesOrtami {
        var durum = AudioManager.MODE_NORMAL
        var odakVer = true
        var istek = 0
        var birakma = 0
        var odakOlayi: ((Int) -> Unit)? = null
        var modOlayi: ((Int) -> Unit)? = null
        override fun mod() = durum
        override fun odakIste(degisti: (Int) -> Unit): Boolean { istek++; odakOlayi = degisti; return odakVer }
        override fun odakBirak() { birakma++ }
        override fun izle(degisti: (Int) -> Unit) { modOlayi = degisti }
        override fun izlemeyiBirak() { modOlayi = null }
    }
}
