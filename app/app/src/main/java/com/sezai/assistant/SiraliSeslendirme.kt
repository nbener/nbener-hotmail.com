package com.sezai.assistant

import android.os.Handler
import android.os.Looper

// Ana iş parçacığında kullanılır. İptal/eski veya yinelenen callback yeni parça başlatamaz.
internal class SiraliSeslendirme(private val oku: (String, () -> Unit, () -> Unit) -> Unit) {
    private var nesil = 0
    private val is_ = Handler(Looper.getMainLooper())
    fun durdur() { nesil++ }

    fun baslat(parcalar: List<String>, araMs: Long = 0, bitince: () -> Unit, hata: () -> Unit) {
        val kimlik = ++nesil
        var sira = 0
        fun ilerle() {
            if (kimlik != nesil) return
            if (sira == parcalar.size) { nesil++; bitince(); return }
            val beklenen = sira++
            oku(parcalar[beklenen], {
                if (kimlik != nesil || sira != beklenen + 1) return@oku
                if (araMs > 0 && sira < parcalar.size) {
                    is_.postDelayed({ ilerle() }, araMs)
                } else {
                    ilerle()
                }
            }, {
                if (kimlik == nesil && sira == beklenen + 1) { nesil++; hata() }
            })
        }
        ilerle()
    }

    companion object {
        // Cümle sınırlarında böl: .!? + boşluk/satırsonu, min 40 karakter. 500 üstü zorla böl.
        fun parcalaCumle(metin: String, maxSinir: Int = 500): List<String> {
            val sonuc = mutableListOf<String>()
            val sb = StringBuilder()
            var i = 0
            val m = metin.trim()
            while (i < m.length) {
                val c = m[i]
                sb.append(c)
                if (c in ".!?" && sb.length > 40 && i + 1 < m.length && (m[i + 1] == ' ' || m[i + 1] == '\n')) {
                    sonuc.add(sb.toString().trim())
                    sb.clear()
                    i += 2
                    continue
                }
                if (sb.length >= maxSinir) {
                    val lastSpace = sb.lastIndexOf(' ')
                    if (lastSpace > maxSinir / 2) {
                        sonuc.add(sb.substring(0, lastSpace).trim())
                        sb.delete(0, lastSpace + 1)
                    } else {
                        sonuc.add(sb.toString().trim())
                        sb.clear()
                    }
                }
                i++
            }
            if (sb.isNotBlank()) sonuc.add(sb.toString().trim())
            return sonuc.filter { it.isNotBlank() }
        }

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
