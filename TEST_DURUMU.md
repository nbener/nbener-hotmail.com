# Test durumu

Her çalışma başlangıcında CLAUDE.md, DURUM.md ve KALAN_ISLER.md ile birlikte okunur. Sonuç yalnızca yeni çalışmanın kimliği ve beklenen test sayısı doğrulanınca başarılı sayılır. Sahte ağ testi canlı model testi yerine geçmez.

| Alan | Son doğrulanan sonuç | Kapsam / sınır |
| --- | --- | --- |
| Yeni karakter | 29 Ekim: 6/6 durağan + 6/6 canlı View kontrolü PASS; ana ekran ve gerçek TTS ağız hareketi görüldü | Hareket azaltma/ekran/lifecycle sinyalleri debug koşullarıyla test edildi. Fiziksel kilit, gerçek pil ve doğal mikrofon sohbeti hâlâ ayrı kabul işi. |
| Sohbet motoru | 29 Ekim 2026: 21/21 PASS | Gerçek cihaz + Keystore + sahte ağ. Dört sağlayıcıda Kayseri → konumu → nüfusu istek geçmişi; iptal/sıfırlama; açık web isteği, Groq aracı, sağlayıcı sınırı ve büyük yanıt kontrolü. Canlı model anlamlandırması denenmedi. |
| Dinleme olayları | 29 Ekim 2026: 6/6 PASS | Sahte tanıma oturumu; eski/çift olay, izin hatası, sesli bitirme. Gerçek mikrofon kullanılmadı. |
| Komut yorumlama | 29 Ekim 2026: 20/20 PASS | Yalnızca yorumlama; alarm/arama çalıştırılmaz. |
| APK | Web bağlantılı son APK: derleme, zipalign ve apksigner PASS; kurulum Success | Güncel kaynakta sonraki değişiklikler varsa yeniden derlenmesi gerekir. |
| Yazılı ekran | Mesaj alanı ve Gönder cihazda görüldü | İlk hint-metin sorgusu hatalıydı; EditText sınıfı ve düğme üzerinden düzeltildi. Gerçek yazışma denenmedi. |
| Görüşme önceliği | 29 Ekim 2026: 8/8 PASS | Sahte telefon/VoIP/odak olayları; gerçek cihazda mod dinleyicisi kaydı. Gerçek telefon/WhatsApp çağrısı denenmedi. |
| Ekran yardımcısı | 29 Ekim: 9/9 sessiz test PASS, ayar UI PASS | Gerçek hizmet izni kapalı. Ekran/ses/API kabul testi henüz yapılmadı. |
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

## 29 Ekim — ekran erişimi geliştirmesi sürüyor
- Kullanıcı ekran erişimi, istekle herhangi bir açık ekranı okuma/yorumlama ve telefon genelinde eller serbest sesli yardım hedefini onayladı; CLAUDE.md'ye kaydedildi.
- EkranOkuyucuServisi, ekran ayarları/inceleme ekranı ve EkranMetni/EkranAkisi yazıldı. Otomatik okuma ayrı anahtarla seçilen uygulamada; açık istekle erişilebilirlik düğmesinden herhangi bir ekran metni incelemesi. Metinler bellekte, yapay zekâ aktarımı yalnızca Yorumla ve oku seçimiyle. Parola/taslak atlanır. Genel sesli uyandırma/uzaktan sesle başlatma henüz yok.
- İlk ./build.sh derlemesi Kotlin'de yorumla düğmesi/metodu ad çakışması ve ekleme sırasında eksik görülen otomatikHedefMi ile başarısız oldu. Düğme yeniden adlandırıldı; yardımcı tanımı kaynakta mevcut. Yeniden derleme/test gerekiyor. Kaynak yedeği: yedek/ekran-okuma-oncesi-20261029-112050.tar.gz.

## Ekran yardımcısı yeni APK — 29 Ekim 2026
- İlk derleme başarısız (yorumla ad çakışması, düzenleme esnasında eksik yardımcı tanımı); kaynak düzeltildi. Son derleme, zipalign, apksigner ve kurulum başarılı.
- Son aynı APK: ekran 9/9, sohbet 21/21, dinleme 6/6, görüşme 8/8 PASS. Toplam 44 sessiz test. Ekran testleri sentetik metinler/politika ve Android hizmet metadatasıdır; gerçek başka uygulama ekranını okuma testi sayılmaz.
- Ayar Activity'si açıldı; izin kapalı, otomatik anahtar kapalı/pasif olduğu UI XML ve görselde doğrulandı. Sistem erişim izni açılmadı.
- Gerçek hizmet bağlanma, erişilebilirlik düğmesinden metin alma, Termux erişilebilir metni, ekran konuşması/yorum ve kilit/görüşme kabul testleri kullanıcı izninden sonra. Ekran fotoğrafı/görsel model ve küresel sesli kumanda kapsamı henüz uygulanmadı.
- Mevcut eski APK ile kullanıcı gösteriminde gerçek yazılı istek/yanıt UI üzerinden doğrulandı ve ardından uygulama kullanıcı isteğiyle kapatıldı. Bu yeni APK'yı gerçek API açısından ayrıca doğrulamaz.

## Yeni karakter — sürmekte
- SezaiKarakterView ve TTS/durum bağlantısı yazılıyor. Yeni kaynak henüz derlenip kurulmadı; görsel durumlar ve hareketin durma koşulları doğrulanmadı. KARAKTER_TASARIMI.md sözleşme kaydıdır, başarı kanıtı değildir.

- Karakterli ilk APK ./build.sh ile derlendi; zipalign ve apksigner verify başarılı, pm install -r Success. İlk `python3 test/karakter_test.py` denemesi FAIL: Test görünürlüğünü kaybetmeden tamamlandı. Görsel siyahtı; `dumpsys power` mWakefulness=Dozing, mHoldingDisplaySuspendBlocker=false gösterdi. Bu bir görsel başarı değildir. Telefonun ekranının açılması/kilidin çözülmesi kullanıcıdan istendi; ekranı zorla açık tutma veya wake lock eklenmedi.

## Son karakter APK doğrulaması
- Son kaynak ./build.sh, zipalign ve apksigner verify PASS; kurulum Success. Aynı APK üzerinde karakter durağan 6/6 + ekran 9/9 + sohbet 21/21 + dinleme 6/6 + görüşme 8/8 = 50 PASS.
- Durağan Android Canvas görselinde bekleme, dinleme, düşünme, konuşma ağzı, mutluluk ve şefkat/gece paleti incelendi. Durağan/bağlanmamış View için kare kuyruğu kurulmadı. Son görsel /sdcard/Download/sezai-karakter-duragan-1793270314030295687.png.
- Canlı `python3 test/karakter_test.py` hâlâ BAŞARILI DEĞİL: ilk tur görünürlüğü kaybetti, `dumpsys power` Dozing gösterdi, siyah ekran görüntüsü alındı. Telefonun açılması kullanıcıdan istendi. Canlı blink, kare hızı, azaltılmış hareket/sade mod, ekran-off/lifecycle duruş testleri henüz geçmedi.
- Gerçek TTS ses yüksekliği ölçülmez; ağız ritmi metne dayanır. Kullanıcının konuşması veya gerçek TTS ile görsel senkron kabulü yapılmadı. Sessiz eski testler bunu doğrulamaz.

## Son geçerli canlı karakter sonucu — 29 Ekim
- Ekran Awake ve sistem animasyon ölçeği 1.0 iken `python3 test/karakter_test.py`: 6/6 PASS. İlk Dozing/FAIL geçmiş kaydıdır; son tur kendi test_id raporuyla doğrulandı.
- Altı durum, bekleme çiziminin 30 fps altında kalması, azaltılmış hareket/sade mod/ekran-kapalı ve yaşam döngüsü sinyallerinde kare kuyruğu duruşu geçti. Bunlar debug bayrak/sinyal testleri; sistem ayarı veya fiziksel ekran kapatılmadı.
- Ana ekran görseli kontrol edildi. Gerçek TTS sabit cümlesi sırasında iki görüntüde ağız açıklığının değiştiği görüldü. Test mikrofon ve gerçek model çağrısı içermez; yaklaşık ritimdir.
- Önceki son APK testleri: durağan karakter 6 + ekran 9 + sohbet 21 + dinleme 6 + görüşme 8 = 50; bu tur canlı karakter 6 ilave kontrol geçti. Bu sayı bütün uçtan uca sesli konuşmanın geçtiği anlamına gelmez.

## 29 Ekim — söz alma son ölçüm ve kurulu sürüm
- “dur” bitirme listesinden çıkarıldı; dur/Sezai çağrısı, devam isteği ve benzer kelimeler ayrılıyor. Konu geçmişi silinmiyor; son isteği esas alma talimatı eklendi. MainActivity karakterinde gecikmiş ses-bitiş olayı yalnız konuşma durumunu sonlandırır.
- İlk yeni APK build/zipalign/imza/kurulum başarılı. Dinleme 9/9, sohbet 21/21, görüşme 8/8, ekran 9/9 PASS (47 sessiz kontrol). Sohbet testleri kontrollü ağ yanıtıdır; canlı Türkçe yönlendirme başarısı değildir.
- Gerçek cihaz yerel tanıma yeteneği false. Gerçek TTS + standart tanıma denemesi SES_ODAGI_KESILDI ile bitti; kullanıcı sözü algılanmadan odak kesildi. Eşzamanlı söz kesme BAŞARISIZ ve üretime bağlanmadı; mevcut üretim TTS sırasında dinlemez.
- Komutlar, benzersiz test kimlikleri, tam sonuçlar ve yerel Vosk deneme önerisi SOZ_KESME_TASARIMI.md içinde. Ham tanınan metin kaydedilmedi. Yeni kütüphane/model indirilmedi. Gerçek yankı ayırma, kesme gecikmesi, doğal takip konuşması ve pil ölçümü açık.

- Son küçük karakter olay korumasından sonra APK yeniden derlendi, zipalign/imza doğrulandı ve pm install -r Success alındı. Aynı SON APK: dinleme 9 + sohbet 21 + görüşme 8 + ekran 9 + durağan karakter 6 = 53/53 PASS. Durağan Canvas testi canlı animasyon veya gerçek söz kesme testi değildir. git diff --check geçti. Motor denemesi için kullanıcıya Vosk 35 MB + Android kütüphanesi önerisi sunuldu; henüz onay gelmedi, bağımlılık eklenmedi.

## 29 Ekim — kullanıcı devam onayı, Vosk sınırlı denemesi
- Kullanıcı `~` gönderdi; önceki Vosk 35 MB Türkçe model + Android kütüphanesi deneme önerisine devam onayı olarak alındı ve kullanıcıya açıklandı. Varsayılan üretim motoru değiştirilmiyor.
- Yedek: yedek/vosk-deneme-oncesi-20261029-142010.tar.gz. Başlangıç boş alan 2,3 GB.
- Resmî vosk-model-small-tr-0.3.zip indirildi; zip testzip temiz. SHA256 8c8d07cec1bce31add14967c16891c84152cdf76d391e33c08278119b9ea96e5 (yerel bütünlük kaydı, yayıncı imza doğrulaması değildir). Açılmış boyut 58.767.995 bayt; eski model düzeninde final.mdl kökte. Gr.fst içinde dur/sezai baytları bulundu; bu doğru tanıma garantisi değildir.
- Vosk 0.3.75 AAR ARM64 libvosk.so içeriyor, minCompileSdk=1; SDK34 değiştirilmedi. debugImplementation Vosk 0.3.75 ve JNA 5.18.1 eklendi; release bağımlılığı yok. Yerel model/indirmeler gitignore ile dışarıda.
- VoskSesTestActivity ve test/vosk_ses_test.py: bilgi modunda mikrofon açmadan model; dinle/birlikte/yanki modlarında en çok 12 saniye AudioRecord16000 mono PCM16, varsa AEC; ekran/odak/görüşme kesintisinde bırakma. Ham ses/metin dosyası veya ağ gönderimi yok. Ana ekran/üretim söz kesme akışına bağlı değil.
- İlk derleme sürüyor. Yeni kaynak için başarı iddiası yok. Sırada model yükleme, eşzamanlılık ve kendi TTS yankısı ölçümü var; gerçek kullanıcının sesini tanıma/istek devamı ve pil kabulü hâlâ açık.

## 29 Ekim — Vosk cihaz denemesi sonucu ve son APK
- İlk debug derleme/imza geçti. Son kaynak kontrolünde yeni yankı test cümlesi APK DEX içinde bulunmadı (derleme sürerken kaynak düzenlenmişti); kurulum bu kontrolde durduruldu: `Son kaynak APKda doğrulanmadı`. Yeniden derlendi; yeni cümle ve eski am/final.mdl yolunun bulunmaması DEX'te doğrulandı. İlk deneme APK'sı kuruldu. Sonra eklenen lisans varlıkları için yeniden build/zipalign/apksigner doğrulandı, son APK 4 third-party varlığıyla kuruldu: pm install -r Success. Model APK içine konmadı; uygulamanın özel files dizininde.
- İlk bilgi denemesi `python3 test/vosk_ses_test.py`, test_id=1793273257506987794: MODEL_HAZIR, model_acilis_ms=957, kaynaklar_birakildi=true. Mikrofon açılmadı.
- İlk gerçek TTS+mikrofon/AEC denemesi `python3 test/vosk_ses_test.py --kip yanki`, test_id=1793273269132740394: tts_basladi=true, mikrofon_basladi=true, aec_mevcut/etkin=true, tanima_olayi_var=true. Ses odağı kesilmedi. 11660 ms ses, 5487 ms çözümleme thread CPU, tepe_genlik=32768, bitiş SURE_DOLDU, kaynaklar_birakildi=true. Kısa örnekte soz_alma_duyuldu gelmedi; bunun doğru yankı ayrımı mı, modelin çağrıyı tanımaması mı olduğu belirlenmedi. TTS tamamlanma bayrağı gelmedi, süre sınırında kesildi. Kendi sesinden yanlış tetiklenme olmadığına genel kanıt sayılmaz. Derleme aynı sırada çalıştığından duvar saati performansı temsili değildir; CPU sayısı yalnız bu denemedir.
- SON lisans varlıkları dahil kurulu APK üzerinde bilgi: test_id=1793273405053999040 MODEL_HAZIR, açılış1271ms, bitiş PSS188458KB, kaynaklar bırakıldı. PSS kapanış sonrası süreç ölçümüdür, tepe bellek veya model boyutu değildir.
- SON APK gerçek arka plan: `python3 test/vosk_ses_test.py --kip dinle --arka-plan`, test_id=1793273410219406642. Home tuşu gönderildi; mikrofon_basladi=true, aec_etkin=true, ses3800ms, çözüm CPU1600ms; GORUNURLUK_KAYBI ve kaynaklar_birakildi=true. Fiziksel ekran kilidi değil, gerçek Activity arka plan testi. Son appops RECORD_AUDIO çıktısında running yok; son süre12s054ms. Kalıcı dinleme servisi/wake lock eklenmedi.
- SON APK sessiz regresyon: dinleme9/9, sohbet21/21, görüşme8/8, ekran9/9 =47/47 PASS. Sohbet yanıtları kontrollü/sahte ağdır; canlı model/gerçek görüşme kabulü değildir. test/vosk_ses_test.py py_compile ve git diff --check geçti. Karakter kaynak kodu bu aşamada değişmedi, animasyon testi yeniden koşulmadı.
- ÖNEMLİ SINIR: Bu yalnız debug motor denemesidir. MainActivity TTS sırasında hâlâ dinlemez. Gerçek insanın dur/Sezai diyerek kesmesi, sonraki isteğin ilk kelimelerinin korunması ve doğal konu yönlendirmesi henüz denenmedi. AEC etkin bayrağı yankı giderme başarısı değildir. CPU maliyeti kayda değer; gerçek pil ölçümü yapılmadı. Varsayılan üretim motoru değiştirilmedi.
- Kullanıcıya gerçek ses testi için hazır olup olmadığı soruldu; henüz yanıt yok. Hazır olduğunda önce `python3 test/vosk_ses_test.py --kip dinle` ile çağrıyı, sonra `--kip birlikte` ile TTS sırasında sözü alma bayrağını ölç. Birlikte testinin başlangıcında kullanıcıya haber ver. Yakalanan metin saklanmaz; bu debug test sadece sesi durdurur, isteğin devamını modele göndermez. Başarıdan sonra aynı AudioRecord oturumunda devam isteğini toplama ve ana akışa kontrollü bağlama ayrı adım. Human test olmadan yok sayma/başarılı yazma.
- Model/kütüphane kaynak/lisans kaydı VOSK_DENEME_LISANSLARI.md ve debug assets/third-party içinde. Yerel model/zip/tar gitignore'da; dışarı kod/model yükleme yok. Yeni debug bağımlılıkları release motorunu değiştirmez.
