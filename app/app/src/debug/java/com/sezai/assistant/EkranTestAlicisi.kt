package com.sezai.assistant

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

// Gerçek ekran, ses ve ağ kullanılmaz; erişim izni açılmaz. Yalnızca sentetik metinler.
class EkranTestAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rapor = mutableListOf<String>()
        fun kontrol(ad: String, islem: () -> Unit) {
            try { islem(); rapor.add("PASS $ad") } catch (_: Exception) { rapor.add("FAIL $ad") }
        }
        kontrol("Ekran komutları hitap ve noktalama ile tanınıyor") {
            listOf("ekranımda ne var?", "Sezai, ekranı incele", "lütfen ekranımı analiz et", "ekranda ne var lütfen").forEach {
                check(SohbetKomutlari.ekranInceleMi(it))
            }
        }
        kontrol("Olumsuz ve alıntı ekran ifadeleri işlem başlatmıyor") {
            listOf("ekranı inceleme", "ekranı analiz et demedim", "ekranımda ne var sorusu", "ekranı okuma").forEach {
                check(!SohbetKomutlari.ekranInceleMi(it))
            }
        }
        kontrol("Kapalı mod ve başka uygulama otomatik okumaya alınmıyor") {
            check(!EkranMetni.otomatikHedefMi(false, "com.termux", "com.termux"))
            check(!EkranMetni.otomatikHedefMi(true, "com.termux", "com.baska"))
            check(!EkranMetni.otomatikHedefMi(true, null, null))
            check(EkranMetni.otomatikHedefMi(true, "com.termux", "com.termux"))
        }
        kontrol("Parola taslak ve görünmeyen alanlar atlanıyor") {
            check(!EkranMetni.alinabilirMi(true, true, false, "android.widget.TextView"))
            check(!EkranMetni.alinabilirMi(true, false, true, "android.view.View"))
            check(!EkranMetni.alinabilirMi(true, false, false, "android.widget.EditText"))
            check(!EkranMetni.alinabilirMi(false, false, false, "android.widget.TextView"))
            check(EkranMetni.alinabilirMi(true, false, false, "android.widget.TextView"))
        }
        kontrol("İlk görünümde geçmiş okunmuyor") {
            check(EkranAkisi().yeni(listOf("Başlık", "Eski mesaj")).isEmpty())
        }
        kontrol("Sabit ekran tekrarlanmıyor yeni mesaj okunuyor") {
            val akis = EkranAkisi(); val ilk = listOf("Başlık", "Eski mesaj")
            akis.yeni(ilk); check(akis.yeni(ilk).isEmpty())
            check(akis.yeni(ilk + "Yeni mesaj") == "Yeni mesaj")
        }
        kontrol("Büyüyen yanıtın yalnızca eklenen kısmı okunuyor") {
            val akis = EkranAkisi(); akis.yeni(listOf("Başlık", "Merhaba"))
            check(akis.yeni(listOf("Başlık", "Merhaba dünya")) == " dünya")
            check(akis.yeni(listOf("Başlık", "Merhaba dünya")).isEmpty())
        }
        kontrol("İlişkisiz ekran bütün geçmişi yeniden okutmaz") {
            val akis = EkranAkisi(); akis.yeni(listOf("Birinci ekran"))
            check(akis.yeni(listOf("İkinci ekran", "Eski içerik")).isEmpty())
        }
        kontrol("Terminal kaymasında yalnızca yeni satır alınır") {
            val akis = EkranAkisi(); akis.yeni(listOf("A", "B", "C"))
            check(akis.yeni(listOf("B", "C", "D")) == "D")
            check(akis.yeni(listOf("C", "D", "A")).isEmpty())
        }
        kontrol("Sıfırlama ve uzun metin sınırı korunur") {
            val akis = EkranAkisi(); akis.yeni(listOf("Başlık"))
            check(akis.yeni(listOf("Başlık", "x".repeat(14000))).length <= 12000)
            akis.sifirla(); check(akis.yeni(listOf("Başlık", "Yeni başlangıç")).isEmpty())
        }
        kontrol("Sistem ekran hizmetini doğru izin ve yetenekle görüyor") {
            val yonetici = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val hizmet = yonetici.installedAccessibilityServiceList.first {
                it.resolveInfo.serviceInfo.name == EkranOkuyucuServisi::class.java.name
            }
            check(hizmet.resolveInfo.serviceInfo.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE")
            check(hizmet.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT != 0)
            check(hizmet.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES == 0)
            check(hizmet.eventTypes and AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED == 0)
        }
        context.openFileOutput("ekran_test_sonuc.txt", Context.MODE_PRIVATE).bufferedWriter().use {
            it.write("TEST_ID ${intent.getStringExtra("test_id")}\n" + rapor.joinToString("\n"))
        }
    }
}
