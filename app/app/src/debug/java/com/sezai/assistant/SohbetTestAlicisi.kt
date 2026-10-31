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
                kontrol("Döküm önceki turları sırayla ve çift halinde koruyor") {
                    val d = SohbetDokumu()
                    d.ekle("Kayseri", "Bir şehir.")
                    d.ekle("Konumu?", "İç Anadolu.")
                    check(d.metin() == "Siz: Kayseri\n\nSezai: Bir şehir.\n\n────────\n\nSiz: Konumu?\n\nSezai: İç Anadolu.")
                }
                kontrol("Döküm büyüyünce eski turu bütünüyle bırakıyor") {
                    val d = SohbetDokumu()
                    d.ekle("ESKI_SORU", "ESKI_YANIT")
                    repeat(12) { d.ekle("soru$it", "yanıt$it") }
                    check(!d.metin().contains("ESKI_"))
                    check(d.metin().contains("soru11") && d.metin().contains("yanıt11"))
                    d.ekle("x".repeat(40000), "y".repeat(40000))
                    check(d.metin().length < 32500 && d.metin().contains("…"))
                    check(!d.metin().contains("soru11"))
                }
                kontrol("Yeni sohbet dökümü temizliyor, oturumlar ayrı kalıyor") {
                    val d = SohbetDokumu()
                    d.ekle("Özel deneme", "Yanıt")
                    check(SohbetDokumu().metin().isEmpty())
                    d.temizle()
                    check(d.metin().isEmpty())
                }
                kontrol("Geçmiş okuma komutu olumsuz ve alıntı ifadelerden ayrılıyor") {
                    for (s in listOf("Konuşma geçmişi", "Sezai, konuşma geçmişini sesli oku!", "Lütfen sohbet geçmişini oku."))
                        check(SohbetKomutlari.gecmisOkuMu(s))
                    for (s in listOf("Konuşma geçmişini okuma", "Konuşma geçmişi ne demek", "Geçmişi oku demedim", "Sohbeti bitir"))
                        check(!SohbetKomutlari.gecmisOkuMu(s))
                }
                kontrol("Geçmiş ses metni rolleri ve sırayı koruyor") {
                    val d = SohbetDokumu(); d.ekle("Soru", "Yanıt")
                    check(d.sesMetni() == "Siz şöyle dediniz: Soru\nSezai şöyle yanıtladı: Yanıt")
                    d.temizle(); check(d.sesMetni().isEmpty())
                }
                kontrol("Uzun ses metni sınır aşmadan eksiksiz parçalanıyor") {
                    val s = (1..1500).joinToString(" ") { "kelime$it" }
                    val p = SiraliSeslendirme.parcala(s)
                    check(p.size > 1 && p.all { it.length <= 2800 })
                    check(p.joinToString(" ") == s)
                    check(SiraliSeslendirme.parcala("").isEmpty())
                    check(SiraliSeslendirme.parcala("x".repeat(6000)).joinToString("") == "x".repeat(6000))
                }
                kontrol("Sıralı okumada yinelenen bitiş ve eski hata tekrar ilerletmiyor") {
                    val okunan = mutableListOf<String>(); val bitti = mutableListOf<() -> Unit>(); val hata = mutableListOf<() -> Unit>()
                    var tamam = 0; var hatalar = 0
                    val o = SiraliSeslendirme { s, b, h -> okunan.add(s); bitti.add(b); hata.add(h) }
                    o.baslat(listOf("bir", "iki"), { tamam++ }, { hatalar++ })
                    bitti[0](); bitti[0](); hata[0]()
                    check(okunan == listOf("bir", "iki") && hatalar == 0)
                    bitti[1](); bitti[1](); check(tamam == 1)
                }
                kontrol("İptal edilen geçmiş okuması yeni parça veya bitiş üretmiyor") {
                    val bitti = mutableListOf<() -> Unit>(); var okunan = 0; var tamam = 0
                    val o = SiraliSeslendirme { _, b, _ -> okunan++; bitti.add(b) }
                    o.baslat(listOf("eski", "devam"), { tamam++ }, {})
                    o.durdur(); bitti[0](); check(okunan == 1 && tamam == 0)
                    o.baslat(listOf("yeni"), { tamam++ }, {})
                    bitti[0](); check(okunan == 2 && tamam == 0)
                    bitti[1](); check(tamam == 1)
                }
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
                for (saglayici in listOf(SohbetSaglayici.GROQ, SohbetSaglayici.GEMINI, SohbetSaglayici.OPENROUTER)) {
                    kontrol("${saglayici.adi} istek biçimi ve sohbet bağlamı") {
                        val baglantilar = mutableListOf<SahteBaglanti>()
                        val motor = SohbetMotoru(kasa, saglayici) {
                            SahteBaglanti(yanit = """{"choices":[{"message":{"content":"Merhaba","reasoning":"Gizli düşünce"}}]}""")
                                .also { baglantilar.add(it) }
                        }.also { motorlar.add(it) }
                        check(iste(motor, "Adım Test") == "Merhaba")
                        check(iste(motor, "Adımı hatırla") == "Merhaba")
                        val b = baglantilar.last()
                        val istek = JSONObject(b.giden.toString("UTF-8"))
                        check(istek.getString("model") == saglayici.model)
                        check(!istek.has("input") && !istek.has("tools"))
                        val mesajlar = istek.getJSONArray("messages")
                        check(mesajlar.length() == 4)
                        check(mesajlar.getJSONObject(0).getString("role") == "system")
                        check(mesajlar.getJSONObject(1).getString("content") == "Adım Test")
                        check(mesajlar.getJSONObject(2).getString("role") == "assistant")
                        check(!b.instanceFollowRedirects)
                        check(b.getRequestProperty("Authorization") == "Bearer sezai-test-anahtari")
                        if (saglayici == SohbetSaglayici.GROQ) check(!istek.getBoolean("include_reasoning"))
                        if (saglayici == SohbetSaglayici.OPENROUTER) check(istek.getString("model") == "openrouter/free")
                    }
                }
                kontrol("Parçalı yanıt okunuyor, düşünce alanı seslendirilmiyor") {
                    val motor = SohbetMotoru(kasa, SohbetSaglayici.GROQ) {
                        SahteBaglanti(yanit = """{"choices":[{"message":{"content":[{"type":"text","text":"Selam"}],"reasoning":"Düşünce"}}]}""")
                    }.also { motorlar.add(it) }
                    check(iste(motor, "Merhaba") == "Selam")
                }
                kontrol("Ayrı anahtar kasaları birbirini etkilemiyor") {
                    val diger = AnahtarKasasi(context, "sohbet_test_ikinci")
                    try {
                        diger.kaydet("ikinci-test-anahtari")
                        check(kasa.oku() == "sezai-test-anahtari")
                        diger.sil()
                        check(!diger.varMi() && kasa.varMi())
                        check(kasa.oku() == "sezai-test-anahtari")
                        check(SohbetSaglayici.entries.map { it.kasaAdi }.distinct().size == 4)
                        check(SohbetSaglayici.entries.all { URL(it.adres).protocol == "https" })
                    } finally { diger.sil() }
                }
                for (saglayici in SohbetSaglayici.entries) {
                    kontrol("${saglayici.adi}: Kayseri-konumu-nüfusu bağlamı duraklatmada korunuyor") {
                        val baglantilar = mutableListOf<SahteBaglanti>()
                        val yanit = if (saglayici.yanitApi)
                            """{"output":[{"type":"message","content":[{"type":"output_text","text":"Kayseri İç Anadolu Bölgesindedir."}]}]}"""
                        else """{"choices":[{"message":{"content":"Kayseri İç Anadolu Bölgesindedir."}}]}"""
                        val motor = SohbetMotoru(kasa, saglayici) {
                            SahteBaglanti(yanit = yanit).also { baglantilar.add(it) }
                        }.also { motorlar.add(it) }
                        check(iste(motor, "Kayseri nerede?") == "Kayseri İç Anadolu Bölgesindedir.")
                        // Ekrandan çıkış ve giriş yöntemi değişimi aynı motoru duraklatır.
                        motor.iptal()
                        iste(motor, "konumu")
                        val istek = JSONObject(baglantilar.last().giden.toString("UTF-8"))
                        val mesajlar = istek.getJSONArray(if (saglayici.yanitApi) "input" else "messages")
                        val bas = if (saglayici.yanitApi) 0 else 1
                        check(mesajlar.length() == bas + 3)
                        check(mesajlar.getJSONObject(bas).getString("content") == "Kayseri nerede?")
                        check(mesajlar.getJSONObject(bas + 1).getString("content") == "Kayseri İç Anadolu Bölgesindedir.")
                        check(mesajlar.getJSONObject(bas + 2).getString("content") == "konumu")
                        motor.iptal()
                        iste(motor, "nüfusu")
                        val nufusIstegi = JSONObject(baglantilar.last().giden.toString("UTF-8"))
                            .getJSONArray(if (saglayici.yanitApi) "input" else "messages")
                        check(nufusIstegi.length() == bas + 5)
                        check(nufusIstegi.getJSONObject(bas).getString("content") == "Kayseri nerede?")
                        check(nufusIstegi.getJSONObject(bas + 2).getString("content") == "konumu")
                        check(nufusIstegi.getJSONObject(bas + 4).getString("content") == "nüfusu")
                        // Açıkça yeni sohbet başlatılması önceki konuyu taşımamalı.
                        motor.sifirla(); iste(motor, "Yeni konu")
                        val yeni = JSONObject(baglantilar.last().giden.toString("UTF-8"))
                            .getJSONArray(if (saglayici.yanitApi) "input" else "messages")
                        check(yeni.length() == bas + 1)
                        check(yeni.getJSONObject(bas).getString("content") == "Yeni konu")
                    }
                }
                kontrol("Açık web isteği ve olumsuz ifadeler ayrılıyor") {
                    for (metin in listOf("Webden bak", "Kayseri nüfusunu internetten araştır", "Webde Kayseri için bakabilir misin?"))
                        check(WebIstegi.istendiMi(metin))
                    for (metin in listOf("Kayseri nerede?", "Webden bakma", "İnternetten araştırma yapma", "Webden bak demedim", "Webde ne demek?"))
                        check(!WebIstegi.istendiMi(metin))
                }
                kontrol("Groq web aracı zorunlu ve kaynaklı yanıt gösteriliyor") {
                    val b = SahteBaglanti(yanit = """{"choices":[{"message":{"content":"Deneme yanıtı. https://example.org/kayseri"}}]}""")
                    val motor = SohbetMotoru(kasa, SohbetSaglayici.GROQ) { b }.also { motorlar.add(it) }
                    check(iste(motor, "Kayseri nüfusu", true) == "Deneme yanıtı. https://example.org/kayseri")
                    val istek = JSONObject(b.giden.toString("UTF-8"))
                    check(istek.getString("model") == SohbetSaglayici.GROQ.model)
                    check(istek.getString("tool_choice") == "required")
                    check(istek.getJSONArray("tools").getJSONObject(0).getString("type") == "browser_search")
                    check(!istek.has("citation_options"))
                    check(istek.getString("reasoning_effort") == "low")
                }
                kontrol("Desteklenmeyen sağlayıcı web isteğinde başka sunucuya geçmiyor") {
                    for (saglayici in SohbetSaglayici.entries.filter { it != SohbetSaglayici.GROQ }) {
                        val adet = AtomicInteger()
                        val motor = SohbetMotoru(kasa, saglayici) { adet.incrementAndGet(); SahteBaglanti() }
                            .also { motorlar.add(it) }
                        check(iste(motor, "Webden bak").startsWith("HATA:Bu bağlantıda web"))
                        check(adet.get() == 0)
                    }
                }
                kontrol("Web aracı reddedilirse arama yapılmış gibi yanıt dönmüyor") {
                    val motor = SohbetMotoru(kasa, SohbetSaglayici.GROQ) { SahteBaglanti(400, """{"error":{"message":"citation_options reddedildi, OZEL_SORGU sezai-test-anahtari"}}""") }
                        .also { motorlar.add(it) }
                    var tani = ""
                    motor.testHataTanisi = { tani = it }
                    val hata = iste(motor, "Webden bak")
                    check(tani.contains("[GİZLİ]") && !tani.contains("sezai-test-anahtari"))
                    check(hata.startsWith("HATA:Web aracı"))
                    check(hata.contains("HTTP 400") && hata.contains("citation_options"))
                    check(!hata.contains("OZEL_SORGU"))
                }
                kontrol("Web sonrası normal mesajda arama tekrarlanmıyor, bağlam korunuyor") {
                    val baglantilar = mutableListOf<SahteBaglanti>()
                    val motor = SohbetMotoru(kasa, SohbetSaglayici.GROQ) {
                        SahteBaglanti(yanit = """{"choices":[{"message":{"content":"Deneme yanıtı"}}]}""")
                            .also { baglantilar.add(it) }
                    }.also { motorlar.add(it) }
                    iste(motor, "Kayseri nüfusunu webden araştır")
                    iste(motor, "Kısaltır mısın?")
                    val ilk = JSONObject(baglantilar.first().giden.toString("UTF-8"))
                    val son = JSONObject(baglantilar.last().giden.toString("UTF-8"))
                    check(ilk.has("tools") && !son.has("tools"))
                    check(son.getJSONArray("messages").length() == 4)
                    check(son.getJSONArray("messages").getJSONObject(1).getString("content") == "Kayseri nüfusunu webden araştır")
                }
                kontrol("Aşırı büyük web yanıtı bellek sınırında durduruluyor") {
                    val b = SahteBaglanti(yanit = "x".repeat(1_048_577))
                    val motor = SohbetMotoru(kasa, SohbetSaglayici.GROQ) { b }.also { motorlar.add(it) }
                    check(iste(motor, "Webden bak").startsWith("HATA:Yanıt işlenemeyecek kadar büyük"))
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

    private fun iste(motor: SohbetMotoru, metin: String, web: Boolean = WebIstegi.istendiMi(metin)): String {
        val bitti = CountDownLatch(1)
        var cevap = ""
        motor.gonder(metin, { cevap = it; bitti.countDown() }, { cevap = "HATA:$it"; bitti.countDown() }, webAramasi = web)
        check(bitti.await(3, TimeUnit.SECONDS))
        return cevap
    }

    private class SahteBaglanti(
        private val kod: Int = 200,
        private val yanit: String = """{"output":[{"type":"message","content":[{"type":"output_text","text":"Merhaba, nasılsınız?"}]}]}""",
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
            yanit.toByteArray()
        )
        override fun getErrorStream() = ByteArrayInputStream(yanit.toByteArray())
    }
}
