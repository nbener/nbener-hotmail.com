package com.sezai.assistant

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

// Uygulama açık ve boştayken "Sezai" sözcüğünü bekler (yalnızca bu sözcüğe kısıtlı Vosk dilbilgisi).
// Ses diske/ağa gitmez, metin saklanmaz. Sezai konuşurken, sohbette, görüşmede ve arka planda çalışmaz.
// Her durumda çağıran taraf durdur() der; mikrofon ve model iş parçacığı bitince bırakılır.
internal class SezaiUyanma(private val context: Context, private val uyandi: () -> Unit) {
    private val anaIs = Handler(Looper.getMainLooper())
    private val modelYolu = File(context.filesDir, "vosk-model-small-tr-0.3")
    @Volatile private var istenen = false
    private var calisiyor = false // yalnız ana iş parçacığında okunur/yazılır
    @Volatile private var kayit: AudioRecord? = null

    fun modelVarMi() = File(modelYolu, "final.mdl").isFile

    fun baslat() {
        istenen = true
        if (calisiyor || !modelVarMi()) return
        calisiyor = true
        Thread({ calis() }, "sezai-uyanma").start()
    }

    fun durdur() {
        istenen = false
        // Okuma beklemesinde takılmasın
        try { kayit?.stop() } catch (_: RuntimeException) {}
    }

    private fun calis() {
        var model: Model? = null
        var tanici: Recognizer? = null
        var mikrofon: AudioRecord? = null
        var duyuldu = false
        var yenidenDene = false // yalnız dışarıdan durdurulup hemen tekrar istenirse; hata sonrası döngü yok
        try {
            LibVosk.vosk_set_log_level(-1)
            model = Model(modelYolu.absolutePath)
            tanici = Recognizer(model, 16000f, "[\"sezai\", \"[unk]\"]")
            val enAz = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(enAz > 0) { "TAMPON" }
            mikrofon = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(enAz * 2, 6400))
            check(mikrofon.state == AudioRecord.STATE_INITIALIZED) { "MIKROFON" }
            val ses = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (!istenen || ses.mode != AudioManager.MODE_NORMAL) return
            kayit = mikrofon
            mikrofon.startRecording()
            val veri = ShortArray(1600)
            var tur = 0
            while (istenen) {
                val n = mikrofon.read(veri, 0, veri.size)
                if (n <= 0) { yenidenDene = mikrofon.recordingState != AudioRecord.RECORDSTATE_RECORDING; break }
                if (!istenen) break
                if (++tur % 20 == 0 && ses.mode != AudioManager.MODE_NORMAL) break // görüşme başladı
                val tam = tanici.acceptWaveForm(veri, n)
                val metin = JSONObject(if (tam) tanici.result else tanici.partialResult).optString(if (tam) "text" else "partial")
                if (metin.split(' ').contains("sezai")) { duyuldu = true; break }
            }
        } catch (e: Exception) {
            Log.w("Sezai", "UYANMA hata: ${e.javaClass.simpleName}")
        } catch (e: LinkageError) {
            Log.w("Sezai", "UYANMA kutuphane hata: ${e.javaClass.simpleName}")
        } finally {
            kayit = null
            try { mikrofon?.stop() } catch (_: RuntimeException) {}
            mikrofon?.release(); tanici?.close(); model?.close()
            anaIs.post {
                calisiyor = false
                if (duyuldu && istenen) { istenen = false; Log.i("Sezai", "UYANMA sezai duyuldu"); uyandi() }
                else if (istenen && yenidenDene) baslat()
                else istenen = false
            }
        }
    }
}
