package com.sezai.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

// Yalnızca kullanıcı ses özelliğini kullanırken etkindir; görüşme bitince kendiliğinden başlamaz.
internal class GorusmeOnceligi(private val ortam: SesOrtami, private val kesildi: () -> Unit) {
    constructor(context: Context, kesildi: () -> Unit) : this(AndroidSesOrtami(context), kesildi)
    private var etkin = false
    private var nesil = 0

    fun izle() { ortam.izle { mod -> if (etkin && mod != AudioManager.MODE_NORMAL) kes() } }

    fun hazirlan(seslendir: Boolean): Boolean {
        birakOdak()
        if (ortam.mod() != AudioManager.MODE_NORMAL) { etkin = false; return false }
        etkin = true
        if (seslendir) {
            val sira = nesil
            val verildi = ortam.odakIste { degisim ->
                if (sira == nesil && etkin && degisim < 0) kes()
            }
            if (!verildi || !etkin || ortam.mod() != AudioManager.MODE_NORMAL) {
                durdur(); return false
            }
        }
        return true
    }

    fun birakOdak() { nesil++; ortam.odakBirak() }
    fun durdur() { etkin = false; birakOdak() }
    private fun kes() { durdur(); kesildi() }
    fun kapat() { durdur(); ortam.izlemeyiBirak() }
}

internal interface SesOrtami {
    fun mod(): Int
    fun odakIste(degisti: (Int) -> Unit): Boolean
    fun odakBirak()
    fun izle(degisti: (Int) -> Unit)
    fun izlemeyiBirak()
}

private class AndroidSesOrtami(context: Context) : SesOrtami {
    private val ses = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val anaIs = Handler(Looper.getMainLooper())
    private var istek: AudioFocusRequest? = null
    private var dinleyici: AudioManager.OnModeChangedListener? = null
    override fun mod(): Int = try { ses.mode } catch (_: RuntimeException) { AudioManager.MODE_INVALID }
    override fun odakIste(degisti: (Int) -> Unit): Boolean {
        val yeni = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAcceptsDelayedFocusGain(false).setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener({ degisti(it) }, anaIs).build()
        istek = yeni
        return try { ses.requestAudioFocus(yeni) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED }
        catch (_: RuntimeException) { false }
    }
    override fun odakBirak() {
        val eski = istek; istek = null
        if (eski != null) try { ses.abandonAudioFocusRequest(eski) } catch (_: RuntimeException) {}
    }
    override fun izle(degisti: (Int) -> Unit) {
        if (Build.VERSION.SDK_INT >= 31 && dinleyici == null) {
            val yeni = AudioManager.OnModeChangedListener { degisti(it) }
            ses.addOnModeChangedListener(Executor { anaIs.post(it) }, yeni)
            dinleyici = yeni
        }
    }
    override fun izlemeyiBirak() {
        if (Build.VERSION.SDK_INT >= 31) dinleyici?.let { ses.removeOnModeChangedListener(it) }
        dinleyici = null
    }
}
