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
    // %100 üstü ses (en çok %250): TTS parametresi 1.0'ı aşamaz ve LoudnessEnhancer bu cihazda duyulur fark yaratmadı.
    // Bu yüzden sentez dosyaya alınır, PCM örnekleri yumuşak sınırlayıcıyla (tanh) büyütülür ve AudioTrack ile çalınır.
    @Volatile private var guc = 1f
    @Volatile private var guclCalan: android.media.AudioTrack? = null
    fun yuzdeAyarla(yuzde: Int) {
        val y = yuzde.coerceIn(10, 250)
        sesDuzeyi = if (y >= 100) null else y / 100f
        guc = if (y > 100) y / 100f else 1f
    }
    private fun guclCalDurdur() {
        val c = guclCalan; guclCalan = null
        if (c != null) { try { c.setPlaybackPositionUpdateListener(null); c.stop() } catch (_: Exception) {}; try { c.release() } catch (_: Exception) {} }
    }
    // WAV (PCM16) dosyasını okur: (örnekleme hızı, kanal sayısı, örnekler)
    private fun wavOku(f: java.io.File): Triple<Int, Int, ShortArray>? {
        val b = f.readBytes()
        if (b.size < 44 || String(b, 0, 4) != "RIFF") return null
        val bb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val kanal = bb.getShort(22).toInt(); val hz = bb.getInt(24); val bit = bb.getShort(34).toInt()
        if (bit != 16 || kanal !in 1..2 || hz <= 0) return null
        var o = 12
        while (o + 8 <= b.size) {
            val ad = String(b, o, 4); var boy = bb.getInt(o + 4)
            if (ad == "data") {
                if (boy < 0 || o + 8 + boy > b.size) boy = b.size - o - 8
                val n = boy / 2
                return Triple(hz, kanal, ShortArray(n) { bb.getShort(o + 8 + it * 2) })
            }
            o += 8 + boy + (boy and 1)
        }
        return null
    }
    private fun guclCal(f: java.io.File, sira: Int, basladi: () -> Unit, bitti: () -> Unit, hata: () -> Unit) {
        try {
            val (hz, kanal, ornek) = wavOku(f) ?: run { hata(); return }
            if (ornek.isEmpty()) { hata(); return }
            val g = guc
            for (k in ornek.indices) ornek[k] = (32767.0 * kotlin.math.tanh(g * ornek[k] / 32768.0)).toInt().toShort()
            val iz = android.media.AudioTrack.Builder()
                .setAudioAttributes(android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(android.media.AudioFormat.Builder().setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(hz).setChannelMask(if (kanal == 1) android.media.AudioFormat.CHANNEL_OUT_MONO else android.media.AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(ornek.size * 2).setTransferMode(android.media.AudioTrack.MODE_STATIC).build()
            iz.write(ornek, 0, ornek.size)
            val kare = ornek.size / kanal
            if (gorselNesil.get() != sira) { iz.release(); return }
            guclCalDurdur(); guclCalan = iz
            iz.setNotificationMarkerPosition(maxOf(1, kare - 1))
            iz.setPlaybackPositionUpdateListener(object : android.media.AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: android.media.AudioTrack?) {
                    if (gorselNesil.get() == sira && guclCalan === iz) { guclCalDurdur(); bitti() }
                }
                override fun onPeriodicNotification(t: android.media.AudioTrack?) {}
            }, gorselAnaIs)
            iz.play(); basladi()
        } catch (e: Exception) { Log.w("Sezai", "Guclu ses calinamadi: ${e.javaClass.simpleName} ${e.message}"); guclCalDurdur(); hata() }
        finally { f.delete() }
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
        val dosya = if (guc > 1f) java.io.File(context.cacheDir, "sezai-tts-$sira.wav") else null
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
            override fun onStart(id: String?) { if (id == konusmaKimligi && dosya == null) bildir(KonusmaGorselOlayi.Tur.BASLADI, metin) }
            override fun onDone(id: String?) {
                if (id != konusmaKimligi) return
                if (dosya == null) { bildir(KonusmaGorselOlayi.Tur.BITTI); bitince?.invoke(); return }
                if (gorselNesil.get() != sira) { dosya.delete(); return }
                guclCal(dosya, sira, { bildir(KonusmaGorselOlayi.Tur.BASLADI, metin) },
                    { bildir(KonusmaGorselOlayi.Tur.BITTI); bitince?.invoke() },
                    { bildir(KonusmaGorselOlayi.Tur.HATA); hata?.invoke() })
            }
            override fun onStop(id: String?, interrupted: Boolean) { if (id == konusmaKimligi) bildir(KonusmaGorselOlayi.Tur.DURDU) }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == konusmaKimligi) { Log.w("Sezai", "TTS hata"); bildir(KonusmaGorselOlayi.Tur.HATA); hata?.invoke() }
            }
        })
        // Varsayılan çağrılar aynı null parametreyi kullanır; sistem ses ayarı değiştirilmez.
        val parametre = sesDuzeyi?.let { duzey -> Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, duzey) } }
        val sonuc = if (dosya != null) t.synthesizeToFile(metin, parametre, dosya, konusmaKimligi)
                    else t.speak(metin, TextToSpeech.QUEUE_FLUSH, parametre, konusmaKimligi)
        if (sonuc == TextToSpeech.ERROR) { dosya?.delete(); bildir(KonusmaGorselOlayi.Tur.HATA); hata?.invoke() }
    }

    override fun durdur() {
        val sira = gorselNesil.incrementAndGet()
        if (gorselOlayi != null) gorselAnaIs.post {
            if (gorselNesil.get() == sira) gorselOlayi?.invoke(KonusmaGorselOlayi(KonusmaGorselOlayi.Tur.DURDU))
        }
        guclCalDurdur()
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

    override fun kapat() { pcmAkisi = null; pcmTanisi = null; durdur(); tts?.shutdown(); tts = null; gorselOlayi = null }
}

internal data class PcmTaniOlayi(val hz: Int = 0, val bicim: Int = 0, val kanal: Int = 0, val bayt: Int = 0)

// Yalnızca görsel geri bildirim; ses akışını ve ses odağını değiştirmez.
internal data class KonusmaGorselOlayi(val tur: Tur, val metin: String = "") {
    enum class Tur { BASLADI, BITTI, DURDU, HATA }
}
