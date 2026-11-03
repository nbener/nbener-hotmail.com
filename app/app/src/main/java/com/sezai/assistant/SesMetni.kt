package com.sezai.assistant

// Ekrandaki yanıt korunur; yalnız ses motoruna giden metinden emoji dizileri çıkarılır.
internal object SesMetni {
    fun temizle(metin: String): String {
        val anahtarsiz = metin.replace(Regex("[0-9#*]\\uFE0F?\\u20E3"), " ")
        val sonuc = StringBuilder()
        var i = 0
        while (i < anahtarsiz.length) {
            val c = Character.codePointAt(anahtarsiz, i)
            i += Character.charCount(c)
            when {
                c in 0x1F000..0x1FAFF || c in 0x2600..0x27BF ||
                    c in 0x1FC00..0x1FFFF || c == 0x00A9 || c == 0x00AE ||
                    c == 0x203C || c == 0x2049 || c == 0x2122 || c == 0x2139 ||
                    c in 0x2194..0x2199 || c in 0x21A9..0x21AA ||
                    c in 0x231A..0x231B || c == 0x2328 || c == 0x23CF ||
                    c in 0x23E9..0x23F3 || c in 0x23F8..0x23FA || c == 0x24C2 ||
                    c in 0x25AA..0x25AB || c == 0x25B6 || c == 0x25C0 ||
                    c in 0x25FB..0x25FE || c in 0x2934..0x2935 ||
                    c in 0x2B05..0x2B07 || c in 0x2B1B..0x2B1C || c == 0x2B50 ||
                    c == 0x2B55 || c == 0x3030 || c == 0x303D || c == 0x3297 || c == 0x3299 -> sonuc.append(' ')
                c == 0x200D || c == 0xFE0E || c == 0xFE0F || c == 0x20E3 || c in 0xE0020..0xE007F -> Unit
                else -> sonuc.appendCodePoint(c)
            }
        }
        return sonuc.toString().replace(Regex("[ \t]+"), " ").trim()
    }
}
