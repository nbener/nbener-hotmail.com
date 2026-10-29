package com.sezai.assistant

// Yalnız debug ve yapay sinyal; normal uygulama bu nesneyi yüklemez.
internal object YankiYerel {
    init { System.loadLibrary("sezai_aec") }
    external fun yapayOlc(): DoubleArray
}
