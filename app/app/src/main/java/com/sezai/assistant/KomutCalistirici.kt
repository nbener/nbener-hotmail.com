package com.sezai.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

// Komutu gerçekten yapar ve Sezai'nin söyleyeceği cevabı döndürür
class KomutCalistirici(private val context: Context) {
    private val tr = Locale("tr", "TR")

    fun calistir(komut: Komut): String = when (komut) {
        is Komut.SaatSor -> LocalDateTime.now().let { "Saat ${it.hour} ${if (it.minute == 0) "tam" else it.minute.toString().padStart(2, '0')}" }
        is Komut.TarihSor -> LocalDateTime.now().let {
            "Bugün ${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.FULL, tr)} ${it.year}, ${it.dayOfWeek.getDisplayName(TextStyle.FULL, tr)}"
        }
        is Komut.AlarmKur -> alarmKur(komut.saat, komut.dakika)
        is Komut.AlarmIptal -> alarmIptal()
        is Komut.Yardim -> "Saati ve tarihi söyleyebilirim, alarm ve zamanlayıcı kurabilirim. " +
            "Birini aramanızı sağlarım: 'Annemi ara' veya 'beş yüz elli numarasını ara' deyin. " +
            "YouTube'da arama yapabilirim: 'YouTube'da şarkı adı ara' deyin. " +
            "Konumunuzu söyleyebilir, kamerayla çevrenizi anlatabilir, ekranınızda ne olduğunu okuyabilirim. " +
            "Sessiz moda geçebilirim. Görünümü, arka plan dinlemeyi ve konuşma sesini değiştirebilirim. " +
            "Hatırla deyip bir şey öğretebilirsiniz. Not alıp sonra okuyabilirim. Açıkça isterseniz internette araştırma da yapabilirim."
        is Komut.NotKaydet -> if (NotDefteri(context).ekle(komut.metin)) "Notu kaydettim." else "Notu kaydedemedim."
        is Komut.NotOku -> NotDefteri(context).son(komut.adet).let { l ->
            if (l.isEmpty()) "Kayıtlı notunuz yok."
            else if (l.size == 1) "Son notunuz: ${l[0]}"
            else l.mapIndexed { i, n -> "${i + 1}. not: $n" }.joinToString(". ")
        }
        is Komut.Sessiz -> { BildirimKumanda.sessizAyarla(context, komut.ac); if (komut.ac) "Sessiz moda geçtim. Çıkmak için bildirimdeki ya da ana ekrandaki düğmeyi kullanın." else "Sessiz moddan çıktım." }
        is Komut.Hatirla -> if (NotDefteri(context, "hafiza.txt").ekle(komut.metin)) "Tamam, bunu hatırlayacağım." else "Bunu kaydedemedim."
        is Komut.Hatirlat -> NotDefteri(context, "hafiza.txt").son(5).let { l ->
            if (l.isEmpty()) "Henüz bir şey öğretmediniz."
            else "Hatırladıklarım: " + l.joinToString(". ")
        }
        is Komut.Gorunum -> {
            context.getSharedPreferences("gorunum", Context.MODE_PRIVATE).edit().putBoolean("gece", komut.gece).apply()
            if (komut.gece) "Gece görünümüne geçtim." else "Gündüz görünümüne geçtim."
        }
        is Komut.ArkaPlan -> {
            if (komut.ac && BildirimKumanda.sessizMi(context)) {
                "Sessiz moddasınız, önce sessiz moddan çıkın."
            } else {
                // Reddedilen açma isteği sonraki açılış için dinleme tercihi bırakmamalı.
                context.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE).edit().putBoolean("arka_plan", komut.ac).apply()
                if (komut.ac) { SezaiArkaPlanServisi.baslat(context); "Arka planda dinlemeye başladım." }
                else { SezaiArkaPlanServisi.durdur(context); "Arka plan dinlemeyi kapattım." }
            }
        }
        is Komut.SesAyar -> {
            val o = context.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE)
            val yeni = (o.getInt("tts_seviye", 100) + komut.fark).coerceIn(10, 250)
            o.edit().putInt("tts_seviye", yeni).apply()
            "Konuşma sesi yüzde $yeni."
        }
        is Komut.KonumGoster -> "Konumunuzu alıyorum." // gerçek işi MainActivity yapar (izin ve eşzamansız sonuç)
        is Komut.Kamera -> "Kamerayı açıyorum." // gerçek işi çağıran (uygulama/servis) yapar: çekim, izin ve eşzamansız yorum
        is Komut.ZamanlayiciKur -> zamanlayiciKur(komut.saniye)
        is Komut.AramaYap -> "Arıyorum." // gerçek işi komutVer yapar: kişi arama ve arama başlatma
        is Komut.YouTubeAra -> youtubeAc(komut.sorgu)
        is Komut.MuzikCal -> muzikAc(komut.sorgu)
        is Komut.FenerAyar -> fenerAyarla(komut.ac)
        is Komut.MesajOku -> mesajlariOku(komut.adet)
        is Komut.Bilinmiyor -> "Bunu anlayamadım, tekrar söyler misiniz?"
    }

    private fun baslat(i: Intent): Boolean = try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i); true
    } catch (e: Exception) { Log.w("Sezai", "Intent hata: $e"); false }

    private fun alarmKur(saat: Int, dakika: Int): String {
        val ok = baslat(Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, saat)
            putExtra(AlarmClock.EXTRA_MINUTES, dakika)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Sezai")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        })
        Log.i("Sezai", "ALARM kur $saat:$dakika ok=$ok")
        val d = dakika.toString().padStart(2, '0')
        return if (ok) "Tamam, alarmı $saat $d için kurdum." else "Alarmı kuramadım."
    }

    private fun alarmIptal(): String {
        val ok = baslat(Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Sezai")
        })
        Log.i("Sezai", "ALARM iptal ok=$ok")
        return if (ok) "Sezai alarmlarını iptal ettim." else "Alarmı iptal edemedim."
    }

    private fun mesajlariOku(adet: Int): String {
        return try {
            val izin = context.checkSelfPermission(android.Manifest.permission.READ_SMS)
            if (izin != android.content.pm.PackageManager.PERMISSION_GRANTED) return "SMS okumak için izin gerekiyor."
            val cursor = context.contentResolver.query(
                android.net.Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null, null, "date DESC LIMIT $adet"
            ) ?: return "Mesaj okunamadı."
            val sonuclar = mutableListOf<String>()
            cursor.use { c ->
                while (c.moveToNext() && sonuclar.size < adet) {
                    val gonderen = c.getString(0) ?: "bilinmiyor"
                    val icerik = c.getString(1)?.take(150) ?: ""
                    sonuclar.add("$gonderen: $icerik")
                }
            }
            Log.i("Sezai", "MESAJ oku adet=${sonuclar.size}")
            if (sonuclar.isEmpty()) "Yeni mesajınız yok."
            else if (sonuclar.size == 1) "Son mesaj, ${sonuclar[0]}"
            else "Son ${sonuclar.size} mesaj. " + sonuclar.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString(". ")
        } catch (e: Exception) { Log.w("Sezai", "MESAJ hata: $e"); "Mesajları okuyamadım." }
    }

    private fun fenerAyarla(ac: Boolean): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return "Bu cihazda fener desteklenmiyor."
            cm.setTorchMode(id, ac)
            Log.i("Sezai", "FENER ac=$ac id=$id")
            if (ac) "Feneri açtım." else "Feneri kapattım."
        } catch (e: Exception) { Log.w("Sezai", "FENER hata: $e"); "Feneri değiştiremedim." }
    }

    private fun youtubeAc(sorgu: String): String {
        val temiz = sorgu.trim()
        if (temiz.isBlank()) return "Ne aramak istediğinizi söyleyin."
        val ok = baslat(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(temiz)}")))
        Log.i("Sezai", "YOUTUBE ara='$temiz' ok=$ok")
        return if (ok) "YouTube'da '$temiz' arıyorum." else "YouTube açılamadı."
    }

    private fun muzikAc(sorgu: String): String {
        val temiz = sorgu.trim()
        if (temiz.isBlank()) return "Ne çalmak istediğinizi söyleyin."
        // Önce YouTube Music dene, olmazsa YouTube
        val ok = baslat(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=${Uri.encode(temiz)}")))
            || baslat(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(temiz)}")))
        Log.i("Sezai", "MUZIK ara='$temiz' ok=$ok")
        return if (ok) "Tamam, '$temiz' açıyorum." else "Müzik açılamadı."
    }

    private fun zamanlayiciKur(saniye: Int): String {
        val ok = baslat(Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, saniye)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Sezai")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        })
        Log.i("Sezai", "ZAMANLAYICI $saniye sn ok=$ok")
        val dk = saniye / 60; val sn = saniye % 60
        val metin = if (dk > 0 && sn == 0) "$dk dakika" else if (dk > 0) "$dk dakika $sn saniye" else "$sn saniye"
        return if (ok) "Tamam, $metin için zamanlayıcı kurdum." else "Zamanlayıcıyı kuramadım."
    }
}
