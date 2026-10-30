package com.sezai.assistant

// Yalnız debug ve yapay sinyal; normal uygulama bu nesneyi yüklemez.
internal object YankiYerel {
    init { System.loadLibrary("sezai_aec") }
    external fun yapayOlc(): DoubleArray
    external fun olustur(hz: Int): Long
    external fun isle(kimlik: Long, mikrofon: ShortArray, referans: ShortArray, cikis: ShortArray)
    external fun birak(kimlik: Long)
}

// İşleme ile kapatma yarışmasın; serbest bırakılmış native adrese erişilmez.
internal class YankiOturumu(hz: Int) : AutoCloseable {
    private var kimlik = YankiYerel.olustur(hz)
    @Synchronized fun isle(mikrofon: ShortArray, referans: ShortArray, cikis: ShortArray) {
        check(kimlik != 0L) { "AEC kapalı" }
        YankiYerel.isle(kimlik, mikrofon, referans, cikis)
    }
    @Synchronized override fun close() {
        if (kimlik != 0L) { YankiYerel.birak(kimlik); kimlik = 0 }
    }
}
