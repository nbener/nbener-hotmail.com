# Test durumu

Her çalışma başlangıcında CLAUDE.md, DURUM.md ve KALAN_ISLER.md ile birlikte okunur. Sonuç yalnızca yeni çalışmanın kimliği ve beklenen test sayısı doğrulanınca başarılı sayılır. Sahte ağ testi canlı model testi yerine geçmez.

| Alan | Son doğrulanan sonuç | Kapsam / sınır |
| --- | --- | --- |
| Sohbet motoru | 29 Ekim 2026: 21/21 PASS | Gerçek cihaz + Keystore + sahte ağ. Dört sağlayıcıda Kayseri → konumu → nüfusu istek geçmişi; iptal/sıfırlama; açık web isteği, Groq aracı, sağlayıcı sınırı ve büyük yanıt kontrolü. Canlı model anlamlandırması denenmedi. |
| Dinleme olayları | 29 Ekim 2026: 6/6 PASS | Sahte tanıma oturumu; eski/çift olay, izin hatası, sesli bitirme. Gerçek mikrofon kullanılmadı. |
| Komut yorumlama | 29 Ekim 2026: 20/20 PASS | Yalnızca yorumlama; alarm/arama çalıştırılmaz. |
| APK | Web bağlantılı son APK: derleme, zipalign ve apksigner PASS; kurulum Success | Güncel kaynakta sonraki değişiklikler varsa yeniden derlenmesi gerekir. |
| Yazılı ekran | Mesaj alanı ve Gönder cihazda görüldü | İlk hint-metin sorgusu hatalıydı; EditText sınıfı ve düğme üzerinden düzeltildi. Gerçek yazışma denenmedi. |
| Görüşme önceliği | 29 Ekim 2026: 8/8 PASS | Sahte telefon/VoIP/odak olayları; gerçek cihazda mod dinleyicisi kaydı. Gerçek telefon/WhatsApp çağrısı denenmedi. |
| Modern görünüm | Son APK derlendi/imza doğrulandı/kuruldu; son hizalama kontrolü TAMAMLANMADI | İlk sürüm görsel olarak kontrol edildi. Son kontrolde aranan düğme UI ağacında bulunamadı (StopIteration); sebep doğrulanmadı. Son hizalama geçti sayılmaz. |

## Doğrulama komutları

- Derleme: `proot-distro login ubuntu -- bash -c 'cd /root/sezai && ./build.sh'`
- İmzalı APK: proot içinde `zipalign -c 4 app/app/build/outputs/apk/debug/app-debug.apk` ve `apksigner verify app/app/build/outputs/apk/debug/app-debug.apk`
- `python3 test/sohbet_test.py`
- `python3 test/dinleme_test.py`
- Görüşme kesinti testleri: `python3 test/gorusme_test.py`
- Yorumlayıcı değişirse: `python3 test/cihaz_yorumla_test.py`

Gerçek telefon/WhatsApp kabul testi, kullanıcıyla uygun zamanda yapılmalı. Test için kendiliğinden arama, alarm veya ses başlatılmaz. Kişisel anahtarlar ve konuşmalar rapora yazılmaz.

## Web bağlantısı geliştirmesi

- Genişletilmiş sohbet paketi cihazda 21/21 PASS. Aynı APK üzerinde görüşme 8/8 ve dinleme 6/6 PASS. Bunlar sahte ağ/ses olaylarıyla çalıştı.
- Groq browser_search istek biçimi resmî dokümanla karşılaştırıldı; canlı arama/hesap erişimi henüz denenmedi.
- `python3 test/web_canli_test.py` gerçek API/arama kotası kullanır; sabit bir kamu bilgisi sorgusunu ayrı debug Activity içinde gönderir. Anahtar ve kullanıcının sohbet geçmişi betiğe/rapora çıkmaz. Yanıt alındı statüsü, kaynağın doğru olduğuna otomatik onay değildir.
- Son APK derlendi, zipalign/apksigner geçti ve telefona kuruldu. Gerçek web denemesi sürüyor; sonucu ayrıca kaydedilecek.

- İlk canlı web denemesi: IPTAL (test Activity onStop). Arama başarı/başarısızlığı doğrulanmadı. Test düzeneği düzeltildi; sonraki deneme bu denemenin devamı veya otomatik tekrarı sayılmayacak, ayrı gerçek sorgu olacaktır.

## 29 Ekim canlı web hata tanısı
- onStop düzeltmesi ve güvenli HTTP tanısı içeren APK derlendi, zipalign/apksigner geçti, kurulum Success. Sohbet 21/21 PASS; sunucu hata metninin özel içeriği gösterilmedi.
- İki ayrı canlı deneme hizmet reddi döndürdü; sonuncusu HTTP 400, ilgili alan model. Gerçek web araması BAŞARILI DEĞİL. Bu alan kesin ret nedenini tek başına kanıtlamaz.
- Standart hata kodu tanısı sonradan eklendi; bu son değişikliğin derleme/kurulum/testi sürüyor.

- Son standart kod tanısı dahil APK: derleme/imza/hizalama PASS, kurulum Success. Sohbet 21/21, dinleme 6/6, görüşme 8/8 PASS. Son canlı web yine HTTP 400; bilinen kod listesine uyan hizmet kodu alınmadı.
- Ekran kontrolü PASS: alanlar ve eşit düğme sınırları. Görsel incelendi; ayar düğmelerinin altı ekran sınırında kesiliyor, tam görünürlük iddiası yok.
