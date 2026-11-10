package com.sezai.assistant

import java.util.Locale

// Konuşmadan gelen Türkçe cümleyi komuta çevirir (yan etkisiz, test edilebilir)
sealed class Komut {
    object SaatSor : Komut()
    object TarihSor : Komut()
    data class AlarmKur(val saat: Int, val dakika: Int) : Komut()
    object AlarmIptal : Komut()
    object Yardim : Komut()
    data class Gorunum(val gece: Boolean) : Komut()
    data class ArkaPlan(val ac: Boolean) : Komut()
    data class SesAyar(val fark: Int) : Komut()
    data class Hatirla(val metin: String) : Komut()
    object Hatirlat : Komut()
    data class Sessiz(val ac: Boolean) : Komut()
    data class NotKaydet(val metin: String) : Komut()
    data class NotOku(val adet: Int) : Komut()
    object KonumGoster : Komut()
    data class Kamera(val on: Boolean) : Komut()
    data class ZamanlayiciKur(val saniye: Int) : Komut()
    data class Bilinmiyor(val metin: String) : Komut()
}

object KomutYorumlayici {
    private val trLocale = Locale("tr", "TR")

    private val sayilar = mapOf(
        "sıfır" to 0, "bir" to 1, "iki" to 2, "üç" to 3, "dört" to 4, "beş" to 5, "altı" to 6,
        "yedi" to 7, "sekiz" to 8, "dokuz" to 9, "on" to 10, "yirmi" to 20, "otuz" to 30,
        "kırk" to 40, "elli" to 50, "altmış" to 60
    )
    private val ekler = setOf("", "a", "e", "ya", "ye", "da", "de", "ta", "te", "dan", "den", "tan", "ten", "ı", "i", "yı", "yi", "u", "ü", "lık", "lik", "luk", "lük")
    // Önek eşleşmesi "buçukluk" gibi başka sözcükleri yarım saat saymamalı.
    private val bucukBicimleri = setOf("buçuk", "buçukta", "buçuktan", "buçuğa", "buçuğu")

    fun normalize(metin: String): List<String> =
        metin.lowercase(trLocale).replace(Regex("[^0-9a-zçğıöşü: .]"), " ").replace(Regex("\\s+"), " ").trim()
            .split(" ").filter { it.isNotEmpty() }

    // Bir sözcük sayıysa değerini döndürür: "yedi", "yedide", "7", "7'ye" (apostrof boşluğa dönmüştü: "7 ye")
    private fun sayiDeger(kelime: String): Int? {
        kelime.toIntOrNull()?.let { return it }
        if (kelime in sayilar) return sayilar[kelime]
        // ekli: en uzun sayı sözcüğü + ek
        return sayilar.entries.sortedByDescending { it.key.length }
            .firstOrNull { kelime.startsWith(it.key) && kelime.removePrefix(it.key) in ekler }?.value
    }

    // Peş peşe sayı sözcüklerini toplar: "yirmi beş" -> 25 (kaç sözcük tükettiğiyle)
    private fun sayiOku(t: List<String>, i: Int): Pair<Int, Int>? {
        val ilk = sayiDeger(t[i]) ?: return null
        if (t[i].toIntOrNull() != null) return ilk to 1
        if (ilk == 10 && i + 1 < t.size) {
            val ikinci = sayilar.keys.firstOrNull { t[i + 1].startsWith(it) && t[i + 1].removePrefix(it) in ekler }?.let { sayilar[it] }
            if (ikinci != null && ikinci in 1..9) return (10 + ikinci) to 2
        }
        if (ilk in listOf(20, 30, 40, 50) && i + 1 < t.size) {
            val ikinci = sayilar[t[i + 1].takeWhile { it.isLetter() }.let { s -> sayilar.keys.firstOrNull { s.startsWith(it) && s.removePrefix(it) in ekler } ?: "" }]
            if (ikinci != null && ikinci in 1..9) return (ilk + ikinci) to 2
        }
        return ilk to 1
    }

    fun yorumla(metin: String): Komut {
        val t = normalize(metin)
        val m = t.joinToString(" ")
        if (t.isEmpty()) return Komut.Bilinmiyor(metin)

        // Ana ekran işlevleri: görünüm, arka plan dinleme, konuşma sesi
        if (m.contains("gece mod") || m.contains("karanlık mod") || m.contains("koyu mod")) return Komut.Gorunum(!(m.contains("kapat") || m.contains("çık")))
        if (m.contains("gündüz mod") || m.contains("aydınlık mod") || m.contains("açık mod")) return Komut.Gorunum(false)
        if ((m.contains("arka plan") || m.contains("kilitliyken")) && m.contains("dinle")) return Komut.ArkaPlan(!(m.contains("kapat") || m.contains("durdur") || m.contains("bırak")))
        if ((m.contains("sesini") || m.contains("sesi ") || m.endsWith("sesi")) && (m.contains("artır") || m.contains("yükselt") || m.contains("aç"))) return Komut.SesAyar(25)
        if ((m.contains("sesini") || m.contains("sesi ") || m.endsWith("sesi")) && (m.contains("azalt") || m.contains("kıs") || m.contains("alçalt"))) return Komut.SesAyar(-25)
        // Sessiz mod: açma sesle olur; kapatma dinleme kapalı olduğundan düğme/bildirimle (ama söylenirse de anlaşılır)
        // "sessiz ol"/"pasif ol" yalnız tam sözcükle: "sessiz oldu", "sessiz olur mu", "sessiz olmak" gibi
        // ortam konuşmaları sessiz modu tetiklemesin (05.11 yanlış tetikleme incelemesi, ISBIRLIGI.md).
        // Kullanıcı isteğiyle (10.11) "sessiz olsun" komut sayılır; "pasif olsun" için istek yok.
        fun olEmri(kok: String, olsunDa: Boolean = false): Boolean = t.indices.any { i ->
            t[i] == kok && (t.getOrNull(i + 1) == "ol" || (olsunDa && t.getOrNull(i + 1) == "olsun") ||
                (t.getOrNull(i + 1) == "olur" && t.getOrNull(i + 2) == "musun") ||
                (t.getOrNull(i + 1) == "olabilir" && t.getOrNull(i + 2) == "misin"))
        }
        if (m.contains("sessiz mod") || m.contains("sessize al") || olEmri("sessiz", olsunDa = true) || m.contains("pasif mod") || olEmri("pasif")) {
            return Komut.Sessiz(!(m.contains("kapat") || m.contains("çık") || m.contains("bitir") || m.contains("kaldır")))
        }
        // Hafıza: "şunu hatırla: ..." kaydeder; "ne hatırlıyorsun / ne öğrendin" okur
        Regex("(?:^|\\s)(?:şunu hatırla|bunu hatırla|hatırla|öğren|şunu öğren)\\b[\\s:,.-]*").find(metin.lowercase(trLocale))?.let { k ->
            val icerik = metin.substring(k.range.last + 1).trim().trim('.', ',', ':', '-', ' ')
            if (icerik.isNotEmpty() && !m.contains("ne hatırl")) return Komut.Hatirla(icerik)
        }
        if (m.contains("ne hatırlıyorsun") || m.contains("ne öğrendin") || m.contains("neleri hatırlıyorsun") || m.contains("hatırladıklarını")) return Komut.Hatirlat
        // Sesli not: "not al: yarın süt al" -> kaydet; "son notu oku" -> oku
        val kayit = Regex("(?:^|\\s)(?:not al|notu al|not kaydet|notu kaydet|not et|not yaz|not defterine yaz|notlara ekle)\\b[\\s:,.-]*").find(metin.lowercase(trLocale))
        if (kayit != null) {
            val icerik = metin.substring(kayit.range.last + 1).trim().trim('.', ',', ':', '-', ' ')
            return if (icerik.isNotEmpty()) Komut.NotKaydet(icerik) else Komut.Bilinmiyor(metin)
        }
        val notKelimesi = t.any { it in setOf("not", "notu", "note", "notum", "notumu", "notlar", "notları", "notlarım", "notlarımı") }
        if (notKelimesi && (m.contains("oku") || m.contains("söyle") || m.contains("dinlet") || m.contains("ne yazıyor") || m.contains("son not")))
        {
            // "son 3 notu oku" / "son üç notu oku": "son" sözcüğünden sonraki sayı (en çok 10)
            val sonIdx = t.indexOf("son")
            val sayi = if (sonIdx >= 0 && sonIdx + 1 < t.size) sayiOku(t, sonIdx + 1)?.first else null
            return Komut.NotOku(sayi?.coerceIn(1, 10) ?: if (t.any { it.startsWith("notlar") } && !m.contains("son not")) 3 else 1)
        }

        // "Neler yapabilirsin?" gibi dar ifadeler; genel "yardım" sözcüğü tek başına eşleşmez
        if (m.contains("neler yapabilirsin") || m.contains("ne yapabilirsin") || m.contains("ne işe yararsın") ||
            m.contains("sana neler söyleyebilirim") || m.contains("hangi komutları")) return Komut.Yardim

        if (t.any { it.startsWith("konumum") } || m.contains("neredeyim") || m.contains("bulunduğum yer")) return Komut.KonumGoster
        // Kamera: "kamerayı aç ve resim çek", "ne görüyorsun"; ön/arka belirtilmezse arka kamera
        val kameraKelimesi = t.any { it.startsWith("kamera") }
        if ((kameraKelimesi && (m.contains("aç") || m.contains("çek") || m.contains("bak") || m.contains("göster"))) ||
            m.contains("resim çek") || m.contains("fotoğraf çek") || m.contains("fotoğrafını çek") ||
            m.contains("ne görüyorsun") || m.contains("etrafımda ne var") || m.contains("karşımda ne var")) {
            return Komut.Kamera(on = m.contains("ön kamera") || m.contains("selfie"))
        }
        if (m.contains("saat kaç") || m.contains("saati söyle")) return Komut.SaatSor
        if (m.contains("tarih") || m.contains("bugün günlerden") || m.contains("hangi gün") || m.contains("bugün ayın")) return Komut.TarihSor

        val alarmVar = t.any { it.startsWith("alarm") }
        if (alarmVar && (m.contains("iptal") || m.contains("sil") || m.contains("kapat") || m.contains("kaldır"))) return Komut.AlarmIptal
        if (alarmVar && (m.contains("kur") || m.contains("ayarla") || m.contains("çal") || m.contains("koy") || m.contains("ekle")))
            return zamanOku(t)?.let { (s, d) -> Komut.AlarmKur(s, d) } ?: Komut.Bilinmiyor(metin)

        if (t.any { it.startsWith("zamanlayıcı") || it.startsWith("sayaç") || it.startsWith("kronometre") || it.startsWith("geri") } &&
            (m.contains("kur") || m.contains("ayarla") || m.contains("başlat") || m.contains("koy")) || m.contains("dakika sonra uyar") || m.contains("saniye sonra uyar")) {
            sureOku(t)?.let { return Komut.ZamanlayiciKur(it) }
        }
        return Komut.Bilinmiyor(metin)
    }

    // "7:30", "saat yedi buçuk", "akşam altıda", "sabah 6.45"
    private fun zamanOku(t: List<String>): Pair<Int, Int>? {
        val m = t.joinToString(" ")
        var saat: Int? = null
        var dakika = 0
        Regex("(\\d{1,2})[:.](\\d{2})").find(m)?.let { saat = it.groupValues[1].toInt(); dakika = it.groupValues[2].toInt() }
        if (saat == null) {
            var i = 0
            while (i < t.size) {
                // "dakika" ve "saniye" öncesindeki sayılar süredir, saat değildir
                val oku = sayiOku(t, i)
                if (oku != null && !(i + oku.second < t.size && t[i + oku.second].let { it.startsWith("dakika") || it.startsWith("saniye") })) {
                    if (oku.first in 0..24 && t[i] != "saat") {
                        saat = oku.first
                        val j = i + oku.second
                        if (j < t.size && t[j] in bucukBicimleri) dakika = 30
                        else if (j < t.size) sayiOku(t, j)?.let { if (it.first in 1..59) dakika = it.first }
                        break
                    }
                    i += oku.second
                } else i++
            }
        }
        var s = saat ?: return null
        if (s > 24 || dakika > 59) return null
        val akşam = m.contains("akşam") || m.contains("öğleden sonra") || m.contains("ikindi")
        val gece = m.contains("gece")
        if (akşam && s < 12) s += 12
        else if (gece && s in 6..11) s += 12
        else if (gece && s == 12) s = 0
        return (s % 24) to dakika
    }

    // "5 dakika", "on dakika", "bir buçuk saat", "30 saniye"
    private fun sureOku(t: List<String>): Int? {
        var toplam = 0
        var bulundu = false
        var i = 0
        while (i < t.size) {
            val oku = sayiOku(t, i)
            if (oku != null) {
                val j = i + oku.second
                var deger = oku.first.toDouble()
                var k = j
                if (k < t.size && t[k] in bucukBicimleri) { deger += 0.5; k++ }
                if (k < t.size) {
                    val birim = t[k]
                    val carpan = when {
                        birim.startsWith("saniye") -> 1
                        birim.startsWith("dakika") || birim.startsWith("dk") -> 60
                        birim.startsWith("saat") -> 3600
                        else -> 0
                    }
                    if (carpan > 0) { toplam += (deger * carpan).toInt(); bulundu = true; i = k + 1; continue }
                }
                i = j
            } else i++
        }
        return if (bulundu && toplam > 0) toplam else null
    }
}

// Sessiz moda geçmeden önce istenen sözlü onayı yorumlar (yanlış duyma sessiz modu açmasın diye)
object SessizOnayi {
    private val tr = Locale("tr", "TR")
    private fun temiz(metin: String) = metin.lowercase(tr).trim().trim('.', ',', '!', '?', ' ')
    fun evetMi(metin: String): Boolean = temiz(metin).let { m ->
        m == "evet" || m == "olur" || m == "tamam" || m == "onaylıyorum" || m == "evet geç" || m == "evet geçebilirsin" || m == "geç"
    }
    fun hayirMi(metin: String): Boolean = temiz(metin).let { m ->
        m.startsWith("hayır") || m.startsWith("vazgeç") || m == "yok" || m == "olmaz" || m == "istemiyorum" || m == "geçme"
    }
    const val SORU = "Sessiz moda geçeyim mi? Evet ya da hayır deyin."
}
