package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Yalnızca debug sürümünde bulunur; sürekli servis veya dosya taraması çalıştırmaz.
class GelistirmeDurumAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mesaj = intent.getStringExtra("mesaj")?.trim()?.take(300) ?: return
        if (mesaj.isEmpty()) return
        context.getSharedPreferences("gelistirme_durumu", Context.MODE_PRIVATE).edit()
            .putString("mesaj", mesaj)
            .putLong("zaman", System.currentTimeMillis())
            .apply()
    }
}
