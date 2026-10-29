package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer

// Sahte tanıma oturumları kullanır; mikrofon ve ağ açılmaz.
class DinlemeTestAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val testId = intent.getStringExtra("test_id") ?: "elle"
        val bekleyen = goAsync()
        Handler(Looper.getMainLooper()).post {
            val rapor = mutableListOf<String>()
            fun kontrol(ad: String, dene: () -> Unit) {
                try { dene(); rapor.add("PASS $ad") }
                catch (_: Exception) { rapor.add("FAIL $ad") }
            }
            try {
                kontrol("Eski hata yeni dinlemeyi kapatmıyor") {
                    val a = SahteOturum(); val b = SahteOturum()
                    val sira = ArrayDeque(listOf(a, b)); var hata = 0
                    val motor = AndroidDinleme(context) { sira.removeFirst() }
                    motor.dinle({}, { hata++ }); motor.dinle({}, { hata++ })
                    a.dinleyici.onError(SpeechRecognizer.ERROR_CLIENT)
                    check(hata == 0 && b.kapatma == 0)
                    motor.kapat()
                    check(hata == 0 && b.kapatma == 1)
                }
                kontrol("Eski sonuç yeni dinlemeye karışmıyor") {
                    val a = SahteOturum(); val b = SahteOturum()
                    val sira = ArrayDeque(listOf(a, b)); val gelen = mutableListOf<String>()
                    val motor = AndroidDinleme(context) { sira.removeFirst() }
                    motor.dinle({ gelen.add(it) }, {}); motor.dinle({ gelen.add(it) }, {})
                    a.dinleyici.onResults(sonuc("eski"))
                    check(gelen.isEmpty() && b.kapatma == 0)
                    b.dinleyici.onResults(sonuc("yeni"))
                    check(gelen == listOf("yeni") && b.kapatma == 1)
                }
                kontrol("Tekrarlanan sonuç ve hata tek yanıt üretiyor") {
                    val a = SahteOturum(); var gelen = 0; var hata = 0
                    val motor = AndroidDinleme(context) { a }
                    motor.dinle({ gelen++ }, { hata++ })
                    a.dinleyici.onResults(sonuc("merhaba"))
                    a.dinleyici.onResults(sonuc("merhaba"))
                    a.dinleyici.onError(SpeechRecognizer.ERROR_CLIENT)
                    check(gelen == 1 && hata == 0)
                }
                kontrol("İzin hatasında dinleme kapanıp anlaşılır hata veriyor") {
                    val a = SahteOturum(izinYok = true); val hatalar = mutableListOf<String>()
                    val motor = AndroidDinleme(context) { a }
                    motor.dinle({}, { hatalar.add(it) })
                    check(hatalar == listOf("Mikrofon izni yok") && a.kapatma == 1)
                }
                kontrol("Sesli bitirme hitap ve noktalama ile tanınıyor") {
                    check(SohbetKomutlari.bitirMi("Sezai, sohbeti bitir."))
                    check(!SohbetKomutlari.bitirMi("DUR!"))
                    check(SohbetKomutlari.bitirMi("Konuşmayı bitir…"))
                }
                kontrol("Dur ve Sezai sözü alır sohbeti bitirmez") {
                    for (metin in listOf("dur", "DUR!", "Sezai", "Sezai, dur!", "Dur Sezai")) {
                        check(!SohbetKomutlari.bitirMi(metin))
                        check(SohbetKomutlari.sozAlmaMetni(metin) == "")
                    }
                }
                kontrol("Söz alma arkasındaki istek değiştirilmeden korunur") {
                    check(SohbetKomutlari.sozAlmaMetni("Sezai, peki nüfusu kaç?") == "peki nüfusu kaç?")
                    check(SohbetKomutlari.sozAlmaMetni("Dur, başka konu: yemek yapalım.") == "başka konu: yemek yapalım.")
                }
                kontrol("Alıntı ve benzer sözcükler söz alma sayılmaz") {
                    for (metin in listOf("durum nedir", "durak nerede", "dur demedim", "Sezai kelimesi ne demek", "Bana dur dediler"))
                        check(SohbetKomutlari.sozAlmaMetni(metin) == null)
                }
                kontrol("Normal konuşma yanlışlıkla sohbeti bitirmiyor") {
                    check(!SohbetKomutlari.bitirMi("Sohbeti bitir demedim"))
                    check(!SohbetKomutlari.bitirMi("Sezai bugün nasılsın?"))
                    check(!SohbetKomutlari.bitirMi("Sohbeti bitirmek istemiyorum"))
                }
            } finally {
                context.openFileOutput("dinleme_test_sonuc.txt", Context.MODE_PRIVATE).bufferedWriter().use {
                    it.write("TEST_ID $testId\n" + rapor.joinToString("\n"))
                }
                bekleyen.finish()
            }
        }
    }

    private fun sonuc(metin: String) = Bundle().apply {
        putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(metin))
    }

    private class SahteOturum(private val izinYok: Boolean = false) : TanimaOturumu {
        lateinit var dinleyici: RecognitionListener
        var kapatma = 0
        override fun baslat(dinleyici: RecognitionListener, istek: Intent) {
            this.dinleyici = dinleyici
            if (izinYok) throw SecurityException()
        }
        override fun durdur() {}
        override fun kapat() {
            kapatma++
            // destroy ile eşzamanlı gelebilen eski hatayı da canlandır.
            dinleyici.onError(SpeechRecognizer.ERROR_CLIENT)
        }
    }
}
