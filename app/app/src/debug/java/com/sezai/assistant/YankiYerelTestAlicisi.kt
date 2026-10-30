package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.abs

// DUMP ile korunan mikrofonsuz JNI yükleme ve yapay AEC kabul testi.
class YankiYerelTestAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rapor = JSONObject().put("test_id", intent.getStringExtra("test_id"))
            .put("mikrofon_kullanildi", false).put("gercek_insan_sesi", false)
        try {
            val d = YankiYerel.yapayOlc()
            check(d.size == 5 && d.all { it.isFinite() })
            check(d[0] > 15 && abs(d[1]) < 1 && d[2] > 0.75 && d[2] < 1.25 && d[3] > 6)
            val oturum = YankiOturumu(24000)
            try {
                val sifir = ShortArray(240)
                val cikis = ShortArray(240)
                repeat(20) { oturum.isle(sifir, sifir, cikis) }
                check(cikis.all { it == 0.toShort() })
                var reddedildi = false
                try { oturum.isle(ShortArray(1), sifir, cikis) } catch (_: IllegalArgumentException) { reddedildi = true }
                check(reddedildi)
            } finally { oturum.close() }
            oturum.close() // Çift kapatma güvenli olmalı.
            var kapaliReddi = false
            try { oturum.isle(ShortArray(240), ShortArray(240), ShortArray(240)) }
            catch (_: IllegalStateException) { kapaliReddi = true }
            check(kapaliReddi)
            var hizReddi = false
            try { YankiOturumu(0) } catch (_: IllegalArgumentException) { hizReddi = true }
            check(hizReddi)
            val akis = YankiAkisDeneyi.dene()
            rapor.put("akis24_yakin_katsayi", akis[0]).put("akis24_artik_azalma_db", akis[1])
            check(akis.all { it.isFinite() } && akis[0] > 0.75 && akis[0] < 1.25 && akis[1] > 6)
            val ornekleme = JSONArray()
            rapor.put("ornekleme", ornekleme)
            SesOrneklemeDeneyi.dene(ornekleme)
            rapor.put("akis_kare_boyutu_ve_kapatma", true)
            rapor.put("yapay_yanki_azalma_db", d[0]).put("referanssiz_kontrol_db", d[1])
                .put("yapay_ikinci_ses_katsayisi", d[2]).put("birlikte_artik_azalma_db", d[3])
                .put("islem_cpu_ms", d[4]).put("sonuc", "PASS")
        } catch (e: Exception) {
            rapor.put("sonuc", "FAIL").put("hata_turu", e.javaClass.simpleName)
        } catch (e: LinkageError) {
            rapor.put("sonuc", "FAIL").put("hata_turu", e.javaClass.simpleName)
        }
        context.openFileOutput("yanki_yerel_test.json", Context.MODE_PRIVATE).bufferedWriter().use {
            it.write(rapor.toString())
        }
    }
}
