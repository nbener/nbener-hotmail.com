package com.sezai.assistant

import java.util.Locale

// Yalnızca açık web isteği aracı etkinleştirir; güncel görünen her soru otomatik arama değildir.
internal object WebIstegi {
    fun istendiMi(metin: String): Boolean {
        val yazi = metin.lowercase(Locale("tr", "TR"))
        if (Regex("\\b(bakma|kullanma|istemiyorum)\\b").containsMatchIn(yazi) || yazi.contains("demedim") ||
            yazi.contains("arama yapma") || yazi.contains("araştırma yapma")) return false
        return Regex("\\b(webden|webde|internetten|internette)\\b[^.!?]{0,100}\\b(bak|bakar|bakabilir|ara|arar|araştır|araştırır|araştırabilir|bul|kontrol et|arama yap)\\b").containsMatchIn(yazi)
    }
}
