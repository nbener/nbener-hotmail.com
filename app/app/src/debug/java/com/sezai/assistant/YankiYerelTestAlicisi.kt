package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
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
