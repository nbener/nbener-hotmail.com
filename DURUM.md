# GÜNCEL KULLANICI TALİMATI
- Kullanıcı 29 Ekim sabahı 06.00 saat sınırını kaldırdı. Saat nedeniyle durma; %80 kota sınırı korunuyor.
- Kullanıcı erken durmayı istemiyor: canlı API anahtarı olmayan aşamada bağımsız geliştirme ve sessiz testlerle devam et, durumu kaydet.
- Saat gözcüsü iptal edildi; yalnızca kota izleyen süreç çalışıyor. Aşağıdaki eski 06.00 kayıtları geçmiş bilgidir; bu yeni talimat geçerlidir.

# SEZAI - DURUM (her adımdan sonra güncelle, her oturumda ilk bunu oku)

Son güncelleme: 2026-10-29

## Ne yapıldı
- Sezai bağımsız projedir (Zeki ile ilişkisi yok). Paket adı: com.sezai.assistant. Kurallar: CLAUDE.md
- Araç zinciri kuruldu ve doğrulandı (proot Ubuntu 26.04 aarch64): JDK 17, Gradle 8.10.2 (~/tools), Android platform 34 (~/android-sdk), aapt2/apksigner/zipalign (apt). SDK lisansı kullanıcı onayıyla kabul edildi.
- Disk açıldı: Download'dan ggml-large-v3 ve eski loglar silindi (kullanıcı yetkisiyle). Boş alan ~3-4 GB; /sdcard ayrı kart değil, aynı bölüm.
- Boş "Merhaba, ben Sezai" APK'sı derlendi, Shizuku/rish ile kuruldu, ekranda doğrulandı.
- Ses altyapısı kararı: Android TTS/STT + KonusmaMotoru/DinlemeMotoru arayüzleri. Piper/espeak-ng GPL lisans riski nedeniyle ertelendi (satış öncesi lisans kontrolü şart). Google TTS varsayılan: 5 yerel Türkçe ses (ama, cfs, efu, mfm, tmc), cinsiyet API'de yok, kullanıcı kulakla belirleyecek. Samsung TTS: 1 kadın ses.
- Kod (app/app/src/main/java/com/sezai/assistant): SesArayuzleri, AndroidKonusma, AndroidDinleme, MainActivity (test ekranı: Konuş/Dinle/Sesi değiştir), KomutYorumlayici (Türkçe cümle -> komut), KomutCalistirici (saat, tarih, alarm kur/iptal, zamanlayıcı).
- Git deposu başlatıldı (yerel, uzak depo yok).

- `9b481a2` commitindeki saat ifadesi yorumlama iyileştirmesi ve test günlüğü düzeltmesi için derleme Ubuntu proot komutuyla doğrulandı; debug APK üretildi (çıkış kodu 0). Bu değişiklikler cihazda doğrulandı: 17 temel cümle beklenen sonucu verdi.

## Derleme nasıl yapılır
- Derleme araçları Termux içindeki Ubuntu proot ortamında kuruludur (bu ortam Termux'un içinde çalışır, başka platform değildir).
- Termux'tan `proot-distro login ubuntu -- bash -c "cd /root/sezai && ./build.sh"` debug APK derler (ilk derleme ~3 dk).
- `./build.sh kur` derler, Shizuku/rish ile telefona kurar ve açar.
- Ortam: `source ~/sezai/env.sh` (JDK 17, Gradle 8.10.2, aapt2 override).
- Telefona kurulum: tıklayarak kurulum çalışmadı; pm /sdcard'ı okuyamaz, bu yüzden dosya /data/local/tmp'ye kopyalanıp rish ile kurulur (build.sh bunu yapar). Shizuku kapalıysa önce Shizuku uygulamasından başlatılmalı. rish kopyası ~/rish, RISH_APPLICATION_ID=com.termux gerekir.
- Test: rish ile `am start ... --es yorumla "cümle"` (yalnızca yorumla), `--es komut "cümle"` (çalıştır), `logcat -d | grep Sezai`.
- İmza: şu an debug anahtarı. Release için Sezai'nin KENDİ anahtarı oluşturulacak (şifreyi kullanıcı belirler), apksigner verify zorunlu, yedek ~/sezai/yedek (git'e girmez).

## Sıradaki adım
1. ÖNCELİK: Karşılıklı sesli sohbet. Sohbet et -> Türkçe konuşma tanıma -> OpenAI Responses -> Türkçe seslendirme -> yeniden dinleme. Diğer özellikler sonraya bırakıldı.
2. Canlı API anahtarı henüz yok. Uygulamadaki Sohbet bağlantısı ekranından kullanıcının kendi anahtarı girilecek; anahtarı sohbetten isteme, APK'ya/repo veya günlüklere koyma. Gerçek API ve kullanıcının sesiyle uçtan uca deneme henüz yapılmadı.
3. TAMAMLANDI: Son debug APK derlendi, zipalign/apksigner doğrulandı ve pm install -r ile kuruldu. Altı sessiz sohbet testi, 20 yorumlama testi ve anahtar ekranı kontrolleri geçti. Canlı sesli görüşme anahtar ve kullanıcının gerçek konuşmasıyla henüz denenmedi.
4. Kullanıcı uyuyor: ses çıkaran test, gerçek arama, yeni alarm veya zamanlayıcı başlatma. Gereksiz soru sormadan bağımsız işleri sürdür.
5. Sohbet doğrulandıktan sonra ses eşlemesi, arama, müzik/radyo, YouTube ve diğer hedef özellikler ele alınacak.
6. Sezai'ye özel release imza anahtarı ve ticari dağıtım mimarisi sonra. Mevcut sohbet prototipi kişisel API anahtarıyla çalışır; geliştiricinin ortak API anahtarını mobil uygulamaya gömme.

## Bu geceki çalışma sınırları ve kullanıcı tercihleri
- 29 Ekim 2026 İstanbul saatiyle 06.00'da dur. Kota kullanımı herhangi bir pencerede %80'e ulaşırsa daha erken dur. `python3 scripts/calisma_kontrol.py` mevcut oturumun yalnızca rate_limits alanlarını kontrol eder; her ana adımdan önce çalıştır.
- Kota her ana adımda scripts/calisma_kontrol.py ile kontrol edildi; son kayıtta %40 / %6 idi, güncel olarak tekrar okunmalı.
- Kullanıcı gerekli becerileri kullanmaya izin verdi; şu aşamada OpenAI Docs kullanıldı, yeni paket veya beceri kurulmadı.
- Ekranı kapatan komut çalıştırma. Bu, uygulamanın ekranını kalıcı açık tutma talebi değil; FLAG_KEEP_SCREEN_ON kaldırıldı, son APK'da bulunmamalı.
- Kullanıcı açıkça Termux'un uyurken çalışması için önlem istedi: `termux-wake-lock` etkin, dumpsys power içinde termux:service-wakelock doğrulandı. com.termux deviceidle beyaz listesine eklendi (önceden yoktu). Standby bucket 10, RUN_ANY_IN_BACKGROUND varsayılan allow; bunlar değiştirilmedi. CPU kilidi pil tüketir; çalışma sonunda `termux-wake-unlock` ile bırakılmalı, geçici deviceidle istisnası kaldırılmalı. Android'in süreci hiç öldürmeyeceği garanti edilemez.

## Bilinen sorunlar
- Tıklayarak APK kurulumu başlamıyor (neden belirsiz); Shizuku gerekli, telefon yeniden başlayınca Shizuku durabilir.
- TTS beklemeden yorumlama günlüğü üretimi cihazda doğrulandı. rish uzun standart çıktılarda karakter kaybedebiliyor; yeni cihaz testi yalnızca test günlüklerini benzersiz geçici dosyadan okur ve kendi dosyasını temizler. Sistem günlüğünü silmez.
- DÜZELTİLDİ: "buçuk" kontrolünün geniş önek eşleşmesi geçersiz kelimeleri kabul ediyordu. "bir buçukluk saat zamanlayıcı kur" önceden 5400 saniye dönüyordu; artık Bilinmiyor. Geçerli çekimler ve normal komutlar cihazda doğrulandı.
- Android konuşma tanıma çoğu durumda internet ister; çevrimdışı ihtiyacı belirsiz.
- Disk dar (~3-4 GB boş). Derleme önbelleği ~1,1 GB.
- Pil: sürekli dinleme yok (mikrofon yalnızca butonla). WakeService gibi arka plan servisi eklenmedi; eklenirse pil ölçülmeli.
- Kadın/erkek ses eşlemesi henüz yok.

## 2026-10-29 cihaz doğrulaması
- Değişiklik öncesi kaynak yedeği: yedek/bucuk-duzeltme-oncesi-20261029-014545.tar.gz.
- KomutYorumlayici.kt içindeki buçuk eşleşmesi geçerli sözcüklerle sınırlandı.
- Yeni testler: test/cihaz_yorumla_test.py ve test/cihaz_yorumla_cumleler.txt (20 cümle; yalnızca yorumlar). Önceden var olan test/yorumla_test.sh ve test/yorumla_cumleler.txt değiştirilmedi.
- Ubuntu proot içinde ./build.sh, zipalign -c 4 ve apksigner verify başarılı; pm install -r sonucu Success. Ardından cihaz testi 20/20 başarılı.
- Shizuku/rish shell bağlantısı çalışıyor. Samsung Saat SET_ALARM ve SET_TIMER intentlerini karşılıyor. Gerçek alarm/zamanlayıcı testi henüz yapılmadı.
- Pil: yeni arka plan servisi veya sürekli dinleme eklenmedi; uyandırma kilidi alınmadı.
- Kullanıcı kalan işleri sırayla uygulayıp test ederek sürdürmemi istedi. Android Termux sürecini durdurursa kesintisiz çalışma garantisi yok.

## Sesli sohbet ve durum kutusu (29 Ekim 2026)
- Yedekler: yedek/durum-kutusu-oncesi-20261029.tar.gz, yedek/sesli-sohbet-oncesi-20261029.tar.gz.
- MainActivity: üstte zaman damgalı son çalışma bildirimi (debug), Sohbet et/Sohbeti bitir ve Sohbet bağlantısı ekranı. Sohbet sırasında telefon komutları kendiliğinden çalıştırılmaz. Uygulamadan çıkınca mikrofon, seslendirme ve API isteği durdurulur.
- AnahtarKasasi: Android Keystore AES-GCM; anahtar giriş ekranı FLAG_SECURE; APK'da/repoda açık anahtar yok. SohbetMotoru: HTTPS Responses API, gpt-4.1-mini, store=false, bellekte sınırlı sohbet bağlamı, süre sınırları ve iptal desteği. Ek kütüphane kurulmadı.
- AndroidDinleme artık kullanıcının konuşma metnini günlüğe yazmıyor. AndroidKonusma seslendirme hatasını sohbet ekranına bildirebiliyor.
- Debug test alıcıları android.permission.DUMP ile korunur; release manifestine girmez. test/sohbet_test.py gerçek API/mikrofon kullanmadan test eder; ayrı test anahtarı kullanır, kullanıcının anahtarına dokunmaz.
- Altı cihaz testi geçti: anahtar yokken ağ çağrısı yapılmaması; Android Keystore şifreleme/geri okuma; ikinci tur sohbet bağlamı; 401 ve 429 hataları; iptal edilen yanıtın gösterilmemesi.
- rish'i Ubuntu proot içinde çağırmak aralıklı Failed transaction (2147483646) ve çıktı kaybı üretti. Doğrudan Termux'tan rootfs/root/rish/rish çağrısı başarılı; cihaz testleri ve durum bildirme betiği bu yönteme geçirildi. Derleme hâlâ Ubuntu proot içinde yapılır.
- Gerçek alarm testi: 23:58 Sezai alarmının açık oluştuğu doğrulandı. Son kontrolde Sezai alarm anahtarı false (kapalı); kişisel alarmlara dokunulmadı. Uygulamanın iptal komutu soğuk TTS başlangıcı ve rish hataları nedeniyle tek başına güvenilir doğrulanmadı; şimdilik tamamlandı sayma. Yeni alarm oluşturma.
- Üst durum kutusu ve yeni sohbet düğmeleri cihaz arayüzünde görüldü. Android düğme yazılarını büyük harf gösterir; UI testleri harf büyüklüğünü dikkate almamalı.

## Son doğrulama ve devam bilgisi
- Son APK: /sdcard/Download/Sezai-debug.apk. Ubuntu proot ./build.sh başarılı, zipalign -c 4 ve apksigner verify başarılı, telefona kurulum Success. Yeni parola maskeleme ve sesli sohbet bitirme kodlarının DEX içinde bulunduğu da kontrol edildi.
- `python3 test/sohbet_test.py`: son cihaz turu 6/6 PASS. Her çalışmanın benzersiz kimliği kontrol edilir; eski rapor başarılı sayılmaz. `python3 test/cihaz_yorumla_test.py`: 20/20 PASS. Bu testler gerçek API çağrısı, alarm veya mikrofon başlatmaz.
- UI: Sohbet et, Sohbet bağlantısı ve üst durum alanı görüldü. Parola alanının password=true olduğu doğrulandı; boş Kaydet sonrası anahtarın saklanmadığı ayrıca kontrol edildi ve diyalog kapatıldı. Gerçek API anahtarı girilmedi.
- Bulunup düzeltilen UI hatası: setSingleLine çağrısı parola dönüşümünü bozuyordu. Sıra düzeltildi ve PasswordTransformationMethod açıkça ayarlandı. Anahtar ekranı ayrıca FLAG_SECURE kullanır.
- Sohbet içindeyken "sohbeti bitir/kapat", "görüşmeyi bitir", "konuşmayı bitir" cümleleri API'ye gitmeden oturumu bitirecek şekilde eklendi. Gerçek sesli deneme henüz yapılmadı.
- Cihaz test raporu aktarımı: run-as çıktısını doğrudan Download'a yönlendirmek boş dosya üretebiliyor. `run-as ... cat ... | /system/bin/cat > ...` yöntemi son turda başarılı oldu; test/sohbet_test.py bunu kullanır. Boş/eksik çıktı başarılı kabul edilmez.
- `scripts/gece_siniri.py` ayrı yerel süreçte çalışıyor; yalnızca bu gece 06.00 / %80 sınırını izler, geliştirme yapmaz. Sınırda calisma_durdu.json oluşturur, termux-wake-unlock çalıştırır ve geçici com.termux deviceidle istisnasını kaldırmayı dener; sonra durur. Android süreci öldürürse zamanlama garanti edilemez. Günlük: /data/data/com.termux/files/usr/tmp/sezai-gece-siniri.log; süreç kimliği /data/data/com.termux/files/usr/tmp/sezai-gece-siniri.lock içinde. Uygulama ekranını açık tutma ayarı eklenmedi.
- Gerçek konuşmayı doğrulamanın sonraki adımı: kullanıcı uyanınca uygulamadaki Sohbet bağlantısı alanına kendi OpenAI API anahtarını girer, Sohbet et ile mikrofon iznini verip konuşur. Anahtarı sohbete veya git'e yazma. Anahtar olmadığı için canlı bağlantı kurulmuş gibi rapor verme.

- Son ekran görsel olarak da kontrol edildi: başlık, üst durum kutusu, Sohbet et, Sohbet bağlantısı ve mevcut düğmeler okunuyor; son ekran /sdcard/Download/sezai-son-test.png.
- Kullanıcının ilk verdiği /sdcard/Download/ekran_son.png dosyası değiştirilmedi. Başlangıçtaki test/yorumla_test.sh ve test/yorumla_cumleler.txt dosyaları değiştirilmedi ve bu değişikliklerin commitine eklenmedi.
- OpenAI bağlantısının dayanağı: https://developers.openai.com/api/docs/guides/conversation-state ve https://developers.openai.com/api/docs/models/gpt-4.1-mini (bu oturumda açılıp kontrol edildi).

## 05.32 sonrası devam ve dinleme düzeltmesi
- Kullanıcı erken durulduğunu belirtti; bağımsız sohbet geliştirmesi ve sessiz testlerle devam ediliyor. Sonra 06.00 sınırını açıkça kaldırdı; yalnızca %80 kota sınırı kaldı. Eski saat gözcüsü kendi PID'si doğrulanarak durduruldu ve kota gözcüsü olarak yeniden başlatıldı.
- AndroidDinleme'de eski RecognitionListener olayları yeni tanıyıcıyı kapatabiliyordu. Olaylar oturum kimliğiyle filtreleniyor; kapatma öncesinde etkin tanıyıcı referansı temizleniyor. Başlatma sırasında izin hatası anlaşılır şekilde işleniyor.
- Tanıma oturumu küçük bir arayüzle ayrıldı; debug testleri mikrofonu açmadan gecikmiş/eski/çift olayları canlandırabiliyor.
- SohbetKomutlari ile hitap ve noktalama içeren bitirme istekleri tanınıyor; normal konuşmadaki "sohbeti bitir demedim" benzeri cümleler oturumu kapatmıyor.
- Yeni debug APK derlendi; zipalign/apksigner doğrulandı; pm install -r Success. `python3 test/dinleme_test.py`: 6/6 PASS (mikrofon, ağ, gerçek anahtar kullanılmadı).
- Kullanıcı ücretsiz güçlü/hızlı sağlayıcıları sordu. Groq, Gemini, OpenRouter, ModelScope, Cloudflare, Hugging Face ve Cerebras güncel resmî kaynaklarla karşılaştırıldı; yanıt verildi. Şimdi Groq/Gemini/OpenRouter'ın uygulamada seçilebilir olması hazırlanacak.
- Beş saatlik kota penceresi yenilendi; son kontrol %6 / haftalık %8 idi. Sınır her aşamada yeniden okunur.

## Ücretsiz kotalı sağlayıcılar (05.55 sonrası)
- Groq, Gemini ve OpenRouter seçim ekranı eklendi; OpenAI korunuyor. Her sağlayıcının kasası ayrı, adresler sabit HTTPS, otomatik sağlayıcı veya ücretli modele geçiş yok. Açıklamalarda kota ve veri koşulları gösterilir.
- Yeni motor yalnızca seçilen hizmete uygun istek gönderir. Mikrofon izni anahtar kontrolünden sonra istenir; izin reddinde açıklama gösterilir.
- Debug APK derlendi, zipalign/apksigner başarılı, telefona kurulum Success. Sohbet testleri 11/11 ve dinleme testleri 6/6 PASS. Testler sahte ağ ve ayrı test anahtarlarıyla çalışır.
- Bağlantı ekranında Groq varsayılanı ve password=true görüldü. Seçim değiştirme/iptal UI testi, kullanıcı telefonu kullanırken başka ekran öne geldiği için tamamlanmadı. Otomatik dokunuşlar durduruldu. Gerçek API denenmedi.
- Resmî kaynakların karşılaştırması UCRETSIZ_AI_SECENEKLERI.md içinde.
- Kullanıcı yazarak sohbet de istedi; sıradaki değişiklik mikrofon ve seslendirme kullanmadan metin gönderme/yanıt gösterme.

## Yazılı sohbet çalışması
- MainActivity'ye çok satırlı mesaj alanı ve Gönder düğmesi eklendi. Aynı sohbet motoru/sağlayıcı bellekteki bağlamı korur; metin modunda mikrofon izni istenmez ve TTS çağrılmaz.
- Bekleyen istekte çift gönderim engellenir. Hata halinde taslak korunur; yanıt geldiğinde kullanıcı yeni metin yazmışsa silinmez. Bitir/uygulamadan çıkış, isteği ve sohbet bağlamını kapatır.
- Kaynak yedeği: yedek/yazili-sohbet-oncesi-20261029.tar.gz.
- Yeni APK Ubuntu proot içinde derlendi; zipalign/apksigner doğrulandı, kurulum Success. Son APK üzerinde ortak sohbet testleri 11/11 PASS. Yazı alanı ve Gönder düğmesi cihaz arayüzünde görüldü; otomatik dokunuş yapılmadı. İlk UI kontrolü hint metnini XML text alanında aradığı için başarısızdı; gerçek EditText sınıfı ve Gönder düğmesi üzerinden doğrulandı. Canlı API anahtarı olmadığı için gerçek yazışma henüz denenmedi.

## Sohbet bağlamı düzeltmesi (Kayseri → konumu → nüfusu)
- Kullanıcı ardışık kısa mesajların önceki konuya bağlanmadığını bildirdi. Kodda doğrulanan iki kayıp yolu: onStop geçmişi kapatıyordu; sesli/yazılı geçiş yeni motor oluşturuyordu. Aynı ekranda aralıksız metin akışında geçmiş zaten gönderiliyordu; modelin canlı yanıtı henüz incelenmedi.
- MainActivity: geçici duraklatma isteği/mikrofonu/TTS'i durdurur, aynı motor ve geçmiş kalır. Sesli/yazılı giriş ortak motoru kullanır. Yeni sohbet düğmesi açıkça geçmişi sıfırlar. Dinlemeyi durdurmak veya sesli bitirme sözü, konu geçmişini silmez.
- Ekran dönüşünde motor onRetainNonConfigurationInstance ile devredilir, eski Activity geri çağrıları iptal edilir. Son görünen yanıt/taslak Bundle ile geri yüklenir. Geçmiş diske kaydedilmez; Android uygulama sürecini tamamen sonlandırırsa korunmaz. Sağlayıcı değiştirmek yeni motor başlatır, eski sağlayıcı geçmişi diğer hizmete taşınmaz.
- SohbetMotoru talimatı kısa takip mesajlarını önceki konuya bağlayacak biçimde netleştirildi. Dört sağlayıcı için Kayseri nerede? → konumu → nüfusu zinciri, duraklatma ve açık sıfırlama testleri eklendi. Sahte ağ testleri modelin gerçek cevabını/gerçek nüfus bilgisini doğrulamaz, istek geçmişini doğrular.
- Yedek: yedek/sohbet-baglami-oncesi-20261029.tar.gz. Ubuntu proot derlemesi, zipalign ve apksigner başarılı. APK son üç mesajlık test kodunu içeriyor; pm install -r Success. Cihazda test/sohbet_test.py 15/15 PASS. Canlı model anlamlandırması bu sahte ağ testinin kapsamı dışındadır.

## Bağlam zinciri/verimlilik görüşmesi
- Kullanıcı her turda geçmişin tekrar gönderilmesini sorguladı. Mevcut uygulama en son 8 mesajı (4 tur) tekrar gönderir; sınırsız geçmiş aktarımı yoktur. Henüz kimlikle sunucu zincirine veya özet belleğe geçilmedi.
- Resmî doküman: OpenAI previous_response_id ile devamı destekler; önceki bağlam tokenları yine giriş tokenı olarak faturalandırılır. Sunucuda saklama koşulları ayrıca ele alınmalı, mevcut store=false kendiliğinden değiştirilmedi. Kaynak: https://developers.openai.com/api/docs/guides/conversation-state
- Groq Responses belgesindeki Unsupported Features bölümü previous_response_id ve store desteklenmediğini açıkça belirtiyor; girişteki genel stateful ifadesine güvenip geçiş yapma. Kaynak: https://console.groq.com/docs/responses-api
- Tasarım seçeneği: aktif konu + gerektiğinde güncellenen kısa özet + son mesajlar. Bu yalnızca öneri; henüz uygulanmadı. Konu çözümleme genel olmalı, Kayseri'ye özel kelime kuralıyla belirti gizlenmemeli. Belirsiz takip mesajlarında açıklama istenir.
