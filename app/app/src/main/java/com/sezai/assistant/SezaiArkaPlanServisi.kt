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
// Aşama 2: uyanınca "Buyurun" der, bir istek dinler, sohbet modeliyle yanıtlar (en çok 4 takip) ve uyanma beklemesine döner.
// Yalnız sohbet modelini kullanır; mesaj, arama, kişi gibi kişisel verilere dokunmaz (kilitliyken okunmaz).
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
    private var dinleme: AndroidDinleme? = null
    private val sozKesme by lazy { SozKesme(this) { sozKesildi() } }
    private var konusanNesil = -1
    private var sohbet: SohbetMotoru? = null
    private var sohbetSaglayicisi: SohbetSaglayici? = null
    private var nesil = 0 // eski geri çağrıların yeni turu bozmasını önler
    private val enFazlaTakip = 4

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
            // Kilitliyken konum sorusu için konum türü de gerekir; yalnız konum izni verilmişse eklenir
            val konumIzni = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val kameraIzni = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val tur = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or (if (konumIzni) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0) or
                (if (kameraIzni) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0)
            startForeground(BILDIRIM, bildirim(), tur)
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
        val benim = ++nesil
        sozKesme.hazirla() // model arka planda yüklenir; ilk yanıta kadar hazır olur
        konus("Buyurun, sizi dinliyorum.", benim) { dinleTur(benim, 0) }
    }

    // Konuşmayı bitirince devam eder; hata veya iptalde uyanma beklemesine döner
    private fun konus(metin: String, benim: Int, kesilebilir: Boolean = false, sonra: () -> Unit) {
        if (benim != nesil || kapaniyor) return
        val k = konusma
        if (k == null) { uyanmayaDon(benim, 3000); return }
        // Uygulamadaki ses çubuğu ayarı her konuşmadan önce okunur
        val yuzde = getSharedPreferences("ses_ayari", MODE_PRIVATE).getInt("tts_seviye", 100).coerceIn(10, 100)
        k.sesDuzeyi = if (yuzde >= 100) null else yuzde / 100f
        konusanNesil = if (kesilebilir) benim else -1
        k.konus(metin,
            bitince = { anaIs.post {
                if (benim == nesil && !kapaniyor) { sozKesme.durdur(); sonra() }
            } },
            hata = { anaIs.post {
                if (benim == nesil && !kapaniyor) { sozKesme.durdur(); uyanmayaDon(benim, 3000) }
            } })
        if (kesilebilir) sozKesme.baslat()
    }

    // Kullanıcı yanıt okunurken "dur"/"Sezai" dedi: okumayı kes ve yeni isteği dinle
    private fun sozKesildi() {
        val eski = konusanNesil
        if (kapaniyor || eski != nesil) return
        Log.i("Sezai", "ARKAPLAN soz kesildi")
        konusanNesil = -1
        konusma?.durdur()
        val yeni = ++nesil
        dinleTur(yeni, 0)
    }

    private fun uyanmayaDon(benim: Int, gecikmeMs: Long) {
        if (benim != nesil || kapaniyor) return
        nesil++
        dinleme?.kapat(); sohbet?.iptal()
        yenidenDinle(gecikmeMs)
    }

    private fun dinleTur(benim: Int, takip: Int) {
        if (benim != nesil || kapaniyor) return
        val d = dinleme ?: AndroidDinleme(this).also { dinleme = it }
        if (!d.kullanilabilirMi()) { uyanmayaDon(benim, 3000); return }
        d.dinle(
            sonuc = { metin -> anaIs.post { if (benim == nesil && !kapaniyor) yanitla(metin, benim, takip) } },
            // Ses duyulmadı/anlaşılmadı: ilk istekte kısa bilgi, takipte sessizce uyanma beklemesine dön
            hata = { _ -> anaIs.post {
                if (benim != nesil || kapaniyor) return@post
                if (takip == 0) konus("Sizi duyamadım. İsterseniz tekrar Sezai deyin.", benim) { uyanmayaDon(benim, 1500) }
                else uyanmayaDon(benim, 1500)
            } })
    }

    // "Kamerayı aç, resim çek / ne görüyorsun": tek kare çek, sağlayıcıya yorumlat, sesle anlat
    private fun kameraBak(on: Boolean, benim: Int, sonra: () -> Unit) {
        val saglayici = SohbetSaglayici.secili(this)
        val kasa = AnahtarKasasi(this, saglayici.kasaAdi)
        val anahtarVar = try { !kasa.oku().isNullOrBlank() } catch (_: Exception) { false }
        if (!anahtarVar) { konus("Görüntüyü yorumlamak için önce uygulamadan sohbet bağlantısını kurmanız gerekiyor.", benim) { uyanmayaDon(benim, 1500) }; return }
        konus(if (on) "Ön kameraya bakıyorum." else "Tamam, bakıyorum.", benim) {
            KameraYakala(this).cek(on) { jpeg, mesaj ->
                if (benim != nesil || kapaniyor) return@cek
                if (jpeg == null) { konus(mesaj, benim) { uyanmayaDon(benim, 1500) }; return@cek }
                val ayar = getSharedPreferences("ses_ayari", MODE_PRIVATE).getString("gorsel_model", null)
                val yorum = GorselYorum(kasa, saglayici, ayar)
                yorum.yorumla(jpeg, "Bu resimde ne var? Bana anlat.",
                    sonuc = { metin -> yorum.kapat(); if (benim == nesil && !kapaniyor) konus(metin.take(1200), benim, kesilebilir = true) { sonra() } },
                    hata = { m -> yorum.kapat(); if (benim == nesil && !kapaniyor) konus(m.take(250), benim) { uyanmayaDon(benim, 1500) } })
            }
        }
    }

    private fun yanitla(metin: String, benim: Int, takip: Int) {
        if (SohbetKomutlari.bitirMi(metin)) {
            konus("Tamam, çağırdığınızda buradayım.", benim) { uyanmayaDon(benim, 1500) }; return
        }
        // Ekran metni yalnız yerelde okunur; modele/ağa gönderilmez. Kilitliyken servis metin vermez.
        if (SohbetKomutlari.ekranInceleMi(metin)) {
            val ekran = EkranOkuyucuServisi.disEkranMetni()
            val soz = when {
                ekran == null -> "Ekranı okumak için erişilebilirlik ayarlarından Sezai ekran yardımcısını açmanız gerekiyor."
                ekran.isBlank() -> "Ekrandan okunabilir metin alamadım."
                else -> ekran.replace(Regex("\\s+"), " ").take(600)
            }
            konus(soz, benim) { if (takip < enFazlaTakip) dinleTur(benim, takip + 1) else uyanmayaDon(benim, 1500) }
            return
        }
        // Uygulamadaki gibi: konum/saat/tarih/alarm/zamanlayıcı yerelde çözülür, modele gitmez
        val komut = KomutYorumlayici.yorumla(metin)
        if (komut !is Komut.Bilinmiyor) {
            val devam = { if (takip < enFazlaTakip) dinleTur(benim, takip + 1) else uyanmayaDon(benim, 1500) }
            if (komut is Komut.Kamera) {
                kameraBak(komut.on, benim) { devam() }
            } else if (komut is Komut.KonumGoster) {
                val konum = KonumYardimcisi(this)
                if (!konum.izinVarMi()) { konus("Konum izni verilmemiş. Uygulamadan izin verebilirsiniz.", benim) { uyanmayaDon(benim, 1500) }; return }
                konum.al { _, soz -> if (benim == nesil && !kapaniyor) konus(soz, benim) { devam() } }
            } else {
                val cevap = KomutCalistirici(this).calistir(komut)
                Log.i("Sezai", "ARKAPLAN yerel komut -> '$cevap'")
                konus(cevap, benim) { devam() }
            }
            return
        }
        val saglayici = SohbetSaglayici.secili(this)
        val anahtarVar = try { !AnahtarKasasi(this, saglayici.kasaAdi).oku().isNullOrBlank() } catch (_: Exception) { false }
        if (!anahtarVar) {
            konus("Sohbet için önce uygulamadan bağlantıyı kurmanız gerekiyor.", benim) { uyanmayaDon(benim, 1500) }; return
        }
        if (sohbet == null || sohbetSaglayicisi != saglayici) {
            sohbet?.kapat()
            sohbet = SohbetMotoru(AnahtarKasasi(this, saglayici.kasaAdi), saglayici)
            sohbetSaglayicisi = saglayici
        }
        sohbet?.gonder(metin,
            sonuc = { yanit -> anaIs.post {
                if (benim != nesil || kapaniyor) return@post
                konus(yanit.take(1200), benim, kesilebilir = true) { if (takip < enFazlaTakip) dinleTur(benim, takip + 1) else uyanmayaDon(benim, 1500) }
            } },
            hata = { mesaj -> anaIs.post {
                if (benim == nesil && !kapaniyor) konus(mesaj.take(200), benim) { uyanmayaDon(benim, 1500) }
            } })
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
        nesil++
        sozKesme.kapat()
        dinleme?.kapat(); dinleme = null
        sohbet?.kapat(); sohbet = null
        uyanma?.durdur(); uyanma = null
        konusma?.kapat(); konusma = null
        calisiyor = false
        Log.i("Sezai", "ARKAPLAN servis durdu")
        super.onDestroy()
    }
}
