package com.sezai.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.TextView
import org.json.JSONObject

// Shell/DUMP ile başlayan tek, en çok 12 saniyelik TTS + mikrofon uyumluluk denemesi.
// Tanınan kişisel metin raporlanmaz, API isteği yok. Üretim dinleme akışına bağlanmaz.
class EszamanliSesTestActivity : Activity() {
    private val anaIs = Handler(Looper.getMainLooper())
    private var tts: AndroidKonusma? = null
    private var tanici: SpeechRecognizer? = null
    private val rapor = JSONObject()
    private var bitti = false
    private var basladi = false
    private var kimlik = ""
    private val odak by lazy { GorusmeOnceligi(this) { bitir("SES_ODAGI_KESILDI") } }
    private lateinit var yazi: TextView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        kimlik = intent.getStringExtra("test_id") ?: "elle"
        val yerel = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        rapor.put("yerel_tanima_var", yerel)
        yazi = TextView(this).apply { text = "Eşzamanlı ses uygunluğu deneniyor. En çok 12 saniye. Konuşmanın arasına Dur veya Sezai diyebilirsiniz."; textSize = 18f; setPadding(32,48,32,32) }
        setContentView(yazi)
        if (state != null) { bitir("YENIDEN_OLUSTURULDU"); return }
        if (intent.getBooleanExtra("yalniz_bilgi", false)) { bitir("YETENEK_OKUNDU"); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { bitir("MIKROFON_IZNI_YOK"); return }
        val yerelIste = intent.getBooleanExtra("yerel", true)
        rapor.put("yerel_istendi", yerelIste)
        if (yerelIste && !yerel) { bitir("YEREL_TANIMA_YOK"); return }
        odak.izle()
        tts = AndroidKonusma(this, "com.google.android.tts") { hazir -> anaIs.post {
            if (bitti) return@post
            if (!hazir) { bitir("TTS_HAZIR_DEGIL"); return@post }
            tts!!.gorselOlayi = { olay ->
                if (olay.tur == KonusmaGorselOlayi.Tur.BASLADI && !basladi && !bitti) {
                    basladi = true; rapor.put("tts_basladi", true)
                    anaIs.postDelayed({ dinlemeyiAc(yerelIste) }, 500)
                }
            }
            if (!odak.hazirlan(true)) { bitir("SES_ODAGI_YOK"); return@post }
            tts!!.konus("Bu kısa bir ses denemesidir. Birlikte sakin ve anlaşılır bir konuşma yapabilmek için konuşma ile dinlemenin aynı anda çalışmasını kontrol ediyoruz. Bu denemede herhangi bir internet sorusu gönderilmiyor.",
                bitince = { anaIs.post { rapor.put("tts_tamamlandi", true); if (!bitti) bitir("DENEME_TAMAMLANDI") } },
                hata = { anaIs.post { bitir("TTS_HATASI") } })
        } }
        anaIs.postDelayed({ bitir("SURE_DOLDU") }, 12000)
    }
    private fun dinlemeyiAc(yerel: Boolean) {
        if (bitti) return
        try {
            tanici = if (yerel && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
                else SpeechRecognizer.createSpeechRecognizer(this)
            tanici!!.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { rapor.put("dinleme_hazir", true) }
                override fun onBeginningOfSpeech() { rapor.put("ses_basladi", true) }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { rapor.put("tanima_hata_kodu", error) }
                override fun onResults(results: Bundle?) { degerlendir(results) }
                override fun onPartialResults(partialResults: Bundle?) { degerlendir(partialResults) }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            tanici!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        } catch (_: Exception) { bitir("TANIMA_BASLATILAMADI") }
    }
    private fun degerlendir(b: Bundle?) {
        val metin = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
        rapor.put("tanima_olayi_var", true)
        if (SohbetKomutlari.sozAlmaMetni(metin) != null) { rapor.put("soz_alma_duyuldu", true); bitir("SOZ_ALMA_DUYULDU") }
    }
    private fun bitir(sonuc: String) {
        if (bitti) return
        bitti = true; anaIs.removeCallbacksAndMessages(null)
        tanici?.destroy(); tanici = null; tts?.kapat(); odak.kapat()
        rapor.put("test_id", kimlik).put("sonuc", sonuc)
        openFileOutput("eszamanli_ses_test.json", MODE_PRIVATE).bufferedWriter().use { it.write(rapor.toString()) }
        yazi.text = "Ses denemesi: $sonuc"
        finish()
    }
    override fun onStop() { bitir("GORUNURLUK_KAYBI"); super.onStop() }
}
