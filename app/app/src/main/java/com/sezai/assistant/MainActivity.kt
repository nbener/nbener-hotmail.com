package com.sezai.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.ScrollView
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
    private var calismaDurumu: TextView? = null
    private val ilerleme by lazy { getSharedPreferences("gelistirme_durumu", MODE_PRIVATE) }
    private val ilerlemeDinleyici = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        runOnUiThread { ilerlemeyiGoster() }
    }
    private var sohbet: SohbetMotoru? = null
    private var sohbetAcik = false
    private var yaziliSohbetAcik = false
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
    private lateinit var calistirici: KomutCalistirici

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            calismaDurumu = TextView(this).apply {
                textSize = 17f
                setTextColor(Color.rgb(225, 240, 255))
                setBackgroundColor(Color.rgb(30, 55, 78))
                setPadding(28, 22, 28, 22)
            }.also { kutu.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 24 }) }
            ilerlemeyiGoster()
        }
        durumYazisi = TextView(this).apply { text = "Merhaba, ben Sezai"; textSize = 28f; setTextColor(Color.BLACK) }
        kutu.addView(durumYazisi)
        fun dugme(yazi: String, tikla: () -> Unit) = Button(this).apply {
            text = yazi; textSize = 22f; setOnClickListener { tikla() }
        }.also { kutu.addView(it, LinearLayout.LayoutParams(-1, 220).apply { topMargin = 32 }) }

        konusma = AndroidKonusma(this, intent.getStringExtra("motor") ?: "com.google.android.tts") { tamam ->
            runOnUiThread {
                sesler = konusma.sesListesi()
                if (!tamam) durumYazisi.text = "Türkçe ses hazır değil"
                intent.getStringExtra("oto")?.let { konusma.konus(it) }
                // test için: yalnızca yorumla (komutu çalıştırma) ya da çalıştır
                intent.getStringExtra("komut")?.let { komutVer(it) }
            }
        }
        dinleme = AndroidDinleme(this)
        // test için: yalnızca yorumla, komutu çalıştırma (TTS'i beklemez)
        intent.getStringExtra("yorumla")?.let { Log.i("Sezai", "YORUM '$it' -> ${KomutYorumlayici.yorumla(it)}") }
        calistirici = KomutCalistirici(this)

        sohbetDugmesi = dugme("Sohbet et") { if (sohbetAcik || yaziliSohbetAcik) sohbetBitir() else sohbetBaslat() }
        // Yanıt uzadığında da sohbeti bitirme düğmesi üstte erişilebilir kalsın.
        kutu.removeView(sohbetDugmesi)
        kutu.addView(sohbetDugmesi, kutu.indexOfChild(durumYazisi))
        mesajAlani = EditText(this).apply {
            hint = "Sezai’ye yazın"
            textSize = 20f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2; maxLines = 3
            filters = arrayOf(android.text.InputFilter.LengthFilter(2000))
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        kutu.addView(mesajAlani, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 24 })
        gonderDugmesi = Button(this).apply {
            text = "Gönder"; textSize = 22f
            setOnClickListener { yaziliMesajGonder() }
        }
        kutu.addView(gonderDugmesi, LinearLayout.LayoutParams(-1, -2))
        dugme("Sohbet bağlantısı") { sohbetBitir(false); baglantiAyarlariniAc() }
        dugme("Konuş") { sohbetBitir(false); konusma.konus("Merhaba, ben Sezai. Size nasıl yardımcı olabilirim?") }
        dugme("Dinle") { sohbetBitir(false); dinle() }
        dugme("Sesi değiştir") {
            sohbetBitir(false)
            if (sesler.isNotEmpty()) {
                sesSirasi = (sesSirasi + 1) % sesler.size
                konusma.sesSec(sesler[sesSirasi])
                durumYazisi.text = "Ses ${sesSirasi + 1}/${sesler.size}: ${sesler[sesSirasi]}"
                konusma.konus("Merhaba, ben Sezai. Ben ${sesSirasi + 1} numaralı sesim.")
            }
        }
        setContentView(ScrollView(this).apply { addView(kutu) })
    }

    private fun komutVer(metin: String) {
        sohbetBitir(false)
        val cevap = calistirici.calistir(KomutYorumlayici.yorumla(metin))
        Log.i("Sezai", "KOMUT '$metin' -> '$cevap'")
        durumYazisi.text = cevap
        konusma.konus(cevap)
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
        sohbetBitir(false)
        sohbet = SohbetMotoru(kasa, saglayici)
        sohbetAcik = true; sohbetNesli++
        sohbetDugmesi.text = "Sohbeti bitir"
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
            if (!anahtarVar) { sohbetBitir(false); baglantiAyarlariniAc(); return }
            sohbetBitir(false)
            sohbet = SohbetMotoru(kasa, saglayici)
            yaziliSohbetAcik = true
            sohbetDugmesi.text = "Sohbeti bitir"
        }
        val motor = sohbet ?: return
        val nesil = sohbetNesli
        gonderDugmesi.isEnabled = false
        gonderDugmesi.text = "Yanıt bekleniyor..."
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
            .hideSoftInputFromWindow(mesajAlani.windowToken, 0)
        durumYazisi.text = "Siz: $metin\n\nSezai düşünüyor..."
        motor.gonder(metin,
            sonuc = { cevap ->
                if (yaziliSohbetAcik && nesil == sohbetNesli) {
                    durumYazisi.text = "Siz: $metin\n\nSezai: $cevap"
                    if (mesajAlani.text.toString().trim() == metin) mesajAlani.text.clear()
                    gonderDugmesi.isEnabled = true; gonderDugmesi.text = "Gönder"
                }
            },
            hata = { mesaj ->
                if (yaziliSohbetAcik && nesil == sohbetNesli) {
                    durumYazisi.text = "Siz: $metin\n\n$mesaj"
                    gonderDugmesi.isEnabled = true; gonderDugmesi.text = "Gönder"
                }
            })
    }

    private fun sohbetDinle(nesil: Int) {
        if (!sohbetAcik || nesil != sohbetNesli) return
        durumYazisi.text = "Dinliyorum..."
        dinleme.dinle(
            sonuc = { metin -> runOnUiThread {
                if (sohbetAcik && nesil == sohbetNesli) {
                    if (SohbetKomutlari.bitirMi(metin)) {
                        sohbetBitir()
                        return@runOnUiThread
                    }
                    durumYazisi.text = "Siz: $metin\n\nSezai düşünüyor..."
                    sohbet?.gonder(metin,
                        sonuc = { cevap ->
                            if (sohbetAcik && nesil == sohbetNesli) {
                                durumYazisi.text = "Siz: $metin\n\nSezai: $cevap"
                                konusma.konus(cevap,
                                    bitince = { anaIs.postDelayed({ sohbetDinle(nesil) }, 350) },
                                    hata = { runOnUiThread {
                                        if (sohbetAcik && nesil == sohbetNesli) {
                                            sohbetBitir(false)
                                            durumYazisi.append("\n\nSes okunamadı. Yanıtı ekrandan okuyabilirsiniz.")
                                        }
                                    } })
                            }
                        },
                        hata = { mesaj ->
                            if (sohbetAcik && nesil == sohbetNesli) { sohbetBitir(false); durumYazisi.text = mesaj }
                        })
                }
            } },
            hata = { mesaj -> runOnUiThread {
                if (sohbetAcik && nesil == sohbetNesli) { sohbetBitir(false); durumYazisi.text = "$mesaj. Sohbet et düğmesiyle tekrar başlayabilirsiniz." }
            } })
    }

    private fun sohbetBitir(mesajGoster: Boolean = true) {
        sohbetAcik = false; yaziliSohbetAcik = false; sohbetNesli++
        anaIs.removeCallbacksAndMessages(null)
        sohbet?.kapat(); sohbet = null; dinleme.kapat(); konusma.durdur()
        if (::sohbetDugmesi.isInitialized) sohbetDugmesi.text = "Sohbet et"
        if (::gonderDugmesi.isInitialized) { gonderDugmesi.isEnabled = true; gonderDugmesi.text = "Gönder" }
        if (mesajGoster) durumYazisi.text = "Sohbet bitti."
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
        ilerleme.registerOnSharedPreferenceChangeListener(ilerlemeDinleyici)
        ilerlemeyiGoster()
    }

    private fun dinle() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1); return
        }
        if (!dinleme.kullanilabilirMi()) { durumYazisi.text = "Konuşma tanıma yok"; return }
        durumYazisi.text = "Dinliyorum..."
        dinleme.dinle(
            sonuc = { runOnUiThread { durumYazisi.text = "Duydum: $it"; komutVer(it) } },
            hata = { runOnUiThread { durumYazisi.text = it } }
        )
    }

    override fun onRequestPermissionsResult(kod: Int, i: Array<out String>, s: IntArray) {
        super.onRequestPermissionsResult(kod, i, s)
        if (s.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            if (kod == 1) dinle() else if (kod == 2) sohbetBaslat()
        } else if (kod == 1 || kod == 2) {
            durumYazisi.text = "Sesli konuşma için mikrofon izni gerekiyor."
        }
    }

    override fun onStop() {
        ilerleme.unregisterOnSharedPreferenceChangeListener(ilerlemeDinleyici)
        sohbetBitir(false)
        dinleme.durdur(); konusma.durdur(); super.onStop()
    }
    override fun onDestroy() { sohbet?.kapat(); dinleme.kapat(); konusma.kapat(); super.onDestroy() }
}
