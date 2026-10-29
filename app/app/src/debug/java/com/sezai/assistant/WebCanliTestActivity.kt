package com.sezai.assistant

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import org.json.JSONObject

// Yalnızca shell/DUMP izniyle açılır. Sabit, kişisel veri içermeyen tek gerçek web sorgusu.
// Kullanıcının sohbet geçmişine, taslağına ve anahtar ayarlarına yazmaz.
class WebCanliTestActivity : Activity() {
    private var motor: SohbetMotoru? = null
    private var tamamlandi = false
    private var testId = ""
    @Volatile private var tani = ""

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        testId = intent.getStringExtra("test_id")?.take(80) ?: "elle"
        setContentView(TextView(this).apply {
            text = "Sezai web bağlantısı deneniyor. Tek sorgu; mikrofon ve seslendirme kapalı."
            textSize = 18f; setPadding(40, 64, 40, 40)
        })
        if (state != null) {
            bitir("IPTAL", "Test ekranı yeniden oluşturuldu; yinelenen sorgu gönderilmedi.")
            return
        }
        val saglayici = SohbetSaglayici.secili(this)
        if (saglayici != SohbetSaglayici.GROQ) {
            bitir("DESTEKLENMIYOR", "Seçili sağlayıcı Groq değil; başka hizmete geçilmedi.")
            return
        }
        motor = SohbetMotoru(AnahtarKasasi(this, saglayici.kasaAdi), saglayici).also { it.testHataTanisi = { bilgi -> tani = bilgi } }
        motor!!.gonder(
            "Groq resmi browser_search belgesini webden araştır. Desteklenen model kimliklerini kısaca belirt ve resmi kaynak adresini yaz. Bilgiyi bulamazsan bunu açıkça söyle.",
            sonuc = { bitir("YANIT_ALINDI", it) },
            hata = { bitir("HATA", it) }, webAramasi = true)
    }

    private fun bitir(sonuc: String, mesaj: String) {
        if (tamamlandi) return
        tamamlandi = true
        val rapor = JSONObject().put("test_id", testId).put("sonuc", sonuc).put("mesaj", mesaj).put("tani", tani)
        openFileOutput("web_canli_test_sonuc.json", MODE_PRIVATE).bufferedWriter().use { it.write(rapor.toString()) }
        finish()
    }

    // Sabit kamu bilgisi sorgusu ekran arka plana geçince otomatik tekrarlanmaz veya iptal edilmez.
    // Bu davranış yalnızca debug testi içindir; üretim sohbetinin onStop koruması değişmez.
    override fun onDestroy() {
        if (!tamamlandi) bitir("IPTAL", "Test ekranı yok edildi; sonuç doğrulanamadı.")
        motor?.kapat()
        super.onDestroy()
    }
}
