package com.sezai.assistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

// Yalnız DUMP korumalı debug hattı. Varsayılan sessiz; ham ses yalnız sınırlı bellekte.
// İlk prototip bütün sabit cümleyi hazırlar; üretim akışına ve doğal sohbete bağlı değildir.
class YankiHatTestActivity : Activity() {
    private val anaIs = Handler(Looper.getMainLooper())
    private val etkin = AtomicBoolean(true)
    private val kaynakKilidi = Any()
    private val pcmKilidi = Any()
    private val rapor = JSONObject()
    private val pcm = ShortArray(48000 * 12)
    private var pcmSayisi = 0
    private var pcmTamam = false
    private var hamTepe = 0
    private var hz = 0
    private var bicim = 0
    private var kanal = 0
    private var genlik = 0f
    private var cikisKullanimi = AudioAttributes.USAGE_ASSISTANT
    private var referansGecikmeMs = 0
    private var calisan = false
    private var bitis = ""
    private var tts: AndroidKonusma? = null
    private var kayit: AudioRecord? = null
    private var cikis: AudioTrack? = null
    private lateinit var yazi: TextView
    private val odak by lazy { GorusmeOnceligi(this) { bitir("SES_ODAGI_KESILDI") } }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val kip = intent.getStringExtra("kip") ?: "sessiz"
        val cikisTuru = intent.getStringExtra("cikis") ?: "asistan"
        referansGecikmeMs = intent.getIntExtra("referans_gecikme_ms", 0)
        cikisKullanimi = if (cikisTuru == "medya") AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ASSISTANT
        genlik = if (kip == "sessiz") 0f else intent.getFloatExtra("genlik", 0.2f)
        rapor.put("test_id", intent.getStringExtra("test_id") ?: "elle").put("kip", kip)
            .put("cikis_turu", cikisTuru)
            .put("referans_gecikme_ms", referansGecikmeMs)
            .put("cikis_genlik", if (genlik.isFinite()) genlik.toDouble() else JSONObject.NULL).put("kisisel_ses_saklandi", false)
        yazi = TextView(this).apply { text = "Referanslı ses hattı hazırlanıyor…"; textSize = 18f }
        setContentView(yazi)
        if (state != null) { bitir("YENIDEN_OLUSTURULDU"); return }
        if (kip !in setOf("sessiz", "yanki") || cikisTuru !in setOf("asistan", "medya") ||
            referansGecikmeMs !in 0..200 || !genlik.isFinite() || genlik !in 0f..0.3f) {
            bitir("GECERSIZ_KIP_GENLIK"); return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            bitir("MIKROFON_IZNI_YOK"); return
        }
        odak.izle()
        if (!odak.hazirlan(true)) { bitir("SES_ODAGI_YOK"); return }
        anaIs.postDelayed({ bitir("HAZIRLIK_ZAMAN_ASIMI") }, 30000)
        tts = AndroidKonusma(this, "com.google.android.tts", cumleSesDuzeyi = 0f) { hazir -> anaIs.post {
            if (!etkin.get()) return@post
            if (!hazir) { bitir("TTS_HAZIR_DEGIL"); return@post }
            // Uygulamada seçilen kayıtlı sesi kullan; yoksa motorun varsayılanı kalır.
            val kayitliSes = getSharedPreferences("ses_ayari", MODE_PRIVATE).getString("ses_adi", null)
            if (kayitliSes != null) rapor.put("tts_ses", kayitliSes).put("tts_ses_uygulandi", tts!!.sesSec(kayitliSes))
            tts!!.pcmTanisi = { olay ->
                if (olay.hz > 0) synchronized(pcmKilidi) {
                    hz = olay.hz; bicim = olay.bicim; kanal = olay.kanal
                }
            }
            tts!!.pcmAkisi = { veri ->
                if (etkin.get()) synchronized(pcmKilidi) {
                    if (pcmTamam) {
                        // Sentez tamamlandıktan sonra referans dizisi değişmez.
                    } else if (veri.size % 2 != 0 || pcmSayisi + veri.size / 2 > pcm.size) {
                        anaIs.post { bitir("PCM_TAMPON_BICIM_HATASI") }
                    } else for (i in veri.indices step 2) {
                        val ornek = (((veri[i + 1].toInt() and 255) shl 8) or (veri[i].toInt() and 255)).toShort().toInt()
                        hamTepe = maxOf(hamTepe, abs(ornek))
                        pcm[pcmSayisi++] = (ornek * genlik).toInt().toShort()
                    }
                }
            }
            tts!!.konus(if (intent.getBooleanExtra("yalniz_tanici", false)) "Dur. Sezai. Dur. Sezai." else "Bu, ses hattını kontrol etmek için hazırlanmış kısa bir deneme cümlesidir.",
                bitince = { anaIs.post { if (etkin.get()) kayitBaslat() } },
                hata = { anaIs.post { bitir("TTS_HATASI") } })
        } }
    }

    private fun kayitBaslat() {
        if (calisan || !etkin.get()) return
        tts?.kapat(); tts = null
        synchronized(pcmKilidi) {
            pcmTamam = true
            rapor.put("tts_pcm_hz", hz).put("tts_pcm_bicim", bicim).put("tts_pcm_kanal", kanal)
                .put("tts_pcm_ornek", pcmSayisi).put("tts_ham_tepe", hamTepe)
            if (hz !in setOf(16000, 24000, 48000) || bicim != AudioFormat.ENCODING_PCM_16BIT || kanal != 1 || pcmSayisi == 0 || hamTepe == 0) {
                bitir("PCM_DESTEKLENMIYOR"); return
            }
        }
        anaIs.removeCallbacksAndMessages(null)
        if (intent.getBooleanExtra("yalniz_tanici", false)) {
            // Bilinen yapay cümle doğrudan modele gider; mikrofon ve hoparlör açılmaz.
            calisan = true
            Thread({
                try {
                    YankiOturumu(hz).use { donusum ->
                        YankiCagriTanisi(File(filesDir, "vosk-model-small-tr-0.3")).use { tani ->
                            val giris = ShortArray(hz / 100)
                            val cikis = ShortArray(160)
                            var i = 0
                            while (i < pcmSayisi && etkin.get()) {
                                giris.fill(0)
                                val n = minOf(giris.size, pcmSayisi - i)
                                pcm.copyInto(giris, 0, i, i + n)
                                tani.ekle(cikis, donusum.donustur(giris, cikis))
                                i += n
                            }
                            tani.tamamla()
                            tani.close()
                            val sonuc = tani.rapor()
                            anaIs.post { rapor.put("yapay_cagri_tanisi", sonuc) }
                        }
                    }
                    anaIs.post { calisan = false; bitir("TANICI_KONTROL_BITTI") }
                } catch (e: Exception) {
                    anaIs.post { calisan = false; rapor.put("hata_turu", e.javaClass.simpleName); bitir("TANICI_KONTROL_HATASI") }
                }
            }, "sezai-yapay-cagri").start()
            return
        }
        anaIs.postDelayed({ bitir("HAT_ZAMAN_ASIMI") }, 15000)
        yazi.text = if (genlik == 0f) "Sessiz referans hattı: mikrofon açık, çıkış sesi sıfır."
            else "Referanslı yankı denemesi: mikrofon ve kontrollü ses açık."
        calisan = true
        Thread({ hatDene() }, "sezai-aec-hat").start()
    }

    private fun hatDene() {
        var mikrofon: AudioRecord? = null
        var hoparlor: AudioTrack? = null
        var aec: YankiOturumu? = null
        var yazici: Thread? = null
        var cagriTanisi: YankiCagriTanisi? = null
        var otomatikKazanc: android.media.audiofx.AutomaticGainControl? = null
        val yazmaBitti = AtomicBoolean(false)
        var sonuc = "HAT_BITTI"
        val olcum = JSONObject()
        val gecikmeTanisi = YankiGecikmeOlcumu()
        var girisEnerji = 0.0
        var cikisEnerji = 0.0
        var eslesen = 0
        var zamanYok = 0
        var kareSayisi = 0
        var cpuNs = 0L
        // 250ms dilimlerde mikrofon/referans/AEC çıkışı RMS değerleri; yalnız sayılar.
        val dilimler = JSONArray()
        var dilimMic = 0.0; var dilimRef = 0.0; var dilimTemiz = 0.0; var dilimKare = 0
        var okunan = 0L
        var tanimaOrnek = 0L
        try {
            if (intent.getBooleanExtra("cagri_tanisi", false)) {
                cagriTanisi = YankiCagriTanisi(File(filesDir, "vosk-model-small-tr-0.3"))
            }
            val kare = hz / 100
            val recMin = AudioRecord.getMinBufferSize(hz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val outMin = AudioTrack.getMinBufferSize(hz, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(recMin > 0 && outMin > 0) { "TAMPON" }
            val ses = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
            val hamIstendi = intent.getBooleanExtra("ham_mikrofon", false)
            val hamDestek = ses.getProperty(android.media.AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
            val kaynak = if (hamIstendi && hamDestek) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
            olcum.put("ham_mikrofon_istendi", hamIstendi).put("ham_mikrofon_destek", hamDestek)
            mikrofon = AudioRecord(kaynak, hz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(recMin * 2, kare * 8))
            hoparlor = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                .setUsage(cikisKullanimi).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(hz).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(maxOf(outMin * 2, kare * 8)).setTransferMode(AudioTrack.MODE_STREAM).build()
            check(mikrofon.state == AudioRecord.STATE_INITIALIZED && hoparlor.state == AudioTrack.STATE_INITIALIZED)
            if (hamIstendi && android.media.audiofx.AutomaticGainControl.isAvailable()) {
                otomatikKazanc = android.media.audiofx.AutomaticGainControl.create(mikrofon.audioSessionId)
                otomatikKazanc?.enabled = false
            }
            olcum.put("otomatik_kazanc_etkin", otomatikKazanc?.enabled)
            aec = YankiOturumu(hz)
            val rec = mikrofon
            val track = hoparlor
            synchronized(kaynakKilidi) {
                if (!etkin.get()) return
                kayit = rec; cikis = track
                rec.startRecording()
                check(rec.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                track.play()
            }
            olcum.put("mikrofon_basladi", true).put("audiotrack_basladi", true)
                .put("audiotrack_usage", track.audioAttributes.usage)
                .put("kayit_hz", rec.sampleRate).put("kayit_kaynagi", rec.audioSource)
            rec.routedDevice?.let { olcum.put("giris_cihaz_turu", it.type) }
            yazici = Thread({
                try {
                    var yazilan = 0
                    while (etkin.get() && yazilan < pcmSayisi) {
                        val n = track.write(pcm, yazilan, minOf(kare, pcmSayisi - yazilan), AudioTrack.WRITE_NON_BLOCKING)
                        if (n < 0) { anaIs.post { bitir("CIKIS_YAZMA_HATASI") }; break }
                        yazilan += n
                        if (n == 0) Thread.sleep(3)
                    }
                } catch (_: Exception) { anaIs.post { bitir("CIKIS_HATASI") } }
                finally { yazmaBitti.set(true) }
            }, "sezai-aec-cikis").apply { start() }
            val mic = ShortArray(kare)
            val ref = ShortArray(kare)
            val temiz = ShortArray(kare)
            val tanima16 = ShortArray(160)
            olcum.put("ornekleme_gecikme16", aec.orneklemeGecikmesi())
            val recTs = AudioTimestamp()
            val trackTs = AudioTimestamp()
            val bas = SystemClock.elapsedRealtime()
            while (etkin.get() && SystemClock.elapsedRealtime() - bas < 12000) {
                var n = 0
                while (etkin.get() && n < kare) {
                    val adet = rec.read(mic, n, kare - n, AudioRecord.READ_BLOCKING)
                    if (adet <= 0) { if (etkin.get()) sonuc = "GIRIS_OKUMA_HATASI"; break }
                    n += adet
                }
                if (n != kare || !etkin.get()) break
                ref.fill(0)
                if (rec.getTimestamp(recTs, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS && track.getTimestamp(trackTs)) {
                    val yakalamaNs = recTs.nanoTime + (okunan - recTs.framePosition) * 1000000000L / hz
                    val konum = trackTs.framePosition + (yakalamaNs - trackTs.nanoTime) * hz / 1000000000L -
                        referansGecikmeMs * hz / 1000L
                    if (konum >= 0 && konum + kare <= pcmSayisi) {
                        pcm.copyInto(ref, 0, konum.toInt(), konum.toInt() + kare)
                        eslesen++
                    }
                } else zamanYok++
                gecikmeTanisi.ekle(mic, ref)
                val once = Debug.threadCpuTimeNanos()
                aec.isle(mic, ref, temiz)
                val tanimaAdet = aec.donustur(temiz, tanima16)
                tanimaOrnek += tanimaAdet
                cpuNs += Debug.threadCpuTimeNanos() - once
                cagriTanisi?.ekle(tanima16, tanimaAdet)
                for (i in 0 until kare) {
                    girisEnerji += mic[i].toDouble() * mic[i]
                    cikisEnerji += temiz[i].toDouble() * temiz[i]
                    dilimMic += mic[i].toDouble() * mic[i]
                    dilimRef += ref[i].toDouble() * ref[i]
                    dilimTemiz += temiz[i].toDouble() * temiz[i]
                }
                if (++dilimKare == 25) {
                    val d = 25.0 * kare
                    dilimler.put(JSONArray().put(sqrt(dilimMic / d).toInt()).put(sqrt(dilimRef / d).toInt()).put(sqrt(dilimTemiz / d).toInt()))
                    dilimMic = 0.0; dilimRef = 0.0; dilimTemiz = 0.0; dilimKare = 0
                }
                okunan += kare; kareSayisi++
                if (kareSayisi == 1) anaIs.post {
                    if (etkin.get()) openFileOutput("yanki_hat_hazir.json", MODE_PRIVATE).bufferedWriter().use {
                        it.write(JSONObject().put("test_id", intent.getStringExtra("test_id"))
                            .put("native_kare", 1).put("mikrofon_basladi", true).toString())
                    }
                }
                if (yazmaBitti.get() && (track.playbackHeadPosition.toLong() and 0xffffffffL) >= pcmSayisi &&
                    SystemClock.elapsedRealtime() - bas > pcmSayisi * 1000L / hz + 300) break
            }
            cagriTanisi?.tamamla()
            olcum.put("native_kare", kareSayisi).put("zaman_eslesen_kare", eslesen)
                .put("zaman_yok_kare", zamanYok).put("kayit_ses_ms", okunan * 1000 / hz)
                .put("aec_thread_cpu_ms", cpuNs / 1000000).put("yazma_bitti", yazmaBitti.get())
                .put("calinan_ornek", track.playbackHeadPosition.toLong() and 0xffffffffL)
                .put("cikis_underrun", track.underrunCount)
                .put("tanima16_ornek", tanimaOrnek)
                .put("enerji_dilimleri_250ms_mic_ref_temiz", dilimler)
            if (!yazmaBitti.get() || (track.playbackHeadPosition.toLong() and 0xffffffffL) < pcmSayisi) sonuc = "SURE_DOLDU"
            track.routedDevice?.let { olcum.put("cikis_cihaz_turu", it.type) }
            if (girisEnerji > 0 && cikisEnerji > 0) olcum.put("toplam_enerji_farki_db", 10 * log10(girisEnerji / cikisEnerji))
        } catch (e: Exception) { sonuc = "HAT_HATASI"; olcum.put("hata_turu", e.javaClass.simpleName) }
        catch (e: LinkageError) { sonuc = "NATIVE_HATASI"; olcum.put("hata_turu", e.javaClass.simpleName) }
        finally {
            etkin.set(false)
            synchronized(kaynakKilidi) {
                kayit = null; cikis = null
                try { mikrofon?.stop() } catch (_: RuntimeException) {}
                try { hoparlor?.pause() } catch (_: RuntimeException) {}
            }
            try { yazici?.join(1000) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            val yaziciDurdu = yazici?.isAlive != true
            if (yaziciDurdu) hoparlor?.release()
            otomatikKazanc?.release(); mikrofon?.release(); aec?.close()
            cagriTanisi?.let {
                it.close()
                olcum.put("cagri_tanisi", it.rapor())
            }
            val gecikme = gecikmeTanisi.bitir()
            for (ad in gecikme.keys()) olcum.put(ad, gecikme.get(ad))
            synchronized(pcmKilidi) { if (yaziciDurdu) pcm.fill(0) }
            olcum.put("kaynaklar_birakildi", yaziciDurdu)
            val son = sonuc
            anaIs.post {
                for (ad in olcum.keys()) rapor.put(ad, olcum.get(ad))
                calisan = false
                if (bitis.isEmpty()) bitis = son
                bitir(bitis)
            }
        }
    }

    private fun bitir(neden: String) {
        if (bitis.isEmpty()) bitis = neden
        etkin.set(false)
        anaIs.removeCallbacksAndMessages(null)
        tts?.kapat(); tts = null; odak.kapat()
        synchronized(kaynakKilidi) {
            try { kayit?.stop() } catch (_: RuntimeException) {}
            try { cikis?.pause() } catch (_: RuntimeException) {}
        }
        if (calisan) { yazi.text = "Ses hattı duruyor…"; return }
        synchronized(pcmKilidi) { pcm.fill(0) }
        if (!rapor.has("kaynaklar_birakildi")) rapor.put("kaynaklar_birakildi", true)
        rapor.put("sonuc", bitis)
        openFileOutput("yanki_hat_test.json", MODE_PRIVATE).bufferedWriter().use { it.write(rapor.toString()) }
        finish()
    }

    override fun onStop() { if (bitis.isEmpty()) bitir("GORUNURLUK_KAYBI"); super.onStop() }
}
