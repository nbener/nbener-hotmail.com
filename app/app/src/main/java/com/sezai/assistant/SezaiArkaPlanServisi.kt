package com.sezai.assistant

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

// Kullanıcı düğmeyle açınca "Sezai" sözcüğünü ekran kilitliyken de bekleyen mikrofon ön plan servisi.
// Kalıcı bildirim zorunludur; düğme veya bildirimdeki Kapat ile mikrofon tamamen bırakılır.
// Aşama 1: uyanınca sesle "Buyurun" der ve dinlemeye döner. Kilitliyken kişisel veri okumaz.
class SezaiArkaPlanServisi : Service() {
    companion object {
        private const val KANAL = "sezai_dinleme"
        private const val BILDIRIM = 41
        private const val DURDUR = "com.sezai.assistant.ARKA_PLAN_DURDUR"
        @Volatile var calisiyor = false
            private set

        fun baslat(context: Context) = context.startForegroundService(Intent(context, SezaiArkaPlanServisi::class.java))
        fun durdur(context: Context) { context.stopService(Intent(context, SezaiArkaPlanServisi::class.java)) }
    }

    private val anaIs = Handler(Looper.getMainLooper())
    private var uyanma: SezaiUyanma? = null
    private var konusma: AndroidKonusma? = null
    private var konusmaHazir = false
    private var kapaniyor = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == DURDUR) {
            // Kullanıcı kapattı: bir sonraki uygulama açılışında kendiliğinden yeniden başlamasın.
            getSharedPreferences("ses_ayari", MODE_PRIVATE).edit().putBoolean("arka_plan", false).apply()
            stopSelf(); return START_NOT_STICKY
        }
        if (calisiyor) return START_NOT_STICKY
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Sezai", "ARKAPLAN mikrofon izni yok"); stopSelf(); return START_NOT_STICKY
        }
        bildirimKur()
        try {
            startForeground(BILDIRIM, bildirim(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } catch (e: Exception) {
            Log.w("Sezai", "ARKAPLAN on plan baslatilamadi: ${e.javaClass.simpleName}"); stopSelf(); return START_NOT_STICKY
        }
        calisiyor = true
        konusma = AndroidKonusma(this, "com.google.android.tts") { hazir ->
            anaIs.post {
                if (kapaniyor) return@post
                konusmaHazir = hazir
                // Uygulamada seçilen kayıtlı sesi kullan
                getSharedPreferences("ses_ayari", MODE_PRIVATE).getString("ses_adi", null)?.let { konusma?.sesSec(it) }
            }
        }
        uyanma = SezaiUyanma(this) { sezaiDendi() }
        uyanma?.baslat()
        Log.i("Sezai", "ARKAPLAN servis basladi")
        return START_NOT_STICKY
    }

    private fun sezaiDendi() {
        if (kapaniyor) return
        Log.i("Sezai", "ARKAPLAN sezai duyuldu")
        val ses = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!konusmaHazir || ses.mode != AudioManager.MODE_NORMAL) { yenidenDinle(3000); return }
        konusma?.konus("Buyurun, sizi dinliyorum.",
            bitince = { anaIs.post { yenidenDinle(1500) } },
            hata = { anaIs.post { yenidenDinle(3000) } })
    }

    // Kendi sesini "Sezai" diye duymasın diye konuşma bittikten kısa süre sonra dinlemeye döner
    private fun yenidenDinle(gecikmeMs: Long) {
        anaIs.postDelayed({ if (!kapaniyor) uyanma?.baslat() }, gecikmeMs)
    }

    private fun bildirimKur() {
        val yonetici = getSystemService(NotificationManager::class.java)
        yonetici.createNotificationChannel(NotificationChannel(KANAL, "Sezai dinleme", NotificationManager.IMPORTANCE_LOW))
    }

    private fun bildirim(): Notification {
        val ac = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val kapat = PendingIntent.getService(this, 1,
            Intent(this, SezaiArkaPlanServisi::class.java).setAction(DURDUR), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, KANAL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Sezai dinliyor")
            .setContentText("\"Sezai\" deyince yanıt verir. Mikrofon açık.")
            .setContentIntent(ac)
            .addAction(Notification.Action.Builder(null, "Kapat", kapat).build())
            .setOngoing(true).build()
    }

    override fun onDestroy() {
        kapaniyor = true
        anaIs.removeCallbacksAndMessages(null)
        uyanma?.durdur(); uyanma = null
        konusma?.kapat(); konusma = null
        calisiyor = false
        Log.i("Sezai", "ARKAPLAN servis durdu")
        super.onDestroy()
    }
}
