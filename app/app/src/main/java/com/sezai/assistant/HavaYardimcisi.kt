package com.sezai.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.net.URL

// Open-Meteo ücretsiz hava durumu: https://open-meteo.com (anahtar gerektirmez)
// Mevcut hava ve sonraki 24 saatlik tahmin; pil dostu (tek istek, bağlantı bırakılır)
class HavaYardimcisi(private val context: Context) {
    private val anaIs = Handler(Looper.getMainLooper())

    // sonuc: Türkçe okunabilir hava açıklaması; ağ veya konum hatası varsa hata metni
    // yarinMi: true → yarınki tahmin, false → şimdiki durum
    fun al(yer: String, yarinMi: Boolean, sonuc: (String) -> Unit) {
        if (yer.isBlank()) {
            val konum = KonumYardimcisi(context)
            if (!konum.izinVarMi()) { sonuc("Hava durumu için konum izni gerekiyor."); return }
            konumAl { lat, lon, adres ->
                if (lat == null || lon == null) { sonuc("Konumunuzu alamadım, hava durumunu gösteremiyorum."); return@konumAl }
                havaCek(lat, lon, adres ?: "", yarinMi, sonuc)
            }
        } else {
            yerKodla(yer) { lat, lon ->
                if (lat == null || lon == null) { sonuc("$yer için konum bulunamadı."); return@yerKodla }
                havaCek(lat, lon, yer, yarinMi, sonuc)
            }
        }
    }

    private fun konumAl(sonuc: (Double?, Double?, String?) -> Unit) {
        val konum = KonumYardimcisi(context)
        konum.al { ekran, _ ->
            // ekran: "Konumunuz: mahalle, ilçe\nlat, lon" biçiminde; lat/lon'u parse ediyoruz
            val latLonRegex = Regex("""(-?\d+\.\d+),\s*(-?\d+\.\d+)""")
            val m = latLonRegex.find(ekran)
            if (m != null) {
                val lat = m.groupValues[1].toDoubleOrNull()
                val lon = m.groupValues[2].toDoubleOrNull()
                val adres = ekran.lines().firstOrNull()?.removePrefix("Konumunuz: ")?.substringBefore("\n")
                sonuc(lat, lon, adres)
            } else sonuc(null, null, null)
        }
    }

    private fun yerKodla(yer: String, sonuc: (Double?, Double?) -> Unit) {
        Thread {
            try {
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=${java.net.URLEncoder.encode(yer, "UTF-8")}&count=1&language=tr&format=json"
                val json = JSONObject(URL(url).readText())
                val sonuclar = json.optJSONArray("results")
                val ilk = sonuclar?.optJSONObject(0)
                val lat = ilk?.optDouble("latitude")
                val lon = ilk?.optDouble("longitude")
                anaIs.post { sonuc(if (lat != null && !lat.isNaN()) lat else null, if (lon != null && !lon.isNaN()) lon else null) }
            } catch (e: Exception) {
                Log.w("Sezai", "HAVA yerKodla hata: $e")
                anaIs.post { sonuc(null, null) }
            }
        }.start()
    }

    private fun havaCek(lat: Double, lon: Double, yerAdi: String, yarinMi: Boolean, sonuc: (String) -> Unit) {
        Thread {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,relative_humidity_2m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&timezone=auto&forecast_days=2"
                val json = JSONObject(URL(url).readText())
                val metin = if (yarinMi) yarin(json, yerAdi) else bugun(json, yerAdi)
                anaIs.post { sonuc(metin) }
            } catch (e: Exception) {
                Log.w("Sezai", "HAVA cek hata: $e")
                anaIs.post { sonuc("Hava durumu alınamadı, internet bağlantısını kontrol edin.") }
            }
        }.start()
    }

    private fun bugun(json: JSONObject, yerAdi: String): String {
        val c = json.optJSONObject("current") ?: return "Hava durumu verisi alınamadı."
        val sicaklik = c.optDouble("temperature_2m", Double.NaN)
        val hissedilen = c.optDouble("apparent_temperature", Double.NaN)
        val kod = c.optInt("weather_code", -1)
        val nem = c.optDouble("relative_humidity_2m", Double.NaN)
        val ruzgar = c.optDouble("wind_speed_10m", Double.NaN)

        val durum = kodTurkce(kod)
        val yer = if (yerAdi.isNotBlank()) "$yerAdi'nde" else "Şu an"
        val sb = StringBuilder()
        sb.append("$yer hava $durum.")
        if (!sicaklik.isNaN()) sb.append(" Sıcaklık ${sicaklik.toInt()} derece")
        if (!hissedilen.isNaN() && !sicaklik.isNaN() && kotlin.math.abs(hissedilen - sicaklik) > 2)
            sb.append(", ama ${hissedilen.toInt()} derece hissettiriyor")
        if (!sicaklik.isNaN()) sb.append(".")
        if (!nem.isNaN()) sb.append(" Nem yüzde ${nem.toInt()}.")
        if (!ruzgar.isNaN() && ruzgar > 20) sb.append(" Rüzgar saatte ${ruzgar.toInt()} kilometre.")
        Log.i("Sezai", "HAVA bugun=$sb")
        return sb.toString()
    }

    private fun yarin(json: JSONObject, yerAdi: String): String {
        val d = json.optJSONObject("daily") ?: return "Tahmin verisi alınamadı."
        val kodlar = d.optJSONArray("weather_code") ?: return "Tahmin verisi alınamadı."
        val maks = d.optJSONArray("temperature_2m_max")
        val min = d.optJSONArray("temperature_2m_min")
        val yagisOlasiligi = d.optJSONArray("precipitation_probability_max")
        val kod = kodlar.optInt(1, -1)
        val durum = kodTurkce(kod)
        val yer = if (yerAdi.isNotBlank()) "$yerAdi'nde" else ""
        val sb = StringBuilder()
        sb.append("Yarın${if (yer.isNotBlank()) " $yer" else ""} hava $durum.")
        if (maks != null && min != null) {
            val yuksek = maks.optDouble(1, Double.NaN)
            val dusuk = min.optDouble(1, Double.NaN)
            if (!yuksek.isNaN() && !dusuk.isNaN())
                sb.append(" En yüksek ${yuksek.toInt()}, en düşük ${dusuk.toInt()} derece.")
        }
        if (yagisOlasiligi != null) {
            val olasilik = yagisOlasiligi.optInt(1, -1)
            if (olasilik >= 40) sb.append(" Yağış ihtimali yüzde $olasilik.")
        }
        Log.i("Sezai", "HAVA yarin=$sb")
        return sb.toString()
    }

    // WMO hava durum kodu → Türkçe açıklama
    private fun kodTurkce(kod: Int): String = when (kod) {
        0 -> "açık ve güneşli"
        1 -> "çoğunlukla açık"
        2 -> "parçalı bulutlu"
        3 -> "bulutlu"
        45, 48 -> "sisli"
        51, 53 -> "hafif çiseleyen"
        55 -> "çiseleyen"
        56, 57 -> "dondurucu çiseleyen"
        61 -> "hafif yağmurlu"
        63 -> "yağmurlu"
        65 -> "şiddetli yağmurlu"
        66, 67 -> "buzlu yağmurlu"
        71 -> "hafif karlı"
        73 -> "karlı"
        75 -> "yoğun karlı"
        77 -> "kar taneli"
        80 -> "hafif sağanak"
        81 -> "sağanak yağışlı"
        82 -> "şiddetli sağanak"
        85, 86 -> "kar sağanağı"
        95 -> "fırtınalı"
        96, 99 -> "dolu ile fırtınalı"
        else -> "belirsiz"
    }
}
