package com.sezai.assistant

// Android, gerçek ekran, mikrofon ve ağ kullanmadan üretim komut kodunu sınar.
fun main() {
    var adet = 0
    fun kontrol(ad: String, sonuc: Boolean) {
        check(sonuc) { "FAIL $ad" }
        adet++
        println("PASS $ad")
    }
    listOf("ekranımda ne var?", "ekranda ne var", "Sezai, ekranı incele!",
        "lütfen ekranımı analiz et", "ekranda ne var lütfen", "ekranımı incele",
        "ekranı analiz et", "SEZAİ EKRANIMDA NE VAR?").forEach {
        kontrol("Ekran isteği: $it", SohbetKomutlari.ekranInceleMi(it))
    }
    listOf("ekranı inceleme", "ekranı analiz et demedim", "ekranımda ne var sorusu",
        "ekranı okuma", "ekranı incele demiyorum", "", "ekran", "ekranda ne var bilmiyorum").forEach {
        kontrol("İstek olmayan ifade: $it", !SohbetKomutlari.ekranInceleMi(it))
    }
    kontrol("Bitirme ekran incelemez", !SohbetKomutlari.ekranInceleMi("sohbeti bitir"))
    kontrol("Geçmiş ekran incelemez", !SohbetKomutlari.ekranInceleMi("konuşma geçmişi"))
    kontrol("Ekran isteği sohbeti bitirmez", !SohbetKomutlari.bitirMi("ekranımda ne var"))
    kontrol("Ekran isteği geçmiş okutmaz", !SohbetKomutlari.gecmisOkuMu("ekranımda ne var"))
    val istek = SohbetKomutlari.sozAlmaMetni("Sezai, ekranımda ne var?")
    kontrol("Söz alma sonrası ekran komutu korunur", istek != null && SohbetKomutlari.ekranInceleMi(istek))
    println("Sonuç: $adet/$adet başarılı (yerel JVM; cihaz kabulü değildir)")
}
