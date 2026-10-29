package com.sezai.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

// Android konuşma tanıma; mikrofon yalnızca dinle() çağrılınca açılır (pil için sürekli dinleme yok)
class AndroidDinleme internal constructor(
    private val context: Context,
    private val oturumOlustur: () -> TanimaOturumu = { AndroidTanimaOturumu(context) }
) : DinlemeMotoru {
    private var tanici: TanimaOturumu? = null

    override fun kullanilabilirMi() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun dinle(sonuc: (String) -> Unit, hata: (String) -> Unit) {
        kapat()
        val r = oturumOlustur()
        tanici = r
        val dinleyici = object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                if (tanici !== r) return
                val metin = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                Log.i("Sezai", "STT sonucu alındı")
                kapat()
                if (metin.isNullOrBlank()) hata("Anlayamadım") else sonuc(metin)
            }
            override fun onError(kod: Int) {
                if (tanici !== r) return
                Log.w("Sezai", "STT hata kodu=$kod")
                kapat(); hata(hataMetni(kod))
            }
            override fun onReadyForSpeech(p: Bundle?) { Log.i("Sezai", "STT hazir") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
        }
        try {
            r.baslat(dinleyici, Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            })
        } catch (_: SecurityException) {
            if (tanici === r) { kapat(); hata("Mikrofon izni yok") }
        } catch (_: Exception) {
            if (tanici === r) { kapat(); hata("Dinleme başlatılamadı") }
        }
    }

    private fun hataMetni(kod: Int) = when (kod) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Anlayamadım"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ses duymadım"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "İnternet gerekiyor"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni yok"
        else -> "Hata ($kod)"
    }

    override fun durdur() { tanici?.durdur() }
    override fun kapat() {
        // destroy sırasında gelen eski olaylar yeni oturumu kapatmamalı.
        val eski = tanici
        tanici = null
        eski?.kapat()
    }
}

// Android oturumu ile test oturumu aynı geri çağrı yaşam döngüsünü kullanır.
internal interface TanimaOturumu {
    fun baslat(dinleyici: RecognitionListener, istek: Intent)
    fun durdur()
    fun kapat()
}

private class AndroidTanimaOturumu(context: Context) : TanimaOturumu {
    private val tanici = SpeechRecognizer.createSpeechRecognizer(context)
    override fun baslat(dinleyici: RecognitionListener, istek: Intent) {
        tanici.setRecognitionListener(dinleyici)
        tanici.startListening(istek)
    }
    override fun durdur() { tanici.stopListening() }
    override fun kapat() { tanici.destroy() }
}
