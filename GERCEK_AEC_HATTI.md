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
