package com.sezai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Bildirimden elle kumanda: Sessiz mod ve Arka plan dinleme düğmeleri
object BildirimKumanda {
    const val ACIK_BILDIRIM = 42
    const val SESSIZ_DEGISTIR = "com.sezai.assistant.SESSIZ_DEGISTIR"
    private const val KANAL = "sezai_acik"

    fun sessizMi(c: Context) = c.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE).getBoolean("sessiz", false)

    fun sessizAyarla(c: Context, sessiz: Boolean) {
        c.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE).edit().putBoolean("sessiz", sessiz).apply()
        // Sessiz modda mikrofon tutan servis çalışmaz; çıkınca arka plan ayarı açıksa uygulama açılışında yeniden başlar
        if (sessiz) SezaiArkaPlanServisi.durdur(c)
        acikBildirimiGuncelle(c)
    }

    fun sessizAksiyonu(c: Context): Notification.Action {
        val pi = PendingIntent.getBroadcast(c, 10, Intent(c, SessizAlici::class.java).setAction(SESSIZ_DEGISTIR), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(null, if (sessizMi(c)) "Sessizden çık" else "Sessiz mod", pi).build()
    }

    // Arka plan dinleme: kapalıysa servisi başlatır (bildirime dokunma kullanıcı etkileşimidir), açıksa kapatır
    fun arkaPlanAksiyonu(c: Context): Notification.Action {
        val acik = SezaiArkaPlanServisi.calisiyor
        val i = Intent(c, SezaiArkaPlanServisi::class.java).setAction(if (acik) SezaiArkaPlanServisi.DURDUR else SezaiArkaPlanServisi.ACIK_ISTEK)
        val pi = if (acik) PendingIntent.getService(c, 11, i, PendingIntent.FLAG_IMMUTABLE)
                 else PendingIntent.getForegroundService(c, 12, i, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(null, if (acik) "Arka planı kapat" else "Arka planda dinle", pi).build()
    }

    fun acikBildirimiGuncelle(c: Context) {
        if (c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val y = c.getSystemService(NotificationManager::class.java)
        if (SezaiArkaPlanServisi.calisiyor) { y.cancel(ACIK_BILDIRIM); return } // servisin kendi bildirimi var
        y.createNotificationChannel(NotificationChannel(KANAL, "Sezai açık", NotificationManager.IMPORTANCE_LOW))
        val ac = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val sessiz = sessizMi(c)
        y.notify(ACIK_BILDIRIM, Notification.Builder(c, KANAL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(if (sessiz) "Sezai sessiz modda" else "Sezai açık")
            .setContentText(if (sessiz) "Konuşmaz ve dinlemez." else "Dokunarak Sezai'ye dön.")
            .setContentIntent(ac).setOnlyAlertOnce(true)
            .addAction(sessizAksiyonu(c)).addAction(arkaPlanAksiyonu(c)).build())
    }
}

class SessizAlici : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == BildirimKumanda.SESSIZ_DEGISTIR) BildirimKumanda.sessizAyarla(c, !BildirimKumanda.sessizMi(c))
    }
}
