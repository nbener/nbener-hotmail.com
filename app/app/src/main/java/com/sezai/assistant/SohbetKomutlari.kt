package com.sezai.assistant

// Bitirmek ile sözü almak farklıdır; ikisi de mevcut sohbet geçmişini kendiliğinden silmez.
object SohbetKomutlari {
    fun gecmisOkuMu(metin: String): Boolean {
        var t = KomutYorumlayici.normalize(metin).map { it.trim('.', ':') }.filter { it.isNotEmpty() }
        if (t.firstOrNull() == "sezai") t = t.drop(1)
        if (t.firstOrNull() == "lütfen") t = t.drop(1)
        if (t.lastOrNull() == "lütfen") t = t.dropLast(1)
        return t.joinToString(" ") in setOf("konuşma geçmişi", "sohbet geçmişi", "konuşma geçmişini oku",
            "sohbet geçmişini oku", "konuşma geçmişini sesli oku", "sohbet geçmişini sesli oku", "geçmişi oku")
    }
    private val bitirme = setOf("sohbeti bitir", "sohbeti kapat", "görüşmeyi bitir", "konuşmayı bitir")
    private val hitap = Regex("^\\s*(?:dur|sezai)(?=[\\s,.!?;:…]|$)[\\s,.!?;:…]*", RegexOption.IGNORE_CASE)
    fun bitirMi(metin: String): Boolean {
        val sozcukler = KomutYorumlayici.normalize(metin.trim().trimEnd('.', '!', '?', '…'))
        val ifade = (if (sozcukler.firstOrNull() == "sezai") sozcukler.drop(1) else sozcukler).joinToString(" ")
        return ifade in bitirme
    }
    // null: söz alma komutu değil; boş: yalnız çağrı; dolu: çağrıdan sonraki yeni istek.
    fun sozAlmaMetni(metin: String): String? {
        var kalan = metin.trim()
        var bulundu = false
        while (true) {
            val eslesme = hitap.find(kalan) ?: break
            bulundu = true; kalan = kalan.substring(eslesme.range.last + 1).trimStart()
        }
        if (!bulundu) return null
        val ilk = KomutYorumlayici.normalize(kalan).firstOrNull()
        if (ilk in setOf("demedim", "demiyorum", "kelimesi", "kelimesini")) return null
        return kalan
    }
}
