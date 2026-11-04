package com.sezai.assistant

// Yalnız görüntüleme içindir; API bağlamını değiştirmez, diske yazılmaz.
internal class SohbetDokumu {
    companion object {
        // Ekran ve arka plan servisi aynı süreçteki sınırlı dökümü paylaşır; diske yazılmaz.
        val ortak = SohbetDokumu()
    }
    private data class Tur(val soru: String, val yanit: String)
    private val turlar = ArrayList<Tur>()

    fun ekle(soru: String, yanit: String) {
        fun sinirla(s: String) = if (s.length > 15999) s.take(15998) + "…" else s
        turlar.add(Tur(sinirla(soru), sinirla(yanit)))
        while (turlar.size > 12 || turlar.sumOf { it.soru.length + it.yanit.length } > 32000) {
            turlar.removeAt(0)
        }
    }

    fun temizle() { turlar.clear() }
    fun sesMetni(): String = turlar.joinToString("\n\n") { "Siz şöyle dediniz: ${it.soru}\nSezai şöyle yanıtladı: ${it.yanit}" }
    fun metin(): String = turlar.joinToString("\n\n────────\n\n") { "Siz: ${it.soru}\n\nSezai: ${it.yanit}" }
    fun sonTurMetni(): String = turlar.lastOrNull()?.let { "Siz: ${it.soru}\n\nSezai: ${it.yanit}" }.orEmpty()
    fun gorunum(anlik: String = ""): String = listOf(metin(), anlik).filter { it.isNotBlank() }.joinToString("\n\n")
}
