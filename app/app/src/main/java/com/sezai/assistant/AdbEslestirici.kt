package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * ADB Wireless Pairing ve uzaktan ayar BroadcastReceiver.
 *
 * ADB pairing:
 *   am broadcast -a com.sezai.ADB_ESLESME --es host "192.168.1.x" --ei port 12345 --es pin "123456"
 *
 * Sağlayıcı seç (GROQ/GEMINI/OPENROUTER/OPENAI/ALGION):
 *   am broadcast -a com.sezai.AYAR_SET --es saglayici ALGION
 *
 * API anahtarı kaydet:
 *   am broadcast -a com.sezai.AYAR_SET --es saglayici ALGION --es anahtar "free"
 */
class AdbEslestirici : BroadcastReceiver() {

    companion object {
        private const val ETIKET = "Sezai"
        const val EYLEM = "com.sezai.ADB_ESLESME"
        const val AYAR_EYLEM = "com.sezai.AYAR_SET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AYAR_EYLEM) { ayarla(context, intent); return }
        if (intent.action != EYLEM) return

        // Intent extras'tan bağlantı bilgilerini al
        val host = intent.getStringExtra("host")
        val port = intent.getIntExtra("port", -1)
        val pin = intent.getStringExtra("pin")

        // Zorunlu alanları doğrula
        if (host.isNullOrBlank()) {
            Log.e(ETIKET, "ADB_PAIR hata: 'host' parametresi eksik")
            return
        }
        if (port <= 0 || port > 65535) {
            Log.e(ETIKET, "ADB_PAIR hata: 'port' parametresi geçersiz ($port)")
            return
        }
        if (pin.isNullOrBlank()) {
            Log.e(ETIKET, "ADB_PAIR hata: 'pin' parametresi eksik")
            return
        }

        Log.i(ETIKET, "ADB_PAIR isteği alındı: $host:$port (PIN: ${pin.take(2)}****)")

        // Eşleştirmeyi arka planda başlat (AdbEslestirme kendi thread'ini açar)
        AdbEslestirme.eslestir(host, port, pin) { basarili, mesaj ->
            Log.i(ETIKET, "ADB_PAIR sonuç: basarili=$basarili mesaj=$mesaj")
        }
    }

    private fun ayarla(context: Context, intent: Intent) {
        val saglayiciAdi = intent.getStringExtra("saglayici") ?: return
        val saglayici = try { SohbetSaglayici.valueOf(saglayiciAdi) } catch (_: Exception) {
            Log.w(ETIKET, "AYAR_SET bilinmeyen sağlayıcı: $saglayiciAdi"); return
        }
        saglayici.sec(context)
        Log.i(ETIKET, "AYAR_SET saglayici=$saglayiciAdi")
        val anahtar = intent.getStringExtra("anahtar") ?: return
        try {
            AnahtarKasasi(context, saglayici.kasaAdi).kaydet(anahtar)
            Log.i(ETIKET, "AYAR_SET anahtar kaydedildi saglayici=$saglayiciAdi uzunluk=${anahtar.length}")
        } catch (e: Exception) {
            Log.w(ETIKET, "AYAR_SET anahtar kaydedilemedi: $e")
        }
    }
}
