package com.sezai.assistant

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File

// Piper TTS (doğal insan sesi) — model filesDir/vits-piper-tr_TR-dfki-medium altında,
// yoksa /sdcard/Download/vits-piper-tr_TR-dfki-medium.tar.bz2 ile kurulur.
class SherpaMotor(private val context: Context) {
    private val modelDiz = File(context.filesDir, "vits-piper-tr_TR-dfki-medium")

    @Volatile var hazir = false
        private set
    @Volatile private var tts: OfflineTts? = null

    fun hazirla() {
        if (hazir) return
        Thread {
            if (!File(modelDiz, "tr_TR-dfki-medium.onnx").exists()) {
                if (!modelKur()) return@Thread
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
            } catch (e: Exception) {
                Log.w("Sezai", "SHERPA yuklenemedi: ${e.javaClass.simpleName}: ${e.message}")
            }
        }.start()
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

    private fun modelKur(): Boolean {
        val tar = File("/sdcard/Download/vits-piper-tr_TR-dfki-medium.tar.bz2")
        if (!tar.exists()) {
            Log.w("Sezai", "SHERPA model tar.bz2 yok: ${tar.absolutePath}")
            return false
        }
        Log.i("Sezai", "SHERPA model kuruluyor: ${tar.absolutePath}")
        return try {
            modelDiz.deleteRecursively()
            val p = ProcessBuilder("tar", "-xjf", tar.absolutePath, "-C", context.filesDir.absolutePath)
                .redirectErrorStream(true).start()
            val cikis = p.waitFor()
            val tamam = File(modelDiz, "tr_TR-dfki-medium.onnx").exists()
            Log.i("Sezai", "SHERPA model kuruldu cikis=$cikis tamam=$tamam")
            tamam
        } catch (e: Exception) {
            Log.w("Sezai", "SHERPA model kurulum hatasi: ${e.message}")
            false
        }
    }
}
