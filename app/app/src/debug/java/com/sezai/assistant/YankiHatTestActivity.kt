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
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10

// Yalnız DUMP korumalı debug hattı. Varsayılan sessiz; ham ses yalnız sınırlı bellekte.
// İlk prototip bütün sabit cümleyi hazırlar; üretim akışına ve doğal sohbete bağlı değildir.
class YankiHatTestActivity : Activity() {
    private val anaIs = Handler(Looper.getMainLooper())
    private val etkin = AtomicBoolean(true)
    private val kaynakKilidi = Any()
    private val pcmKilidi = Any()
    private val rapor = JSONObject()
    private val pcm = ShortArray(24000 * 12)
    private var pcmSayisi = 0
    private var pcmTamam = false
    private var hamTepe = 0
    private var hz = 0
    private var bicim = 0
    private var kanal = 0
    private var genlik = 0f
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
        genlik = if (kip == "sessiz") 0f else intent.getFloatExtra("genlik", 0.2f)
        rapor.put("test_id", intent.getStringExtra("test_id") ?: "elle").put("kip", kip)
            .put("cikis_genlik", if (genlik.isFinite()) genlik.toDouble() else JSONObject.NULL).put("kisisel_ses_saklandi", false)
        yazi = TextView(this).apply { text = "Referanslı ses hattı hazırlanıyor…"; textSize = 18f }
        setContentView(yazi)
        if (state != null) { bitir("YENIDEN_OLUSTURULDU"); return }
        if (kip !in setOf("sessiz", "yanki") || !genlik.isFinite() || genlik !in 0f..0.3f) {
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
            tts!!.konus("Bu, ses hattını kontrol etmek için hazırlanmış kısa bir deneme cümlesidir.",
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
            if (hz != 24000 || bicim != AudioFormat.ENCODING_PCM_16BIT || kanal != 1 || pcmSayisi == 0 || hamTepe == 0) {
                bitir("PCM_DESTEKLENMIYOR"); return
            }
        }
        anaIs.removeCallbacksAndMessages(null)
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
        val yazmaBitti = AtomicBoolean(false)
        var sonuc = "HAT_BITTI"
        val olcum = JSONObject()
        var girisEnerji = 0.0
        var cikisEnerji = 0.0
        var eslesen = 0
        var zamanYok = 0
        var kareSayisi = 0
        var cpuNs = 0L
        var okunan = 0L
        var tanimaOrnek = 0L
        try {
            val kare = hz / 100
            val recMin = AudioRecord.getMinBufferSize(hz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val outMin = AudioTrack.getMinBufferSize(hz, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(recMin > 0 && outMin > 0) { "TAMPON" }
            mikrofon = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, hz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(recMin * 2, kare * 8))
            hoparlor = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(hz).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(maxOf(outMin * 2, kare * 8)).setTransferMode(AudioTrack.MODE_STREAM).build()
            check(mikrofon.state == AudioRecord.STATE_INITIALIZED && hoparlor.state == AudioTrack.STATE_INITIALIZED)
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
                    val konum = trackTs.framePosition + (yakalamaNs - trackTs.nanoTime) * hz / 1000000000L
                    if (konum >= 0 && konum + kare <= pcmSayisi) {
                        pcm.copyInto(ref, 0, konum.toInt(), konum.toInt() + kare)
                        eslesen++
                    }
                } else zamanYok++
                val once = Debug.threadCpuTimeNanos()
                aec.isle(mic, ref, temiz)
                tanimaOrnek += aec.donustur(temiz, tanima16)
                cpuNs += Debug.threadCpuTimeNanos() - once
                for (i in 0 until kare) {
                    girisEnerji += mic[i].toDouble() * mic[i]
                    cikisEnerji += temiz[i].toDouble() * temiz[i]
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
            olcum.put("native_kare", kareSayisi).put("zaman_eslesen_kare", eslesen)
                .put("zaman_yok_kare", zamanYok).put("kayit_ses_ms", okunan * 1000 / hz)
                .put("aec_thread_cpu_ms", cpuNs / 1000000).put("yazma_bitti", yazmaBitti.get())
                .put("calinan_ornek", track.playbackHeadPosition.toLong() and 0xffffffffL)
                .put("cikis_underrun", track.underrunCount)
                .put("tanima16_ornek", tanimaOrnek)
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
            mikrofon?.release(); aec?.close()
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
