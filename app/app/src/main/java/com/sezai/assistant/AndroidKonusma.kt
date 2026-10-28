package com.sezai.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

// Android'in kendi TTS motoru (Google/Samsung); ek boyut ve pil maliyeti yok
class AndroidKonusma(context: Context, motor: String? = null, hazir: (Boolean) -> Unit) : KonusmaMotoru {
    private var tts: TextToSpeech? = null
    private var hazirlandi = false
    private val trTr = Locale("tr", "TR")

    init {
        tts = TextToSpeech(context.applicationContext, { durum ->
            hazirlandi = durum == TextToSpeech.SUCCESS &&
                tts?.setLanguage(trTr).let { it != TextToSpeech.LANG_MISSING_DATA && it != TextToSpeech.LANG_NOT_SUPPORTED }
            Log.i("Sezai", "TTS hazir=$hazirlandi motor=${tts?.defaultEngine} sesler=${sesListesi()}")
            tts?.voices?.filter { trMi(it.locale.language) }?.forEach {
                Log.i("Sezai", "TRSES ${it.name} ozellik=${it.features} kalite=${it.quality} cevrimici=${it.isNetworkConnectionRequired}")
            }
            hazir(hazirlandi)
        }, motor)
    }

    override fun hazirMi() = hazirlandi

    override fun konus(metin: String, bitince: (() -> Unit)?) {
        val t = tts ?: return
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { bitince?.invoke() }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { Log.w("Sezai", "TTS hata id=$id") }
        })
        t.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "sezai-konus")
    }

    override fun durdur() { tts?.stop() }

    private fun trMi(dil: String) = dil == "tr" || dil == "tur"

    // Yalnızca kurulu Türkçe sesler; internetsiz çalışanlar (local) başa gelir
    override fun sesListesi(): List<String> =
        tts?.voices?.filter { trMi(it.locale.language) && !it.features.contains("notInstalled") }
            ?.sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
            ?.map { it.name } ?: emptyList()

    override fun sesSec(ad: String): Boolean {
        val v = tts?.voices?.firstOrNull { it.name == ad } ?: return false
        return tts?.setVoice(v) == TextToSpeech.SUCCESS
    }

    override fun kapat() { tts?.stop(); tts?.shutdown(); tts = null }
}
