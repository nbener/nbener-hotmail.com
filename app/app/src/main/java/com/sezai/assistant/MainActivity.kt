package com.sezai.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.ScrollView
import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Sezai ses deneme ekranı: konuş, dinle, ses değiştir
class MainActivity : Activity() {
    companion object {
        // false yapınca özgün S simgesine dönülür; eski çizim aşağıda aynen korunur.
        private const val YENI_KARAKTER_ACIK = true
    }
    private var karakter: SezaiKarakterView? = null
    private var calismaDurumu: TextView? = null
    private val ilerleme by lazy { getSharedPreferences("gelistirme_durumu", MODE_PRIVATE) }
    private val ilerlemeDinleyici = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        runOnUiThread { ilerlemeyiGoster() }
    }
    private val sesOnceligi by lazy {
        GorusmeOnceligi(this) {
            sohbetDuraklat()
            durumYazisi.text = "Görüşme veya başka ses kullanımı nedeniyle Sezai duraklatıldı. Sohbetin konusu korunuyor."
        }
    }
    private var sohbet: SohbetMotoru? = null
    private var sohbetSaglayicisi: SohbetSaglayici? = null
    private var sohbetDokumu = SohbetDokumu.ortak
    private data class SohbetDevri(val motor: SohbetMotoru?, val saglayici: SohbetSaglayici?, val dokum: SohbetDokumu)
    private var sohbetAcik = false
    private var yaziliSohbetAcik = false
    private lateinit var webSecimi: android.widget.CheckBox
    private lateinit var mesajAlani: EditText
    private lateinit var gonderDugmesi: Button
    private var sohbetNesli = 0
    private lateinit var sohbetDugmesi: Button
    private val anaIs = Handler(Looper.getMainLooper())
    private lateinit var konusma: KonusmaMotoru
    private lateinit var dinleme: DinlemeMotoru
    private lateinit var durumYazisi: TextView
    private var sesler: List<String> = emptyList()
    private var sesSirasi = 0
    private val uyanma by lazy { SezaiUyanma(this) { sezaiDendi() } }
    private var gorunur = false
    private var elleDinliyor = false
    private var konusuyor = false
    private var gecmisOkunuyor = false
    private val gecmisOkuyucu by lazy { SiraliSeslendirme { metin, bitince, hata -> seslendir(metin, bitince, hata) } }
    private val konum by lazy { KonumYardimcisi(this) }
    private val sesAyari by lazy {
        getSharedPreferences("ses_ayari", MODE_PRIVATE).also { o ->
            // Kullanıcı isteği (1 Kasım): konuşma sesi %250; bir kez uygulanır, sonra çubuk serbest.
            if (!o.getBoolean("tts_250_uygulandi", false)) o.edit().putInt("tts_seviye", 250).putBoolean("tts_250_uygulandi", true).apply()
        }
    }
    private val VARSAYILAN_SES = "tr-tr-x-ama-local"
    private lateinit var calistirici: KomutCalistirici

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (lastNonConfigurationInstance as? SohbetDevri)?.let {
            sohbet = it.motor; sohbetSaglayicisi = it.saglayici
            sohbetDokumu = it.dokum
        }
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            isFocusableInTouchMode = true
        }
        val baslik = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        if (YENI_KARAKTER_ACIK) {
            karakter = SezaiKarakterView(this).apply { setOnClickListener { karakterTiklandi() } }
            baslik.addView(karakter, LinearLayout.LayoutParams(dp(104), dp(112)))
        } else baslik.addView(TextView(this).apply {
            text = "S"; textSize = 27f; gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE); background = kart("#087F82", 18)
        }, LinearLayout.LayoutParams(dp(56), dp(56)))
        baslik.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0)
            addView(TextView(this@MainActivity).apply {
                text = "Sezai"; textSize = 28f; setTextColor(Color.parseColor(r("#172E3D")))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Sesli ve yazılı yol arkadaşınız"; textSize = 14f
                setTextColor(Color.parseColor(r("#536B78")))
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        kutu.addView(baslik, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            calismaDurumu = TextView(this).apply {
                textSize = 12f; maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.parseColor(r("#405B6A")))
                background = kart("#E6EFF3", 14)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                contentDescription = "Son geliştirme durumu. Ayrıntıyı açmak için dokunun."
                setOnClickListener { maxLines = if (maxLines == 3) Int.MAX_VALUE else 3 }
            }.also { kutu.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }) }
            ilerlemeyiGoster()
        }
        durumYazisi = TextView(this).apply {
            text = "Merhaba, ben Sezai.\nSize nasıl yardımcı olabilirim?"
            textSize = 19f; setTextColor(Color.parseColor(r("#172E3D")))
            autoLinkMask = android.text.util.Linkify.WEB_URLS
            linksClickable = true
            setLinkTextColor(Color.parseColor(rLink()))
            setLineSpacing(dp(5).toFloat(), 1f)
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = kart("#FFFFFF", 20, "#DFE8ED")
            minHeight = dp(112); setTextIsSelectable(true)
        }
        kutu.addView(durumYazisi, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        fun dugme(yazi: String, tikla: () -> Unit) = Button(this).apply {
            text = yazi; dugmeGorunumu(this, false); setOnClickListener { tikla() }
        }.also { kutu.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }) }

        konusma = AndroidKonusma(this, intent.getStringExtra("motor") ?: "com.google.android.tts") { tamam ->
            runOnUiThread {
                sesler = konusma.sesListesi()
                sesAyari.getInt("tts_seviye", 100).coerceIn(10, 250).let { y -> (konusma as? AndroidKonusma)?.yuzdeAyarla(y) }
                // Kayıtlı ses yoksa varsayılan: 2 numaralı ses (ama-local); kullanıcı sonra değiştirebilir
                val kayitli = sesAyari.getString("ses_adi", VARSAYILAN_SES) ?: VARSAYILAN_SES
                val sira = sesler.indexOf(kayitli)
                val uygulandi = sira >= 0 && konusma.sesSec(kayitli)
                if (uygulandi) sesSirasi = sira
                Log.i("Sezai", "SES varsayilan=$kayitli sira=${sira + 1} uygulandi=$uygulandi")
                if (!tamam) durumYazisi.text = "Türkçe ses hazır değil"
                intent.getStringExtra("oto")?.let { seslendir(it) }
                // test için: yalnızca yorumla (komutu çalıştırma) ya da çalıştır
                intent.getStringExtra("komut")?.let { komutVer(it) }
            }
        }
        (konusma as AndroidKonusma).sozAlindi = {
            if (gorunur && konusuyor && !BildirimKumanda.sessizMi(this)) {
                Log.i("Sezai", "EKRAN soz kesildi")
                val sohbetVardi = sohbetAcik
                sohbetDuraklat() // eski yanıt ve sıralı okuma geri çağrılarını iptal eder, geçmişi korur
                durumYazisi.text = "Sizi dinliyorum."
                anaIs.postDelayed({
                    if (gorunur && !BildirimKumanda.sessizMi(this)) {
                        if (sohbetVardi) {
                            sohbetAcik = true
                            sohbetDugmesi.text = "Dinlemeyi durdur"
                            sohbetDinle(sohbetNesli)
                        } else sezaiDendi()
                    }
                }, 200)
            }
        }
        (konusma as AndroidKonusma).gorselOlayi = { olay -> runOnUiThread {
            when (olay.tur) {
                KonusmaGorselOlayi.Tur.BASLADI -> {
                    karakterDurumu(SezaiKarakterView.Durum.KONUSUYOR, yanitDuygusu(olay.metin))
                    karakter?.konusmaBasladi(olay.metin)
                }
                KonusmaGorselOlayi.Tur.HATA -> { konusuyor = false; uyanmaGuncelle(); karakterDurumu(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI) }
                KonusmaGorselOlayi.Tur.BITTI, KonusmaGorselOlayi.Tur.DURDU -> {
                    konusuyor = false; uyanmaGuncelle()
                    if (karakter?.durum == SezaiKarakterView.Durum.KONUSUYOR) karakter?.konusmaBitti()
                }
            }
        } }
        dinleme = AndroidDinleme(this)
        // test için: yalnızca yorumla, komutu çalıştırma (TTS'i beklemez)
        intent.getStringExtra("yorumla")?.let { Log.i("Sezai", "YORUM '$it' -> ${KomutYorumlayici.yorumla(it)}") }
        calistirici = KomutCalistirici(this)

        sohbetDugmesi = dugme("Sesli sohbet et") {
            if (sohbetAcik) {
                sohbetDuraklat()
                durumYazisi.append("\n\nDinleme durdu. Aynı sohbete yazarak veya konuşarak devam edebilirsiniz.")
            } else sohbetBaslat()
        }
        dugmeGorunumu(sohbetDugmesi, true)
        // Uzun yanıtlarda sesli oturum düğmesi yanıtın üstünde kalır.
        kutu.removeView(sohbetDugmesi)
        kutu.addView(sohbetDugmesi, kutu.indexOfChild(durumYazisi))
        webSecimi = android.widget.CheckBox(this).apply {
            text = "Webde araştır · Groq (kota/ücret)"; textSize = 14f
            setTextColor(Color.parseColor(r("#405B6A"))); minHeight = dp(48)
            buttonTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.parseColor(rLink()), Color.parseColor(r("#405B6A"))))
        }
        kutu.addView(webSecimi, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        mesajAlani = EditText(this).apply {
            hint = "Sezai’ye yazın"
            textSize = 17f
            setTextColor(Color.parseColor(r("#172E3D")))
            setHintTextColor(Color.parseColor(r("#607884")))
            background = kart("#FFFFFF", 16, "#C6D7DF")
            setPadding(dp(16), dp(12), dp(16), dp(12))
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2; maxLines = 3
            filters = arrayOf(android.text.InputFilter.LengthFilter(2000))
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        kutu.addView(mesajAlani, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        gonderDugmesi = Button(this).apply {
            text = "Gönder"; dugmeGorunumu(this, true)
            setOnClickListener { yaziliMesajGonder() }
        }
        kutu.addView(gonderDugmesi, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val yeniSohbetDugmesi = Button(this).apply {
            text = "Yeni sohbet"; dugmeGorunumu(this, false)
            setOnClickListener { sohbetBitir(); mesajAlani.text.clear(); webSecimi.isChecked = false }
        }
        val baglantiDugmesi = dugme("Sohbet bağlantısı") { sohbetDuraklat(); baglantiAyarlariniAc() }
        kutu.removeView(baglantiDugmesi)
        kutu.addView(LinearLayout(this).apply {
            isBaselineAligned = false
            addView(yeniSohbetDugmesi, LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = dp(5) })
            addView(baglantiDugmesi, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(5) })
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        dugme("Konuşma geçmişi") { konusmaGecmisiniAc() }
        dugme(if (gece) "Görünüm: Gece (gündüze geç)" else "Görünüm: Gündüz (geceye geç)") {
            getSharedPreferences("gorunum", MODE_PRIVATE).edit().putBoolean("gece", !gece).apply()
            recreate()
        }
        dugme("Ekran yardımcısı") {
            sohbetDuraklat()
            startActivity(android.content.Intent(this, EkranOkumaAyarActivity::class.java))
        }
        kutu.addView(TextView(this).apply {
            text = "Ses araçları"; textSize = 13f
            setTextColor(Color.parseColor(r("#536B78")))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22); bottomMargin = dp(2) })
        val uyanmaDugmesi = dugme("") {}
        fun uyanmaYazisi() { uyanmaDugmesi.text = "Sezai deyince uyan: " + (if (uyanmaAyariAcik()) "Açık" else "Kapalı") }
        uyanmaYazisi()
        uyanmaDugmesi.setOnClickListener {
            sesAyari.edit().putBoolean("uyanma", !uyanmaAyariAcik()).apply()
            uyanmaYazisi(); uyanmaGuncelle()
            durumYazisi.text = if (uyanmaAyariAcik()) "Uygulama açıkken Sezai dediğinizde sizi dinlerim. Bu sırada mikrofon açık kalır."
                else "Sezai deyince uyanma kapatıldı. Mikrofon kapalı."
        }
        val sesYazisi = TextView(this).apply { textSize = 14f; setTextColor(Color.parseColor(r("#536B78"))) }
        fun sesYazisiniGuncelle(y: Int) { sesYazisi.text = "Konuşma sesi: %$y (telefon medya sesi ayrıdır)" }
        val sesCubugu = android.widget.SeekBar(this).apply {
            max = 240 // %10..%250
            val kayitliYuzde = sesAyari.getInt("tts_seviye", 100).coerceIn(10, 250)
            progress = kayitliYuzde - 10
            sesYazisiniGuncelle(kayitliYuzde)
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: android.widget.SeekBar?, deger: Int, kullanici: Boolean) {
                    val y = deger + 10
                    sesYazisiniGuncelle(y)
                    if (kullanici) { sesAyari.edit().putInt("tts_seviye", y).apply(); (konusma as? AndroidKonusma)?.yuzdeAyarla(y) }
                }
                override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(sb: android.widget.SeekBar?) { sohbetDuraklat(); seslendir("Ses bu kadar.") }
            })
        }
        kutu.addView(sesYazisi, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        kutu.addView(sesCubugu, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        val sessizBtn = dugme("") { BildirimKumanda.sessizAyarla(this, !BildirimKumanda.sessizMi(this)) }
        sessizDugmesi = sessizBtn
        sessizYazisiGuncelle()
        val arkaPlanDugmesi = dugme("") {}
        fun arkaPlanYazisi() { arkaPlanDugmesi.text = "Kilitliyken de dinle: " + (if (arkaPlanAyariAcik()) "Açık" else "Kapalı") }
        arkaPlanYazisi()
        arkaPlanDugmesi.setOnClickListener {
            val ac = !arkaPlanAyariAcik()
            sesAyari.edit().putBoolean("arka_plan", ac)
                .putBoolean("arka_plan_kullanici_kapatti", !ac).apply()
            if (ac) arkaPlanBaslat() else SezaiArkaPlanServisi.durdur(this)
            arkaPlanYazisi(); uyanmaGuncelle()
            durumYazisi.text = if (ac) "Kilitliyken de Sezai deyince yanıt veririm. Bildirimde mikrofon açık görünür."
                else "Arka plan dinleme kapatıldı. Mikrofon kapalı."
        }
        dugme("Konuş") { sohbetDuraklat(); seslendir("Merhaba, ben Sezai. Size nasıl yardımcı olabilirim?") }
        dugme("Dinle") { sohbetDuraklat(); dinle() }
        dugme("Sesi değiştir") {
            sohbetDuraklat()
            if (sesler.isNotEmpty()) {
                sesSirasi = (sesSirasi + 1) % sesler.size
                konusma.sesSec(sesler[sesSirasi])
                sesAyari.edit().putString("ses_adi", sesler[sesSirasi]).apply()
                durumYazisi.text = "Ses ${sesSirasi + 1}/${sesler.size}: ${sesler[sesSirasi]}"
                seslendir("Merhaba, ben Sezai. Ben ${sesSirasi + 1} numaralı sesim.")
            }
        }
        dugme("Çıkış (Sezai'yi tamamen kapat)") { BildirimKumanda.tamCikis(this) }
        window.statusBarColor = Color.parseColor(r("#F3F7F8")); window.navigationBarColor = Color.parseColor(r("#F3F7F8"))
        window.decorView.systemUiVisibility = if (gece) 0 else (android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        karakter?.geceOnizleme = gece
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(r("#F3F7F8")))
            isFillViewport = true; clipToPadding = false
            addView(kutu)
        })
        webSecimi.isChecked = savedInstanceState?.getBoolean("web_secimi", false) ?: false
        savedInstanceState?.getString("mesaj_taslagi")?.let { mesajAlani.setText(it) }
        savedInstanceState?.getString("son_yanit")?.let { durumYazisi.text = it }
        if (sohbetDokumu.metin().isNotBlank()) sohbetYazisiniGoster()
    }

    private fun sohbetYazisiniGoster(anlik: String = "") {
        durumYazisi.text = sohbetDokumu.gorunum(anlik)
    }

    private fun karakterDurumu(durum: SezaiKarakterView.Durum, his: SezaiKarakterView.Duygu = SezaiKarakterView.Duygu.DOGAL) {
        karakter?.durumAyarla(durum, his)
    }
    private fun yanitDuygusu(metin: String): SezaiKarakterView.Duygu {
        val yazi = metin.lowercase(java.util.Locale("tr", "TR"))
        return when {
            listOf("üzgünüm", "başınız sağ olsun", "kötü haber", "hata oluştu", "yanıt alınamadı").any { it in yazi } -> SezaiKarakterView.Duygu.SEFKATLI
            listOf("tebrik", "sevindim", "güzel haber").any { it in yazi } && "değil" !in yazi -> SezaiKarakterView.Duygu.MUTLU
            else -> SezaiKarakterView.Duygu.DOGAL
        }
    }

    // Gece/gündüz görünümü: kullanıcı düğmesiyle seçilir, "gorunum" ayarında saklanır. Açık palet anahtar, koyu karşılığı değerdir.
    private var bildirimIzniIstendi = false
    private var sessizDugmesi: android.widget.Button? = null
    private val gece by lazy { getSharedPreferences("gorunum", MODE_PRIVATE).getBoolean("gece", true) }
    private val koyuPalet = mapOf(
        "#F3F7F8" to "#0F1A20", "#FFFFFF" to "#1B2A33", "#172E3D" to "#EAF2F5", "#536B78" to "#9FB5C0",
        "#405B6A" to "#B8CBD4", "#E6EFF3" to "#24363F", "#DFE8ED" to "#2E424C", "#C6D7DF" to "#3A505B",
        "#607884" to "#8CA3AE", "#234552" to "#D8E6EC", "#D8E4E9" to "#33474F")
    private fun r(acik: String): String = if (gece) koyuPalet[acik.uppercase()] ?: acik else acik
    private fun rLink(): String = if (gece) "#5FD3D6" else "#087F82"

    private fun dp(deger: Int): Int = (deger * resources.displayMetrics.density + 0.5f).toInt()

    private fun kart(renk: String, yaricap: Int, cerceve: String? = null) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(r(renk))); cornerRadius = dp(yaricap).toFloat()
            if (cerceve != null) setStroke(dp(1), Color.parseColor(r(cerceve)))
        }

    private fun dugmeGorunumu(dugme: Button, ana: Boolean) {
        dugme.isAllCaps = false; dugme.textSize = 16f
        dugme.minHeight = dp(52); dugme.minimumHeight = dp(52)
        dugme.setPadding(dp(12), dp(10), dp(12), dp(10))
        dugme.setTextColor(android.content.res.ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Color.parseColor(r(if (ana) "#D8ECEC" else "#6D818C")), if (ana) Color.WHITE else Color.parseColor(r("#234552")))))
        dugme.background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.parseColor(if (ana) "#334CFFF2" else "#18087F82")),
            kart(if (ana) "#087F82" else "#FFFFFF", 14, if (ana) null else "#D8E4E9"), null)
    }

    private fun seslendir(metin: String, bitince: (() -> Unit)? = null, hata: (() -> Unit)? = null) {
        konusuyor = true; uyanma.durdur() // kendi sesini "Sezai" diye duymasın
        if (!sesOnceligi.hazirlan(true)) {
            konusuyor = false
            sohbetDuraklat()
            durumYazisi.text = "Görüşme veya başka ses kullanımı sırasında Sezai seslendirme yapmıyor."
            return
        }
        val nesil = sohbetNesli
        konusma.konus(metin,
            bitince = { runOnUiThread {
                if (nesil == sohbetNesli) {
                    if (sohbetAcik) sesOnceligi.birakOdak() else sesOnceligi.durdur()
                    bitince?.invoke()
                }
            } },
            hata = { runOnUiThread {
                if (nesil == sohbetNesli) {
                    sesOnceligi.durdur()
                    hata?.invoke()
                    karakterDurumu(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI)
                }
            } })
    }

    // "Sezai deyince uyan": yalnız ekran açık, uygulama boşta (sohbet/konuşma/dinleme yok) iken mikrofonu dinler
    private fun uyanmaAyariAcik() = sesAyari.getBoolean("uyanma", true)
    private fun arkaPlanAyariAcik() = sesAyari.getBoolean("arka_plan", true)
    private fun arkaPlanBaslat() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1); return
        }
        if (!BildirimKumanda.bildirimIzniVar(this)) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4)
        }
        SezaiArkaPlanServisi.baslat(this)
    }
    private fun uyanmaGuncelle() {
        // Arka plan servisi mikrofonu tutarken uygulama içi uyanma ikinci mikrofon açmaz
        val olmali = !SezaiArkaPlanServisi.calisiyor && gorunur && hasWindowFocus() && !sohbetAcik && !yaziliSohbetAcik && !konusuyor && !gecmisOkunuyor && !elleDinliyor && uyanmaAyariAcik() && !BildirimKumanda.sessizMi(this) &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && uyanma.modelVarMi()
        if (olmali) uyanma.baslat() else uyanma.durdur()
    }
    // Karaktere dokununca dinleme başlar: anahtar varsa sohbet, yoksa tek seferlik dinleme. Meşgulken yok sayılır.
    private fun karakterTiklandi() {
        if (BildirimKumanda.sessizMi(this)) { durumYazisi.text = "Sessiz moddasınız. Çıkmak için Sessiz mod düğmesine basın."; return }
        if (sohbetAcik || elleDinliyor || konusuyor || gecmisOkunuyor) return
        val saglayici = SohbetSaglayici.secili(this)
        val anahtarVar = try { !AnahtarKasasi(this, saglayici.kasaAdi).oku().isNullOrBlank() } catch (_: Exception) { false }
        if (anahtarVar) sohbetBaslat() else dinle()
    }

    private fun sezaiDendi() {
        if (!gorunur) return
        Log.i("Sezai", "UYANMA tetiklendi")
        durumYazisi.text = "Buyurun, sizi dinliyorum."
        val saglayici = SohbetSaglayici.secili(this)
        val anahtarVar = try { !AnahtarKasasi(this, saglayici.kasaAdi).oku().isNullOrBlank() } catch (_: Exception) { false }
        if (anahtarVar && !sohbetAcik) sohbetBaslat() else dinle()
    }

    // Konumu tek seferlik alır, ekranda gösterir ve söyler; sohbet açıksa (nesil) dinlemeye döner
    private fun konumGoster(nesil: Int?) {
        if (!konum.izinVarMi()) {
            durumYazisi.text = "Konumunuzu göstermek için izin gerekiyor. Açılan pencerede izin verin, sonra tekrar isteyin."
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 3)
            return
        }
        durumYazisi.text = "Konumunuzu alıyorum..."
        konum.al { yazi, soz ->
            durumYazisi.text = yazi
            Log.i("Sezai", "KONUM sonuc alindi")
            if (sohbetAcik && nesil != null && nesil == sohbetNesli)
                sohbet?.ekleGecmis("neredeyim", soz)
            if (nesil == null) seslendir(soz)
            else if (sohbetAcik && nesil == sohbetNesli) seslendir(soz, bitince = { anaIs.postDelayed({ sohbetDinle(nesil) }, 350) })
        }
    }

    // Tek kare çekip sağlayıcıya yorumlatır ve sesle anlatır; sohbet açıksa (nesil) dinlemeye döner
    private fun kameraBak(on: Boolean, nesil: Int?) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            durumYazisi.text = "Kamera için izin gerekiyor. Açılan pencerede izin verin, sonra tekrar isteyin."
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 5); return
        }
        val saglayici = SohbetSaglayici.secili(this)
        val kasa = AnahtarKasasi(this, saglayici.kasaAdi)
        val anahtarVar = try { !kasa.oku().isNullOrBlank() } catch (_: Exception) { false }
        if (!anahtarVar) { durumYazisi.text = "Görüntüyü yorumlamak için önce Sohbet bağlantısından anahtarı girin."; return }
        durumYazisi.text = "Kameraya bakıyorum..."
        fun devam() { if (nesil != null && sohbetAcik && nesil == sohbetNesli) anaIs.postDelayed({ sohbetDinle(nesil) }, 350) }
        KameraYakala(this).cek(on) { jpeg, mesaj ->
            if (jpeg == null) { durumYazisi.text = mesaj; seslendir(mesaj, bitince = { devam() }); return@cek }
            val yorum = GorselYorum(kasa, saglayici, sesAyari.getString("gorsel_model", null))
            yorum.yorumla(jpeg, "Bu resimde ne var? Bana anlat.",
                sonuc = { metin -> yorum.kapat(); durumYazisi.text = metin; seslendir(metin.take(2800), bitince = { devam() }) },
                hata = { m -> yorum.kapat(); durumYazisi.text = m; seslendir(m, bitince = { devam() }) })
        }
    }

    private fun ekraniIncele() {
        sohbetDuraklat()
        val metin = EkranOkuyucuServisi.disEkranMetni()
        if (metin == null) {
            durumYazisi.text = "Ekranı incelemek için Android erişilebilirlik ayarlarından Sezai ekran okuyucusunu açın."
            return
        }
        EkranIncelemeBellek.metin = metin
        startActivity(android.content.Intent(this, EkranIncelemeActivity::class.java))
    }

    private var sessizOnayBekliyor = false // sessiz moda geçmeden önce onay

    private fun komutVer(metin: String) {
        sohbetDuraklat()
        if (sessizOnayBekliyor) {
            sessizOnayBekliyor = false
            if (SessizOnayi.evetMi(metin) || SessizOnayi.hayirMi(metin)) {
                val cevap = if (SessizOnayi.evetMi(metin)) calistirici.calistir(Komut.Sessiz(true)) else "Tamam, sessiz moda geçmedim."
                Log.i("Sezai", "KOMUT sessiz mod onay yaniti -> '$cevap'")
                sohbetDokumu.ekle(metin, cevap); sohbetYazisiniGoster(); seslendir(cevap)
                return
            }
        }
        if (SohbetKomutlari.ekranInceleMi(metin)) { ekraniIncele(); return }
        if (SohbetKomutlari.gecmisOkuMu(metin)) { gecmisiSesliOku(null); return }
        val komut = KomutYorumlayici.yorumla(metin)
        if (komut is Komut.KonumGoster) { konumGoster(null); return }
        if (komut is Komut.Kamera) { kameraBak(komut.on, null); return }
        if (komut is Komut.Sessiz && komut.ac) {
            sessizOnayBekliyor = true
            Log.i("Sezai", "KOMUT sessiz mod onay istendi")
            sohbetDokumu.ekle(metin, SessizOnayi.SORU); sohbetYazisiniGoster(); seslendir(SessizOnayi.SORU)
            return
        }
        val cevap = calistirici.calistir(komut)
        Log.i("Sezai", "KOMUT '$metin' -> '$cevap'")
        sohbetDokumu.ekle(metin, cevap)
        sohbetYazisiniGoster()
        seslendir(cevap)
    }

    private fun baglantiAyarlariniAc() {
        val saglayicilar = SohbetSaglayici.entries
        var secilen = SohbetSaglayici.secili(this)
        val alan = EditText(this).apply {
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        val aciklama = TextView(this)
        fun yenile() {
            alan.hint = "${secilen.adi} API anahtarı"
            aciklama.text = "${secilen.aciklama()}\nKonuşmanızın yazıya çevrilmiş hali ${secilen.adi} hizmetine gönderilir. Anahtar bu telefonda şifreli saklanır. Ücretli hesaplarda ücret oluşabilir."
        }
        val secim = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item, saglayicilar.map { it.adi })
            setSelection(saglayicilar.indexOf(secilen))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val yeni = saglayicilar[position]
                    if (yeni != secilen) { alan.text.clear(); alan.error = null; secilen = yeni }
                    yenile()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
        yenile()
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(36, 12, 36, 12)
            addView(secim); addView(aciklama); addView(alan)
        }
        val pencere = AlertDialog.Builder(this).setTitle("Sohbet bağlantısı").setView(kutu)
            .setPositiveButton("Kaydet", null).setNegativeButton("Vazgeç", null)
            .setNeutralButton("Bağlantıyı kaldır") { _, _ ->
                try {
                    AnahtarKasasi(this, secilen.kasaAdi).sil()
                    if (sohbetSaglayicisi == secilen) sohbetBitir(false)
                    durumYazisi.text = "${secilen.adi} bağlantısı kaldırıldı."
                } catch (_: Exception) { durumYazisi.text = "Bağlantı ayarı kaldırılamadı." }
            }.create()
        pencere.show()
        pencere.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        pencere.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val anahtar = alan.text.toString().trim()
            val kasa = AnahtarKasasi(this, secilen.kasaAdi)
            if ((anahtar.isBlank() && !kasa.varMi()) || anahtar.any { it.isWhitespace() }) {
                alan.error = "Geçerli bir API anahtarı girin."
            } else try {
                if (anahtar.isNotBlank()) kasa.kaydet(anahtar)
                secilen.sec(this)
                alan.text.clear()
                durumYazisi.text = "${secilen.adi} bağlantısı kaydedildi. Sohbet et düğmesine basabilirsiniz."
                pencere.dismiss()
            } catch (_: Exception) { alan.error = "Bağlantı güvenli biçimde kaydedilemedi." }
        }
        pencere.setOnDismissListener { alan.text.clear() }
    }

    private fun sohbetBaslat() {
        val saglayici = SohbetSaglayici.secili(this)
        val kasa = AnahtarKasasi(this, saglayici.kasaAdi)
        val anahtarVar = try { !kasa.oku().isNullOrBlank() } catch (_: Exception) { false }
        if (!anahtarVar) { baglantiAyarlariniAc(); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2); return
        }
        if (!dinleme.kullanilabilirMi()) { durumYazisi.text = "Konuşma tanıma yok"; return }
        if (!konusma.hazirMi()) { durumYazisi.text = "Ses motoru hazırlanıyor. Biraz sonra tekrar deneyin."; return }
        sohbetDuraklat()
        sohbetHazirla(kasa, saglayici)
        sohbetAcik = true; sohbetNesli++
        sohbetDugmesi.text = "Dinlemeyi durdur"
        sohbetDinle(sohbetNesli)
    }

    private fun yaziliMesajGonder() {
        val metin = mesajAlani.text.toString().trim()
        if (metin.isBlank()) { mesajAlani.error = "Bir mesaj yazın."; return }
        if (!gonderDugmesi.isEnabled) return
        if (!yaziliSohbetAcik) {
            val saglayici = SohbetSaglayici.secili(this)
            val kasa = AnahtarKasasi(this, saglayici.kasaAdi)
            val anahtarVar = try { !kasa.oku().isNullOrBlank() } catch (_: Exception) { false }
            if (!anahtarVar) { sohbetDuraklat(); baglantiAyarlariniAc(); return }
            sohbetDuraklat()
            sohbetHazirla(kasa, saglayici)
            yaziliSohbetAcik = true
        }
        val web = webSecimi.isChecked || WebIstegi.istendiMi(metin)
        val motor = sohbet ?: return
        val nesil = sohbetNesli
        gonderDugmesi.isEnabled = false
        gonderDugmesi.text = "Yanıt bekleniyor..."
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
            .hideSoftInputFromWindow(mesajAlani.windowToken, 0)
        karakterDurumu(SezaiKarakterView.Durum.DUSUNUYOR)
        sohbetYazisiniGoster("Siz: $metin\n\n" + if (web) "Web araştırması isteniyor..." else "Sezai düşünüyor...")
        motor.gonder(metin,
            sonuc = { cevap ->
                if (yaziliSohbetAcik && nesil == sohbetNesli) {
                    sohbetDokumu.ekle(metin, cevap)
                    karakterDurumu(SezaiKarakterView.Durum.BEKLEME, yanitDuygusu(cevap))
                    sohbetYazisiniGoster()
                    if (mesajAlani.text.toString().trim() == metin) mesajAlani.text.clear()
                    gonderDugmesi.isEnabled = true; gonderDugmesi.text = "Gönder"
                }
            },
            hata = { mesaj ->
                if (yaziliSohbetAcik && nesil == sohbetNesli) {
                    karakterDurumu(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI)
                    sohbetYazisiniGoster("Siz: $metin\n\n$mesaj")
                    gonderDugmesi.isEnabled = true; gonderDugmesi.text = "Gönder"
                }
            }, webAramasi = web)
    }

    private fun sohbetDinle(nesil: Int) {
        if (!sohbetAcik || nesil != sohbetNesli) return
        if (!sesOnceligi.hazirlan(false)) {
            sohbetDuraklat()
            durumYazisi.text = "Görüşme sırasında mikrofon açılmıyor. Görüşmeden sonra tekrar başlatabilirsiniz."
            return
        }
        karakterDurumu(SezaiKarakterView.Durum.DINLIYOR)
        sohbetYazisiniGoster("Dinliyorum...")
        dinleme.dinle(
            sonuc = { metin -> runOnUiThread {
                if (sohbetAcik && nesil == sohbetNesli) {
                    if (SohbetKomutlari.bitirMi(metin)) {
                        sohbetDuraklat()
                        sohbetYazisiniGoster("Dinleme durdu. Sohbetin konusu korunuyor.")
                        return@runOnUiThread
                    }
                    val sozAlma = SohbetKomutlari.sozAlmaMetni(metin)
                    if (sozAlma != null) {
                        konusma.durdur(); sohbet?.iptal()
                        if (sozAlma.isBlank()) {
                            sohbetYazisiniGoster("Seni dinliyorum. Konuşmanın konusu korunuyor.")
                            anaIs.postDelayed({ sohbetDinle(nesil) }, 200)
                            return@runOnUiThread
                        }
                    }
                    val istekMetni = sozAlma ?: metin
                    if (SohbetKomutlari.ekranInceleMi(istekMetni)) { ekraniIncele(); return@runOnUiThread }
                    if (SohbetKomutlari.gecmisOkuMu(istekMetni)) { gecmisiSesliOku(nesil); return@runOnUiThread }
                    if (KomutYorumlayici.yorumla(istekMetni) is Komut.KonumGoster) { konumGoster(nesil); return@runOnUiThread }
                    (KomutYorumlayici.yorumla(istekMetni) as? Komut.Kamera)?.let { kameraBak(it.on, nesil); return@runOnUiThread }
                    karakterDurumu(SezaiKarakterView.Durum.DUSUNUYOR)
                    sohbetYazisiniGoster("Siz: $istekMetni\n\nSezai düşünüyor...")
                    val webAc = WebIstegi.istendiMi(istekMetni) || WebIstegi.gercekZamanliMi(istekMetni)
                    sohbet?.gonder(istekMetni,
                        sonuc = { cevap ->
                            if (sohbetAcik && nesil == sohbetNesli) {
                                sohbetDokumu.ekle(istekMetni, cevap)
                                sohbetYazisiniGoster()
                                val sesliCevap = if (cevap.length > 3000) cevap.take(2800) + "… Devamını ekrandan okuyabilirsiniz." else cevap
                                seslendir(sesliCevap,
                                    bitince = { anaIs.postDelayed({ sohbetDinle(nesil) }, 350) },
                                    hata = { runOnUiThread {
                                        if (sohbetAcik && nesil == sohbetNesli) {
                                            sohbetDuraklat()
                                            durumYazisi.append("\n\nSes okunamadı. Yanıtı ekrandan okuyabilirsiniz.")
                                        }
                                    } })
                            }
                        },
                        hata = { mesaj ->
                            if (sohbetAcik && nesil == sohbetNesli) { sohbetDuraklat(); karakterDurumu(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI); sohbetYazisiniGoster(mesaj) }
                        }, webAramasi = webAc)
                }
            } },
            hata = { mesaj -> runOnUiThread {
                if (sohbetAcik && nesil == sohbetNesli) { sohbetDuraklat(); karakterDurumu(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI); sohbetYazisiniGoster("$mesaj. Sohbet et düğmesiyle tekrar başlayabilirsiniz.") }
            } })
    }

    private fun sohbetHazirla(kasa: AnahtarKasasi, saglayici: SohbetSaglayici) {
        if (sohbet == null || sohbetSaglayicisi != saglayici) {
            sohbet?.kapat()
            sohbet = SohbetMotoru(kasa, saglayici, { NotDefteri(applicationContext, "hafiza.txt").son(10) })
            sohbetSaglayicisi = saglayici
        }
    }

    private fun sohbetDuraklat() {
        gecmisOkuyucu.durdur(); gecmisOkunuyor = false
        uyanma.durdur()
        elleDinliyor = false; konusuyor = false
        sohbetAcik = false; yaziliSohbetAcik = false; sohbetNesli++
        anaIs.removeCallbacksAndMessages(null)
        sesOnceligi.durdur()
        sohbet?.iptal(); dinleme.kapat(); konusma.durdur()
        karakterDurumu(SezaiKarakterView.Durum.BEKLEME)
        if (::sohbetDugmesi.isInitialized) sohbetDugmesi.text = "Sesli sohbet et"
        if (::gonderDugmesi.isInitialized) { gonderDugmesi.isEnabled = true; gonderDugmesi.text = "Gönder" }
        // Aynı olayda sohbet/konuşma başlatılabilir; son duruma göre boştaki dinlemeyi yenile.
        anaIs.post { uyanmaGuncelle() }
    }

    private fun sohbetBitir(mesajGoster: Boolean = true) {
        sohbetDuraklat()
        sohbet?.kapat(); sohbet = null; sohbetSaglayicisi = null
        sohbetDokumu.temizle()
        if (mesajGoster) durumYazisi.text = "Yeni sohbet başladı. Size nasıl yardımcı olabilirim?"
    }

    private fun konusmaGecmisiniAc() {
        sohbetDuraklat()
        val yazi = TextView(this).apply {
            text = sohbetDokumu.metin().ifBlank { "Bu oturumda henüz tamamlanmış bir mesaj yok." }
            textSize = 18f
            setTextColor(Color.parseColor(r("#172E3D")))
            setBackgroundColor(Color.parseColor(r("#FFFFFF")))
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setLineSpacing(dp(4).toFloat(), 1f)
            autoLinkMask = android.text.util.Linkify.WEB_URLS
            linksClickable = true
            setLinkTextColor(Color.parseColor(rLink()))
            setTextIsSelectable(true)
        }
        AlertDialog.Builder(this).setTitle("Konuşma geçmişi")
            .setView(ScrollView(this).apply { addView(yazi) })
            .setNeutralButton("Sesli oku") { _, _ -> gecmisiSesliOku(null) }
            .setPositiveButton("Kapat", null).show()
    }

    private fun gecmisiSesliOku(sohbetSirasi: Int?) {
        if (BildirimKumanda.sessizMi(this)) {
            sohbetDuraklat()
            sohbetYazisiniGoster("Sessiz mod açık. Geçmişi sesli dinlemek için Sessiz mod düğmesinden konuşmaya dönün.")
            return
        }
        if (!konusma.hazirMi()) {
            sohbetDuraklat()
            sohbetYazisiniGoster("Ses motoru hazır değil. Biraz sonra Sesli oku düğmesini tekrar deneyin.")
            return
        }
        val metin = sohbetDokumu.sesMetni().ifBlank { "Bu oturumda henüz tamamlanmış bir mesaj yok." }
        val nesil = sohbetNesli
        gecmisOkunuyor = true; uyanma.durdur()
        sohbetYazisiniGoster("Konuşma geçmişini okuyorum.")
        gecmisOkuyucu.baslat(SiraliSeslendirme.parcala(metin), bitince = {
            gecmisOkunuyor = false
            if (nesil == sohbetNesli) {
                sohbetYazisiniGoster("Konuşma geçmişi okundu.")
                if (sohbetSirasi != null && sohbetAcik) anaIs.postDelayed({ sohbetDinle(sohbetSirasi) }, 350)
                else uyanmaGuncelle()
            }
        }, hata = {
            gecmisOkunuyor = false
            if (nesil == sohbetNesli) { sohbetDuraklat(); sohbetYazisiniGoster("Geçmiş seslendirilemedi. Konuşma geçmişi düğmesinden okuyabilirsiniz.") }
        })
    }

    private fun ilerlemeyiGoster() {
        val mesaj = ilerleme.getString("mesaj", null)
        val zaman = ilerleme.getLong("zaman", 0L)
        val saat = if (zaman > 0) DateTimeFormatter.ofPattern("dd.MM HH:mm")
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(zaman)) else ""
        calismaDurumu?.text = if (mesaj == null) "Çalışma durumu\nHenüz bildirim yok."
            else "Çalışma durumu · Son bildirim $saat\n$mesaj"
    }

    override fun onStart() {
        super.onStart()
        karakter?.etkinlestir(true)
        sesOnceligi.izle()
        ilerleme.registerOnSharedPreferenceChangeListener(ilerlemeDinleyici)
        ilerlemeyiGoster()
    }

    override fun onResume() {
        super.onResume()
        gorunur = true
        if (sohbetDokumu.metin().isNotBlank()) sohbetYazisiniGoster()
        BildirimKumanda.arayuzAcik = true
        BildirimKumanda.anaEkran = this
        acikBildirimiGoster()
        sesAyari.registerOnSharedPreferenceChangeListener(sessizDinleyici)
        sesAyari.registerOnSharedPreferenceChangeListener(sesDinleyici)
        getSharedPreferences("gorunum", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(gorunumDinleyici)
        sessizYazisiGuncelle()
        // Model kuruluysa arka planı etkinleştir; kullanıcı daha önce kapatmışsa koru
        if (uyanma.modelVarMi() && !sesAyari.getBoolean("arka_plan_kullanici_kapatti", false))
            sesAyari.edit().putBoolean("arka_plan", true).apply()
        // Ön planda tek mikrofon sahibi Activity; arka plan servisiyle çift dinlemeyi önler.
        if (SezaiArkaPlanServisi.calisiyor) {
            SezaiArkaPlanServisi.durdur(this) // arka_plan tercihini değiştirmez
            anaIs.postDelayed({ if (gorunur) uyanmaGuncelle() }, 300)
        }
        uyanmaGuncelle()
    }

    // Uygulama açıkken durum çubuğunda sessiz bildirim (Sessiz mod / Arka plan düğmeleriyle); servis yok
    private fun sessizYazisiGuncelle() {
        sessizDugmesi?.text = if (BildirimKumanda.sessizMi(this)) "Sessiz mod: Açık (konuşmaya dön)" else "Sessiz mod: Kapalı (sustur)"
    }

    private fun acikBildirimiGoster() {
        if (!BildirimKumanda.bildirimIzniVar(this)) {
            if (!bildirimIzniIstendi) { bildirimIzniIstendi = true; requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4) }
            return
        }
        BildirimKumanda.acikBildirimiGuncelle(this)
    }

    // Bildirimden sessiz mod değişince çalışan ekranı da hemen susturur/uyandırır
    private val gorunumDinleyici = android.content.SharedPreferences.OnSharedPreferenceChangeListener { o, anahtar ->
        if (anahtar == "gece" && o.getBoolean("gece", true) != gece) recreate()
    }
    private val sesDinleyici = android.content.SharedPreferences.OnSharedPreferenceChangeListener { o, anahtar ->
        if (anahtar == "tts_seviye") (konusma as? AndroidKonusma)?.yuzdeAyarla(o.getInt("tts_seviye", 100).coerceIn(10, 250))
    }
    private val sessizDinleyici = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, anahtar ->
        if (anahtar == "sessiz") {
            if (BildirimKumanda.sessizMi(this)) { konusma.durdur(); uyanma.durdur() }
            uyanmaGuncelle(); sessizYazisiGuncelle()
        }
    }

    override fun onPause() {
        sesAyari.unregisterOnSharedPreferenceChangeListener(sessizDinleyici)
        sesAyari.unregisterOnSharedPreferenceChangeListener(sesDinleyici)
        getSharedPreferences("gorunum", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(gorunumDinleyici)
        gorunur = false
        uyanma.durdur()
        // onStop'ta Activity artık görünmez: Android 14 mikrofon servisini reddedebilir.
        // Kullanıcının açık arka plan tercihini, görünürlük bitmeden devret.
        if (arkaPlanAyariAcik() && !BildirimKumanda.sessizMi(this) && !isChangingConfigurations) {
            sohbetDuraklat()
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                try { SezaiArkaPlanServisi.baslat(this) }
                catch (e: RuntimeException) {
                    Log.w("Sezai", "ARKAPLAN devir baslatilamadi: ${e.javaClass.simpleName}")
                    sohbetYazisiniGoster("Kilitliyken dinleme başlatılamadı. Sezai'yi açıp Kilitliyken de dinle seçeneğini yeniden açın.")
                }
            }
        }
        super.onPause()
    }

    override fun onWindowFocusChanged(odakVar: Boolean) {
        super.onWindowFocusChanged(odakVar)
        uyanmaGuncelle()
    }

    private fun dinle() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1); return
        }
        if (!dinleme.kullanilabilirMi()) { durumYazisi.text = "Konuşma tanıma yok"; return }
        if (!sesOnceligi.hazirlan(false)) {
            sohbetDuraklat()
            durumYazisi.text = "Görüşme sırasında mikrofon açılmıyor. Görüşmeden sonra tekrar başlatabilirsiniz."
            return
        }
        elleDinliyor = true; uyanma.durdur()
        karakterDurumu(SezaiKarakterView.Durum.DINLIYOR)
        durumYazisi.text = "Dinliyorum..."
        dinleme.dinle(
            sonuc = { runOnUiThread { elleDinliyor = false; durumYazisi.text = "Duydum: $it"; komutVer(it) } },
            hata = { runOnUiThread { elleDinliyor = false; uyanmaGuncelle(); sesOnceligi.durdur(); karakterDurumu(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI); durumYazisi.text = it } }
        )
    }

    override fun onRequestPermissionsResult(kod: Int, i: Array<out String>, s: IntArray) {
        super.onRequestPermissionsResult(kod, i, s)
        if (s.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            if (kod == 1) dinle() else if (kod == 2) sohbetBaslat() else if (kod == 3) konumGoster(null) else if (kod == 5) kameraBak(false, null)
        } else if (kod == 3) {
            durumYazisi.text = "Konum izni verilmedi, konumunuzu gösteremem."
        } else if (kod == 1 || kod == 2) {
            durumYazisi.text = "Sesli konuşma için mikrofon izni gerekiyor."
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("yorumla")?.let { Log.i("Sezai", "YORUM '$it' -> ${KomutYorumlayici.yorumla(it)}") }
        intent.getStringExtra("oto")?.let { seslendir(it) }
        intent.getStringExtra("komut")?.let { komutVer(it) }
    }

    override fun onStop() {
        gorunur = false
        uyanma.durdur()
        karakter?.etkinlestir(false)
        ilerleme.unregisterOnSharedPreferenceChangeListener(ilerlemeDinleyici)
        sohbetDuraklat()
        sesOnceligi.kapat()
        dinleme.durdur(); konusma.durdur()
        super.onStop()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("web_secimi", webSecimi.isChecked)
        outState.putString("mesaj_taslagi", mesajAlani.text.toString())
        // Android'in durum kaydına bütün dökümü taşımıyoruz; önceki gibi yalnız son tur.
        outState.putString("son_yanit", sohbetDokumu.sonTurMetni().ifBlank { durumYazisi.text.toString() })
        super.onSaveInstanceState(outState)
    }

    override fun onRetainNonConfigurationInstance(): Any = SohbetDevri(sohbet, sohbetSaglayicisi, sohbetDokumu)

    override fun onDestroy() {
        if (BildirimKumanda.anaEkran === this) BildirimKumanda.anaEkran = null
        if (!isChangingConfigurations) getSystemService(android.app.NotificationManager::class.java).cancel(BildirimKumanda.ACIK_BILDIRIM)
        if (!isChangingConfigurations) BildirimKumanda.arayuzAcik = false
        if (!isChangingConfigurations) sohbet?.kapat()
        dinleme.kapat(); konusma.kapat(); super.onDestroy()
    }
}
