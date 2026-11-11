package com.sezai.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// Telefon yeniden başlatıldığında veya uygulama güncellendiğinde arka plan servisi otomatik başlar.
// Yalnız kullanıcı daha önce "arka planda dinle" açmışsa başlatılır; tercih kaydedilmemiş veya
// açık kapatılmışsa servis başlatılmaz.
class BaslangicAlici : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Intent.ACTION_BOOT_COMPLETED && i.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val tercih = c.getSharedPreferences("ses_ayari", Context.MODE_PRIVATE)
        val arkaAcik = tercih.getBoolean("arka_plan", false)
        val kullKapatti = tercih.getBoolean("arka_plan_kullanici_kapatti", false)
        Log.i("Sezai", "BASLANGIC eylem=${i.action} arkaAcik=$arkaAcik kullKapatti=$kullKapatti")
        if (arkaAcik && !kullKapatti && !BildirimKumanda.sessizMi(c)) {
            SezaiArkaPlanServisi.baslat(c)
        }
    }
}
