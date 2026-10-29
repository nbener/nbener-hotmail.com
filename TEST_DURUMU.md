# Test durumu

Her çalışma başlangıcında CLAUDE.md, DURUM.md ve KALAN_ISLER.md ile birlikte okunur. Sonuç yalnızca yeni çalışmanın kimliği ve beklenen test sayısı doğrulanınca başarılı sayılır. Sahte ağ testi canlı model testi yerine geçmez.

| Alan | Son doğrulanan sonuç | Kapsam / sınır |
| --- | --- | --- |
| Sohbet motoru | 29 Ekim 2026: 15/15 PASS | Gerçek cihaz + Keystore + sahte ağ. Dört sağlayıcıda Kayseri → konumu → nüfusu istek geçmişi; iptal ve sıfırlama. Canlı model anlamlandırması denenmedi. |
| Dinleme olayları | 29 Ekim 2026: 6/6 PASS | Sahte tanıma oturumu; eski/çift olay, izin hatası, sesli bitirme. Gerçek mikrofon kullanılmadı. |
| Komut yorumlama | 29 Ekim 2026: 20/20 PASS | Yalnızca yorumlama; alarm/arama çalıştırılmaz. |
| APK | Bağlam düzeltmesi APK'sı: derleme, zipalign ve apksigner PASS; kurulum Success | Güncel kaynakta sonraki değişiklikler varsa yeniden derlenmesi gerekir. |
| Yazılı ekran | Mesaj alanı ve Gönder cihazda görüldü | İlk hint-metin sorgusu hatalıydı; EditText sınıfı ve düğme üzerinden düzeltildi. Gerçek yazışma denenmedi. |
| Görüşme önceliği | 29 Ekim 2026: 8/8 PASS | Sahte telefon/VoIP/odak olayları; gerçek cihazda mod dinleyicisi kaydı. Gerçek telefon/WhatsApp çağrısı denenmedi. |
| Modern görünüm | HENÜZ YAPILMADI | Native Android görünümü yenilenecek. CSS motoru yok. |

## Doğrulama komutları

- Derleme: `proot-distro login ubuntu -- bash -c 'cd /root/sezai && ./build.sh'`
- İmzalı APK: proot içinde `zipalign -c 4 app/app/build/outputs/apk/debug/app-debug.apk` ve `apksigner verify app/app/build/outputs/apk/debug/app-debug.apk`
- `python3 test/sohbet_test.py`
- `python3 test/dinleme_test.py`
- Görüşme test betiği hazırlanıyor: `python3 test/gorusme_test.py`
- Yorumlayıcı değişirse: `python3 test/cihaz_yorumla_test.py`

Gerçek telefon/WhatsApp kabul testi, kullanıcıyla uygun zamanda yapılmalı. Test için kendiliğinden arama, alarm veya ses başlatılmaz. Kişisel anahtarlar ve konuşmalar rapora yazılmaz.
