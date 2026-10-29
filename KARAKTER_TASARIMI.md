# Sezai karakteri — ortak ajan kaydı

Bu kayıt Codex, Claude/Anthropic ve projeyi devralan tüm ajanlar içindir. Önce CLAUDE.md, DURUM.md, KALAN_ISLER.md ve TEST_DURUMU.md okunur. Proje özel kalır.

## Kullanıcının tasarım kuralları

- Özgün, sıcak, sevimli ve güven veren yol arkadaşı. Zeki'nin maskotu kopyalanmaz; robot, anten, kulaklık veya siyah ekranlı yüz kullanılmaz.
- Yeşil ağırlıklı gövde, şeftali yüz ve kehribar vurgu; yumuşak yuvarlak biçimler. Göz beyazı/iris/bebek/ışık yansıması, kaş, yanak ve ağız ayrı Canvas çizim katmanlarıdır.
- Harici görsel, yeni kütüphane veya mimari geçiş yok. Tek custom View/Canvas; açık/gece paleti.
- 3–6 saniye rastgele göz kırpma, yavaş bakış/nefes/süzülme. Bekleme, dinleme, düşünme ve konuşma ifadeleri. Mutluluk kısa gülümseme, hata/üzüntü şefkatli ifade. 300 ms yumuşak geçiş hedefi.
- Pil öncelikli: beklemede yaklaşık 15 fps, diğer durumlarda en çok 24 fps. Arka plan, ekran kapalı, pencere odaksız veya kaydırılıp görünmez olunca kare planlama durur. Sistem animasyonları kaldırınca statik ifade; sadeMod araç modu için hareketsiz hazırlıktır.
- Mevcut butonlar/ses akışı korunur; dosya silinmez. Önce analiz, sonra dosya/amaç açıklaması, uygulama, telefonda derleme/imza/kurulum ve dürüst doğrulama.

## Analizle doğrulanan mevcut durum

- Çalışma başında SEZAI.md bulunmadı. Ana ekran MainActivity.kt içinde native View/LinearLayout/ScrollView ile kuruluyor; Compose veya XML layout yok.
- Sezai kaynaklarında robot görseli ve ses dalgası paneli bulunmadı. Başlıkta turkuaz S simgesi vardı. Bu gerçek simge korunur; olmayan robotu varmış gibi raporlama.
- AndroidDinleme.onRmsChanged kullanılmıyor. AndroidKonusma'da TTS ses yüksekliği yok. Konuşma için TTS onStart/onDone/onStop/onError ve metin harf/noktalama ritmi kullanılacak; gerçek ses yüksekliği veya kesin fonem senkronu iddia edilmez.
- Kadın/erkek ses değişiminde karakter kimliği değişmez. Gerçek araç modu henüz uygulanmadığı için sadeMod hazırlığı araç entegrasyonu sayılmaz.

## Dosyalar ve geri dönüş

- SezaiKarakterView.kt: ayrı çizim, ifadeler, ritim ve görünürlük/enerji koşulları.
- MainActivity.kt: başlık karakteri ve mevcut dinleme/model/TTS durumlarının görsele bağlanması.
- AndroidKonusma.kt: mevcut ses davranışına ek görsel olay bildirimleri. Mikrofon/ses kaydı veya ses yüksekliği örneklemesi eklenmez.
- MainActivity.YENI_KARAKTER_ACIK = false yapınca önceki S simgesi kullanılır. Eski TextView çizimi silinmez.
- Debug önizleme/test dosyaları üretim ses akışından bağımsız, DUMP izniyle korunmuş olacak. Sentetik görsel önizleme gerçek mikrofon/TTS kabul testi sayılmaz.

## Devam durumu

Son kaynak derlendi, zipalign/apksigner doğrulandı ve telefona kuruldu. Son aynı APK'da altı durağan Canvas ifadesi 6/6, ekran politikası 9/9, sohbet 21/21, dinleme 6/6, görüşme 8/8 geçti (toplam 50). Durağan çizim görseli incelendi; bu, canlı animasyon/TTS kabul testi değildir.

İlk canlı önizleme testi telefon Dozing durumunda olduğu için görünürlük kaybıyla başarısız oldu; ekran görüntüsü siyahtı. Kullanıcıdan ekranı açıp kilidi çözmesi istendi. Canlı göz kırpma/ritim, 300 ms geçiş, gerçek ekran/arka plan duruşu ve ana ekran yerleşimi kabul testi bekliyor. `python3 test/karakter_test.py` bunun içindir; henüz geçti sayılmaz.

Son durağan görsel: /sdcard/Download/sezai-karakter-duragan-1793270314030295687.png. Kaynak yedeği: yedek/karakter-oncesi-20261029-132319.tar.gz. TTS onStart/onDone/onStop/onError görsel bildirimleri ana iş parçacığında nesil ve benzersiz konuşma kimliğiyle korunur. Duygu seçimi birkaç açık ifadeye dayalı basit kuraldır, genel duygu anlama iddiası değildir.

Resmî API dayanakları: https://developer.android.com/reference/android/animation/ValueAnimator ve https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener .
