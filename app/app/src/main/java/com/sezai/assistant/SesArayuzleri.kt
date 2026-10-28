package com.sezai.assistant

// Ses motorları bu arayüzlerin arkasında durur; Piper/Vosk gibi motorlara geçmek kolay olsun
interface KonusmaMotoru {
    fun hazirMi(): Boolean
    fun konus(metin: String, bitince: (() -> Unit)? = null)
    fun durdur()
    fun sesListesi(): List<String>
    fun sesSec(ad: String): Boolean
    fun kapat()
}

interface DinlemeMotoru {
    fun kullanilabilirMi(): Boolean
    fun dinle(sonuc: (String) -> Unit, hata: (String) -> Unit)
    fun durdur()
    fun kapat()
}
