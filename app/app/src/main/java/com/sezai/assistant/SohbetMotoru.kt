package com.sezai.assistant

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

// Kısa sohbet geçmişi yalnızca bellekte tutulur. İstek sırasında çalışan tek iş parçacığı kullanılır.
class SohbetMotoru(
    private val kasa: AnahtarKasasi,
    private val saglayici: SohbetSaglayici = SohbetSaglayici.OPENAI,
    private val baglantiAc: () -> HttpURLConnection = {
        URL(saglayici.adres).openConnection() as HttpURLConnection
    }
) {
    private val isci = Executors.newSingleThreadExecutor()
    private val anaIs = Handler(Looper.getMainLooper())
    private val nesil = AtomicInteger()
    private val gecmis = mutableListOf<Pair<String, String>>()
    @Volatile private var baglanti: HttpURLConnection? = null
    private var gorev: Future<*>? = null

    fun sifirla() { iptal(); synchronized(gecmis) { gecmis.clear() } }

    fun gonder(metin: String, sonuc: (String) -> Unit, hata: (String) -> Unit) {
        iptal()
        val sira = nesil.get()
        gorev = isci.submit {
            var b: HttpURLConnection? = null
            try {
                val anahtar = try { kasa.oku() } catch (_: Exception) {
                    throw SohbetHatasi("Bağlantı anahtarı okunamadı. Sohbet bağlantısından yeniden kaydedin.")
                }
                if (anahtar.isNullOrBlank()) throw SohbetHatasi("Önce Sohbet bağlantısı bölümünden API anahtarını girin.")
                val girdi = JSONArray()
                synchronized(gecmis) {
                    gecmis.takeLast(8).forEach { (rol, yazi) -> girdi.put(JSONObject().put("role", rol).put("content", yazi)) }
                }
                girdi.put(JSONObject().put("role", "user").put("content", metin.take(2000)))
                val talimat = "Adın Sezai. Türkçe konuşan sıcak, sabırlı ve güvenilir bir sesli yol arkadaşısın. " +
                    "Kısa, doğal, kolay anlaşılan yanıtlar ver; genellikle iki üç cümle yeterli. Önceki konuşmayı takip et. Kısa takip mesajlarını önceki konuya bağla; örneğin bir yerden sonra söylenen konumu, o yerin konumunu sorar. Konu belliyse yeniden hangi yer olduğunu sorma. " +
                    "Bilmediğini açıkça söyle. Gerçekte yapmadığın arama, alarm, araştırma veya telefon işlemini yaptığını söyleme. " +
                    "Bu sohbette telefon araçlarına ve güncel web bilgisine erişimin yok. Madde işaretleri yerine konuşma dilini kullan."
                val istek = if (saglayici.yanitApi) {
                    JSONObject().put("model", saglayici.model).put("store", false)
                        .put("instructions", talimat).put("max_output_tokens", 300).put("input", girdi)
                } else {
                    val mesajlar = JSONArray().put(JSONObject().put("role", "system").put("content", talimat))
                    for (i in 0 until girdi.length()) mesajlar.put(girdi.getJSONObject(i))
                    JSONObject().put("model", saglayici.model).put("messages", mesajlar).put("stream", false).apply {
                        if (saglayici == SohbetSaglayici.GROQ) {
                            put("max_completion_tokens", 1024); put("reasoning_effort", "low"); put("include_reasoning", false)
                        } else {
                            put("max_tokens", 1024)
                            if (saglayici == SohbetSaglayici.GEMINI) put("reasoning_effort", "low")
                        }
                    }
                }
                b = baglantiAc()
                baglanti = b
                if (nesil.get() != sira) return@submit
                b.requestMethod = "POST"
                b.connectTimeout = 15000
                b.readTimeout = 45000
                b.instanceFollowRedirects = false
                b.doOutput = true
                b.setRequestProperty("Authorization", "Bearer $anahtar")
                b.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                b.outputStream.use { it.write(istek.toString().toByteArray(Charsets.UTF_8)) }
                when (val kod = b.responseCode) {
                    in 200..299 -> Unit
                    401, 403 -> throw SohbetHatasi("Bağlantı anahtarı veya model erişimi kabul edilmedi. Sohbet bağlantısını kontrol edin.")
                    429 -> throw SohbetHatasi("Yapay zekâ kullanım sınırına ulaşıldı. API kotasını kontrol edin veya daha sonra deneyin.")
                    in 500..599 -> throw SohbetHatasi("Yapay zekâ hizmeti şu anda yanıt veremiyor. Biraz sonra tekrar deneyelim.")
                    else -> throw SohbetHatasi("Yapay zekâ bağlantısı kurulamadı. Hata kodu: $kod.")
                }
                val yanit = JSONObject(b.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val parcalar = mutableListOf<String>()
                val cikti = if (saglayici.yanitApi) yanit.optJSONArray("output") ?: JSONArray() else JSONArray()
                if (!saglayici.yanitApi) {
                    val mesaj = yanit.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                    val icerik = mesaj?.opt("content")
                    if (icerik is String) parcalar.add(icerik)
                    else if (icerik is JSONArray) {
                        for (i in 0 until icerik.length()) {
                            val parca = icerik.optJSONObject(i) ?: continue
                            if (parca.optString("type") == "text") parcalar.add(parca.optString("text"))
                        }
                    }
                    if (parcalar.isEmpty() && mesaj?.opt("refusal") is String) parcalar.add(mesaj.optString("refusal"))
                }
                for (i in 0 until cikti.length()) {
                    val oge = cikti.optJSONObject(i) ?: continue
                    if (oge.optString("type") != "message") continue
                    val icerik = oge.optJSONArray("content") ?: continue
                    for (j in 0 until icerik.length()) {
                        val parca = icerik.optJSONObject(j) ?: continue
                        when (parca.optString("type")) {
                            "output_text" -> parcalar.add(parca.optString("text"))
                            "refusal" -> parcalar.add(parca.optString("refusal"))
                        }
                    }
                }
                val cevap = parcalar.joinToString("\n").trim().take(4000)
                if (cevap.isBlank()) throw SohbetHatasi("Yanıt alınamadı. Tekrar deneyelim.")
                anaIs.post {
                    if (nesil.get() == sira) {
                        synchronized(gecmis) {
                            gecmis.add("user" to metin.take(2000)); gecmis.add("assistant" to cevap)
                            while (gecmis.size > 8) gecmis.removeAt(0)
                        }
                        sonuc(cevap)
                    }
                }
            } catch (e: Exception) {
                val mesaj = when (e) {
                    is SohbetHatasi -> e.message ?: "Sohbet bağlantısı kurulamadı."
                    is SocketTimeoutException -> "Yanıt gecikti. Tekrar deneyelim."
                    is IOException -> "İnternet bağlantısını kontrol edip tekrar deneyelim."
                    else -> "Yapay zekâ yanıtı işlenemedi. Tekrar deneyelim."
                }
                anaIs.post { if (nesil.get() == sira) hata(mesaj) }
            } finally {
                b?.disconnect()
                if (baglanti === b) baglanti = null
            }
        }
    }

    fun iptal() { nesil.incrementAndGet(); baglanti?.disconnect(); gorev?.cancel(true) }
    fun kapat() { iptal(); isci.shutdownNow(); synchronized(gecmis) { gecmis.clear() } }
    private class SohbetHatasi(mesaj: String) : Exception(mesaj)
}
