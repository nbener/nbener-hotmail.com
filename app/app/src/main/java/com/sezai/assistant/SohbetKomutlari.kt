package com.sezai.assistant

// Sadece bitirme isteğinin tamamını eşleştir; sohbet içindeki alıntıyı komut sayma.
object SohbetKomutlari {
    private val bitirme = setOf("dur", "sohbeti bitir", "sohbeti kapat", "görüşmeyi bitir", "konuşmayı bitir")
    fun bitirMi(metin: String): Boolean {
        val sozcukler = KomutYorumlayici.normalize(metin.trim().trimEnd('.', '!', '?', '…'))
        val ifade = (if (sozcukler.firstOrNull() == "sezai") sozcukler.drop(1) else sozcukler).joinToString(" ")
        return ifade in bitirme
    }
}
