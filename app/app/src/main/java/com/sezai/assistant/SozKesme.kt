package com.sezai.assistant

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

// Sezai bir yanıtı okurken "dur" veya "sezai" sözünü bekler (prototip). Android'in kendi yankı giderme
// özellikli VOICE_COMMUNICATION kaynağı ve dilbilgisiz (serbest) Vosk çözümü kullanılır: dar dilbilgisi
// telefonun kendi sesini hedef sözcüğe zorlayıp yanlış tetikliyordu. Ses diske/ağa gitmez, metin saklanmaz.
// Model ilk kullanımda tek işçide yüklenir; hazır olunca bekleyen konuşma dinlenir. Kapanış kayıt sonrasına sıralanır.
internal class SozKesme(private val context: Context, private val tetik: () -> Unit) {
    private val anaIs = Handler(Looper.getMainLooper())
    private val modelYolu = File(context.filesDir, "vosk-model-small-tr-0.3")
    private val isler = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val nesil = java.util.concurrent.atomic.AtomicInteger()
    private var model: Model? = null // yalnız tek işçi erişir; kayıt bitmeden kapatılmaz
    @Volatile private var kapandi = false
    @Volatile private var kayit: AudioRecord? = null

    private fun modelHazirla(): Model? {
        if (model == null && File(modelYolu, "final.mdl").isFile) {
            try { LibVosk.vosk_set_log_level(-1); model = Model(modelYolu.absolutePath) }
            catch (e: Exception) { Log.w("Sezai", "SOZKESME model yuklenemedi: ${e.javaClass.simpleName}") }
            catch (e: LinkageError) { Log.w("Sezai", "SOZKESME kutuphane yuklenemedi: ${e.javaClass.simpleName}") }
        }
        return model
    }

    fun hazirla() {
        if (!kapandi) isler.execute { if (!kapandi) modelHazirla() }
    }

    fun baslat() {
        if (kapandi) return
        durdur()
        val benim = nesil.get()
        isler.execute {
            val m = if (!kapandi && benim == nesil.get()) modelHazirla() else null
            if (m != null && !kapandi && benim == nesil.get()) calis(m, benim)
        }
    }

    fun durdur() {
        nesil.incrementAndGet()
        try { kayit?.stop() } catch (_: RuntimeException) {}
    }

    fun kapat() {
        if (kapandi) return
        kapandi = true
        durdur()
        isler.execute { model?.close(); model = null }
        isler.shutdown()
    }

    private fun calis(m: Model, benim: Int) {
        var tanici: Recognizer? = null
        var mikrofon: AudioRecord? = null
        var aec: AcousticEchoCanceler? = null
        var gurultu: NoiseSuppressor? = null
        var duyuldu = false
        var sure = 0L
        var ornek = 0L
        var enerji = 0.0
        var tepe = 0
        var tanimaVar = false
        var susturuldu = false
        try {
            tanici = Recognizer(m, 16000f)
            val enAz = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(enAz > 0) { "TAMPON" }
            mikrofon = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(enAz * 2, 6400))
            check(mikrofon.state == AudioRecord.STATE_INITIALIZED) { "MIKROFON" }
            if (AcousticEchoCanceler.isAvailable()) aec = AcousticEchoCanceler.create(mikrofon.audioSessionId)?.also { it.enabled = true }
            if (NoiseSuppressor.isAvailable()) gurultu = NoiseSuppressor.create(mikrofon.audioSessionId)?.also { it.enabled = true }
            val ses = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (kapandi || benim != nesil.get() || ses.mode != android.media.AudioManager.MODE_NORMAL) return
            kayit = mikrofon
            mikrofon.startRecording()
            check(mikrofon.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "KAYIT" }
            Log.i("Sezai", "SOZKESME basladi aec=${aec?.enabled} ns=${gurultu?.enabled} cihaz=${mikrofon.routedDevice?.type}")
            val veri = ShortArray(1600)
            val bas = System.nanoTime()
            var tur = 0
            while (!kapandi && benim == nesil.get()) {
                val n = mikrofon.read(veri, 0, veri.size)
                if (n <= 0) break
                if (kapandi || benim != nesil.get()) break
                ornek += n
                for (i in 0 until n) {
                    val v = veri[i].toInt()
                    enerji += v.toDouble() * v
                    tepe = maxOf(tepe, kotlin.math.abs(v))
                }
                if (android.os.Build.VERSION.SDK_INT >= 29 && mikrofon.activeRecordingConfiguration?.isClientSilenced == true) susturuldu = true
                if (++tur % 10 == 0 && ses.mode != android.media.AudioManager.MODE_NORMAL) break
                val tam = tanici.acceptWaveForm(veri, n)
                val metin = JSONObject(if (tam) tanici.result else tanici.partialResult).optString(if (tam) "text" else "partial")
                if (metin.isNotBlank()) tanimaVar = true
                val kelimeler = metin.split(' ')
                if ("dur" in kelimeler || "sezai" in kelimeler) { duyuldu = true; sure = (System.nanoTime() - bas) / 1000000; break }
            }
        } catch (e: Exception) {
            Log.w("Sezai", "SOZKESME hata: ${e.javaClass.simpleName}")
        } catch (e: LinkageError) {
            Log.w("Sezai", "SOZKESME kutuphane hata: ${e.javaClass.simpleName}")
        } finally {
            kayit = null
            try { mikrofon?.stop() } catch (_: RuntimeException) {}
            aec?.release(); gurultu?.release(); mikrofon?.release(); tanici?.close()
            val rms = if (ornek > 0) kotlin.math.sqrt(enerji / ornek).toInt() else 0
            Log.i("Sezai", "SOZKESME olcum ornek=$ornek rms=$rms tepe=$tepe tanima=$tanimaVar susturuldu=$susturuldu")
            Log.i("Sezai", "SOZKESME kaynaklar birakildi")
            if (duyuldu) anaIs.post {
                if (!kapandi && benim == nesil.get()) {
                    Log.i("Sezai", "SOZKESME tetik ${sure}ms")
                    durdur(); tetik()
                }
            }
        }
    }
}
