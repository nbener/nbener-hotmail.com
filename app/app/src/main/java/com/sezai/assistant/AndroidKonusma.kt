package com.sezai.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

// Android'in kendi TTS motoru (Google/Samsung); ek boyut ve pil maliyeti yok
class AndroidKonusma(private val context: Context, motor: String? = null, private val cumleSesDuzeyi: Float? = null, hazir: (Boolean) -> Unit) : KonusmaMotoru {
    init {
        require(cumleSesDuzeyi == null || (cumleSesDuzeyi.isFinite() && cumleSesDuzeyi in 0f..1f))
    }
    // Cümle başına TTS ses düzeyi (0..1); null ise parametre gönderilmez, sistem ses ayarı değiştirilmez.
    @Volatile var sesDuzeyi: Float? = cumleSesDuzeyi
    // %100 üstü ses (en çok %120) yazılımla güçlendirilir: TTS parametresi 1.0'ı aşamaz.
    private var guclendirici: android.media.audiofx.LoudnessEnhancer? = null
    private var oturumKimligi = 0
    @Volatile private var guc = 1f
    fun yuzdeAyarla(yuzde: Int) {
        val y = yuzde.coerceIn(10, 120)
        sesDuzeyi = if (y >= 100) null else y / 100f
        guc = if (y > 100) y / 100f else 1f
    }
    private fun gucUygula(): Int? {
        val g = guc
        if (g <= 1f) return null
        return try {
            if (guclendirici == null) {
                oturumKimligi = (context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager).generateAudioSessionId()
                guclendirici = android.media.audiofx.LoudnessEnhancer(oturumKimligi).also { it.enabled = true }
            }
            guclendirici?.setTargetGain((2000 * Math.log10(g.toDouble())).toInt()) // mB: %120 ≈ 158
            oturumKimligi
        } catch (e: Exception) { Log.w("Sezai", "Ses guclendirme kullanilamadi: ${e.javaClass.simpleName}"); null }
    }
    internal var gorselOlayi: ((KonusmaGorselOlayi) -> Unit)? = null
    // Tanı yalnız biçim/sayım taşır; ham ses tutulmaz veya başka yere aktarılmaz.
    @Volatile internal var pcmTanisi: ((PcmTaniOlayi) -> Unit)? = null
    // Yalnız açıkça bağlanan tüketiciye, callback süresinde kullanılmak üzere PCM.
    // Normal uygulama tüketici bağlamaz; debug hat belleği sınırlayıp diske yazmaz.
    @Volatile internal var pcmAkisi: ((ByteArray) -> Unit)? = null
    private val gorselNesil = AtomicInteger()
    private val gorselAnaIs = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var hazirlandi = false
    private val trTr = Locale("tr", "TR")

    init {
        tts = TextToSpeech(context.applicationContext, { durum ->
            hazirlandi = durum == TextToSpeech.SUCCESS &&
                tts?.setLanguage(trTr).let { it != TextToSpeech.LANG_MISSING_DATA && it != TextToSpeech.LANG_NOT_SUPPORTED }
            tts?.setAudioAttributes(android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
            Log.i("Sezai", "TTS hazir=$hazirlandi motor=${tts?.defaultEngine} sesler=${sesListesi()}")
            tts?.voices?.filter { trMi(it.locale.language) }?.forEach {
                Log.i("Sezai", "TRSES ${it.name} ozellik=${it.features} kalite=${it.quality} cevrimici=${it.isNetworkConnectionRequired}")
            }
            hazir(hazirlandi)
        }, motor)
    }

    override fun hazirMi() = hazirlandi

    override fun konus(metin: String, bitince: (() -> Unit)?, hata: (() -> Unit)?) {
        val sira = gorselNesil.incrementAndGet()
        val konusmaKimligi = "sezai-konus-$sira"
        fun bildir(tur: KonusmaGorselOlayi.Tur, yazi: String = "") {
            if (gorselOlayi != null) gorselAnaIs.post {
                if (gorselNesil.get() == sira) gorselOlayi?.invoke(KonusmaGorselOlayi(tur, yazi))
            }
        }
        val t = tts
        if (t == null || !hazirlandi) { bildir(KonusmaGorselOlayi.Tur.HATA); hata?.invoke(); return }
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            private fun pcmBildir(olay: PcmTaniOlayi) {
                if (gorselNesil.get() == sira) pcmTanisi?.invoke(olay)
            }
            override fun onBeginSynthesis(id: String?, hz: Int, bicim: Int, kanal: Int) {
                if (id == konusmaKimligi && pcmTanisi != null) pcmBildir(PcmTaniOlayi(hz, bicim, kanal))
            }
            override fun onAudioAvailable(id: String?, audio: ByteArray?) {
                if (id == konusmaKimligi && audio != null && gorselNesil.get() == sira) {
                    if (pcmTanisi != null) pcmBildir(PcmTaniOlayi(bayt = audio.size))
                    pcmAkisi?.invoke(audio)
                }
            }
            override fun onStart(id: String?) { if (id == konusmaKimligi) bildir(KonusmaGorselOlayi.Tur.BASLADI, metin) }
            override fun onDone(id: String?) {
                if (id == konusmaKimligi) { bildir(KonusmaGorselOlayi.Tur.BITTI); bitince?.invoke() }
            }
            override fun onStop(id: String?, interrupted: Boolean) { if (id == konusmaKimligi) bildir(KonusmaGorselOlayi.Tur.DURDU) }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == konusmaKimligi) { Log.w("Sezai", "TTS hata"); bildir(KonusmaGorselOlayi.Tur.HATA); hata?.invoke() }
            }
        })
        // Varsayılan çağrılar aynı null parametreyi kullanır; sistem ses ayarı değiştirilmez.
        val oturum = gucUygula()
        val parametre = if (sesDuzeyi == null && oturum == null) null else Bundle().apply {
            sesDuzeyi?.let { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, it) }
            oturum?.let { putInt(TextToSpeech.Engine.KEY_PARAM_SESSION_ID, it) }
        }
        if (t.speak(metin, TextToSpeech.QUEUE_FLUSH, parametre, konusmaKimligi) == TextToSpeech.ERROR) { bildir(KonusmaGorselOlayi.Tur.HATA); hata?.invoke() }
    }

    override fun durdur() {
        val sira = gorselNesil.incrementAndGet()
        if (gorselOlayi != null) gorselAnaIs.post {
            if (gorselNesil.get() == sira) gorselOlayi?.invoke(KonusmaGorselOlayi(KonusmaGorselOlayi.Tur.DURDU))
        }
        tts?.stop()
    }

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

    override fun kapat() { try { guclendirici?.release() } catch (_: Exception) {}; guclendirici = null; pcmAkisi = null; pcmTanisi = null; durdur(); tts?.shutdown(); tts = null; gorselOlayi = null }
}

internal data class PcmTaniOlayi(val hz: Int = 0, val bicim: Int = 0, val kanal: Int = 0, val bayt: Int = 0)

// Yalnızca görsel geri bildirim; ses akışını ve ses odağını değiştirmez.
internal data class KonusmaGorselOlayi(val tur: Tur, val metin: String = "") {
    enum class Tur { BASLADI, BITTI, DURDU, HATA }
}
