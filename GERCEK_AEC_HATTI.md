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
