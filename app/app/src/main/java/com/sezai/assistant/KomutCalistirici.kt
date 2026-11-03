package com.sezai.assistant

import android.content.Context
import android.content.Intent
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
        is Komut.Yardim -> "Saati ve tarihi söyleyebilirim, alarm ve zamanlayıcı kurabilirim, konumunuzu söyleyebilir, " +
            "kamerayla çevrenizi anlatabilir, ekranınızda ne olduğunu okuyabilir ve sizinle sohbet edebilirim. " +
            "Sessiz moda geçebilirim. Görünümü, arka plan dinlemeyi ve konuşma sesini değiştirebilirim. Hatırla deyip bir şey öğretebilirsiniz. Not alıp sonra okuyabilirim: not al deyip notu söyleyin, son notu oku deyin. Açıkça isterseniz internette araştırma da yapabilirim."
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
