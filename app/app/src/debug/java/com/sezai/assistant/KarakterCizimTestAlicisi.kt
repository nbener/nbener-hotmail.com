package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.json.JSONArray
import org.json.JSONObject

// Cihaz Canvas'ında durağan çizim testi; ekran kapalıyken de çalışır. Ses/API yok.
class KarakterCizimTestAlicisi : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rapor = mutableListOf<String>()
        val resim = Bitmap.createBitmap(900, 1350, Bitmap.Config.ARGB_8888)
        val c = Canvas(resim)
        val boya = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(Color.rgb(243,247,248))
        val isimler = listOf("Beklemede", "Dinliyor", "Düşünüyor", "Konuşuyor", "Mutlu", "Şefkat / gece")
        try {
            for (i in 0..5) {
                val x = (i%2)*450f; val y = (i/2)*450f
                if (i==5) { boya.color = Color.rgb(29,48,43); c.drawRect(x,y,x+450,y+450,boya) }
                boya.color = if (i==5) Color.WHITE else Color.rgb(23,46,61)
                boya.textSize = 27f
                c.drawText(isimler[i], x+30, y+40, boya)
                val v = SezaiKarakterView(context).apply { geceOnizleme = i==5; hareketiAzalt = true }
                when(i) {
                    1 -> v.durumAyarla(SezaiKarakterView.Durum.DINLIYOR)
                    2 -> v.durumAyarla(SezaiKarakterView.Durum.DUSUNUYOR)
                    3 -> v.konusmaBasladi("Merhaba, ben Sezai.")
                    4 -> v.durumAyarla(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.MUTLU)
                    5 -> v.durumAyarla(SezaiKarakterView.Durum.BEKLEME, SezaiKarakterView.Duygu.SEFKATLI)
                }
                v.layout(0,0,400,400)
                c.save(); c.translate(x+25,y+48); v.draw(c); c.restore()
                // Bağlanmamış veya azaltılmış hareketli görünüm zamanlayıcı kurmamalı.
                v.etkinlestir(true)
                check(!v.kareBekliyor)
                rapor.add("PASS ${isimler[i]} Canvas çizimi ve durağan kare kuyruğu")
            }
            context.openFileOutput("karakter-duragan.png", Context.MODE_PRIVATE).use {
                resim.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (_: Exception) { rapor.add("FAIL Canvas çizimi") }
        finally { resim.recycle() }
        context.openFileOutput("karakter_cizim_test.json", Context.MODE_PRIVATE).bufferedWriter().use {
            it.write(JSONObject().put("test_id", intent.getStringExtra("test_id")).put("rapor", JSONArray(rapor)).toString())
        }
    }
}
