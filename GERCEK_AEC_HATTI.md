# Debug kayıt/çalma ve referans hattı

30 Ekim 2026. Üretime bağlı değildir. Yeni bileşen YankiHatTestActivity; DUMP izniyle korunur.

Sabit TTS cümlesi önce sıfır ses katsayısıyla hazırlanır. PCM16/mono/24kHz dışında biçim reddedilir. En çok12s/576KB üretilmiş ses bellekte tutulur; ham mikrofon sesi kalıcı dosyaya/ağa gitmez. Sentez bittiğinde PCM dizisi dondurulur. Kullanıcı metni veya model API çağrısı yoktur.

Kayıt24kHz VOICE_RECOGNITION, çalma24kHz AudioTrack ve native SpeexAEC10ms karelerle çalışacak şekilde yazıldı. Çalınacak örneklerin genliği örnekler üzerinde uygulanır; aynı örnekler referanstır. TTS'nin kendi ses çıkışı0 olduğu için ikinci işitilir ses üretmesi hedeflenmez. AudioRecord ve AudioTrack zaman damgaları aynı monoton saatle referans konumunu kestirir. Zaman damgası yoksa o kareye sıfır referans verilir; eşleşen/eşleşmeyen kare sayısı raporlanır. Bu tasarım gerçek odada gecikmenin doğru eşleştiğini tek başına ispatlamaz.

Kayıt/çalma en çok12s, dış watchdog15s; hazırlık30s. Odak/görüşme/görünürlük kaybında bırakılır. Yazıcı nonblocking I/O kullanır; kaynaklar yazıcı durduktan sonra serbest bırakılır. Native oturum synchronized işleme/kapatma ile korunur; JNI yanlış kare boyutunu reddeder.

## Gerçek sonuç ve sınırlama

- İlk kurulu APK'da JNI kare/kapatma testi1793326688709345903 geçti.
- İlk gerçek hat denemesi1793326719234322710: GORUNURLUK_KAYBI/çıkış1; PCM ve mikrofon başlamadan durdu. Sonraki dumpsys power Dozing, display blocker false ve NotificationShade gösterdi. Bu fiziksel hat başarısı değildir. Ekran zorla açılmadı.
- Sonraki test betiği, başlangıçta Awake doğrulanmazsa Activity'yi başlatmaz. Bu koruma gerçek Dozing durumunda doğru biçimde reddetti; yeni mikrofon denemesi yapılmadı.
- Arka plan testi artık test_id eşleşen ilk gerçek native/mikrofon karesi sinyalini beklemeden Home göndermez; son raporda da mikrofon/native kare şarttır. Bu son arka plan akışı fiziksel cihazda henüz çalıştırılmadı.
- Son kurulu APK JNI testi1793327159043386594: 24kHz akışta ikinci yapay sinyal katsayısı0.962139,artık azalması10.614426dB; aynı zamanda16kHz yapay yankı58.167dB. Yanlış kare/hız, çift kapatma ve kapalı oturuma erişim korumaları geçti. Gerçek insan/oda/pil kabulü değildir.
- Son aynı APK: dinleme9,sohbet21,görüşme8=38 sessiz regresyon PASS. Sohbet yanıtları sahte ağdır.

## Testler

`python3 test/yanki_hat_test.py` gerçek mikrofonu açar, varsayılan çıkış0'dır. Başlangıçta ekranAwake olmalıdır. Test sonucunda native kare/zaman eşleşmesi/tam çalma/kaynak bırakma kontrol edilir. Sessiz çıkış, gerçek yankı giderme veya insan sözü kesme testi sayılmaz.

`--arka-plan`: önce gerçek hat başlangıcını bekler, Home gönderir ve kaynak bırakmayı doğrular. Fiziksel ekran kilitlemez.

`--kip yanki --genlik 0.2`: gerçek ses çıkarır; kullanıcıya haber verilmeden/gece sessiz çalışmada başlatılmaz. Üst sınır0.3. İnsan olmadan kendi TTS'sinin sızıntısı için kullanılabilir; yalnız toplam giriş/çıkış enerji farkı insan sesini koruma kanıtı değildir.

Sonraki adımlar: gerçek ekran-açık sessiz hat kabulü, fiziksel referans/çalma gecikmesi kontrolü,24→16kHz dönüşüm ve çağrı tanıma. Üretimde TTS sırasında dinleme/söz kesme ve kilit desteği tamamlanmadı. Yeni servis veya wake lock eklenmedi; pil ölçülmedi.

API dayanakları: [AudioRecord timestamp](https://developer.android.com/reference/android/media/AudioRecord#getTimestamp(android.media.AudioTimestamp,%20int)), [AudioTrack timestamp ve write](https://developer.android.com/reference/android/media/AudioTrack).


##16kHz dönüşüm aşaması
Son APK testi1793350603706639412:16/24/48kHz girişlerin her1s için16000 örneğe dönüşümü,1000Hz/RMS ve hatalı tampon-kapatma kabulü geçti. Speex quality3,gecikme24çıkışörneği(1.5ms);skip_zeros yok. Debug fiziksel hatta dönüşüm kodu var,tanıyıcı tüketicisi yok. Fiziksel hat hâlâ ekran-Awake engeli nedeniyle doğrulanmadı. Son38 sessiz regresyon geçti.


## 30.10.2026 19:38 — ilk fiziksel sessiz hat ve arka plan kabulü
- `~` devamı, kota %4/%47. Kaynak/APK değişmedi; mevcut kurulu APK cihazda sınandı.
- Sessiz çıkış test1793378262048081498 PASS/HAT_BITTI: gerçek mikrofon24kHz,629kare=6290ms;611 zaman eşleşmesi,zaman damgası yok0;144025 örnek tam çalındı (dijital genlik0),100640 temiz16kHz örnek üretildi. Native AEC+dönüşüm thread CPU359ms; çıkış underrun1. Kaynaklar bırakıldı,ham ses saklanmadı. Bu referans konumu hesaplaması/hat kabulüdür; gerçek akustik gecikme ve yankı giderme kanıtı değildir.
- Arka plan test1793378288002596696 PASS/GORUNURLUK_KAYBI: gerçek mikrofon232kare=2320ms başladıktan sonra Home gönderildi;225 zaman eşleşmesi,37120 temiz16kHz örnek; kaynaklar bırakıldı. Fiziksel kilit veya arka plan dinleme desteği testi değildir.
- Kullanıcıya haber verilerek istenen yanki/genlik0.2 testi Awake ön koşulunu sağlayamadı/çıkış1; Activity/mikrofon/işitilir çalma başlamadı. Son appops RECORD_AUDIO çıktısında running yok. Ekran/izin/rol değiştirilmedi.
- Sessiz mod enerji farkı19.822dB, arka plan turu3.064dB: çıkış0 olduğundan bunlar hoparlör yankı giderme başarısı sayılamaz. CPU süreleri enerji/pil ölçümü değildir.
- Sonraki: ekran açıkken sesli referans/gecikme ve kendi-ses sızıntısını ölçmek; sonra16kHz temiz sesi tanıyıcıya bağlamak. İnsan dur/Sezai,ilk kelimeler/bağlam,pil/kilit ve üretim entegrasyonu hâlâ açık.


## 30.10.2026 23:13 — sesli hat ve gecikme tanısı
- Devam kota başlangıç%5/%47,son%8/%47. İlk sesli girişim Awake ön koşulunda reddedildi; kullanıcının sonraki `~` mesajıyla yeniden denendi.
- ESKİ kurulu APK sesli test1793388177335676987 PASS/HAT_BITTI: genlik0.2,24kHz,627kare/6270ms,610zaman eşleşmesi,zaman yok0;144025 örnek tam çalma,100320 temiz16kHz örnek,CPU389ms,underrun1,kaynak bırakma true. Toplam enerji farkı yalnız0.2552646dB; bu yankı giderme başarısı değildir. İnsan çağrısı/tanıyıcı yok.
- Yedek yedek/gecikme-tanisi-oncesi-20261030.tar.gz. Debug YankiGecikmeOlcumu eklendi:24kHz kareleri sekizli ortalamayla3kHz'e indirip en çok12s için iki36000short tamponda tutar (~144KB); zaman damgalı referansla ±200ms/2ms korelasyon tarar. Pozitif gecikme mikrofonun referansı izlemesidir. Mikrofon tepe/kırpılan örnek sayısı da raporlanır. Ham/verisi dosyaya veya ağa yazılmaz, tanı sonunda tamponlar sıfırlanır. Bu düşük çözünürlüklü ipucudur; gerçek yankı/insan koruma/uygun gecikme düzeltmesi kabulü değildir. Tanı taraması ses kaynakları bırakıldıktan sonra yapılır; rapordaki aec_thread_cpu_ms bu son taramayı içermez.
- Activity kareleri bu tanıya verir; DUMP korumalı yerel receiver yapay -12/0/+12ms gecikme ve sıfır/kısa giriş reddini sınar. Python testleri yeni tanı alanını/kare sayısını şart koşar. Normal üretim ses akışı değişmedi.
- Ubuntu proot ./build.sh çıktı0,zipalign-c4,apksigner sign/verifyv2/v3 PASS; APK native bayt eşleşmesi/yeni DEX ölçüm alanı PASS;pm install-r Success. Yeni native C değişmedi. Son APK mikrofonsuz JNI1793391146819122863 PASS,gecikme_tanisi_testi true; yapay AEC ve16/24/48→16kHz kontrolleri geçti. Python AST/diff--check PASS. Önceki38 regresyon bu yeni APK'da tekrarlanmadı; bu turun kapsamı debug tanı/JNI'dir.
- SON APK sesli tekrar Awake koşulunda reddedildi; yeni tanının fiziksel gecikme/kırpılma sonucu henüz yok. Ekran zorla açılmadı,yeni servis/wake lock yok,pil ölçülmedi.
- Sıradaki: ekran açıkken python3 test/yanki_hat_test.py --kip yanki --genlik 0.2; korelasyon/genlik/kırpılma bulgularına göre kök nedeni ayrıştır. Üretimde dur/Sezai ve temiz ses tanıyıcı bağlantısı hâlâ yok; kilit/pil kabulü açık.


## 30.10.2026 23:17 — gerçek tanı ve sıfır asistan sesi bulgusu
- `~` devamı,kota%8/%47. İlk ön kontrol Awake değildi. Test betiğine --ekran-bekle 0..60s eklendi; yalnız salt okunur sorguyla kullanıcının ekran açmasını bekler,zorla uyandırmaz. Yedek yedek/yanki_hat_test-ekran-bekleme-oncesi-20261030.py. Uygulama/APK değişmedi.
- --ekran-bekle60 gerçek cihazda ekran açılmasını algıladı. Son APK hat1793391305162002959 HAT_BITTI/çıkış0:24kHz625kare/6250ms,611zaman eşleşmesi,144025tam çalma,100000temiz16kHz örnek,CPU264ms,underrun1,kaynak bırakma true. Mikrofon tepe4710,kırpılan0; toplam enerji farkı1.0823dB. Korelasyon -0.0165512,tepe konumu -174ms: ilişki çok zayıf,bu gecikme güvenilir düzeltme kabul edilmedi. Ham ses dosya/ağ yok.
- Sonraki salt okunur dumpsys audio STREAM_ASSISTANT speaker0/15,STREAM_MUSIC3/15; bağımsız cmd media_session volume --stream11 --get de0/15 doğruladı. Test USAGE_ASSISTANT kullanır. Sistem sessizliği zayıf referans için somut adaydır; sonradan ölçülen ayar test anındaki duyulurluğu tek başına kanıtlamaz. Kullanıcıya cümleyi duyup duymadığı soruldu; yanıt henüz yok. Eski sesli turdaki çalma tamamlanması gerçek işitilir yankı kabulü değildir.
- Betik yanki modunda artık STREAM_ASSISTANT sesini önceden doğrular;0 veya okunamayan düzeyde Activity/mikrofon başlamaz. Hiç ses ayarı değiştirilmedi. Gerçek son ön-kontrol bu nedenle çıktı1 ile durdu; ayrıca sahte köprüyle Awake+ses0 durumunda yalnız iki sorgu/Activity başlatmama kontrolü PASS. AST,geçersiz61s bekleme reddi,diff--check PASS. Yeni APK gerekmedi.
- Sonraki: kullanıcı duyulabilir asistan sesini sağladıktan sonra --kip yanki --genlik0.2 --ekran-bekle60 ile tekrar; sesli referans/korelasyon,sonra tanıyıcı bağlantısı. Dur/Sezai,pil,kilit tamamlanmadı. Yeni servis/wake lock yok. Son gerçek kayıt sonrası appops running göstermedi.


## 30.10.2026 23:20 — ses sorgusu çıktı kanalı düzeltmesi
- `~` devamı kota%11/%48; --ekran-bekle60 sonrasında sesli ön kontrol yine reddetti,Activity/mikrofon başlamadı.
- Kök neden bulundu: bu cihazda cmd media_session volume --get başarılı çıkış0 ile bilgiyi stdout yerine stderr'e yazıyor. Önceki betik yalnız stdout okuyordu; sıfırdan bağımsız bütün düzeyleri doğrulanamadı diye reddedebilirdi. test/yanki_hat_test.py cihaz(iki_akis=True) yalnız ses sorgusunda iki kanalı birleştirir. Yedek yedek/yanki_hat_test-ses-okuma-oncesi-20261030.py. Uygulama/APK değişmedi.
- İlk geçici ses denemesi,eski düzey okunamadan durdu/ayar komutu çalışmadı. İkinci denemede doğru okuma0/15; STREAM_ASSISTANT --set3 komutu çıktı0 fakat get yine0/15. Test BAŞLATILMADI. finally --set0/get0 doğrulandı. Son bağımsız STREAM_MUSIC3/15,STREAM_ASSISTANT0/15. Komut başarısı ses değişimi kabul edilmedi; OEM ses politikası/USAGE_ASSISTANT eşlemesi kök nedeni hâlâ bilinmiyor. Sıfır legacy akış düzeyi tek başına gerçek AudioTrack işitilmezliğini kesin kanıtlamaz.
- AST/diff--check PASS; sahte köprüyle stdout ve stderr kanallarının her birinde0reddi/3okuma toplam4/4 PASS. Bu cihazın duyulurluk testi değildir. Yeni fiziksel kayıt alınmadı,yeni APK gerekmedi.
- Kullanıcının cümleyi duyup duymadığı henüz yanıtlanmadı. Sıradaki teknik ayrıştırma: debug AudioTrack için açık medya kullanımını ayrı kontrollü karşılaştırma olarak değerlendir; üretim ses kullanımını değiştirme. Önce sesin gerçekten çıktığı doğrulanmalı,sonra referans korelasyonu/AEC/tanıyıcı. Dur/Sezai ve pil/kilit açık.


## 30.10.2026 23:57 — medya çıkışı ve referans gecikmesi karşılaştırması
- Kullanıcının ardışık `~` devamları; kota başlangıç%1/%48,son%7/%49. Üretim ses akışı değişmedi; yalnız debug YankiHatTestActivity ve test/yanki_hat_test.py. Yedekler medya-cikis-deneyi-oncesi-20261030.tar.gz ve referans-gecikme-deneyi-oncesi-20261030.tar.gz.
- --cikis asistan|medya eklendi(varsayılan asistan); Activity USAGE_ASSISTANT16 veya USAGE_MEDIA1 seçer ve gerçek AudioTrack usage'ını raporlar. Betik buna göre stream11 veya3 ses düzeyi sorgular; rapordaki seçimi doğrular. Sistem ses ayarı bu aşamada değiştirilmedi. Asistan akışı sıfır bulgusu önceki turları tek başına işitilmez saydırmaz; medya karşılaştırması ayrı debug koşuludur.
- İLK medya APK iki fiziksel turu:1793393428528102670 ve1793393464852488385. Medya7/15,genlik0.2,625kare/6250ms,144025tam çalma,100000temiz16kHz örnek; korelasyon0.390481/0.419393,ikisinde132ms;enerji farkı0.05347/0.06583dB;tepe19791/21396,kırpılan0. Native CPU306/255ms;underrun1;tam bırakma true. Korelasyon tekrarlı gecikme ipucudur,insan koruma veya güçlü AEC kabulü değildir.
- Bunun üzerine --referans-gecikme-ms0..200 eklendi(varsayılan0). Yalnız istenen debug turunda zaman damgasından hesaplanan referans konumundan bu kadar geçmiş örnek seçilir. Üretime sabit132/120ms konmadı. Yanlış-1/201 sınır reddi ve AST/diff--check PASS. Sahte köprüde medya/asistan sorgusu ve Intent seçimi2/2 PASS.
- İKİ aşama da Ubuntu proot ./build.sh çıktı0,zipalign-c4,apksigner sign/verifyv2/v3 PASS,pm install-r Success. Son APK /sdcard/Download/Sezai-debug.apk;native bayt eşleşmesi ve DEX yeni alanları PASS. Native C değişmedi. Önceki38 üretim regresyon bu tur tekrarlanmadı; kapsam debug çıkış/gerçek hat/native.
- SON APK120ms tur1793393769687285561:medya11/15,genlik0.2,621kare/6210ms,605eşleşme,144025tam çalma,99360temiz16kHz;korelasyon0.344642,kalan12ms;enerji farkı1.521565dB,tepe22936,kırpılan0,CPU166ms,underrun1,kaynak bırakma true. Önceki7/15 turlarıyla doğrudan etki kıyası yapılmadı.
- SON APK0ms kontrol1793393801653039090 aynı medya11/15/genlik0.2:625kare/6250ms,614eşleşme,100000temiz16kHz;korelasyon0.348591,132ms;enerji farkı0.092805dB,tepe22936,kırpılan0,CPU319ms,underrun1,tam çalma/bırakma true. Sıra120→0 ve insan/oda koşulları kontrol edilmedi; rastgele/çoklu kabul değildir.120ms kaydırma kalan gecikmeyi beklenen120ms azalttı;enerji farkı artışı aday çözümü destekler ama1.52dB güçlü yankı giderme veya insan sesi koruma başarısı değildir.
- SON APK mikrofonsuz native1793393834655973869 PASS:gecikme tanısı,yapay AEC,16/24/48→16kHz ve kare/kapatma kontrolleri. Gerçek turlarda ham ses diske/ağa yazılmadı. Yeni servis/wake lock yok;pil ölçülmedi.
- Sıradaki: 120ms adayının tekrarlı yakın-ses/kendi-TTS kontrolü ve AEC'nin neden sınırlı kaldığını ayrıştır; daha uzun filtre/başlangıç uyumu/oda koşulları ölçülmeden varsayılanı değiştirme. Ardından temiz16kHz sesi tanıyıcıya bağla. İnsan dur/Sezai,devam isteği,pil/kilit/üretim entegrasyonu açık.


## 31.10.2026 02:43 — temiz ses tanıyıcıya bağlandı (yalnız debug)
- Ardışık `~` devamları;kota başlangıç%8/%49,son%13/%50. Önceki APK120ms tekrar1793394346499403101:medya10/15,genlik0.2,635kare/6350ms;enerji farkı1.249816dB,korelasyon0.402722,kalan12ms,kırpılan0,tam çalma/bırakma true. Tekrar zamanlama bulgusunu destekler; medya düzeyi önceki11/15 ile farklı,enerji doğrudan aynı-koşul kıyası değildir.
- Yedek yedek/temiz-ses-cagri-tanisi-oncesi-20261031.tar.gz. Yeni debug YankiCagriTanisi mevcut yerel Türkçe Vosk modelini mikrofon başlamadan açar;AEC+dönüşümden gelen16kHz sesi100ms parçalarla [dur,sezai,[unk]] dilbilgisine verir. Yalnız iki hedef bayrağı,ilk algı anının ses konumu,örnek/CPU/işlem süresi tutulur;tanıma metni/ham ses dosya-ağ yok. close model/tanıyıcı/tampon bırakır. Activity bunu yalnız --cagri-tanisi isteğinde bağlar;normal Sezai davranışı değişmedi. Otomatik çalma kesme/takip isteği YOK.
- Python betiği Intent bayrağını yollar;model kapanması ve tanıyıcıya verilen/çözülen örneklerin temiz16kHz sayısıyla eşleşmesini şart koşar. AST,diff--check ve sahte köprüde seçenek aktarımı PASS.
- Ubuntu proot ./build.sh çıktı0,zipalign-c4,apksigner sign/verifyv2/v3 PASS,pm install-r Success. Son APK /sdcard/Download/Sezai-debug.apk;native bayt eşleşmesi ve yeni DEX tanı alanı PASS. Native C değişmedi. Önceki38 üretim regresyon bu APK'da tekrarlanmadı;bu turun kapsamı debug gerçek ses/tanı kaynak yaşam döngüsü.
- SON APK sesli tanı girişimi medya sesinin sıfır/okunamaz ön kontrolünde reddedildi;Activity/mikrofon/çalma başlamadı,ses ayarı değiştirilmedi.
- SON APK sessiz çıkış testi1793403731814836418 PASS/HAT_BITTI:627kare/6270ms,100320temiz16kHz örneğin tamamı Vosk'a verildi ve çözümlendi;dur=false,sezai=false. Tanıyıcı CPU545ms,en uzun çağrı83ms;AEC+dönüşüm CPU153ms,underrun1;model/mikrofon/tüm kaynaklar bırakıldı. Bu örnek aktarımı kabulüdür;insan komutu söylendiğine dair teyit yok,çağrı duyma başarısı değildir.
- SON APK gerçek arka plan testi1793403776341034109 PASS/GORUNURLUK_KAYBI:304kare/3040ms,48640temiz16kHz örneğin tamamı çözümlendi;dur/sezai=false;tanıyıcıCPU380ms,max57ms;model/mikrofon kaynakları bırakıldı. Son appops running yok. Fiziksel kilit/arka plan dinleme testi değildir.
- Kullanıcıdan insan sesi testine hazır olup olmadığı soruldu. Sıradaki: ekran açıkken sessiz --cagri-tanisi turunda dur ve Sezai söylediğini kullanıcı teyit etsin;ardından duyulur hoparlörde yanlış tetikleme/gerçek çağrı testi. Çalma kesme ve aynı mikrofonla devam isteği daha sonra;üretim/kilit/pil henüz tamamlanmadı. Yeni servis/wake lock yok;CPU enerji ölçümü değildir.


## 31.10.2026 02:45 — insan çağrısı denemesi: Sezai bayrağı var, teyit bekliyor
- `~` devamı,kota%15/%50; kullanıcıya ekran mikrofon-açık yazısını görünce dur,ardından Sezai söylemesi bildirildi. Kaynak/APK değişmedi.
- Son APK sessiz çıkış/çağrı tanısı1793403862792989753 HAT_BITTI:625kare/6250ms,100000 temiz16kHz örneğin tamamı tanıyıcıya verildi/çözüldü. sezai_duyuldu=true,dur_duyuldu=false,ilk_cagri_ses_ms1400. Bu işlenen ses konumudur,gerçek söz başlangıcından gecikme ölçümü değildir.
- Tanıyıcı CPU1026ms,max59ms;AEC+dönüşümCPU279ms;mikrofon tepe22936,kırpılan0;model/mikrofon kaynak bırakma true. Son appops running yok. Ham ses/metin dosya-ağ yok;pil ölçülmedi.
- Kullanıcıya ikisini mi,yalnız Sezai mi,hiçbirini mi söylediği soruldu;henüz yanıt yok. Otomatik Sezai bayrağı tek başına doğrulanmış insan kabulü veya dur başarısı değildir. Kullanıcı ikisini söylediyse bu tur dur kaçırma olarak kaydedilmeli;hiç söylemediyse yanlış tetikleme olarak ayrıştırılmalı. Otomatik çalma kesme bu testte yok.
- Sıradaki adım kullanıcı teyidine bağlı: hedefleri ayrı sessiz kontrol ve ardından duyulur TTS altında çağrı/yanlış tetikleme. Üretimde söz kesme,takip isteği,kilit/pil açık.
