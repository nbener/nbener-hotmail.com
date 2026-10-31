package com.sezai.assistant

import android.os.Debug
import android.os.SystemClock
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

// Tek kayıt iş parçacığında; yalnız iki hedefin bayrakları tutulur, tanıma metni saklanmaz.
// 100ms parçalarla sınırlı debug tanısıdır; çalmayı kesmez, yeni mikrofon açmaz.
internal class YankiCagriTanisi(yol: File) : AutoCloseable {
    private val model: Model
    private val tanici: Recognizer
    // Karşılaştırma: aynı ses dilbilgisiz çözülür; yalnız iki hedefin bayrağı tutulur.
    private val serbest: Recognizer
    private val tampon = ShortArray(1600)
    private var bekleyen = 0
    private var alinan = 0L
    private var cozumlenen = 0L
    private var cpuNs = 0L
    private var enUzunNs = 0L
    private var dur = false
    private var sezai = false
    private var ilkMs = -1L
    // Yalnız kesin sonuçlardan: hedef kelimenin en yüksek güveni ve en uzun süresi (metin tutulmaz).
    private var durKesin = false
    private var sezaiKesin = false
    private var durGuven = -1.0
    private var sezaiGuven = -1.0
    private var serbestDur = false
    private var serbestSezai = false
    private var durSureMs = -1L
    private var sezaiSureMs = -1L
    private var kapali = false

    init {
        check(File(yol, "final.mdl").isFile) { "MODEL_YOK" }
        LibVosk.vosk_set_log_level(-1)
        model = Model(yol.absolutePath)
        try {
            tanici = Recognizer(model, 16000f, "[\"dur\",\"sezai\",\"[unk]\"]")
            tanici.setWords(true)
        }
        catch (e: Throwable) { model.close(); throw e }
        try { serbest = Recognizer(model, 16000f) }
        catch (e: Throwable) { tanici.close(); model.close(); throw e }
    }

    fun ekle(veri: ShortArray, adet: Int) {
        check(!kapali)
        require(adet in 0..veri.size)
        for (i in 0 until adet) {
            tampon[bekleyen++] = veri[i]; alinan++
            if (bekleyen == tampon.size) coz()
        }
    }

    private fun metinOku(json: String, alan: String) {
        val kelimeler = JSONObject(json).optString(alan).split(' ')
        if ("dur" in kelimeler) dur = true
        if ("sezai" in kelimeler) sezai = true
        if (ilkMs < 0 && (dur || sezai)) ilkMs = cozumlenen * 1000 / 16000
        if (alan == "text") kesinOku(json)
    }

    private fun serbestOku(json: String, alan: String) {
        val kelimeler = JSONObject(json).optString(alan).split(' ')
        if ("dur" in kelimeler) serbestDur = true
        if ("sezai" in kelimeler) serbestSezai = true
    }

    private fun kesinOku(json: String) {
        val kelimeler = JSONObject(json).optJSONArray("result") ?: return
        for (i in 0 until kelimeler.length()) {
            val k = kelimeler.getJSONObject(i)
            val guven = k.optDouble("conf", -1.0)
            val sure = ((k.optDouble("end", 0.0) - k.optDouble("start", 0.0)) * 1000).toLong()
            when (k.optString("word")) {
                "dur" -> { durKesin = true; durGuven = maxOf(durGuven, guven); durSureMs = maxOf(durSureMs, sure) }
                "sezai" -> { sezaiKesin = true; sezaiGuven = maxOf(sezaiGuven, guven); sezaiSureMs = maxOf(sezaiSureMs, sure) }
            }
        }
    }

    private fun olc(islem: () -> Unit) {
        val cpu = Debug.threadCpuTimeNanos(); val duvar = SystemClock.elapsedRealtimeNanos()
        try { islem() } finally {
            cpuNs += Debug.threadCpuTimeNanos() - cpu
            enUzunNs = maxOf(enUzunNs, SystemClock.elapsedRealtimeNanos() - duvar)
        }
    }

    private fun coz() {
        cozumlenen += bekleyen
        olc {
            val tam = tanici.acceptWaveForm(tampon, bekleyen)
            metinOku(if (tam) tanici.result else tanici.partialResult, if (tam) "text" else "partial")
            val st = serbest.acceptWaveForm(tampon, bekleyen)
            serbestOku(if (st) serbest.result else serbest.partialResult, if (st) "text" else "partial")
        }
        tampon.fill(0); bekleyen = 0
    }

    fun tamamla() {
        check(!kapali)
        if (bekleyen > 0) coz()
        olc { metinOku(tanici.finalResult, "text"); serbestOku(serbest.finalResult, "text") }
    }

    fun rapor() = JSONObject().put("tanici_ornek", alinan).put("cozumlenen_ornek", cozumlenen)
        .put("dur_duyuldu", dur).put("sezai_duyuldu", sezai)
        .put("dur_kesin", durKesin).put("sezai_kesin", sezaiKesin)
        .put("dur_guven_max", durGuven).put("sezai_guven_max", sezaiGuven)
        .put("dur_sure_ms_max", durSureMs).put("sezai_sure_ms_max", sezaiSureMs)
        .put("serbest_dur", serbestDur).put("serbest_sezai", serbestSezai)
        .put("ilk_cagri_ses_ms", if (ilkMs >= 0) ilkMs else JSONObject.NULL)
        .put("tanici_cpu_ms", cpuNs / 1000000).put("tanici_en_uzun_duvar_ms", enUzunNs / 1000000)
        .put("tanici_birakildi", kapali)

    override fun close() {
        if (kapali) return
        try { serbest.close(); tanici.close() } finally { model.close(); tampon.fill(0); kapali = true }
    }
}
