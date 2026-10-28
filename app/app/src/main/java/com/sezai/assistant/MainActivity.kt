package com.sezai.assistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// Sezai ses deneme ekranı: konuş, dinle, ses değiştir
class MainActivity : Activity() {
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

        dugme("Konuş") { konusma.konus("Merhaba, ben Sezai. Size nasıl yardımcı olabilirim?") }
        dugme("Dinle") { dinle() }
        dugme("Sesi değiştir") {
            if (sesler.isNotEmpty()) {
                sesSirasi = (sesSirasi + 1) % sesler.size
                konusma.sesSec(sesler[sesSirasi])
                durumYazisi.text = "Ses ${sesSirasi + 1}/${sesler.size}: ${sesler[sesSirasi]}"
                konusma.konus("Merhaba, ben Sezai. Ben ${sesSirasi + 1} numaralı sesim.")
            }
        }
        setContentView(kutu)
    }

    private fun komutVer(metin: String) {
        val cevap = calistirici.calistir(KomutYorumlayici.yorumla(metin))
        Log.i("Sezai", "KOMUT '$metin' -> '$cevap'")
        durumYazisi.text = cevap
        konusma.konus(cevap)
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
        if (kod == 1 && s.firstOrNull() == PackageManager.PERMISSION_GRANTED) dinle()
    }

    override fun onStop() { dinleme.durdur(); konusma.durdur(); super.onStop() }
    override fun onDestroy() { dinleme.kapat(); konusma.kapat(); super.onDestroy() }
}
