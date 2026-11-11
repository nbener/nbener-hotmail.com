package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * ADB Wireless Pairing BroadcastReceiver.
 *
 * Termux veya başka bir yerel uygulama şu şekilde tetikler:
 *   am broadcast -a com.sezai.ADB_ESLESME \
 *     --es host "192.168.1.x" \
 *     --ei port 12345 \
 *     --es pin "123456"
 *
 * Sonuç logcat'te "Sezai" etiketiyle görünür:
 *   ADB_PAIR BASARILI: 192.168.1.x:12345
 *   ADB_PAIR BASARISIZ: ...
 *   ADB_PAIR HATA: ...
 */
class AdbEslestirici : BroadcastReceiver() {

    companion object {
        private const val ETIKET = "Sezai"
        const val EYLEM = "com.sezai.ADB_ESLESME"
    }

    override fun onReceive(context: Context, intent: Intent) {
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
            // Sonuç zaten AdbEslestirme içinde logcat'e yazılıyor.
            // Buraya ek işlem (bildirim, intent yayını vb.) eklenebilir.
            Log.i(ETIKET, "ADB_PAIR sonuç: basarili=$basarili mesaj=$mesaj")
        }
    }
}
