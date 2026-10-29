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
| Modern görünüm | 29 Ekim: kaydırma sonrası tam düğme görünürlüğü ve hiza doğrulandı | Görsel incelendi; uygulama düzeni değişmedi. Aktif kullanım korumasındaki eksik düzeltildi; son koruma cihazda henüz denenmedi. |

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

## 29 Ekim — HTTP 400 kök nedeni bulundu
- Yalnızca sabit kamu sorgusunun debug tanısı eklendi; gerçek anahtar maskeleniyor. Maskeleme cihazda sahte yanıtla doğrulandı, sohbet 21/21 PASS. Bu tanı normal sohbet tarafından bağlanmaz.
- Sunucu açıklaması: `model openai/gpt-oss-120b does not support citations` (type=invalid_request_error). Ret nedeni açık citation_options=enabled alanı. Önceki hesap/model erişimi ihtimali bu hata için kanıtlanmadı.
- citation_options alanı kaldırıldı; Browser Search belgesindeki örnek gibi tools + tool_choice korunuyor. Talimat kaynak HTTPS adreslerini istemeye devam ediyor. Düzeltme derleniyor; canlı başarı henüz doğrulanmadı.

## Son geçerli web sonucu — 29 Ekim 2026
- Kök neden sunucudan doğrulandı: `model openai/gpt-oss-120b does not support citations`. Açık citation_options alanı kaldırıldı. Önceki HTTP 400 kayıtları bu düzeltmeden önceki sürümlerdir.
- Düzeltmeli APK derleme, zipalign, apksigner PASS; pm install -r Success. Aynı APK üzerinde sohbet 21/21, dinleme 6/6, görüşme 8/8 PASS.
- Canlı web testi YANIT_ALINDI, çıkış kodu 0. Tek sabit kamu sorgusunda üç desteklenen model kimliği ve resmî Browser Search adresi geldi. Sayfa ayrıca açılıp içerikle eşleştirildi. Ücretsiz erişim/maliyet ve bütün web soruları için genel başarı iddiası yok. Gerçek mikrofon/seslendirme kullanılmadı.

## Ekran kontrolü — 29 Ekim devam
- Önceki test, düğmelerin ekran sınırında kesilmiş olmasını ayıramıyordu. Sınır boşluğu kontrolü eklendi. Cihazda tek kaydırma sonrası tam görünürlük/hiza doğrulandı; görsel incelendi.
- Görselde Dinliyorum... bulundu: testin boş taslak koruması aktif kullanımı engellememişti. Test sesi/mikrofonu başlatmadı fakat kaydırma yaptı. Bu eksik de hata olarak kaydedildi; aktif kullanımda otomatik etkileşim durduruldu.
- Kaydırma öncesine boş başlangıç ekranı kontrolü eklendi. Son koruma py_compile + diff --check PASS; cihazda yeniden çalıştırılmadı. Önceki cihaz başarısı son korumanın kabul testi sayılmaz.
- Uygulama kaynağı değişmedi; son imzalı/kurulu APK değişmedi.
