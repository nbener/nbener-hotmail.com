package com.sezai.assistant

// Ana iş parçacığında kullanılır. İptal/eski veya yinelenen callback yeni parça başlatamaz.
internal class SiraliSeslendirme(private val oku: (String, () -> Unit, () -> Unit) -> Unit) {
    private var nesil = 0
    fun durdur() { nesil++ }

    fun baslat(parcalar: List<String>, bitince: () -> Unit, hata: () -> Unit) {
        val kimlik = ++nesil
        var sira = 0
        fun ilerle() {
            if (kimlik != nesil) return
            if (sira == parcalar.size) { nesil++; bitince(); return }
            val beklenen = sira++
            oku(parcalar[beklenen], {
                if (kimlik == nesil && sira == beklenen + 1) ilerle()
            }, {
                if (kimlik == nesil && sira == beklenen + 1) { nesil++; hata() }
            })
        }
        ilerle()
    }

    companion object {
        fun parcala(metin: String, sinir: Int = 2800): List<String> {
            require(sinir > 0)
            val parcalar = mutableListOf<String>()
            var kalan = metin.trim()
            while (kalan.length > sinir) {
                val bosluk = kalan.lastIndexOf(' ', sinir)
                val son = if (bosluk > sinir / 2) bosluk else sinir
                parcalar.add(kalan.substring(0, son))
                kalan = kalan.substring(son).trimStart()
            }
            if (kalan.isNotBlank()) parcalar.add(kalan)
            return parcalar
        }
    }
}
