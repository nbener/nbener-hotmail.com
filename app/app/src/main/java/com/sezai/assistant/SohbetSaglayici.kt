package com.sezai.assistant

import android.content.Context

// Adresler sabittir; anahtar başka bir sunucuya yönlendirilemez.
enum class SohbetSaglayici(
    val adi: String, val adres: String, val model: String, val kasaAdi: String,
    val yanitApi: Boolean = false
) {
    GROQ("Groq", "https://api.groq.com/openai/v1/chat/completions", "openai/gpt-oss-120b", "sohbet_groq"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-3.8-flash", "sohbet_gemini"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "openrouter/free", "sohbet_openrouter"),
    OPENAI("OpenAI", "https://api.openai.com/v1/responses", "gpt-4.1-mini", "sohbet", true);

    fun aciklama(): String = when (this) {
        GROQ -> "Groq ücretsiz kota sunar. Ücretsiz planındaki sınırlar hesabına bağlıdır. Web araması ayrıca kota/ücrete tabi olabilir; yalnızca açık web isteğinde kullanılır."
        GEMINI -> "Gemini ücretsiz kota sunar. Ücretsiz katmanda içerikler ürün geliştirmede kullanılabilir."
        OPENROUTER -> "Yalnızca openrouter/free üzerinden ücretsiz modeller istenir. Günlük kota uygulanır."
        OPENAI -> "OpenAI API kullanımı ücretli olabilir."
    }

    fun sec(context: Context) {
        check(context.getSharedPreferences("sohbet_hizmeti", Context.MODE_PRIVATE)
            .edit().putString("saglayici", name).commit())
    }

    companion object {
        fun secili(context: Context): SohbetSaglayici {
            val ad = context.getSharedPreferences("sohbet_hizmeti", Context.MODE_PRIVATE).getString("saglayici", null)
            entries.firstOrNull { it.name == ad }?.let { return it }
            // Eski OpenAI anahtarı varsa aynen koru; yeni kurulum ücretsiz kotalı Groq ile başlar.
            return if (context.getSharedPreferences("sohbet_ayarlari", Context.MODE_PRIVATE).contains("veri")) OPENAI else GROQ
        }
    }
}
