package com.sezai.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

// Aladhan.com ücretsiz API: Diyanet İşleri Başkanlığı hesaplama metodu (method=13)
// Anahtar gerektirmez, GPS konumu kullanır
class NamazVaktiYardimcisi(private val context: Context) {
    private val anaIs = Handler(Looper.getMainLooper())
    private val is_ = Executors.newSingleThreadExecutor()

    fun al(sonuc: (String) -> Unit) {
        val konum = KonumYardimcisi(context)
        if (!konum.izinVarMi()) { sonuc("Namaz vakitleri için konum izni gerekiyor."); return }
        konum.al { _, _ -> } // ısıtma; hemen ikinci çağrıyla konum al
        konum.al { ekran, _ ->
            val latLon = Regex("""(-?\d+\.\d+),\s*(-?\d+\.\d+)""").find(ekran)
            if (latLon == null) { sonuc("Konumunuzu alamadım, namaz vakitleri gösterilemiyor."); return@al }
            val lat = latLon.groupValues[1]
            val lon = latLon.groupValues[2]
            is_.execute {
                val metin = cek(lat, lon)
                anaIs.post { sonuc(metin) }
            }
        }
    }

    private fun cek(lat: String, lon: String): String {
        return try {
            val zaman = System.currentTimeMillis() / 1000
            val url = "https://api.aladhan.com/v1/timings/$zaman?latitude=$lat&longitude=$lon&method=13"
            Log.i("Sezai", "NAMAZ url=$url")
            val json = JSONObject(URL(url).readText())
            if (json.optInt("code", 0) != 200) return "Namaz vakitleri alınamadı."
            val t = json.getJSONObject("data").getJSONObject("timings")
            val vakitler = listOf(
                "İmsak" to t.getString("Fajr"),
                "Güneş" to t.getString("Sunrise"),
                "Öğle"  to t.getString("Dhuhr"),
                "İkindi" to t.getString("Asr"),
                "Akşam" to t.getString("Maghrib"),
                "Yatsı" to t.getString("Isha")
            )
            val simdi = LocalTime.now()
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val sonraki = vakitler.firstOrNull { (_, saat) ->
                runCatching { LocalTime.parse(saat.take(5), fmt).isAfter(simdi) }.getOrDefault(false)
            }
            val sb = StringBuilder("Bugünün namaz vakitleri: ")
            vakitler.forEachIndexed { i, (ad, saat) ->
                sb.append("$ad ${saat.take(5)}")
                if (i < vakitler.size - 1) sb.append(", ")
            }
            sb.append(". ")
            if (sonraki != null) {
                val (ad, saat) = sonraki
                val kalan = LocalTime.parse(saat.take(5), fmt).toSecondOfDay() - simdi.toSecondOfDay()
                val dk = kalan / 60
                val sn = kalan % 60
                val kalanMetin = if (dk > 0) "$dk dakika" else "$sn saniye"
                sb.append("Bir sonraki vakit $ad, $kalanMetin sonra.")
            } else {
                sb.append("Bugünkü tüm vakitler geçti.")
            }
            sb.toString()
        } catch (e: Exception) {
            Log.w("Sezai", "NAMAZ hata: $e")
            "Namaz vakitlerine ulaşamadım. İnternet bağlantınızı kontrol edin."
        }
    }
}
