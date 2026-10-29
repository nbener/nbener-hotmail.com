package com.sezai.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

// Tek seferlik konum: sürekli takip yok, işi bitince hemen bırakır (pil dostu)
class KonumYardimcisi(private val context: Context) {
    private val anaIs = Handler(Looper.getMainLooper())
    private val yonetici = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun izinVarMi() = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // sonuc: (ekranda gösterilecek metin, sesli söylenecek metin); ana iş parçacığında çağrılır
    fun al(sonuc: (String, String) -> Unit) {
        if (!izinVarMi()) { sonuc("Konum izni verilmedi.", "Konum izni verilmedi."); return }
        val saglayici = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { try { yonetici.isProviderEnabled(it) } catch (_: Exception) { false } }
        if (saglayici == null) {
            val m = "Telefonun konum ayarı kapalı. Konumu açınca tekrar deneyin."
            sonuc(m, m); return
        }
        var bitti = false
        val iptal = CancellationSignal()
        fun bitir(l: Location?) {
            if (bitti) return
            bitti = true; iptal.cancel()
            val konum = l ?: try { yonetici.getLastKnownLocation(saglayici) } catch (_: SecurityException) { null }
            if (konum == null) { val m = "Konumunuzu alamadım. Açık bir yerde tekrar deneyin."; sonuc(m, m); return }
            adresBul(konum, sonuc)
        }
        try {
            yonetici.getCurrentLocation(saglayici, iptal, context.mainExecutor) { bitir(it) }
            anaIs.postDelayed({ bitir(null) }, 15000)
        } catch (e: Exception) { Log.w("Sezai", "KONUM hata: $e"); bitir(null) }
    }

    private fun adresBul(k: Location, sonuc: (String, String) -> Unit) {
        val koordinat = "%.5f, %.5f (yaklaşık %d metre)".format(Locale.US, k.latitude, k.longitude, k.accuracy.toInt())
        Thread {
            val adres = try {
                if (!Geocoder.isPresent()) null
                else @Suppress("DEPRECATION") Geocoder(context, Locale("tr", "TR")).getFromLocation(k.latitude, k.longitude, 1)?.firstOrNull()
            } catch (e: Exception) { Log.w("Sezai", "ADRES hata: $e"); null }
            val yer = adres?.let { a -> listOfNotNull(a.thoroughfare, a.subLocality, a.subAdminArea ?: a.locality, a.adminArea).distinct().joinToString(", ").ifBlank { null } }
            anaIs.post {
                if (yer != null) sonuc("Konumunuz: $yer\n$koordinat", "Şu an $yer civarındasınız.")
                else sonuc("Konumunuz: $koordinat\nAdres bulunamadı.", "Konumunuzu aldım ama adresi bulamadım. Koordinatlar ekranda.")
            }
        }.start()
    }
}
