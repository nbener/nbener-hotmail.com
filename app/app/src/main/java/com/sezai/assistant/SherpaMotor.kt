package com.sezai.assistant

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.InputStream

// Piper TTS (doğal insan sesi) — model filesDir/vits-piper-tr_TR-dfki-medium altında.
// Kurulum: ACTION_OPEN_DOCUMENT ile kullanıcı tar.bz2 dosyasını seçer, modelKurStream ile yüklenir.
class SherpaMotor(private val context: Context) {
    private val modelDiz = File(context.filesDir, "vits-piper-tr_TR-dfki-medium")

    @Volatile var hazir = false
        private set
    @Volatile private var tts: OfflineTts? = null

    fun modelVarMi() = File(modelDiz, "tr_TR-dfki-medium.onnx").exists()

    // bitince: true = başarılı yüklendi, false = model yok veya hata
    fun hazirla(bitince: ((Boolean) -> Unit)? = null) {
        Log.i("Sezai", "SHERPA hazirla cagrildi hazir=$hazir modelDiz=${modelDiz.absolutePath}")
        if (hazir) { bitince?.invoke(true); return }
        Thread {
            Log.i("Sezai", "SHERPA thread basladi")
            if (!File(modelDiz, "tr_TR-dfki-medium.onnx").exists()) {
                Log.i("Sezai", "SHERPA model dosyasi yok")
                bitince?.invoke(false)
                return@Thread
            }
            try {
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = File(modelDiz, "tr_TR-dfki-medium.onnx").absolutePath,
                            tokens = File(modelDiz, "tokens.txt").absolutePath,
                            dataDir = File(modelDiz, "espeak-ng-data").absolutePath
                        ),
                        numThreads = 2,
                        debug = false,
                        provider = "cpu"
                    )
                )
                tts = OfflineTts(assetManager = null, config = config)
                hazir = true
                Log.i("Sezai", "SHERPA hazir hz=${tts?.sampleRate()}")
                bitince?.invoke(true)
            } catch (e: Exception) {
                Log.w("Sezai", "SHERPA yuklenemedi: ${e.javaClass.simpleName}: ${e.message}")
                bitince?.invoke(false)
            }
        }.start()
    }

    // ACTION_OPEN_DOCUMENT'tan gelen InputStream ile model kur; ardından hazirla() çağrılmalı
    fun modelKurStream(stream: InputStream): Boolean {
        Log.i("Sezai", "SHERPA modelKurStream basladi")
        return try {
            modelDiz.deleteRecursively()
            modelDiz.mkdirs()
            var dosyaSayisi = 0
            stream.use { raw ->
                BZip2CompressorInputStream(raw).use { bz2 ->
                    TarArchiveInputStream(bz2).use { tar ->
                        var girdi = tar.nextEntry
                        while (girdi != null) {
                            val hedef = File(context.filesDir, girdi.name)
                            if (girdi.isDirectory) {
                                hedef.mkdirs()
                            } else {
                                hedef.parentFile?.mkdirs()
                                hedef.outputStream().use { tar.copyTo(it) }
                                dosyaSayisi++
                            }
                            girdi = tar.nextEntry
                        }
                    }
                }
            }
            val tamam = File(modelDiz, "tr_TR-dfki-medium.onnx").exists()
            Log.i("Sezai", "SHERPA modelKurStream bitti tamam=$tamam dosyaSayisi=$dosyaSayisi")
            tamam
        } catch (e: Exception) {
            Log.w("Sezai", "SHERPA modelKurStream hatasi: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    // FloatArray ve sample rate döner; null = hata veya model hazır değil
    fun sentezle(metin: String): Pair<FloatArray, Int>? {
        val t = tts ?: return null
        return try {
            val audio = t.generate(text = metin, sid = 0, speed = 1.0f)
            Pair(audio.samples, audio.sampleRate)
        } catch (e: Exception) {
            Log.w("Sezai", "SHERPA sentez hatasi: ${e.message}")
            null
        }
    }

    fun kapat() {
        hazir = false
        try { tts?.release() } catch (_: Exception) {}
        tts = null
    }
}
