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
    data class AramaYap(val hedef: String) : Komut() // isim veya numara
    data class YouTubeAra(val sorgu: String) : Komut()
    data class MuzikCal(val sorgu: String) : Komut()
    data class FenerAyar(val ac: Boolean) : Komut()
    data class MesajOku(val adet: Int = 3) : Komut()
    data class MesajGonder(val hedef: String, val metin: String) : Komut()
    data class NavigasyonBaslat(val hedef: String) : Komut()
    data class UygulamaAc(val isim: String) : Komut()
    enum class AyarTuru { WIFI, BLUETOOTH, VERI, PARLAKLIK, SES_AYARLARI }
    data class SistemAyar(val tur: AyarTuru) : Komut()
    object AcilArama : Komut() // 112
    data class SesSecim(val erkek: Boolean) : Komut() // erkek=true → erkek ses, false → kadın ses
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
            val ac = !(m.contains("kapat") || m.contains("çık") || m.contains("bitir") || m.contains("kaldır"))
            android.util.Log.i("Sezai", "YORUM sessiz=$ac tetikleyen='${metin.take(60)}'")
            return Komut.Sessiz(ac)
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
        // YouTube: "YouTube'da ... ara/çal/oynat" — sadece "aç" değil; "YouTube'u aç" → UygulamaAc, orijinal büyük/küçük harf korunur
        if ((m.contains("youtube") && !(m.endsWith(" aç") || m.contains("youtube aç") || m.contains("youtube'u aç") || m.contains("youtube'ı aç"))) ||
            (m.contains("video") && (m.contains("ara") || m.contains("bul") || m.contains("izle")))) {
            val sorgu = run {
                val txt = metin.lowercase(trLocale)
                val prefix = listOf("youtube'da", "youtube da", "youtubeda", "youtube'de", "youtube de").firstOrNull { txt.contains(it) }
                val startIdx = if (prefix != null) txt.indexOf(prefix) + prefix.length else 0
                var result = metin.substring(startIdx).trim()
                for (suf in listOf(" ara", " bul", " izle", " çal", " oyna", " açıp", " aç")) {
                    if (result.lowercase(trLocale).endsWith(suf)) result = result.dropLast(suf.length).trim()
                }
                result
            }
            if (sorgu.isNotBlank()) return Komut.YouTubeAra(sorgu)
        }
        // Müzik: "... çal/oynat/aç" — orijinal büyük/küçük harf korunur
        val muzikFilteri = listOf("kamera", "alarm", "zamanlayıcı", "youtube", "harita", "navigasyon")
        if (muzikFilteri.none { m.contains(it) }) {
            val calKelimesi = t.any { it.startsWith("çal") || it.startsWith("oyna") }
            if (calKelimesi || (m.contains("müzik") && (m.contains("aç") || m.contains("başlat")))) {
                val sorgu = run {
                    val txt = metin.lowercase(trLocale)
                    val lastCal = txt.lastIndexOf(" çal")
                    val lastOynat = txt.lastIndexOf(" oynat")
                    when {
                        lastCal >= 0 -> metin.substring(0, lastCal).trim()
                        lastOynat >= 0 -> metin.substring(0, lastOynat).trim()
                        txt.contains("çal ") -> metin.substring(txt.indexOf("çal ") + 4).trim().trimEnd('.')
                        txt.contains("oynat ") -> metin.substring(txt.indexOf("oynat ") + 6).trim().trimEnd('.')
                        txt.contains("müzik aç") -> metin.substring(0, txt.indexOf("müzik aç")).trim()
                        calKelimesi -> {
                            val idx2 = t.indexOfFirst { it.startsWith("çal") || it.startsWith("oyna") }
                            t.take(idx2).joinToString(" ").trim()
                        }
                        else -> ""
                    }
                }
                if (sorgu.isNotBlank()) return Komut.MuzikCal(sorgu)
            }
        }
        // Telefon araması: "annemi ara", "0532 123 45 67'yi ara", "Ahmet'i ara"
        val araIslemi = t.any { it.startsWith("ara") && it.length <= 6 } || m.contains("telefon aç") || m.contains("telefon et")
        if (araIslemi) {
            val stopKelimeler = setOf("alarm", "arka", "plan", "sesle", "internette", "webde", "yardım", "müzik", "youtube")
            if (stopKelimeler.none { m.contains(it) }) {
                val iyelik = mapOf(
                    "annem" to "anne", "annen" to "anne", "abim" to "abi", "abin" to "abi",
                    "ablam" to "abla", "ablan" to "abla", "babam" to "baba", "baban" to "baba",
                    "kardeşim" to "kardeş", "kardeşin" to "kardeş", "amcam" to "amca", "amcan" to "amca",
                    "teyzem" to "teyze", "teyzen" to "teyze", "ninem" to "nine", "dedem" to "dede",
                    "eşim" to "eş", "arkadaşım" to "arkadaş",
                )
                fun aramaHedefi(raw: String): String {
                    val sonSoz = raw.substringAfterLast(" ", raw)
                    return if (iyelik.containsKey(sonSoz)) raw.substringBeforeLast(" ", "").let { if (it.isEmpty()) iyelik[sonSoz]!! else "$it ${iyelik[sonSoz]!!}" }
                    else raw
                }
                val idx = t.indexOfFirst { it.startsWith("ara") && it.length <= 6 }
                val hedef = if (idx > 0) {
                    aramaHedefi(t.take(idx).joinToString(" ").trim().trimEnd('\'', 'y', '\u0131', 'i', 'u', '\u00fc', 'a', 'e'))
                } else {
                    val telIdx = t.indexOf("telefon")
                    if (telIdx > 0) aramaHedefi(t.take(telIdx).joinToString(" ").trim().trimEnd('\'', 'y', '\u0131', 'i', 'u', '\u00fc', 'a', 'e')) else ""
                }
                if (hedef.isNotBlank()) return Komut.AramaYap(hedef)
            }
        }
        // Navigasyon: "Kadıköy'e git", "navigasyonu başlat ... için"
        val navigasyonVar = m.contains("navigasyon") || m.contains("haritalar") ||
            (t.any { it.startsWith("git") } && !m.contains("youtube") && !m.contains("aç")) ||
            Regex("\\b(yol tarifi|yol bul|rota)\\b").containsMatchIn(m)
        if (navigasyonVar) {
            val hedefTemizle: (String) -> String = { txt ->
                listOf("navigasyonu başlat", "navigasyona gir", "haritalar", "yol tarifi ver", "yol bul",
                    "git", "için navigasyon", "için yol tarifi", "rota", "yol").fold(txt) { s, k ->
                    s.replace(k, " ")
                }.trim()
                    .replace(Regex("['\u2019][a-zğüşıöç]*$"), "")  // 'e, 'ya, 'de, 'den gibi hal ekleri
                    .trimEnd(',', '.', '\'', '\u2019').trim()
            }
            val hedef = hedefTemizle(metin.lowercase(Locale("tr", "TR"))).trim()
            if (hedef.isNotBlank() && hedef.length > 2) return Komut.NavigasyonBaslat(hedef)
        }
        // Acil arama: "112 ara", "acil yardım", "ambulans çağır"
        if (m.contains("112") || m.contains("acil yardım") || m.contains("ambulans") || m.contains("itfaiye") || m.contains("155") || m.contains("156") || m.contains("158")) {
            if (m.contains("ara") || m.contains("çağır") || m.contains("bağlan") || m.contains("arama")) return Komut.AcilArama
        }
        // Sistem ayarları: Wi-Fi, Bluetooth, mobil veri, parlaklık, ses
        val wifiVar = m.contains("wifi") || m.contains("wi-fi") || m.contains("kablosuz") || m.contains("wifii")
        if (wifiVar) return Komut.SistemAyar(Komut.AyarTuru.WIFI)
        val btVar = (m.contains("bluetooth") || m.contains("bluetoot") || m.contains("bt ") || m.endsWith("bt")) && !m.contains("youtube")
        if (btVar) return Komut.SistemAyar(Komut.AyarTuru.BLUETOOTH)
        val veriVar = (m.contains("mobil veri") || m.contains("internet") && (m.contains("aç") || m.contains("kapat") || m.contains("kapat") || m.contains("ayar"))) && !m.contains("youtube")
        if (veriVar) return Komut.SistemAyar(Komut.AyarTuru.VERI)
        val parlaklikVar = m.contains("parlaklık") || m.contains("ekran parla") || m.contains("ekranı parla")
        if (parlaklikVar) return Komut.SistemAyar(Komut.AyarTuru.PARLAKLIK)
        val sesAyarVar = (m.contains("ses ayar") || m.contains("zil") && m.contains("ayar")) && !m.contains("tts") && !m.contains("konuşma")
        if (sesAyarVar) return Komut.SistemAyar(Komut.AyarTuru.SES_AYARLARI)
        // Fener: "feneri aç/kapat", "el feneri aç"
        val fenerVar = m.contains("fener") || m.contains("el feneri") || m.contains("torch") || m.contains("ışık aç") || m.contains("ışığı aç")
        if (fenerVar) {
            val kapat = m.contains("kapat") || m.contains("kaldır") || m.contains("söndür")
            return Komut.FenerAyar(!kapat)
        }
        // SMS okuma: "son mesajlarımı oku", "mesajlarımı oku"
        val mesajKelimesi = t.any { it.startsWith("mesaj") || it.startsWith("sms") }
        if (mesajKelimesi && (m.contains("oku") || m.contains("söyle") || m.contains("dinlet") || m.contains("var mı") || m.contains("kaç") || t.contains("ne"))) {
            val sonIdx = t.indexOf("son")
            val sayi = if (sonIdx >= 0 && sonIdx + 1 < t.size) sayiOku(t, sonIdx + 1)?.first else null
            return Komut.MesajOku(sayi?.coerceIn(1, 10) ?: 3)
        }
        // SMS gönderme: "Ahmet'e 'toplantı iptal' mesajı gönder", "anneme şunu yaz: eve geç geleceğim"
        if (mesajKelimesi && (m.contains("gönder") || m.contains("yaz") || m.contains("at "))) {
            val icerikRegex = Regex("[\"'\u2018\u2019\u201c\u201d]([^\"'\u2018\u2019\u201c\u201d]+)[\"'\u2018\u2019\u201c\u201d]")
            val icerik = icerikRegex.find(metin)?.groupValues?.get(1)?.trim()
                ?: run {
                    val ikNoktaIdx = metin.indexOf(':')
                    if (ikNoktaIdx > 0 && ikNoktaIdx < metin.length - 1) metin.substring(ikNoktaIdx + 1).trim() else ""
                }
            if (icerik.isNotBlank()) {
                val mesajIdx = t.indexOfFirst { it.startsWith("mesaj") || it.startsWith("sms") }
                var hedefRaw = if (mesajIdx > 0) t.take(mesajIdx).joinToString(" ").trim()
                               else t.take((t.indexOf("yaz").takeIf { it > 0 } ?: t.size)).joinToString(" ").trim()
                // İçerik tokenları hedef kısmına sızdıysa çıkar (örn. "anneme X mesajı gönder: X")
                val icerikKucuk = icerik.lowercase(trLocale).trim()
                if (hedefRaw.endsWith(" $icerikKucuk") || hedefRaw == icerikKucuk)
                    hedefRaw = hedefRaw.removeSuffix(" $icerikKucuk").removeSuffix(icerikKucuk).trim()
                val iyelik2 = mapOf("annem" to "anne", "annen" to "anne", "babam" to "baba",
                    "baban" to "baba", "abim" to "abi", "ablam" to "abla",
                    "kardeşim" to "kardeş", "eşim" to "eş", "arkadaşım" to "arkadaş")
                val hedef = hedefRaw.replace(Regex("['\u2019][a-zğüşıöç]+$"), "").trim()
                    .let { h -> iyelik2[h] ?: iyelik2[h.dropLast(1)] ?: iyelik2[h.dropLast(2)] ?: h }
                if (hedef.isNotBlank()) return Komut.MesajGonder(hedef, icerik)
            }
        }
        // Ses seçimi: "erkek ses", "kadın ses", "bayan ses", "dişi ses"
        if (m.contains("erkek ses") || (m.contains("erkek") && m.contains("konuş"))) return Komut.SesSecim(true)
        if (m.contains("kadın ses") || m.contains("bayan ses") || m.contains("dişi ses") || (m.contains("kadın") && m.contains("konuş"))) return Komut.SesSecim(false)

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
        // Uygulama açma: "WhatsApp'ı aç", "haritayı aç", "tarayıcıyı aç"
        if (m.contains("aç") && !m.contains("alarm") && !m.contains("fener") && !m.contains("mikrofon")) {
            val uygulamalar = listOf("whatsapp", "telegram", "instagram", "twitter", "spotify",
                "chrome", "firefox", "tarayıcı", "youtube", "harita", "maps",
                "kamera", "galeri", "ayarlar", "takvim", "hesap makinesi", "müzik")
            val bulunan = uygulamalar.firstOrNull { m.contains(it) }
            if (bulunan != null) return Komut.UygulamaAc(bulunan)
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
