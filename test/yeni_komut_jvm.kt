package com.sezai.assistant

// AramaYap, YouTubeAra, MuzikCal, FenerAyar, MesajOku, NavigasyonBaslat komutlarını sınar (yerel JVM).
fun main() {
    var adet = 0
    var hata = 0
    fun kontrol(ad: String, sonuc: Boolean) {
        if (sonuc) { println("PASS $ad"); adet++ }
        else { println("FAIL $ad"); hata++; adet++ }
    }

    // AramaYap
    fun aramaYap(m: String): String? = KomutYorumlayici.yorumla(m).let { if (it is Komut.AramaYap) it.hedef else null }
    kontrol("Arama: annemi ara", aramaYap("annemi ara")?.contains("anne") == true)
    kontrol("Arama: ablama telefon aç", aramaYap("ablama telefon aç")?.contains("abla") == true)
    kontrol("Arama: babamı ara", aramaYap("babamı ara")?.contains("baba") == true)
    kontrol("Arama: YouTube'da ara - arama DEĞİL", KomutYorumlayici.yorumla("YouTube'da Tarkan ara") !is Komut.AramaYap)

    // YouTubeAra
    fun youtubeAra(m: String): String? = KomutYorumlayici.yorumla(m).let { if (it is Komut.YouTubeAra) it.sorgu else null }
    kontrol("YouTube: YouTube'da Tarkan ara", youtubeAra("YouTube'da Tarkan ara")?.contains("tarkan") == true)
    kontrol("YouTube: YouTube'da Taylor Swift şarkısı ara", youtubeAra("YouTube'da Taylor Swift şarkısı ara")?.contains("taylor swift") == true)
    kontrol("YouTube: YouTube'da haber ara", youtubeAra("YouTube'da haber ara") != null)

    // MuzikCal
    fun muzikCal(m: String): String? = KomutYorumlayici.yorumla(m).let { if (it is Komut.MuzikCal) it.sorgu else null }
    kontrol("Müzik: Tarkan Şımarık çal", muzikCal("Tarkan Şımarık çal")?.contains("tarkan") == true)
    kontrol("Müzik: müzik oynat", muzikCal("müzik oynat") != null)
    kontrol("Müzik: klasik müzik çal", muzikCal("klasik müzik çal") != null)

    // FenerAyar
    fun fenerAc(m: String) = KomutYorumlayici.yorumla(m).let { it is Komut.FenerAyar && it.ac }
    fun fenerKapat(m: String) = KomutYorumlayici.yorumla(m).let { it is Komut.FenerAyar && !it.ac }
    kontrol("Fener: feneri aç", fenerAc("feneri aç"))
    kontrol("Fener: el fenerini aç", fenerAc("el fenerini aç"))
    kontrol("Fener: feneri kapat", fenerKapat("feneri kapat"))
    kontrol("Fener: el feneri kapat", fenerKapat("el feneri kapat"))

    // MesajOku
    fun mesajOku(m: String): Int? = KomutYorumlayici.yorumla(m).let { if (it is Komut.MesajOku) it.adet else null }
    kontrol("Mesaj: mesajlarımı oku", mesajOku("mesajlarımı oku") == 3)
    kontrol("Mesaj: son mesajlarımı oku", mesajOku("son mesajlarımı oku") == 3)
    kontrol("Mesaj: son 2 mesajı oku", mesajOku("son 2 mesajı oku") != null)

    // NavigasyonBaslat
    fun navigasyon(m: String): String? = KomutYorumlayici.yorumla(m).let { if (it is Komut.NavigasyonBaslat) it.hedef else null }
    kontrol("Navigasyon: Kadıköy'e git", navigasyon("Kadıköy'e git")?.contains("kadıköy") == true)
    kontrol("Navigasyon: Beşiktaş için yol tarifi", navigasyon("Beşiktaş için yol tarifi ver")?.contains("beşiktaş") == true)
    kontrol("Navigasyon: Ankara'ya navigasyon", navigasyon("Ankara'ya navigasyonu başlat") != null)

    println("\nSonuç: ${adet - hata}/$adet başarılı (yerel JVM; cihaz kabulü değildir)")
    if (hata > 0) System.exit(hata)
}
