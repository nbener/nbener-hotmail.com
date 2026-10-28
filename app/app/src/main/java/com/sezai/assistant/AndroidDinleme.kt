package com.sezai.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

// Android konuşma tanıma; mikrofon yalnızca dinle() çağrılınca açılır (pil için sürekli dinleme yok)
class AndroidDinleme(private val context: Context) : DinlemeMotoru {
    private var tanici: SpeechRecognizer? = null

    override fun kullanilabilirMi() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun dinle(sonuc: (String) -> Unit, hata: (String) -> Unit) {
        kapat()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        tanici = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                val metin = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                Log.i("Sezai", "STT sonucu alındı")
                kapat()
                if (metin.isNullOrBlank()) hata("Anlayamadım") else sonuc(metin)
            }
            override fun onError(kod: Int) {
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
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        })
    }

    private fun hataMetni(kod: Int) = when (kod) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Anlayamadım"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ses duymadım"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "İnternet gerekiyor"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni yok"
        else -> "Hata ($kod)"
    }

    override fun durdur() { tanici?.stopListening() }
    override fun kapat() { tanici?.destroy(); tanici = null }
}
