package com.sezai.assistant

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.widget.*

// Ekran erişiminin kapsamı ve otomatik okuma ayrı, açık kullanıcı seçimleridir.
class EkranOkumaAyarActivity : Activity() {
    private val ayar by lazy { getSharedPreferences("ekran_okuma", MODE_PRIVATE) }
    private lateinit var durum: TextView
    private lateinit var otomatik: Switch
    private var guncelleniyor = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val kutu = ekranKutusu()
        kutu.addView(ekranYazisi("Ekran yardımcısı", 24f))
        kutu.addView(ekranYazisi("Erişim izni açık ekranın metnini almayı sağlar. Açık ekrandayken Android erişilebilirlik düğmesinden Sezai’yi seçin; metni okuyabilir veya Yorumla ve oku ile seçili yapay zekâya gönderebilirsiniz. Metinler dosyaya ya da günlüğe yazılmaz."))
        kutu.addView(ekranYazisi("Bu aşama erişilebilir metni okur; fotoğraf, video ve çizimleri görsel olarak analiz etmez. Genel sesli uyandırma ve telefona dokunmadan komut alma henüz eklenmedi."))
        durum = ekranYazisi("").also { kutu.addView(it) }
        kutu.addView(ekranDugmesi("Android ekran erişimi izni") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        kutu.addView(ekranYazisi("Otomatik okuma uygulaması", 19f))
        val hedefler = mutableListOf("Termux" to "com.termux", "ChatGPT" to "com.openai.chatgpt")
        val sorgu = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(sorgu, 0).map {
            it.loadLabel(packageManager).toString() to it.activityInfo.packageName
        }.filter { it.second != packageName }.sortedBy { it.first }.forEach { hedef ->
            if (hedefler.none { it.second == hedef.second }) hedefler.add(hedef)
        }
        val secim = Spinner(this)
        secim.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hedefler.map { it.first })
        secim.setSelection(hedefler.indexOfFirst { it.second == ayar.getString("paket", "com.termux") }.coerceAtLeast(0))
        secim.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val paket = hedefler[position].second
                if (paket != ayar.getString("paket", "com.termux")) {
                    ayar.edit().putString("paket", paket).putBoolean("otomatik", false).apply()
                    guncelle()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        kutu.addView(secim)
        otomatik = Switch(this).apply {
            text = "Yeni ekran metinlerini otomatik oku"
            textSize = 17f; minHeight = 64
            setOnCheckedChangeListener { _, acik ->
                if (!guncelleniyor) ayar.edit().putBoolean("otomatik", acik).apply()
            }
        }
        kutu.addView(otomatik)
        kutu.addView(ekranYazisi("Otomatik okuma yalnızca seçili uygulama öndeyken çalışır. İlk açılışta eski ekranı okumaz, sonraki yeni metinleri bekler. Termux’ta mesajlar ve komut çıktıları ayrılamayabilir; yazı girişindeki taslaklar okunmaz. Durdurmak için bu anahtarı kapatın. Ekran kilitlenince ve görüşmede ses durur."))
        kutu.addView(ekranDugmesi("Otomatik okumayı durdur") {
            ayar.edit().putBoolean("otomatik", false).apply(); guncelle()
        })
        kutu.addView(ekranDugmesi("Geri dön") { finish() })
    }
    override fun onResume() { super.onResume(); guncelle() }
    private fun guncelle() {
        if (!::otomatik.isInitialized) return
        val yonetici = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val hedef = ComponentName(this, EkranOkuyucuServisi::class.java)
        val izin = yonetici.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
            ComponentName(it.resolveInfo.serviceInfo.packageName, it.resolveInfo.serviceInfo.name) == hedef
        }
        guncelleniyor = true
        otomatik.isEnabled = izin
        otomatik.isChecked = izin && ayar.getBoolean("otomatik", false)
        guncelleniyor = false
        durum.text = if (izin) "Android ekran erişimi açık. Otomatik okuma ayrı anahtarla açılır."
            else "Android ekran erişimi kapalı. Ayarlarda Sezai ekran yardımcısı hizmetini açın."
    }
}

internal fun Activity.ekranKutusu(): LinearLayout {
    val olcek = resources.displayMetrics.density
    val kutu = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding((20 * olcek).toInt(), (20 * olcek).toInt(), (20 * olcek).toInt(), (32 * olcek).toInt())
    }
    setContentView(ScrollView(this).apply {
        setBackgroundColor(Color.parseColor("#F3F7F8")); addView(kutu)
    })
    return kutu
}
internal fun Activity.ekranYazisi(yazi: String, boyut: Float = 16f) = TextView(this).apply {
    text = yazi; textSize = boyut; setTextColor(Color.parseColor("#172E3D"))
    setPadding(0, 14, 0, 14)
}
internal fun Activity.ekranDugmesi(yazi: String, islem: () -> Unit) = Button(this).apply {
    text = yazi; isAllCaps = false; textSize = 17f
    minHeight = (52 * resources.displayMetrics.density).toInt()
    setOnClickListener { islem() }
}
