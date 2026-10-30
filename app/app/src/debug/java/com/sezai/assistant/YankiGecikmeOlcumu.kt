package com.sezai.assistant

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.sqrt

// 24kHz girişte sekizli ortalama: yalnız sınırlı bellekte 3kHz tanı verisi.
// Korelasyon bir gecikme ipucudur; insan sesi korunması/yankı kabulü değildir.
internal class YankiGecikmeOlcumu {
    private val mic = ShortArray(36000)
    private val ref = ShortArray(36000)
    private var sayi = 0
    private var tepe = 0
    private var kirpilan = 0

    fun ekle(giris: ShortArray, referans: ShortArray) {
        require(giris.size == 240 && referans.size == 240)
        for (v in giris) {
            tepe = maxOf(tepe, abs(v.toInt()))
            if (abs(v.toInt()) >= 32760) kirpilan++
        }
        for (i in giris.indices step 8) {
            if (sayi == mic.size) break
            var m = 0; var r = 0
            for (j in i until i + 8) { m += giris[j]; r += referans[j] }
            mic[sayi] = (m / 8).toShort(); ref[sayi] = (r / 8).toShort(); sayi++
        }
    }

    fun bitir(): JSONObject = try {
        korelasyon(mic, ref, sayi).put("mikrofon_tepe", tepe).put("mikrofon_kirpilan_ornek", kirpilan)
    } finally { mic.fill(0); ref.fill(0); sayi = 0 }

    companion object {
        // Pozitif gecikme: mikrofon, zaman damgalı referansı geriden izliyor.
        private fun korelasyon(mic: ShortArray, ref: ShortArray, n: Int): JSONObject {
            var enIyi = 0.0; var gecikme = 0; var bulundu = false
            if (n >= 3000) for (lag in -600..600 step 6) {
                val bas = maxOf(0, lag); val son = minOf(n, n + lag)
                val adet = son - bas
                var m = 0.0; var r = 0.0; var mm = 0.0; var rr = 0.0; var mr = 0.0
                for (i in bas until son) {
                    val x = mic[i].toDouble(); val y = ref[i - lag].toDouble()
                    m += x; r += y; mm += x*x; rr += y*y; mr += x*y
                }
                val me = mm - m*m/adet; val re = rr - r*r/adet
                if (me <= 1 || re <= 1) continue
                val c = (mr - m*r/adet) / sqrt(me*re)
                if (!bulundu || abs(c) > abs(enIyi)) { bulundu = true; enIyi = c; gecikme = lag }
            }
            return JSONObject().put("korelasyon_gecerli", bulundu).put("korelasyon_ornek", n)
                .put("korelasyon", if (bulundu) enIyi else JSONObject.NULL)
                .put("korelasyon_gecikme_ms", if (bulundu) gecikme / 3 else JSONObject.NULL)
                .put("korelasyon_sinirda", bulundu && abs(gecikme) == 600)
        }

        fun dene(): Boolean {
            val ref = ShortArray(6000)
            var s = 1729L
            for (i in ref.indices) { s = (s*1664525 + 1013904223) and 0xffffffffL; ref[i] = (s shr 16).toShort() }
            for (lag in listOf(-36, 0, 36)) {
                val mic = ShortArray(ref.size) { i -> if (i-lag in ref.indices) (ref[i-lag] / 2).toShort() else 0 }
                val d = korelasyon(mic, ref, ref.size)
                check(d.getBoolean("korelasyon_gecerli") && d.getInt("korelasyon_gecikme_ms") == lag/3)
                check(d.getDouble("korelasyon") > 0.999)
                mic.fill(0)
            }
            check(!korelasyon(ShortArray(6000), ref, 6000).getBoolean("korelasyon_gecerli"))
            check(!korelasyon(ref, ShortArray(6000), 6000).getBoolean("korelasyon_gecerli"))
            check(!korelasyon(ref, ref, 100).getBoolean("korelasyon_gecerli"))
            ref.fill(0)
            return true
        }
    }
}
