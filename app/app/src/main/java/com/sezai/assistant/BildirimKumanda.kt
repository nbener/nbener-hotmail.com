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
        val cagiran = Thread.currentThread().stackTrace.drop(2).take(4)
            .joinToString(" < ") { "${it.className.substringAfterLast('.')}::${it.methodName}" }
        android.util.Log.i("Sezai", "SESSIZ ayar=$sessiz cagiran=$cagiran")
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
    fun arkaPlanAksiyonu(c: Context, acik: Boolean = SezaiArkaPlanServisi.calisiyor): Notification.Action {
        val i = Intent(c, SezaiArkaPlanServisi::class.java).setAction(if (acik) SezaiArkaPlanServisi.DURDUR else SezaiArkaPlanServisi.ACIK_ISTEK)
        val pi = if (acik) PendingIntent.getService(c, 11, i, PendingIntent.FLAG_IMMUTABLE)
                 else PendingIntent.getForegroundService(c, 12, i, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(null, if (acik) "Arka planı kapat" else "Arka planda dinle", pi).build()
    }

    const val GORUNUM_DEGISTIR = "com.sezai.assistant.GORUNUM_DEGISTIR"
    const val SES_ARTIR = "com.sezai.assistant.SES_ARTIR"
    const val SES_AZALT = "com.sezai.assistant.SES_AZALT"
    const val UYANMA_DEGISTIR = "com.sezai.assistant.UYANMA_DEGISTIR"
    const val CIKIS = "com.sezai.assistant.CIKIS"
    const val ARKA_DEGISTIR = "com.sezai.assistant.ARKA_DEGISTIR"

    @Volatile var cikiyor = false // Tam çıkış başladı: panel yeniden konmaz
    @Volatile var anaEkran: android.app.Activity? = null // Çıkışta ana ekranı da görev listesinden kaldırmak için

    // Tam çıkış: arka plan dinleme tercihi kapatılır, servis durur, bildirimler kalkar, ekran kapanır, süreç sonlanır
    fun tamCikis(c: Context) {
        val uygulama = c.applicationContext
        cikiyor = true
        arayuzAcik = false
        uygulama.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE).edit().putBoolean("arka_plan", false).apply()
        SezaiArkaPlanServisi.durdur(uygulama)
        val y = uygulama.getSystemService(NotificationManager::class.java)
        y.cancelAll()
        anaEkran?.finishAndRemoveTask()
        // Kapanış geri çağrıları bildirimi yeniden koyamasın diye süreç gecikmeyle sonlandırılır
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            y.cancelAll()
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 700)
    }

    private fun yayin(c: Context, kod: Int, eylem: String) =
        PendingIntent.getBroadcast(c, kod, Intent(c, SessizAlici::class.java).setAction(eylem), PendingIntent.FLAG_IMMUTABLE)

    // Kapalıyken tek satır; açılınca 6 düğmeli kumanda paneli (yalnız dokunmayla çalışır, servis yok)
    @Volatile var arayuzAcik = false // MainActivity yaşıyorsa bildirim güncellenir (kapandıktan sonra hortlamaz)

    // POST_NOTIFICATIONS izni Android 13 (SDK 33) ile geldi; eski sürümlerde izin yoktur ve bildirim serbesttir
    fun bildirimIzniVar(c: Context) = android.os.Build.VERSION.SDK_INT < 33 ||
        c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun acikBildirimiGuncelle(c: Context) {
        if (!bildirimIzniVar(c)) return
        val y = c.getSystemService(NotificationManager::class.java)
        if (SezaiArkaPlanServisi.calisiyor) { y.cancel(ACIK_BILDIRIM); return } // servis aynı paneli kendi bildiriminde gösterir
        y.notify(ACIK_BILDIRIM, panel(c, KANAL).build())
    }

    // Tek satır başlık + açılınca düğme paneli; hem açık-uygulama hem servis bildirimi bunu kullanır
    fun panel(c: Context, kanal: String, arka: Boolean = SezaiArkaPlanServisi.calisiyor): Notification.Builder {
        val y = c.getSystemService(NotificationManager::class.java)
        y.createNotificationChannel(NotificationChannel(kanal, if (kanal == KANAL) "Sezai açık" else "Sezai dinleme", NotificationManager.IMPORTANCE_LOW))
        val ac = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val sessiz = sessizMi(c)
        val gece = c.getSharedPreferences("gorunum", Context.MODE_PRIVATE).getBoolean("gece", true)
        val uyanma = c.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE).getBoolean("uyanma", true)
        // Aynı düğmeler iki görünümde: kapalıyken iki sıra küçük düğme, genişletilince büyük düzen
        fun doldur(v: android.widget.RemoteViews, kisa: Boolean = false) {
            v.setTextViewText(R.id.b_sessiz, if (sessiz) (if (kisa) "Sesli" else "Sessizden çık") else "Sessiz")
            v.setTextViewText(R.id.b_arka, if (kisa) (if (arka) "Arka: açık" else "Arka: kapalı") else if (arka) "Arka planı kapat" else "Arka planda dinle")
            v.setTextViewText(R.id.b_gorunum, if (gece) "Gündüz" else "Gece")
            v.setTextViewText(R.id.b_uyanma, if (kisa) (if (uyanma) "Uyan: açık" else "Uyan: kapalı") else if (uyanma) "Uyanma: açık" else "Uyanma: kapalı")
            v.setOnClickPendingIntent(R.id.b_sessiz, yayin(c, 10, SESSIZ_DEGISTIR))
            v.setOnClickPendingIntent(R.id.b_gorunum, yayin(c, 13, GORUNUM_DEGISTIR))
            v.setOnClickPendingIntent(R.id.b_ses_az, yayin(c, 14, SES_AZALT))
            v.setOnClickPendingIntent(R.id.b_ses_art, yayin(c, 15, SES_ARTIR))
            v.setOnClickPendingIntent(R.id.b_uyanma, yayin(c, 16, UYANMA_DEGISTIR))
            v.setOnClickPendingIntent(R.id.b_cikis, yayin(c, 17, CIKIS))
            v.setOnClickPendingIntent(R.id.b_arka, yayin(c, 18, ARKA_DEGISTIR))
        }
        val v = android.widget.RemoteViews(c.packageName, R.layout.bildirim_kumanda).also { doldur(it) }
        val kucuk = android.widget.RemoteViews(c.packageName, R.layout.bildirim_kumanda_kucuk).also { doldur(it, true) }
        return Notification.Builder(c, kanal)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(if (sessiz) "Sezai · sessiz modda" else if (arka) "Sezai · dinliyor (mikrofon açık)" else "Sezai açık")
            .setContentIntent(ac).setOnlyAlertOnce(true)
            .setStyle(Notification.DecoratedCustomViewStyle()).setCustomContentView(kucuk).setCustomBigContentView(v)
    }
}

class SessizAlici : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val ses = c.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE)
        if (i.action == BildirimKumanda.CIKIS) { BildirimKumanda.tamCikis(c); return }
        if (i.action == BildirimKumanda.ARKA_DEGISTIR) {
            // Açıksa kapatır; kapalıysa açar. Dokunuş açık bir istektir: sessiz moddaysa sessizden de çıkar (yoksa düğme hiçbir şey yapmazdı)
            android.util.Log.i("Sezai", "PANEL arka dugmesi: calisiyor=${SezaiArkaPlanServisi.calisiyor} sessiz=${BildirimKumanda.sessizMi(c)}")
            if (SezaiArkaPlanServisi.calisiyor) {
                ses.edit().putBoolean("arka_plan", false).commit(); SezaiArkaPlanServisi.durdur(c)
            } else {
                if (BildirimKumanda.sessizMi(c)) BildirimKumanda.sessizAyarla(c, false)
                ses.edit().putBoolean("arka_plan", true).apply(); SezaiArkaPlanServisi.baslat(c)
            }
            BildirimKumanda.acikBildirimiGuncelle(c)
            return
        }
        when (i.action) {
            BildirimKumanda.SESSIZ_DEGISTIR -> BildirimKumanda.sessizAyarla(c, !BildirimKumanda.sessizMi(c))
            BildirimKumanda.GORUNUM_DEGISTIR -> {
                val g = c.getSharedPreferences("gorunum", Context.MODE_PRIVATE)
                g.edit().putBoolean("gece", !g.getBoolean("gece", true)).apply()
            }
            BildirimKumanda.SES_ARTIR, BildirimKumanda.SES_AZALT ->
                ses.edit().putInt("tts_seviye", (ses.getInt("tts_seviye", 100) + if (i.action == BildirimKumanda.SES_ARTIR) 25 else -25).coerceIn(10, 250)).apply()
            BildirimKumanda.UYANMA_DEGISTIR -> ses.edit().putBoolean("uyanma", !ses.getBoolean("uyanma", true)).apply()
        }
        BildirimKumanda.acikBildirimiGuncelle(c)
    }
}
