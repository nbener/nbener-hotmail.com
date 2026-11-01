package com.sezai.assistant

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

// Tek Canvas; dosya/görsel/kütüphane yok. Görünmeyen karakter için kare planlanmaz.
class SezaiKarakterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    enum class Durum { BEKLEME, DINLIYOR, DUSUNUYOR, KONUSUYOR }
    enum class Duygu { DOGAL, MUTLU, SEFKATLI }
    var durum: Durum = Durum.BEKLEME
        private set
    var duygu: Duygu = Duygu.DOGAL
        private set
    var sadeMod = false
        set(deger) { field = deger; kosullariGuncelle() }
    internal var hareketiAzalt = false
        set(deger) { field = deger; kosullariGuncelle() }
    internal var geceOnizleme: Boolean? = null
        set(deger) { field = deger; invalidate() }
    internal var cizimGozlemcisi: (() -> Unit)? = null
    internal val kareBekliyor get() = karePlanli
    private val boya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yol = Path()
    private val alan = RectF()
    private val gorunen = Rect()
    private val kirli = Rect()
    private val anaIs = Handler(Looper.getMainLooper())
    private val taraflar = intArrayOf(-1, 1)
    private var sistemHareketiAcik = true
    private var mutlulukBitisi = 0L
    private var etkin = false
    private var bagli = false
    private var pencereGorunur = true
    private var ekranAcik = true
    private var karePlanli = false
    private var gecisBaslangici = 0L
    private val onceki = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f)
    private val hedef = onceki.clone()
    private val ifade = onceki.clone()
    private var sonrakiKirpma = 0L
    private var kirpmaBaslangici = -1000L
    private var sonrakiBakis = 0L
    private var bakisX = 0f
    private var bakisY = 0f
    private var bakisHedefX = 0f
    private var bakisHedefY = 0f
    private var sonKare = 0L
    private var konusmaBaslangici = 0L
    private var ritim = floatArrayOf(0f)
    private var yuvarlakSes = booleanArrayOf(false)
    private val kare = Runnable {
        karePlanli = false
        if (hareketOlabilirMi()) { invalidate(kirli); karePlanla() }
    }
    private val kaydirmaDinleyici = android.view.ViewTreeObserver.OnScrollChangedListener { kosullariGuncelle() }
    private val ayarDinleyici = object : ContentObserver(anaIs) {
        override fun onChange(selfChange: Boolean) { sistemAyariniOku(); kosullariGuncelle() }
    }
    private val ekranDinleyici = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            ekranAcik = intent?.action != Intent.ACTION_SCREEN_OFF
            kosullariGuncelle()
        }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        if (Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
    }
    fun etkinlestir(acik: Boolean) {
        etkin = acik
        if (acik) { sistemAyariniOku(); ekranAcik = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive }
        kosullariGuncelle()
    }
    private fun sistemAyariniOku() {
        sistemHareketiAcik = ValueAnimator.areAnimatorsEnabled() && Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    internal fun ekranDurumu(acik: Boolean) { ekranAcik = acik; kosullariGuncelle() }
    fun durumAyarla(yeni: Durum, his: Duygu = Duygu.DOGAL) {
        if (durum == yeni && duygu == his) return
        val simdi = SystemClock.uptimeMillis()
        ifadeyiGuncelle(simdi, hareketOlabilirMi())
        ifade.copyInto(onceki)
        hedef.fill(0f); hedef[yeni.ordinal] = 1f
        hedef[4] = if (his == Duygu.MUTLU) 1f else 0f
        hedef[5] = if (his == Duygu.SEFKATLI) 1f else 0f
        durum = yeni; duygu = his; gecisBaslangici = simdi
        mutlulukBitisi = if (his == Duygu.MUTLU) simdi + 2400 else 0
        contentDescription = "Sezai: ${yeni.name.lowercase()}"
        kosullariGuncelle()
    }
    fun konusmaBasladi(metin: String) {
        // TTS yüksekliği okunmaz; harfler ve noktalama yaklaşık konuşma ritmini verir.
        val harfler = metin.take(4000).ifEmpty { " " }
        ritim = FloatArray(harfler.length) { i -> when (harfler[i].lowercaseChar()) {
            'a', 'e' -> .9f
            'o', 'ö', 'u', 'ü' -> .72f
            'i', 'ı' -> .44f
            'm', 'b', 'p', ' ', '.', ',', '!', '?', ':', ';' -> .04f
            else -> .36f
        } }
        yuvarlakSes = BooleanArray(harfler.length) { harfler[it].lowercaseChar() in "oöuü" }
        konusmaBaslangici = SystemClock.uptimeMillis()
        durumAyarla(Durum.KONUSUYOR, duygu)
    }
    fun konusmaBitti() { ritim = floatArrayOf(0f); yuvarlakSes = booleanArrayOf(false); durumAyarla(Durum.BEKLEME, duygu) }

    private fun hareketOlabilirMi(): Boolean = etkin && bagli && pencereGorunur && ekranAcik && isShown &&
        hasWindowFocus() && getLocalVisibleRect(gorunen) && !sadeMod && !hareketiAzalt &&
        sistemHareketiAcik

    private fun kosullariGuncelle() {
        removeCallbacks(kare); karePlanli = false
        if (!bagli) return
        if (!hareketOlabilirMi()) {
            hedef.copyInto(ifade); sonKare = 0L
        } else {
            val simdi = SystemClock.uptimeMillis()
            if (sonKare == 0L) { sonrakiKirpma = simdi + Random.nextLong(3000, 6001); sonrakiBakis = simdi + 2000; kirpmaBaslangici = -1000 }
            karePlanla()
        }
        invalidate(kirli)
    }
    private fun karePlanla() {
        if (karePlanli || !hareketOlabilirMi()) return
        karePlanli = true
        // Beklemede 15, diğer durumlarda en çok 24 kare/saniye.
        postDelayed(kare, if (durum == Durum.BEKLEME) 67 else 42)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow(); bagli = true; sistemAyariniOku()
        ekranAcik = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        val filtre = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(ekranDinleyici, filtre, Context.RECEIVER_NOT_EXPORTED)
        else context.registerReceiver(ekranDinleyici, filtre)
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, ayarDinleyici)
        viewTreeObserver.addOnScrollChangedListener(kaydirmaDinleyici)
        kosullariGuncelle()
    }
    override fun onDetachedFromWindow() {
        bagli = false; removeCallbacks(kare); karePlanli = false
        context.unregisterReceiver(ekranDinleyici)
        context.contentResolver.unregisterContentObserver(ayarDinleyici)
        if (viewTreeObserver.isAlive) viewTreeObserver.removeOnScrollChangedListener(kaydirmaDinleyici)
        super.onDetachedFromWindow()
    }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility); pencereGorunur = visibility == VISIBLE
        kosullariGuncelle()
    }
    override fun onVisibilityAggregated(isVisible: Boolean) { super.onVisibilityAggregated(isVisible); kosullariGuncelle() }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) { super.onWindowFocusChanged(hasWindowFocus); kosullariGuncelle() }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); kosullariGuncelle() }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { super.onSizeChanged(w, h, oldw, oldh); kirli.set(0, 0, w, h) }
    private fun ifadeyiGuncelle(simdi: Long, hareket: Boolean) {
        val t = if (hareket) ((simdi - gecisBaslangici) / 300f).coerceIn(0f, 1f) else 1f
        val yumusak = t * t * (3 - 2 * t)
        for (i in ifade.indices) ifade[i] = onceki[i] + (hedef[i] - onceki[i]) * yumusak
    }
    private fun boyayiAyarla(deger: Int, alfa: Int = 255) { boya.color = deger; boya.alpha = alfa; boya.style = Paint.Style.FILL }
    private fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, renk: Int, alfa: Int = 255) {
        boyayiAyarla(renk, alfa); alan.set(x-rx, y-ry, x+rx, y+ry); c.drawOval(alan, boya)
    }
    private fun cizgi(c: Canvas, renk: Int, kalinlik: Float) {
        boyayiAyarla(renk); boya.style = Paint.Style.STROKE; boya.strokeWidth = kalinlik; boya.strokeCap = Paint.Cap.ROUND
        c.drawPath(yol, boya); boya.style = Paint.Style.FILL
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c); cizimGozlemcisi?.invoke()
        val simdi = SystemClock.uptimeMillis()
        val hareket = hareketOlabilirMi()
        if (hareket && mutlulukBitisi > 0 && simdi >= mutlulukBitisi) durumAyarla(durum, Duygu.DOGAL)
        ifadeyiGuncelle(simdi, hareket)
        val dt = if (sonKare == 0L) 0f else minOf((simdi-sonKare)/1000f, .15f)
        if (hareket) {
            if (simdi >= sonrakiKirpma) { kirpmaBaslangici = simdi; sonrakiKirpma = simdi + Random.nextLong(3000, 6001) }
            if (simdi >= sonrakiBakis) { bakisHedefX = Random.nextFloat()*8-4; bakisHedefY = Random.nextFloat()*4-2; sonrakiBakis = simdi + Random.nextLong(2600, 5201) }
            bakisX += (bakisHedefX-bakisX)*minOf(1f, dt*1.3f); bakisY += (bakisHedefY-bakisY)*minOf(1f, dt*1.3f)
            sonKare = simdi
        }
        // Kullanıcı isteği: cihaz gece modundayken de gündüz renkleri kullanılır (gece paleti yalnız önizleme için).
        val gece = geceOnizleme ?: false
        val koyu = Color.rgb(51, 65, 51)
        val yesil = if (gece) Color.rgb(111, 169, 142) else Color.rgb(80, 139, 111)
        val acikYesil = if (gece) Color.rgb(156, 195, 169) else Color.rgb(157, 191, 160)
        val seftali = Color.rgb(247, 211, 169)
        val yanak = Color.rgb(224, 139, 111)
        val kehribar = Color.rgb(224, 170, 89)
        val dinle = ifade[1]; val dusun = ifade[2]; val konus = ifade[3]; val mutlu = ifade[4]; val sefkat = ifade[5]
        val t = if (hareket) simdi/1000.0 else 0.0
        val nefes = if (hareket) sin(t*1.1).toFloat() else 0f
        val yuzme = if (hareket) sin(t*.8).toFloat()*2.1f else 0f
        val nabiz = if (hareket) (.5+.5*sin(t*1.8)).toFloat() else .5f
        val olcek = minOf(width/256f, height/256f)
        c.save(); c.translate((width-256*olcek)/2, (height-256*olcek)/2); c.scale(olcek, olcek)
        // Aura ve yere yakın gölge, yüzün arkasında kalır.
        oval(c, 128f, 119f, 98f+dusun*nabiz*5, 96f+dusun*nabiz*5, if (gece) 0xff35584b.toInt() else 0xffe2eee3.toInt(), 200)
        oval(c, 128f, 232f, 51f-nefes, 6f, yesil, if (gece) 65 else 35)
        oval(c, 191f, 54f, 7f+dusun*nabiz*2, 7f+dusun*nabiz*2, kehribar, 185)
        c.save(); c.translate(0f, yuzme)
        c.scale(1f+nefes*.006f, 1f+nefes*.009f, 128f, 150f)
        // Yumuşak gövde, küçük kollar ve boyna dolanan adaçayı renkli yaka.
        oval(c, 84f, 188f, 16f, 23f, yesil)
        oval(c, 172f, 188f, 16f, 23f, yesil)
        oval(c, 128f, 187f, 48f, 42f, yesil)
        oval(c, 128f, 174f, 31f, 26f, acikYesil)
        oval(c, 110f, 222f, 18f, 8f, yesil); oval(c, 147f, 222f, 18f, 8f, yesil)
        c.save(); c.rotate(-5*dinle+2*sefkat, 128f, 131f)
        // Yüz, hafif alt gölge ve sıcak tek renkli dış hat.
        oval(c, 128f, 119f, 72f, 73f, 0xffd5ab7e.toInt())
        oval(c, 128f, 115f, 71f, 72f, seftali)
        oval(c, 111f, 66f, 31f, 12f, 0xffffe4be.toInt(), 135)
        // Göz beyazları, iris, göz bebeği ve iki ayrı ışık yansıması.
        val kirpma = if (hareket && simdi-kirpmaBaslangici in 0L..210L) sin((simdi-kirpmaBaslangici)/210.0*PI).toFloat() else 0f
        val gozAcik = (1f-kirpma*.96f)*(1f+dinle*.12f-sefkat*.12f)
        val gx = bakisX*ifade[0]+5*dusun
        val gy = bakisY*ifade[0]-6*dusun
        for (taraf in taraflar) {
            val x = 128f+taraf*28
            c.save(); c.scale(1f, gozAcik, x, 111f)
            oval(c, x, 111f, 17.5f, 20f, 0xfffffbef.toInt())
            oval(c, x+gx, 113f+gy, 10f, 13f, 0xff6d8255.toInt())
            oval(c, x+gx, 114f+gy, 6f, 9f, koyu)
            oval(c, x+gx-3.2f, 108.5f+gy, 3.4f, 4.1f, Color.WHITE)
            oval(c, x+gx+3f, 117f+gy, 1.4f, 1.7f, 0xffffedc4.toInt())
            c.restore()
            // Kaş katmanı: şefkatte iç köşe, düşünmede yalnız bir kaş yükselir.
            val vurgu = if (hareket) sin(t*5+taraf).toFloat()*konus*1.3f else 0f
            val yuksel = -dinle*5 - (if (taraf==1) dusun*5 else 0f) + vurgu
            yol.reset(); yol.moveTo(x-13, 83+yuksel-sefkat*(if (taraf==1) 3 else 0))
            yol.quadTo(x, 77+yuksel-sefkat*4, x+13, 83+yuksel-sefkat*(if (taraf==-1) 3 else 0))
            cizgi(c, 0xff7c684e.toInt(), 4f)
            oval(c, 128f+taraf*46, 137f, 13f+mutlu*2, 6f, yanak, (80+konus*40+mutlu*45).toInt())
        }
        // Ağız: kapalı gülümseme, yarı açık, açık ve yuvarlak ünlü şekli.
        val ritimSirasi = if (hareket) (((simdi-konusmaBaslangici)/95).coerceAtLeast(0)%ritim.size).toInt() else 0
        val agiz = if (hareket) ritim[ritimSirasi]*konus else konus*.22f
        val o = hareket && yuvarlakSes[ritimSirasi] && konus>.3f
        val genislik = (16f+mutlu*4-dusun*6-sefkat*4)*(if (o) .58f else 1f)
        if (agiz>.16f) {
            oval(c, 128f, 153f, genislik, 3+agiz*13, 0xff84513f.toInt())
            oval(c, 128f, 157f+agiz*4, genislik*.56f, agiz*4, 0xffe8a083.toInt())
        } else {
            yol.reset(); yol.moveTo(128-genislik, 150f)
            yol.quadTo(128f, 161f+mutlu*3-dusun*5-sefkat*4, 128+genislik, 150f)
            cizgi(c, 0xff84513f.toInt(), 3.4f)
        }
        c.restore(); c.restore(); c.restore()
    }
}
