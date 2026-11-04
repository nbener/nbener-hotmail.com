package com.sezai.assistant

fun main() {
    var adet = 0
    fun dene(ad: String, islem: () -> Unit) { islem(); adet++; println("PASS $ad") }
    dene("Ekran yeniden oluşturulunca arka planın dökümü korunur") {
        val servis = SohbetDokumu.ortak
        servis.temizle(); servis.ekle("Sesli soru", "Sesli yanıt")
        val ekran = SohbetDokumu.ortak
        check(ekran === servis)
        check(ekran.metin().contains("Sesli soru") && ekran.sesMetni().contains("Sesli yanıt"))
    }
    dene("Bağımsız test dökümü ortak geçmişe karışmaz") {
        SohbetDokumu().ekle("Özel test", "Yalıtılmış")
        check(!SohbetDokumu.ortak.metin().contains("Yalıtılmış"))
    }
    dene("Bekleme ve ses hatası geçmişi gizlemez") {
        val d = SohbetDokumu.ortak
        check(d.gorunum("Dinliyorum").contains("Sesli yanıt"))
        check(d.gorunum("Geçmiş seslendirilemedi").contains("Sesli soru"))
    }
    dene("Yeni sohbet bütün paylaşılan görünümü temizler") {
        SohbetDokumu.ortak.temizle()
        check(SohbetDokumu.ortak.metin().isEmpty())
        check(SohbetDokumu.ortak.sesMetni().isEmpty())
    }
    dene("Döküm tur ve toplam metin sınırını korur") {
        val d = SohbetDokumu()
        repeat(20) { d.ekle("soru$it", "yanıt$it") }
        check(!d.metin().contains("Siz: soru7\n"))
        check(d.metin().contains("Siz: soru8\n"))
        d.ekle("x".repeat(20000), "y".repeat(20000))
        check(d.sonTurMetni().length < 32030)
        check(!d.metin().contains("soru19"))
    }
    dene("Sesli metin kayıpsız parçalara bölünür ve iptal ilerlemez") {
        val metin = "ses ".repeat(1600).trim()
        val parcalar = SiraliSeslendirme.parcala(metin)
        check(parcalar.size > 1 && parcalar.all { it.length <= 2800 })
        check(parcalar.joinToString(" ") == metin)
        val bitisler = mutableListOf<() -> Unit>()
        var okunan = 0
        val okuyucu = SiraliSeslendirme { _, bitince, _ -> okunan++; bitisler.add(bitince) }
        okuyucu.baslat(parcalar, {}, { error("Beklenmeyen hata") })
        okuyucu.durdur(); bitisler[0]()
        check(okunan == 1)
    }
    println("Sonuç: $adet/$adet yerel JVM kontrolü; gerçek TTS veya cihaz kabulü değildir.")
}
