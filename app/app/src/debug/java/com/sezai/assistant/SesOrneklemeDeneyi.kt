package com.sezai.assistant

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

// İnsan sesi olmadan süre/frekans/genlik ve yanlış tampon kontrolleri.
internal object SesOrneklemeDeneyi {
    fun dene(rapor: JSONArray) {
        for (hz in listOf(16000, 24000, 48000)) {
            val giris = ShortArray(hz / 100)
            val cikis = ShortArray(160)
            val oturum = YankiOturumu(hz)
            val satir = JSONObject().put("giris_hz", hz).put("cikis_hz", 16000)
            rapor.put(satir)
            var toplam = 0
            var isaret = 0
            var gecis = 0
            var enerji = 0.0
            var olculen = 0
            try {
                repeat(100) { tur ->
                    for (i in giris.indices) giris[i] = (10000 * sin(2 * PI * 1000 * (tur * giris.size + i) / hz)).toInt().toShort()
                    val n = oturum.donustur(giris, cikis)
                    check(n == 160)
                    toplam += n
                    if (tur >= 20) for (i in 0 until n) {
                        val s = cikis[i].toInt()
                        if (isaret <= 0 && s > 0) gecis++
                        isaret = s; enerji += s.toDouble() * s; olculen++
                    }
                }
                val frekans = gecis * 16000.0 / olculen
                val rms = sqrt(enerji / olculen)
                satir.put("toplam_ornek", toplam).put("frekans_hz", frekans).put("rms", rms)
                    .put("gecikme16_ornek", oturum.orneklemeGecikmesi())
                check(toplam == 16000 && abs(frekans - 1000) < 5 && rms in 6300.0..7800.0)
                var tamponReddi = false
                try { oturum.donustur(giris, ShortArray(1)) } catch (_: IllegalArgumentException) { tamponReddi = true }
                check(tamponReddi)
            } finally { oturum.close() }
            var kapaliReddi = false
            try { oturum.donustur(giris, cikis) } catch (_: IllegalStateException) { kapaliReddi = true }
            check(kapaliReddi)
        }
    }
}
