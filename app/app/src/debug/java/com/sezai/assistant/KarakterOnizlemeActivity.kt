package com.sezai.assistant

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

// DUMP izni gerekir. Yalnız görsel/sentetik durumlar: mikrofon, TTS ve API çalıştırmaz.
class KarakterOnizlemeActivity : Activity() {
    private val anaIs = Handler(Looper.getMainLooper())
    private val gorunumler = mutableListOf<SezaiKarakterView>()
    private val rapor = mutableListOf<String>()
    private var kimlik = ""
    private var cizimler = 0
    private var bitti = false
    private fun kontrol(ad: String, dogru: Boolean) { rapor.add((if (dogru) "PASS " else "FAIL ") + ad) }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        kimlik = intent.getStringExtra("test_id") ?: "elle"
        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(18, 10, 18, 10)
            setBackgroundColor(Color.rgb(243,247,248))
        }
        kok.addView(TextView(this).apply {
            text = "Sezai · karakter önizlemesi\nSentetik durumlar · ses/mikrofon/ağ yok"; textSize = 15f
            setTextColor(Color.rgb(23,46,61)); setPadding(8,4,8,12)
        })
        val isimler = listOf("Beklemede", "Dinliyor", "Düşünüyor", "Konuşma ritmi", "Kısa gülümseme", "Şefkat · gece")
        for (satir in 0..2) {
            val row = LinearLayout(this)
            for (sutun in 0..1) {
                val i = satir*2+sutun
                val panel = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(if (i==5) Color.rgb(29,48,43) else Color.rgb(243,247,248))
                }
                panel.addView(TextView(this).apply {
                    text = isimler[i]; textSize = 13f; gravity = android.view.Gravity.CENTER
                    setTextColor(if (i==5) Color.rgb(231,241,232) else Color.rgb(23,46,61))
                })
                val v = SezaiKarakterView(this).apply { geceOnizleme = i==5 }
                gorunumler.add(v)
                panel.addView(v, LinearLayout.LayoutParams(-1, 0, 1f))
                row.addView(panel, LinearLayout.LayoutParams(0, -1, 1f))
            }
            kok.addView(row, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        setContentView(kok)
        gorunumler[0].cizimGozlemcisi = { cizimler++ }
        anaIs.postDelayed({
            gorunumler[1].durumAyarla(SezaiKarakterView.Durum.DINLIYOR)
            gorunumler[2].durumAyarla(SezaiKarakterView.Durum.DUSUNUYOR)
            gorunumler[3].konusmaBasladi("Merhaba. Ben Sezai, bugün nasılsınız? İyi, açık, umutlu, merhaba.")
            gorunumler[4].durumAyarla(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.MUTLU)
            gorunumler[5].durumAyarla(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI)
            kontrol("Altı ifade hedefi doğru", gorunumler[1].durum==SezaiKarakterView.Durum.DINLIYOR &&
                gorunumler[2].durum==SezaiKarakterView.Durum.DUSUNUYOR && gorunumler[3].durum==SezaiKarakterView.Durum.KONUSUYOR &&
                gorunumler[4].duygu==SezaiKarakterView.Duygu.MUTLU && gorunumler[5].duygu==SezaiKarakterView.Duygu.SEFKATLI)
        }, 250)
        // Önizlemenin ilk 5 saniyesi görsel inceleme ve rastgele göz kırpma içindir.
        anaIs.postDelayed({
            val baslangic = cizimler
            anaIs.postDelayed({
                kontrol("Bekleme çizimi 30 fps altında", cizimler-baslangic <= 30)
                gorunumler[0].hareketiAzalt = true
                anaIs.postDelayed({
                    val donuk = cizimler
                    anaIs.postDelayed({
                        kontrol("Azaltılmış harekette kare kuyruğu durur", !gorunumler[0].kareBekliyor && cizimler==donuk)
                        gorunumler[0].hareketiAzalt = false
                        gorunumler[0].sadeMod = true
                        anaIs.postDelayed({
                            kontrol("Sade araç görünümünde kare planlanmaz", !gorunumler[0].kareBekliyor)
                            gorunumler[0].sadeMod = false
                            gorunumler[0].ekranDurumu(false)
                            anaIs.postDelayed({
                                kontrol("Ekran kapalı sinyali kareleri durdurur", !gorunumler[0].kareBekliyor)
                                gorunumler.forEach { it.etkinlestir(false) }
                                anaIs.postDelayed({
                                    val son = cizimler
                                    anaIs.postDelayed({
                                        kontrol("Yaşam döngüsü durunca çizim sürmez", gorunumler.none { it.kareBekliyor } && cizimler==son)
                                        gorunumler.forEach { it.etkinlestir(true) }
                                        gorunumler[0].ekranDurumu(true)
                                        sonucuYaz()
                                    }, 350)
                                }, 180)
                            }, 150)
                        }, 150)
                    }, 350)
                }, 180)
            }, 1000)
        }, 5000)
    }
    private fun sonucuYaz() {
        bitti = true
        openFileOutput("karakter_test_sonuc.json", MODE_PRIVATE).bufferedWriter().use {
            it.write(JSONObject().put("test_id", kimlik).put("rapor", org.json.JSONArray(rapor)).toString())
        }
    }
    override fun onStart() { super.onStart(); gorunumler.forEach { it.etkinlestir(true) } }
    override fun onStop() {
        gorunumler.forEach { it.etkinlestir(false) }
        anaIs.removeCallbacksAndMessages(null)
        if (!bitti) { kontrol("Test görünürlüğünü kaybetmeden tamamlandı", false); sonucuYaz() }
        super.onStop()
    }
}
