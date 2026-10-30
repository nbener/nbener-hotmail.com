package com.sezai.assistant

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

// Gerçek kayıtta kullanılan 24kHz/10ms JNI girişinin yapay ikinci-ses kontrolü.
internal object YankiAkisDeneyi {
    fun dene(): DoubleArray {
        val hz = 24000
        val kare = hz / 100
        val gecmis = ShortArray(512)
        val mikrofon = ShortArray(kare)
        val referans = ShortArray(kare)
        val cikis = ShortArray(kare)
        val yakin = DoubleArray(kare)
        var rastgele = 123456789L
        var yumusak = 0.0
        var yankiEnerji = 0.0
        var artikEnerji = 0.0
        var yakinEnerji = 0.0
        var capraz = 0.0
        val oturum = YankiOturumu(hz)
        try {
            repeat(1200) { tur ->
                for (i in 0 until kare) {
                    val n = tur * kare + i
                    rastgele = (1664525L * rastgele + 1013904223L) and 0xffffffffL
                    yumusak = 0.65 * yumusak + 0.35 * ((rastgele shr 16).toInt() - 32768) * 0.10
                    val uzak = yumusak.toInt().toShort()
                    gecmis[n % 512] = uzak
                    val yanki = 0.55 * gecmis[(n + 512 - 80) % 512] + 0.20 * gecmis[(n + 512 - 160) % 512]
                    yakin[i] = if (tur >= 1000) 700 * sin(2 * PI * 317 * n / hz) + 400 * sin(2 * PI * 719 * n / hz) else 0.0
                    mikrofon[i] = (yanki + yakin[i]).toInt().toShort()
                    referans[i] = uzak
                }
                oturum.isle(mikrofon, referans, cikis)
                if (tur >= 1000) for (i in 0 until kare) {
                    val yanki = mikrofon[i] - yakin[i]
                    val artik = cikis[i] - yakin[i]
                    yankiEnerji += yanki * yanki; artikEnerji += artik * artik
                    yakinEnerji += yakin[i] * yakin[i]; capraz += cikis[i] * yakin[i]
                }
            }
        } finally { oturum.close() }
        return doubleArrayOf(capraz / yakinEnerji, 10 * log10((yankiEnerji + 1) / (artikEnerji + 1)))
    }
}
