package com.sezai.assistant

// Yalnız görüntüleme içindir; API bağlamını değiştirmez, diske yazılmaz.
internal class SohbetDokumu {
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
    fun metin(): String = turlar.joinToString("\n\n────────\n\n") { "Siz: ${it.soru}\n\nSezai: ${it.yanit}" }
}
