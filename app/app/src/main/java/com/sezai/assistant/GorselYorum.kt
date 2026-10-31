package com.sezai.assistant

import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

// Tek kareyi seçili sağlayıcıya yollar ve görmeyen birine yardım eder gibi Türkçe anlatım ister.
// Görüntü yalnız bu istekte ağa gider; saklanmaz. Model adı ayarlanabilir (ses_ayari/gorsel_model).
class GorselYorum(private val kasa: AnahtarKasasi, private val saglayici: SohbetSaglayici, modelAdi: String?) {
    private val anaIs = Handler(Looper.getMainLooper())
    private val isci = Executors.newSingleThreadExecutor()
    // Groq'un metin modeli görüntü okuyamaz; görsel model resmî sayfadaki ad (doğrulanmadı) veya ayardır.
    val model: String = modelAdi?.takeIf { it.isNotBlank() } ?: when (saglayici) {
        SohbetSaglayici.GROQ -> "qwen/qwen3.8-27b"
        else -> saglayici.model
    }

    companion object {
        const val TALIMAT = "Adın Sezai. Görmeyen bir insana yardım eden göz gibisin; Türkçe, sıcak, kısa ve net konuş, en çok beş cümle. " +
            "Önce güvenlik açısından önemli olanı söyle (engel, merdiven, araç, tehlike), sonra kişileri, nesneleri ve okuyabildiğin yazıları, sonra çevreyi anlat. " +
            "Gördüğünü yorumla ve işe yarar bir sonuç çıkar. Emin olmadığın şeyi emin değilmişsin gibi söyle, görmediğini uydurma. " +
            "Görüntü karanlık veya bulanıksa bunu söyle ve ne yapılabileceğini öner. Madde işareti kullanma, konuşma dilinde anlat."
    }

    fun yorumla(jpeg: ByteArray, istek: String, sonuc: (String) -> Unit, hata: (String) -> Unit) {
        isci.submit {
            var b: HttpURLConnection? = null
            try {
                if (saglayici == SohbetSaglayici.OPENROUTER) throw IllegalStateException("Bu bağlantı görüntü yorumunu desteklemiyor. Groq, Gemini veya OpenAI seçebilirsiniz.")
                val anahtar = try { kasa.oku() } catch (_: Exception) { null }
                if (anahtar.isNullOrBlank()) throw IllegalStateException("Önce Sohbet bağlantısı bölümünden API anahtarını girin.")
                val resim = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
                val istekGovde = if (saglayici.yanitApi) {
                    JSONObject().put("model", model).put("store", false).put("instructions", TALIMAT).put("max_output_tokens", 500)
                        .put("input", JSONArray().put(JSONObject().put("role", "user").put("content", JSONArray()
                            .put(JSONObject().put("type", "input_text").put("text", istek))
                            .put(JSONObject().put("type", "input_image").put("image_url", resim)))))
                } else {
                    val icerik = JSONArray().put(JSONObject().put("type", "text").put("text", istek))
                        .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", resim)))
                    JSONObject().put("model", model).put("stream", false)
                        .put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", TALIMAT))
                            .put(JSONObject().put("role", "user").put("content", icerik)))
                        .put(if (saglayici == SohbetSaglayici.GROQ) "max_completion_tokens" else "max_tokens", 700)
                }
                b = URL(saglayici.adres).openConnection() as HttpURLConnection
                b.requestMethod = "POST"; b.connectTimeout = 15000; b.readTimeout = 60000
                b.instanceFollowRedirects = false; b.doOutput = true
                b.setRequestProperty("Authorization", "Bearer $anahtar")
                b.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                b.outputStream.use { it.write(istekGovde.toString().toByteArray(Charsets.UTF_8)) }
                when (val kod = b.responseCode) {
                    in 200..299 -> Unit
                    401, 403 -> throw IllegalStateException("Bağlantı anahtarı veya model erişimi kabul edilmedi.")
                    400, 404 -> throw IllegalStateException("Görüntü modeli kabul edilmedi. Model adı $model. Bu hesapta görüntü destekleyen bir model gerekebilir.")
                    413 -> throw IllegalStateException("Resim çok büyük.")
                    429 -> throw IllegalStateException("Yapay zekâ kullanım sınırına ulaşıldı.")
                    else -> throw IllegalStateException("Görüntü yorumu alınamadı. Hata kodu $kod.")
                }
                val ham = b.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().take(1_048_576) }
                val yanit = JSONObject(ham)
                val metin = StringBuilder()
                if (saglayici.yanitApi) {
                    val cikti = yanit.optJSONArray("output") ?: JSONArray()
                    for (i in 0 until cikti.length()) {
                        val parcalar = cikti.optJSONObject(i)?.optJSONArray("content") ?: continue
                        for (j in 0 until parcalar.length()) parcalar.optJSONObject(j)?.takeIf { it.optString("type") == "output_text" }?.let { metin.append(it.optString("text")) }
                    }
                } else {
                    val icerik = yanit.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content")
                    if (icerik is String) metin.append(icerik)
                }
                val temiz = metin.toString().replace(Regex("(?s)<think>.*?</think>"), "").trim()
                if (temiz.isEmpty()) throw IllegalStateException("Görüntüden bir yorum çıkaramadım.")
                anaIs.post { sonuc(temiz) }
            } catch (e: IllegalStateException) {
                anaIs.post { hata(e.message ?: "Görüntü yorumu başarısız.") }
            } catch (e: Exception) {
                anaIs.post { hata("Görüntü yorumu için bağlantı kurulamadı.") }
            } finally { b?.disconnect() }
        }
    }

    fun kapat() { isci.shutdownNow() }
}
