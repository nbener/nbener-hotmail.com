# Sessiz bekleme, kilit ekranı ve pil araştırması

30 Ekim 2026. Kullanıcı ses çıkarmadan dinleme, pil ve kilit ekranı uygunluğunu araştırmamı istedi. Bu aşama araştırma; arka plan mikrofonu veya yeni servis açılmadı.

## Ne mümkün, ne doğrulandı?

Sezai'nin konuşmadan çağrı beklemesi, konuşurken söz kesmesi ve ekran kilitliyken çalışması farklı işlevlerdir. Hoparlör referanslı AEC yalnız yankı sorununa yöneliktir; düşük enerji veya arka plan çalışma izni sağlamaz.

Kaynakta SezaiUyanma, yalnız görünür/odaklı/boş Activity koşulunda Vosk+AudioRecord çalıştırır. MainActivity.onPause/onStop dinlemeyi kapatır. Manifestte FOREGROUND_SERVICE ve FOREGROUND_SERVICE_MICROPHONE izinleri mevcut; microphone türünde çalışan bir ön plan servisi ise yoktur. Yalnız izin satırı eklenmiş olması kilitte çalışmayı sağlamaz. Bu kaynakta kilit ekranı desteği tamamlanmadı.

Salt okunur cihaz kontrolü Android API31 döndürdü. soundtrigger ve soundtrigger_middleware sistem servisleri listelendi. Filtreli middleware dökümünden modül güç/yetenek bilgisi gelmedi; DSP desteği, erişim ve Sezai modeli çalışabilirliği kanıtlanmadı. Hiç model yüklenmedi, rol/izin değişmedi, ekran kapatılmadı.

## Pil için tasarım önerisi

| Durum | Önerilen iş | Bilinmeyen |
| --- | --- | --- |
| Ses çıkarmadan çağrı bekleme | Hafif yerel anahtar sözcük algılama; uygun ön tampon; gereksiz ağ/TTS/AEC yok | Türkçe çağrı başarısı ve gerçek pil |
| Kullanıcı konuşuyor | Tam tanıma ve istek işleme | Devam isteğinin ilk kelimelerinin korunması |
| Sezai konuşuyor | Çalma referanslı AEC ve konuşma başlangıcı/çağrı algılama | Yankı altında Türkçe kabulü ve CPU maliyeti |
| Dinleme kapalı | Mikrofon/model/işleme kaynaklarını bırak | Kaynakların fiziksel kilit ve süreç kesintisinde bırakılması |

Bu bir mühendislik önerisidir, ölçülmüş enerji sonucu değildir. Yerel olmak ağ ihtiyacını azaltabilir ama mikrofon/işlemci maliyetini ortadan kaldırmaz. Sessizlikte bile yazılım dinleyicisi ses karelerini işlemelidir. VAD ses varlığını saptar, tek başına Sezai sözcüğünü tanımaz. Girişe göre ağır ASR'yi açmak düşünülebilir; ilk heceleri kaybetmemek için bellek içi kısa tampon ve aynı kayıt oturumu gerekir.

Önceki kısa debug testindeki ses3840ms/çözüm CPU1159ms ölçümü sürekli gün boyu kullanımın pil yüzdesi değildir. 0 ses katsayılı TTS PCM testi de düşük güçlü uyandırma testi değildir. WebRTC APM kurulup ölçülmedi; enerji tasarrufu garantisi verilemez.

## Kilit ekranı için belgelenmiş yol

[Android mikrofon ön plan servisi](https://developer.android.com/develop/background-work/services/fgs/service-types#microphone), arka planda mikrofon yakalamayı sürdürme kullanımını tanımlar. Kullanıcının başlattığı dinleme, mikrofon izni ve kullanıcıya görünür servis bildirimiyle Activity dışına taşınabilir. Ekranı açık tutmak gerekmez; kilit ekranında gerçek süreklilik bu telefonda ayrıca denenmelidir.

[Başlatma kısıtları](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start): genel yol servisi uygulama görünürken başlatmaktır. Android12+ arka plandan servis başlatmayı sınırlar; Android14+ mikrofon gibi kullanım sırasına bağlı izinler için daha erken kontrol yapar. Bu cihaz API31, uygulama target34; farklı Android sürümü koşulları karıştırılmamalıdır. Ekran kilitliyken başlamayı her koşulda serbest varsayma. Yeniden başlatma, süreç öldürülmesi, kullanıcı dinlemeyi kapatması ve force-stop ayrı kabul senaryolarıdır; otomatik yeniden dinleme garantisi yok.

[Android güç yönetimi](https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/identify-wls), kayıt/çalma API'lerinin gerekli kilitleri alabileceğini ve medya API'leri kullanılırken otomatik olarak ayrıca manuel wake lock eklenmemesini önerir. Foreground service ile CPU'yu uyanık tutma aynı şey değildir. Sırf kilitte çalışsın diye kalıcı wake lock/beyaz liste ekleme; ihtiyaç ve maliyet ölçülür. Android üretici güç politikalarının sürekliliği etkileyip etkilemediği cihazda doğrulanmalıdır.

Kilit ekranında sesli yanıt vermek, telefon kilidini açmak veya başka uygulamanın ekranını okumak anlamına gelmez. Sesli işlemin kapsamı, bildirim içeriği ve gerekirse kilidi açma akışı ayrı ürün kararlarıdır; bu araştırmada değiştirilmedi.

## Daha düşük güçlü sistem yolu

[AOSP Sound Trigger](https://source.android.com/docs/core/audio/sound-trigger), düşük güçlü akustik tetik algılamayı ve üreticiye özgü HAL/model sözleşmesini açıklar. Bağımsız geliştiricinin herhangi bir Türkçe modeli DSP'ye yükleyebileceği anlamına gelmez. Sistem servislerinin varlığı bu erişime kanıt değildir.

[VoiceInteractionService](https://developer.android.com/reference/android/service/voice/VoiceInteractionService), kullanıcının seçtiği sesli asistan hizmetinin sistem tarafından arka planda tutulabildiğini açıklar. Bu rol DSP/Türkçe model desteğini kendiliğinden sağlamaz. Varsayılan asistan seçimi değiştirilmedi; bu seçenek daha sonra ayrı prototiple ve kullanıcı seçimiyle değerlendirilebilir.

## Ölçüm ve uygulama sırası

1. Mevcut ön plan söz kesme çalışmasını referans ses/AEC ile tamamla; bu araştırma onu değiştirmez veya iptal etmez.
2. Türkçe hafif bekleme için doğru/yanlış çağrı ve ilk hece korunmasını ölç; Vosk tam çözümleme ile seçenekleri aynı koşullarda karşılaştır.
3. Açık kullanıcı başlatması, kapatma eylemi ve bildirimli microphone servisi için ayrı küçük plan hazırla. Kaynak/lifecycle değişikliği gerektirir; bu araştırmada uygulanmadı.
4. Fiziksel kilit altında önce sınırlı süre, sonra daha uzun oturum; görüşme, mikrofon gizlilik anahtarı, izin kaybı ve kullanıcı kapatması test edilir. Ekran kendiliğinden zorla kapatılmaz.
5. Aynı şarj/ekran/ağ/sıcaklık koşullarında dinleme kapalı ve açık ölçümleri karşılaştır: batarya enerji/sayaç verisi varsa kaydet, süreç CPU süresi ve bellek, yanlış uyanma, gecikme ve ses kaybını ayrıca izle. Kısa test veya tek yüzde okumasından saatlik tüketim uydurma.

Kaynaklar okundu ve mevcut kodla karşılaştırıldı. APK, servis, asistan rolü, pil ayarı ve izinler değişmedi; yeni fiziksel kilit veya pil kabul testi yapılmadı.
