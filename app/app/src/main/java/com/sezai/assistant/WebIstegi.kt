package com.sezai.assistant

import java.util.Locale

internal object WebIstegi {
    fun istendiMi(metin: String): Boolean {
        val yazi = metin.lowercase(Locale("tr", "TR"))
        if (Regex("\\b(bakma|kullanma|istemiyorum)\\b").containsMatchIn(yazi) || yazi.contains("demedim") ||
            yazi.contains("arama yapma") || yazi.contains("araştırma yapma")) return false
        // "webden/internetten ara/bak" gibi açık istek
        if (Regex("\\b(webden|webde|internetten|internette)\\b[^.!?]{0,100}\\b(bak|bakar|bakabilir|ara|arar|araştır|araştırır|araştırabilir|bul|kontrol et|arama yap)\\b").containsMatchIn(yazi)) return true
        // "ara bakalım", "bir ara bak", "araştırabilir misin" gibi doğal sesli istek
        if (Regex("\\b(ara bakalım|bir ara bak|araştırır mısın|araştırabilir misin|web'den|webden bak|internetten bak|online bak|çevrimiçi ara)\\b").containsMatchIn(yazi)) return true
        return false
    }

    // Gerçek zamanlı/yerel bilgi gerektiren sorgular: otomatik arama tetikler
    fun gercekZamanliMi(metin: String): Boolean {
        val yazi = metin.lowercase(Locale("tr", "TR"))
        return Regex("\\b(nöbetçi|nobetci|eczane|hastane|benzin|istasyon|trafik|hava durumu|hava nasıl|kaç derece|sıcaklık)\\b").containsMatchIn(yazi) ||
            Regex("\\ben yakın\\b").containsMatchIn(yazi) ||
            Regex("\\b(güncel|son|bugünkü|bugün|bu hafta|bu ay|şu an|şimdiki|canlı)\\b.*(haber|gelişme|döviz|kur|borsa|fiyat|altın)").containsMatchIn(yazi) ||
            Regex("\\b(haber|manşet)\\b").containsMatchIn(yazi) ||
            Regex("\\b(maç sonucu|maç skoru|puan tablosu|lig tablosu|dolar|euro|sterlin|altın fiyat)\\b").containsMatchIn(yazi)
    }
}
