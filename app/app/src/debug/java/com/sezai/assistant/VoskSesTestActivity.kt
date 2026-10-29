package com.sezai.assistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

// Yalnız DUMP yetkili debug denemesi. Ses/metin diske veya ağa gönderilmez.
// Varsayılan bilgi modu mikrofon açmaz. Ses denemesi en çok 12 saniye sürer.
class VoskSesTestActivity : Activity() {
    private val anaIs = Handler(Looper.getMainLooper())
    private val etkin = AtomicBoolean(true)
    private val rapor = JSONObject()
    private var kayit: AudioRecord? = null
    private var tts: AndroidKonusma? = null
    private var bitti = false
    private var calisan = false
    private var sonuc = "TAMAMLANDI"
    private var ttsSuruyor = false
    private var ttsBaslangici = 0L
    private var ilkSoz = false
    private var kip = "bilgi"
    private var ttsGenlik = 1f
    private lateinit var yazi: TextView
    private val odak by lazy { GorusmeOnceligi(this) { bitir("SES_ODAGI_KESILDI") } }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        kip = intent.getStringExtra("kip") ?: "bilgi"
        rapor.put("test_id", intent.getStringExtra("test_id") ?: "elle").put("kip", kip)
        ttsGenlik = intent.getFloatExtra("tts_genlik", 1f)
        yazi = TextView(this).apply {
            text = "Yerel Vosk denemesi: $kip. Hazırlanıyor…"; textSize = 18f; setPadding(32,48,32,32)
        }
        setContentView(yazi)
        if (!ttsGenlik.isFinite() || ttsGenlik !in 0f..1f) { bitir("GECERSIZ_TTS_GENLIK"); return }
        rapor.put("tts_istenen_genlik", ttsGenlik.toDouble())
        if (state != null) { bitir("YENIDEN_OLUSTURULDU"); return }
        if (kip !in setOf("bilgi", "dinle", "birlikte", "yanki")) { bitir("GECERSIZ_KIP"); return }
        if (kip != "bilgi" && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            bitir("MIKROFON_IZNI_YOK"); return
        }
        val yol = File(filesDir, "vosk-model-small-tr-0.3")
        if (!File(yol, "final.mdl").isFile) { bitir("MODEL_YOK"); return }
        odak.izle()
        if (kip != "bilgi" && !odak.hazirlan(kip != "dinle")) { bitir("SES_ODAGI_YOK"); return }
        // Model açılışına da sınır koy; ekran kapanırsa worker sonucu yeni işlem başlatmaz.
        anaIs.postDelayed({ bitir("HAZIRLIK_ZAMAN_ASIMI") }, 30000)
        calisan = true
        Thread({ dene(yol) }, "sezai-vosk-deneme").start()
    }

    private fun dene(yol: File) {
        var model: Model? = null
        var tanici: Recognizer? = null
        var mikrofon: AudioRecord? = null
        var yanki: AcousticEchoCanceler? = null
        val olcum = JSONObject()
        val bas = SystemClock.elapsedRealtime()
        var cpu = 0L
        var ornek = 0L
        var tepe = 0
        var doygun = 0L
        var cozumDuvarNs = 0L
        var enUzunCozumNs = 0L
        var kayitBaslangici = 0L
        try {
            LibVosk.vosk_set_log_level(-1)
            model = Model(yol.absolutePath)
            tanici = Recognizer(model, 16000f)
            olcum.put("model_acildi", true).put("model_acilis_ms", SystemClock.elapsedRealtime() - bas)
            if (kip == "bilgi") {
                tanici.acceptWaveForm(ShortArray(1600), 1600)
            } else if (etkin.get()) {
                val enAz = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                check(enAz > 0) { "TAMPON_DESTEKLENMIYOR" }
                mikrofon = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(enAz * 2, 6400))
                check(mikrofon.state == AudioRecord.STATE_INITIALIZED) { "MIKROFON_HAZIR_DEGIL" }
                if (AcousticEchoCanceler.isAvailable()) {
                    yanki = AcousticEchoCanceler.create(mikrofon.audioSessionId)
                    yanki?.enabled = true
                }
                olcum.put("aec_mevcut", AcousticEchoCanceler.isAvailable()).put("aec_etkin", yanki?.enabled == true)
                // onStop ile startRecording yarışmasın; mikrofon yaşam döngüsünü aynı kilitle koru.
                synchronized(etkin) {
                    if (etkin.get()) { kayit = mikrofon; mikrofon.startRecording() }
                }
                if (etkin.get()) {
                    kayitBaslangici = SystemClock.elapsedRealtime()
                    olcum.put("mikrofon_basladi", mikrofon.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                    olcum.put("kayit_kaynagi", mikrofon.audioSource)
                        .put("kayit_ornekleme_hz", mikrofon.sampleRate)
                        .put("kayit_tampon_kare", mikrofon.bufferSizeInFrames)
                    // Kimlik/adres/ham ses yerine yalnız cihaz türü ve sayısal ses ayarları.
                    mikrofon.routedDevice?.let { olcum.put("giris_cihaz_turu", it.type) }
                    val sesYonetimi = getSystemService(AUDIO_SERVICE) as AudioManager
                    val nitelik = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                    val akis = nitelik.volumeControlStream
                    olcum.put("tts_ses_akisi", akis).put("ses_modu", sesYonetimi.mode)
                    if (akis >= 0) {
                        olcum.put("tts_ses_duzeyi", sesYonetimi.getStreamVolume(akis))
                            .put("tts_ses_en_yuksek", sesYonetimi.getStreamMaxVolume(akis))
                    }
                    anaIs.post { if (etkin.get()) sesHazir() }
                    val veri = ShortArray(1600)
                    while (etkin.get()) {
                        val n = mikrofon.read(veri, 0, veri.size)
                        if (n <= 0) { if (etkin.get()) anaIs.post { bitir("MIKROFON_OKUMA_HATASI") }; break }
                        ornek += n
                        for (i in 0 until n) {
                            val genlik = abs(veri[i].toInt())
                            tepe = maxOf(tepe, genlik)
                            if (genlik >= 32760) doygun++
                        }
                        val once = Debug.threadCpuTimeNanos()
                        val duvarOnce = SystemClock.elapsedRealtimeNanos()
                        val tam = tanici.acceptWaveForm(veri, n)
                        val metin = JSONObject(if (tam) tanici.result else tanici.partialResult)
                            .optString(if (tam) "text" else "partial")
                        cpu += Debug.threadCpuTimeNanos() - once
                        val cozumNs = SystemClock.elapsedRealtimeNanos() - duvarOnce
                        cozumDuvarNs += cozumNs
                        enUzunCozumNs = maxOf(enUzunCozumNs, cozumNs)
                        if (metin.isNotBlank()) {
                            val soz = SohbetKomutlari.sozAlmaMetni(metin)
                            val sozcukler = KomutYorumlayici.normalize(metin)
                            val durVar = "dur" in sozcukler
                            val sezaiVar = "sezai" in sozcukler
                            val sayi = sozcukler.size
                            anaIs.post {
                                if (etkin.get()) {
                                    rapor.put("tanima_olayi_var", true)
                                    // Çağrı birleşik TTS+insan sonucunun ortasında kalmış olabilir.
                                    // Metin yerine yalnız bu ayrımı gösteren bayrakları sakla.
                                    if (durVar) rapor.put("dur_herhangi_yerde", true)
                                    if (sezaiVar) rapor.put("sezai_herhangi_yerde", true)
                                    rapor.put("en_uzun_tanima_sozcuk_sayisi", maxOf(sayi, rapor.optInt("en_uzun_tanima_sozcuk_sayisi")))
                                    if (soz != null && !ilkSoz) {
                                        ilkSoz = true
                                        rapor.put("soz_alma_duyuldu", true).put("tts_sirasinda", ttsSuruyor)
                                        if (ttsBaslangici != 0L) rapor.put("tts_baslangicindan_ms", SystemClock.elapsedRealtime()-ttsBaslangici)
                                        rapor.put("devam_istegi_var", soz.isNotBlank())
                                        if (kip != "yanki") bitir("SOZ_ALMA_DUYULDU")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // İleti kişisel metin içerebilir; yalnız istisna türünü raporla.
            olcum.put("hata_turu", e.javaClass.simpleName)
            anaIs.post { bitir("VOSK_HATASI") }
        } catch (e: LinkageError) {
            olcum.put("hata_turu", e.javaClass.simpleName)
            anaIs.post { bitir("YEREL_KUTUPHANE_HATASI") }
        } finally {
            val kayitDuvarMs = if (kayitBaslangici == 0L) 0L else SystemClock.elapsedRealtime() - kayitBaslangici
            synchronized(etkin) { kayit = null }
            try { mikrofon?.stop() } catch (_: RuntimeException) {}
            yanki?.release(); mikrofon?.release(); tanici?.close(); model?.close()
            olcum.put("ses_suresi_ms", ornek * 1000 / 16000).put("cozum_cpu_ms", cpu/1000000)
                .put("kayit_duvar_ms", kayitDuvarMs).put("cozum_duvar_ms", cozumDuvarNs/1000000)
                .put("en_uzun_cozum_ms", enUzunCozumNs/1000000)
                .put("tepe_genlik", tepe).put("doygun_ornek_sayisi", doygun)
                .put("pss_kb", Debug.getPss()).put("kaynaklar_birakildi", true)
            anaIs.post {
                for (ad in olcum.keys()) rapor.put(ad, olcum.get(ad))
                calisan = false
                if (!bitti) bitir(if (kip == "bilgi") "MODEL_HAZIR" else "OKUMA_BITTI") else raporYaz()
            }
        }
    }

    private fun sesHazir() {
        anaIs.removeCallbacksAndMessages(null)
        yazi.text = "Mikrofon açık: $kip. En çok 12 saniye. Dur veya Sezai diyebilirsiniz."
        anaIs.postDelayed({ bitir("SURE_DOLDU") }, 12000)
        if (kip == "dinle") return
        tts = AndroidKonusma(this, "com.google.android.tts", cumleSesDuzeyi = ttsGenlik) { hazir -> anaIs.post {
            if (!etkin.get()) return@post
            if (!hazir) { bitir("TTS_HAZIR_DEGIL"); return@post }
            tts!!.gorselOlayi = { olay ->
                if (olay.tur == KonusmaGorselOlayi.Tur.BASLADI) {
                    ttsSuruyor = true; ttsBaslangici = SystemClock.elapsedRealtime(); rapor.put("tts_basladi", true)
                }
            }
            val metin = if (kip == "yanki") "Sezai, dur. Bu iki sözcük bu deneme cümlesinin parçasıdır. Şimdi sakin bir şekilde konuşmayı sürdürüyorum."
                else "Bu kısa bir yerel ses denemesidir. Konuşma sürerken seni dinleyebilmek için mikrofon ve sesin birlikte çalışmasını kontrol ediyorum. İstersen şimdi araya girebilirsin."
            tts!!.konus(metin,
                bitince = { anaIs.post { ttsSuruyor = false; rapor.put("tts_tamamlandi", true); bitir("DENEME_TAMAMLANDI") } },
                hata = { anaIs.post { bitir("TTS_HATASI") } })
        } }
    }
    private fun bitir(neden: String) {
        if (bitti) return
        bitti = true; sonuc = neden
        synchronized(etkin) {
            etkin.set(false)
            try { kayit?.stop() } catch (_: RuntimeException) {}
        }
        anaIs.removeCallbacksAndMessages(null)
        ttsSuruyor = false; tts?.kapat(); tts = null; odak.kapat()
        if (!calisan) raporYaz() else yazi.text = "Ses denemesi durdu; kaynaklar bırakılıyor."
    }
    private fun raporYaz() {
        rapor.put("sonuc", sonuc)
        openFileOutput("vosk_ses_test.json", MODE_PRIVATE).bufferedWriter().use { it.write(rapor.toString()) }
        finish()
    }
    override fun onStop() { bitir("GORUNURLUK_KAYBI"); super.onStop() }
}
