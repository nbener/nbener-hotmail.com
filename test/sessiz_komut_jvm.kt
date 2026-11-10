package com.sezai.assistant

// Android, mikrofon ve ağ kullanmadan sessiz mod komut eşleşmesini sınar (yerel JVM; cihaz kabulü değildir).
fun main() {
    var adet = 0
    fun kontrol(ad: String, sonuc: Boolean) {
        check(sonuc) { "FAIL $ad" }
        adet++
        println("PASS $ad")
    }
    fun sessizAc(m: String) = KomutYorumlayici.yorumla(m).let { it is Komut.Sessiz && it.ac }
    fun sessizKapat(m: String) = KomutYorumlayici.yorumla(m).let { it is Komut.Sessiz && !it.ac }
    fun sessizDegil(m: String) = KomutYorumlayici.yorumla(m) !is Komut.Sessiz

    listOf("sessiz moda geç", "sessize al", "pasif ol", "sessiz ol", "Sezai, sessiz ol!",
        "sessiz ol lütfen", "pasif moda geç", "sessiz olur musun", "SESSİZ OL", "biraz sessiz olabilir misin",
        "sessiz olsun", "Sezai sessiz olsun", "ortalık sessiz olsun istiyorum").forEach {
        kontrol("Sessiz aç: $it", sessizAc(it))
    }
    listOf("sessiz modu kapat", "sessiz moddan çık", "pasif modu kapat").forEach {
        kontrol("Sessiz kapat: $it", sessizKapat(it))
    }
    // Ortam konuşması / yanlış tetikleme örnekleri: Sessiz komutu sayılmamalı
    listOf("çocuklar sessiz oldu", "burası sessiz olur mu",
        "sessiz olmak zor", "pasif olmak istemiyorum", "o çok pasif oldu", "sessizlik", "sessiz bir yer",
        "kimse pasif olmasın", "sessiz olmasın", "pasif olsun", "sessiz oldu mu").forEach {
        kontrol("Sessiz değil: $it", sessizDegil(it))
    }
    kontrol("Onay evet", SessizOnayi.evetMi("Evet."))
    kontrol("Onay hayır", SessizOnayi.hayirMi("hayır geçme"))
    kontrol("Belirsiz söz onay sayılmaz", !SessizOnayi.evetMi("evet ama sonra") && !SessizOnayi.hayirMi("evet"))
    println("Sonuç: $adet/$adet başarılı (yerel JVM; cihaz kabulü değildir)")
}
