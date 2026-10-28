package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// Sahte ağ yanıtlarıyla gerçek Android Keystore ve sohbet yaşam döngüsü testi.
// Kullanıcının anahtarı veya sohbet geçmişi okunmaz; gerçek API isteği yapılmaz.
class SohbetTestAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val testId = intent.getStringExtra("test_id") ?: "elle"
        val bekleyen = goAsync()
        Thread {
            val rapor = mutableListOf<String>()
            val kasa = AnahtarKasasi(context, "sohbet_test")
            val motorlar = mutableListOf<SohbetMotoru>()
            fun kontrol(ad: String, islem: () -> Unit) {
                try { islem(); rapor.add("PASS $ad") }
                catch (_: Exception) { rapor.add("FAIL $ad") }
            }
            try {
                kasa.sil()
                kontrol("Anahtar yokken ağ isteği yapılmıyor") {
                    val adet = AtomicInteger()
                    val motor = SohbetMotoru(kasa) { adet.incrementAndGet(); SahteBaglanti() }.also { motorlar.add(it) }
                    check(iste(motor, "Merhaba").startsWith("HATA:Önce")); check(adet.get() == 0)
                }
                kontrol("Android Keystore şifreleme ve geri okuma") {
                    kasa.kaydet("sezai-test-anahtari")
                    check(kasa.oku() == "sezai-test-anahtari")
                    val sifreli = context.getSharedPreferences("sohbet_test_ayarlari", Context.MODE_PRIVATE).getString("veri", "")!!
                    check(!sifreli.contains("sezai-test-anahtari"))
                }
                kontrol("API yanıtı ve ikinci tur sohbet bağlamı") {
                    val baglantilar = mutableListOf<SahteBaglanti>()
                    val motor = SohbetMotoru(kasa) { SahteBaglanti().also { baglantilar.add(it) } }.also { motorlar.add(it) }
                    check(iste(motor, "Benim adım Test.") == "Merhaba, nasılsınız?")
                    check(iste(motor, "Adımı hatırlıyor musun?") == "Merhaba, nasılsınız?")
                    val istek = JSONObject(baglantilar.last().giden.toString("UTF-8"))
                    check(!istek.getBoolean("store"))
                    check(istek.getJSONArray("input").length() == 3)
                    check(istek.getJSONArray("input").getJSONObject(0).getString("content") == "Benim adım Test.")
                    check(istek.getJSONArray("input").getJSONObject(1).getString("role") == "assistant")
                }
                kontrol("Geçersiz API anahtarı anlaşılır hata veriyor") {
                    val motor = SohbetMotoru(kasa) { SahteBaglanti(401) }.also { motorlar.add(it) }
                    check(iste(motor, "Merhaba").startsWith("HATA:Bağlantı anahtarı"))
                }
                kontrol("Kota hatası anlaşılır hata veriyor") {
                    val motor = SohbetMotoru(kasa) { SahteBaglanti(429) }.also { motorlar.add(it) }
                    check(iste(motor, "Merhaba").startsWith("HATA:Yapay zekâ kullanım sınırına"))
                }
                kontrol("İptal edilen yanıt kullanıcıya dönmüyor") {
                    val basladi = CountDownLatch(1)
                    val devam = CountDownLatch(1)
                    val cagrildi = AtomicInteger()
                    val motor = SohbetMotoru(kasa) {
                        SahteBaglanti(basladi = basladi, devam = devam)
                    }.also { motorlar.add(it) }
                    motor.gonder("Merhaba", { cagrildi.incrementAndGet() }, { cagrildi.incrementAndGet() })
                    check(basladi.await(2, TimeUnit.SECONDS))
                    motor.iptal(); devam.countDown(); Thread.sleep(250)
                    check(cagrildi.get() == 0)
                }
            } catch (_: Exception) { rapor.add("FAIL Test hazırlığı") }
            finally {
                motorlar.forEach { it.kapat() }
                try { kasa.sil() } catch (_: Exception) { rapor.add("FAIL Test anahtarı temizliği") }
                context.openFileOutput("sohbet_test_sonuc.txt", Context.MODE_PRIVATE).bufferedWriter().use {
                    it.write("TEST_ID $testId\n" + rapor.joinToString("\n"))
                }
                bekleyen.finish()
            }
        }.start()
    }

    private fun iste(motor: SohbetMotoru, metin: String): String {
        val bitti = CountDownLatch(1)
        var cevap = ""
        motor.gonder(metin, { cevap = it; bitti.countDown() }, { cevap = "HATA:$it"; bitti.countDown() })
        check(bitti.await(3, TimeUnit.SECONDS))
        return cevap
    }

    private class SahteBaglanti(
        private val kod: Int = 200,
        private val basladi: CountDownLatch? = null,
        private val devam: CountDownLatch? = null
    ) : HttpURLConnection(URL("https://api.openai.com/v1/responses")) {
        val giden = ByteArrayOutputStream()
        override fun connect() {}
        override fun disconnect() { devam?.countDown() }
        override fun usingProxy() = false
        override fun getOutputStream() = giden
        override fun getResponseCode(): Int { basladi?.countDown(); devam?.await(2, TimeUnit.SECONDS); return kod }
        override fun getInputStream() = ByteArrayInputStream(
            """{"output":[{"type":"message","content":[{"type":"output_text","text":"Merhaba, nasılsınız?"}]}]}""".toByteArray()
        )
    }
}
