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
// Model ilk kullanımda yüklenir ve servis kapanana kadar bellekte kalır.
internal class SozKesme(private val context: Context, private val tetik: () -> Unit) {
    private val anaIs = Handler(Looper.getMainLooper())
    private val modelYolu = File(context.filesDir, "vosk-model-small-tr-0.3")
    @Volatile private var model: Model? = null
    @Volatile private var istenen = false
    @Volatile private var kayit: AudioRecord? = null
    private var is_: Thread? = null // yalnız ana iş parçacığında

    fun hazirla() {
        if (model != null || !File(modelYolu, "final.mdl").isFile) return
        Thread({
            try { LibVosk.vosk_set_log_level(-1); model = Model(modelYolu.absolutePath) }
            catch (e: Throwable) { Log.w("Sezai", "SOZKESME model yuklenemedi: ${e.javaClass.simpleName}") }
        }, "sezai-sozkesme-model").start()
    }

    fun baslat() {
        val m = model
        if (m == null) { hazirla(); Log.i("Sezai", "SOZKESME model hazir degil"); return }
        if (is_?.isAlive == true) return
        istenen = true
        is_ = Thread({ calis(m) }, "sezai-sozkesme").also { it.start() }
    }

    fun durdur() {
        istenen = false
        try { kayit?.stop() } catch (_: RuntimeException) {}
    }

    fun kapat() {
        durdur()
        try { is_?.join(1500) } catch (_: InterruptedException) {}
        model?.close(); model = null
    }

    private fun calis(m: Model) {
        var tanici: Recognizer? = null
        var mikrofon: AudioRecord? = null
        var aec: AcousticEchoCanceler? = null
        var gurultu: NoiseSuppressor? = null
        var duyuldu = false
        var sure = 0L
        try {
            tanici = Recognizer(m, 16000f)
            val enAz = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(enAz > 0) { "TAMPON" }
            mikrofon = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(enAz * 2, 6400))
            check(mikrofon.state == AudioRecord.STATE_INITIALIZED) { "MIKROFON" }
            if (AcousticEchoCanceler.isAvailable()) aec = AcousticEchoCanceler.create(mikrofon.audioSessionId)?.also { it.enabled = true }
            if (NoiseSuppressor.isAvailable()) gurultu = NoiseSuppressor.create(mikrofon.audioSessionId)?.also { it.enabled = true }
            if (!istenen) return
            kayit = mikrofon
            mikrofon.startRecording()
            check(mikrofon.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "KAYIT" }
            Log.i("Sezai", "SOZKESME basladi aec=${aec?.enabled} ns=${gurultu?.enabled}")
            val veri = ShortArray(1600)
            val bas = System.nanoTime()
            while (istenen) {
                val n = mikrofon.read(veri, 0, veri.size)
                if (n <= 0) break
                if (!istenen) break
                val tam = tanici.acceptWaveForm(veri, n)
                val metin = JSONObject(if (tam) tanici.result else tanici.partialResult).optString(if (tam) "text" else "partial")
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
            Log.i("Sezai", "SOZKESME kaynaklar birakildi")
            if (duyuldu && istenen) { istenen = false; Log.i("Sezai", "SOZKESME tetik ${sure}ms"); anaIs.post { tetik() } }
        }
    }
}
