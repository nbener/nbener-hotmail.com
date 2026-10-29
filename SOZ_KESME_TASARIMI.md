# Konuşma sırasında söz alma — ortak devir kaydı

Codex, Claude/Anthropic ve diğer ajanlar için. 29 Ekim 2026.

## İstenen davranış
Kullanıcı TTS sürerken “dur” veya “Sezai” diyerek sözü alır. Sezai susar ve isteğin devamını dinler. Takip sorusunda konu korunur; yeni konuda son istek esas alınır. Yalnız çağrı sohbeti bitirmez, geçmişi silmez. “Sohbeti bitir” ayrı komuttur.

## Kurulu uygulamadaki kapsam
SohbetKomutlari dur/Sezai çağrısını ve arkasındaki isteği ayırır. MainActivity mevcut tanıma sonucu geldiğinde yalnız çağrıda yeniden dinler, devamındaki isteği sohbet motoruna geçirir. SohbetMotoru yön değiştirme talimatı içerir. Bunlar TTS sırasında duyma özelliği değildir: üretimde TTS sırasında mikrofon hâlâ kapalıdır. Gerçek modelin her konu geçişini doğru yorumladığı doğrulanmadı.

## Cihazda bulunan engel
Android API31; varsayılan tanıma GoogleTTSRecognitionService. DUMP korumalı debug EszamanliSesTestActivity ile tek, en çok 12 saniyelik deneme yapıldı. Ham tanınan metin raporlanmaz; model API isteği yok. Üretime bağlı değildir.

Yeteneği okuma: `am start -W -n com.sezai.assistant/.EszamanliSesTestActivity --ez yalniz_bilgi true --es test_id 1793271921562238043`
Sonuç: `{"yerel_tanima_var":false,"test_id":"1793271921562238043","sonuc":"YETENEK_OKUNDU"}`.

Eşzamanlı deneme: `am start -W -n com.sezai.assistant/.EszamanliSesTestActivity --ez yerel false --es test_id 1793271941885500692`
Sonuç: `{"yerel_tanima_var":false,"yerel_istendi":false,"tts_basladi":true,"test_id":"1793271941885500692","sonuc":"SES_ODAGI_KESILDI"}`.

Test tamamlanamadı: tanıma başlatıldığında ses odağı kesildi; dinleme-hazır veya söz-alma-duyuldu doğrulanmadı. Android STT'yi TTS boyunca döngüye sokma ya da görüşme/odak korumasını kaldırma. EXTRA_PREFER_OFFLINE yerel çalışma garantisi değildir.

## Somut sonraki seçenek: ayrı yerel Türkçe motor denemesi
Vosk `vosk-model-small-tr-0.3`: resmî listede 35M, Türkçe doğruluk ölçümü TBD, Apache 2.0. Bu telefonda denenmedi; başarı/pil iddiası yok. Model ve ARM64 Android kütüphanesi indirilip deneme bileşeninde ölçülmeli. Yeni motor henüz eklenmedi. CLAUDE.md bölüm 4 motor kararının onaya sunulmasını ister.

Önerilen sınırlı kullanım: yalnız etkin sesli sohbetin TTS süresinde AudioRecord ile yerel tanıma; ekran/uygulama kapanışında, TTS bitişinde ve görüşmede kaynakları bırak. Sürekli arka plan mikrofonu açma. Cihaz destekliyorsa VOICE_COMMUNICATION/AcousticEchoCanceler denenebilir; yankıyı gerçekten ayırdığı ölçülmeden varsayılmaz. Kullanıcı çağrısından sonraki kelimeleri kaybetmeden aynı yakalama içinde isteği tamamla. Normal dinleme motorunu başlangıçta değiştirme.

Kabul: insan yokken Sezai kendi sesinden tetiklenmemeli (kendi cümlesinde dur/Sezai dahil); kullanıcı hoparlör üstüne konuşunca kesme gecikmesi ölçülmeli; çağrı ardından takip kelimeleri kaybolmamalı; aynı/yeni konu ve belirsiz istekler denenmeli. Kilit, arka plan, görüşme ve pil/RAM ölçülmeli. “Sezai” model sözlüğünde doğrulanmalı. Başarısızsa varsayılan akış korunur. Kütüphane/SDK/paket güncellemesi kendiliğinden yapılmaz.

Kaynaklar: https://alphacephei.com/vosk/models , https://alphacephei.com/vosk/android , https://developer.android.com/reference/android/speech/SpeechRecognizer .

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

## 29 Ekim — kullanıcı devamıyla birlikte konuşma denemesi
- Kullanıcı `~` gönderdi; kısa ses testi açılacağı ve TTS sürerken dur/Sezai demesi kullanıcıya bildirildi. `python3 test/vosk_ses_test.py --kip birlikte`, test_id=1793273601194412871.
- Sonuç: SURE_DOLDU. tts_basladi/model_acildi/aec_mevcut/aec_etkin/mikrofon_basladi/tanima_olayi_var=true. model_acilis_ms974; ses11900ms; çözüm CPU5093ms; tepe32768; kapanış PSS180988KB; kaynaklar_birakildi=true. soz_alma_duyuldu bayrağı gelmedi. Gerçek söz kesme BAŞARILI DEĞİL. Kullanıcının gerçekten araya girip girmediği bilinmiyor; soru gönderildi. Kişisel metin/ses saklanmadı.
- Kaynak incelemesi: mevcut çağrı ayırıcı yalnız tanınan cümlenin başını kontrol eder. Kendi TTS'si önce tanınıp kullanıcı çağrısı aynı sonucun ortasına eklenirse kaçırabilir. Bunun gerçekleştiği henüz kanıtlanmadı. Yeni debug sayaçları dur/sezai herhangi yerde bayrakları, en uzun tanıma sözcük sayısı ve doygun örnek sayısı; ham metin yine kaydedilmez. Yedek yedek/vosk-tani-oncesi-20261029-143435.tar.gz.
- Python deneme betiği artık birlikte/dinle olumlu testlerinde çağrı algılanmadan süre dolmasını sıfır çıkışla tamamlamaz. Önceki sıfır çıkış yalnız ölçümün tamamlanmasıydı, söz kesme başarı kanıtı değildi. Arka plan/bilgi kipleri kendi ölçütlerini kullanır.
- Yeni debug sayaç kaynağı derleniyor; üretim/ana ekran akışı değişmedi. Kullanıcı geri bildirimi ve yeni cihaz kabulü bekliyor.

- Yeni tanı APK'sı build/zipalign/apksigner verify geçti; DEX içinde doygun_ornek_sayisi bulundu, pm install -r Success. Son APK mikrofon açmayan bilgi testi test_id1793273806423283470: MODEL_HAZIR, model945ms, doygun0, kaynaklar bırakıldı. Bu, yeni sayaçların sesli doğrulaması değildir. Yeni APK ile insan sesi testi tekrar koşulmadı; kullanıcı geri bildirimi hâlâ yok.
- `cmd media_session volume --stream 3 --get`: STREAM_MUSIC 15 / [0..15]. Ses seviyesi değiştirilmedi. Önceki tepe32768 ile birlikte yüksek hoparlör/yankı veya doygunluk olasılığı var; neden olduğu kanıtlanmadı. Sonraki sesli testte doygun örnek sayısı ve çağrı-herhangi-yerde bayraklarını kontrol et. Gerekirse sınırlı ve önceki değeri geri koyan düşük ses karşılaştırması yap; doğrudan çağrıyı tüm metinde arayıp üretime bağlama (kendi TTS'si tetikleyebilir).
- Python py_compile ve git diff --check geçti. Üretim kodu değişmediğinden önceki47 regresyon tekrar koşulmadı. Bu aşama yalnız tanı kaynağı/kurulum ve söz alma başarısız denemesidir; özellik tamamlanmadı.
