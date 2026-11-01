package com.sezai.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityButtonController
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

// Sistem bağlar; BIND_ACCESSIBILITY_SERVICE ile korunur. Hiçbir ekrana eylem göndermez.
class EkranOkuyucuServisi : AccessibilityService() {
    private val anaIs = Handler(Looper.getMainLooper())
    private val ayar by lazy { getSharedPreferences("ekran_okuma", MODE_PRIVATE) }
    private val akis = EkranAkisi()
    private val sira = ArrayDeque<String>()
    private var konusma: KonusmaMotoru? = null
    private var baslatiliyor = false
    private var konusuyor = false
    private var aktifPaket: String? = null
    private var nesil = 0
    private val oncelik by lazy { GorusmeOnceligi(this) { ayar.edit().putBoolean("otomatik", false).apply(); durdur() } }
    private val ayarDinleyici = SharedPreferences.OnSharedPreferenceChangeListener { _, anahtar ->
        if (anahtar == "otomatik" || anahtar == "paket") {
            durdur(); aktifPaket = null
            if (!ayar.getBoolean("otomatik", false)) { konusma?.kapat(); konusma = null; baslatiliyor = false }
        }
    }
    private val ekranKapandi = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { durdur(); aktifPaket = null; disMetniniUnut() }
    }
    private val oku = Runnable { yeniMetniAl() }
    // Son dış uygulamanın metni yalnız bellekte tutulur; Sezai öne geçince "ekranımda ne var" için kullanılır.
    private var sonDisMetin = ""
    private var sonDisZaman = 0L
    private val disKaydet = Runnable { disMetniniHatirla() }
    private val dugme = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) { ekraniIncele() }
    }

    override fun onServiceConnected() {
        canli = this
        ayar.registerOnSharedPreferenceChangeListener(ayarDinleyici)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(ekranKapandi, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)
        else registerReceiver(ekranKapandi, IntentFilter(Intent.ACTION_SCREEN_OFF))
        accessibilityButtonController.registerAccessibilityButtonCallback(dugme, anaIs)
        oncelik.izle()
    }

    private fun kullanilabilirMi(): Boolean =
        (getSystemService(POWER_SERVICE) as PowerManager).isInteractive &&
        !(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val paket = event.packageName?.toString()
        if (disUygulamaMi(paket) && kullanilabilirMi()) {
            // Olay başına tek geciktirilmiş okuma; periyodik sorgu yok.
            anaIs.removeCallbacks(disKaydet)
            anaIs.postDelayed(disKaydet, 1500)
        }
        if (!ayar.getBoolean("otomatik", false) || !kullanilabilirMi()) { durdur(); return }
        val secili = ayar.getString("paket", "com.termux")
        if (!EkranMetni.otomatikHedefMi(true, secili, paket)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { durdur(); aktifPaket = null }
            return
        }
        if (aktifPaket != paket || event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            durdur(); aktifPaket = paket
        }
        anaIs.removeCallbacks(oku)
        // Akış halindeki metin sabitlenince tek okuma; periyodik ekran sorgusu yok.
        anaIs.postDelayed(oku, 1800)
    }

    private fun disUygulamaMi(paket: String?): Boolean =
        !paket.isNullOrBlank() && paket != packageName && paket != "com.android.systemui" &&
        !paket.contains("inputmethod") && !paket.contains("keyboard")

    private fun disMetniniHatirla() {
        if (!kullanilabilirMi()) return
        val kok = rootInActiveWindow ?: return
        try {
            val paket = kok.packageName?.toString()
            if (!disUygulamaMi(paket)) return
            val metin = EkranMetni.topla(kok, paket!!).joinToString("\n")
            if (metin.isBlank()) return
            sonDisMetin = metin.take(SON_METIN_SINIRI)
            sonDisZaman = System.currentTimeMillis()
        } finally { kok.recycle() }
    }

    private fun hatirlananDisMetin(): String =
        if (System.currentTimeMillis() - sonDisZaman <= SON_METIN_OMRU_MS) sonDisMetin else ""

    private fun disMetniniUnut() { anaIs.removeCallbacks(disKaydet); sonDisMetin = ""; sonDisZaman = 0L }

    private fun yeniMetniAl() {
        if (!ayar.getBoolean("otomatik", false) || !kullanilabilirMi()) { durdur(); return }
        val kok = rootInActiveWindow ?: return
        try {
            val paket = ayar.getString("paket", "com.termux") ?: return
            if (kok.packageName?.toString() != paket) { durdur(); aktifPaket = null; return }
            val yeni = akis.yeni(EkranMetni.topla(kok, paket))
            if (yeni.isBlank()) return
            if (sira.size > 24) { durdur(); return }
            yeni.chunked(600).take(24 - sira.size).forEach { sira.addLast(it) }
            sonraki()
        } finally { kok.recycle() }
    }

    private fun sonraki() {
        if (konusuyor || sira.isEmpty()) return
        if (!kullanilabilirMi() || !ayar.getBoolean("otomatik", false)) { durdur(); return }
        val kok = rootInActiveWindow ?: run { durdur(); return }
        val uygun = try { kok.packageName?.toString() == ayar.getString("paket", "com.termux") } finally { kok.recycle() }
        if (!uygun) { durdur(); return }
        if (konusma == null) {
            if (!baslatiliyor) {
                baslatiliyor = true
                konusma = AndroidKonusma(this, "com.google.android.tts") { hazir ->
                    anaIs.post { baslatiliyor = false; if (hazir) sonraki() else durdur() }
                }
            }
            return
        }
        if (!konusma!!.hazirMi()) return
        if (!oncelik.hazirlan(true)) { durdur(); return }
        val siraNo = nesil
        konusuyor = true
        konusma!!.konus(sira.removeFirst(),
            bitince = { anaIs.post {
                if (nesil == siraNo) { konusuyor = false; oncelik.birakOdak(); sonraki() }
            } },
            hata = { anaIs.post { if (nesil == siraNo) durdur() } })
    }

    // Sezai öndeyken (sesli komut) arkadaki uygulamanın penceresini bulur: pencereler önden arkaya sıralıdır.
    // Sezai, sistem arayüzü ve klavye atlanır. Yalnızca açık istekle çağrılır; sorgu döngüsü yoktur.
    private fun disPencereMetni(): String {
        if (!kullanilabilirMi()) return ""
        for (pencere in windows) {
            if (pencere.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
            val kok = pencere.root ?: continue
            try {
                val paket = kok.packageName?.toString().orEmpty()
                if (paket.isBlank() || paket == packageName || paket == "com.android.systemui") continue
                return EkranMetni.topla(kok, paket).joinToString("\n")
            } finally { kok.recycle() }
        }
        return hatirlananDisMetin()
    }

    private fun ekraniIncele() {
        durdur()
        EkranIncelemeBellek.metin = null
        if (!kullanilabilirMi()) return
        val kok = rootInActiveWindow ?: return
        val metin = try {
            val paket = kok.packageName?.toString().orEmpty()
            if (paket == packageName) disPencereMetni()
            else if (paket.isBlank()) "" else EkranMetni.topla(kok, paket).joinToString("\n")
        } finally { kok.recycle() }
        EkranIncelemeBellek.metin = metin
        startActivity(Intent(this, EkranIncelemeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun durdur() {
        nesil++; anaIs.removeCallbacks(oku); sira.clear(); akis.sifirla()
        konusuyor = false; konusma?.durdur(); oncelik.durdur()
    }
    override fun onInterrupt() { durdur() }
    override fun onDestroy() {
        if (canli === this) canli = null
        disMetniniUnut(); durdur(); konusma?.kapat(); oncelik.kapat()
        ayar.unregisterOnSharedPreferenceChangeListener(ayarDinleyici)
        accessibilityButtonController.unregisterAccessibilityButtonCallback(dugme)
        try { unregisterReceiver(ekranKapandi) } catch (_: IllegalArgumentException) {}
        super.onDestroy()
    }

    companion object {
        private const val SON_METIN_SINIRI = 20000
        private const val SON_METIN_OMRU_MS = 10 * 60 * 1000L
        @Volatile private var canli: EkranOkuyucuServisi? = null
        // Ana iş parçacığından çağrılır; servis bağlı değilse null (erişilebilirlik izni kapalı).
        fun disEkranMetni(): String? = canli?.disPencereMetni()
        fun baglimi(): Boolean = canli != null
    }
}
