# Kalan işler ve sorunlar

Her çalışma başlangıcında CLAUDE.md, DURUM.md ve TEST_DURUMU.md okunur. Tamamlanan iş, kanıt ve kalan riskle bu listeden güncel duruma taşınır.

## Öncelikli

- [ ] TTS arasında “dur/Sezai” ile söz alma: Cihaz ölçümü yapıldı: yerel Android tanıma yok; standart tanıma TTS odağını kesiyor. Vosk debug motoru cihazda açıldı; TTS+AudioRecord odağı kesmeden çalıştı, gerçek arka planda kaynak bırakma geçti. Son birlikte testinde çağrı algılanmadı (SURE_DOLDU); kullanıcı çağrıyı söylediğini ama durmadığını doğruladı. Son sessiz-asistan dinle testinde Sezai algılandı; TTS ile birlikte/yankı ayırma hâlâ başarısız. Kota%80 nedeniyle çalışma durdu. Yeni tanı APK’sı çağrı-herhangi-yerde ve doygunluk sayaçları içeriyor. Kullanıcının dur/Sezai sesi, güvenilir yankı ayrımı, devam isteği, pil ölçümü ve ana akışa bağlama gerekli (SOZ_KESME_TASARIMI.md). Şu an TTS sırasında mikrofon kapalı; bu hedef tamamlanmış değil.

- [x] Özgün Canvas karakteri kaynak/olay bağlantıları, derleme-imza-kurulum ve altı durağan ifade doğrulandı; eski S simgesi tek sabitle korunur. Kurallar KARAKTER_TASARIMI.md içinde.
- [x] Karakter canlı View kontrolü 6/6, ana ekran yerleşimi ve gerçek TTS sırasında farklı ağız açıklıkları 29 Ekim doğrulandı. İlk Dozing/FAIL geçmişi korundu.
- [ ] Karakterin doğal mikrofon → model → seslendirme akışındaki durumları, fiziksel kilit/arka plan ve gerçek pil tüketimi kabulü. Debug ekran/lifecycle sinyal testi fiziksel kabul yerine geçmez.

- [ ] Kullanıcı Android erişilebilirlik ayarından Sezai ekran yardımcısını açacak. Yeni APK kurulu; 9/9 politika/sentetik test ve izin kapalı ayar UI doğrulandı.
- [ ] Erişim açılınca gerçek açık ekran metnini alma/okuma/yorumlama ve Termux metin görünürlüğünü doğrula; kilit/uygulama değişimi/görüşme kesintisini test et.
- [ ] Telefon genelinde sesli uyandırma ve kullanıcı konuşarak ekranı inceletme/telefonu yönetme: bu aşamada yalnız ekran erişimi temeli eklendi. Tam eller serbest kullanım henüz yok.

- [x] Groq web araştırması bağlantısı: citation_options kaynaklı HTTP 400 düzeltildi. 29 Ekim canlı sabit sorgu kaynak adresiyle yanıt verdi; içerik resmî sayfayla karşılaştırıldı.
- [ ] Web kullanımının hesaba özel maliyet/kota koşulları ve farklı sorularda kaynak kalitesi; diğer sağlayıcıların web desteği.

- [ ] Görüşme önceliği gerçek kabul testi: APK kuruldu, sessiz testler 8/8 geçti. Gerçek telefon ve WhatsApp çağrısında doğrula. Otomatik mikrofon açılmamalı.
- [x] Modern ekran düğme hizalama kontrolü: 29 Ekim cihaz testi ve görselde aynı üst hiza doğrulandı.
- [x] Alt ayar düğmelerinin tam görünürlüğü: 29 Ekim tek kaydırma sonrası cihaz/görsel kontrolünde doğrulandı; düzen değişikliği gerekmedi.
- [ ] Ekran testinin yeni başlangıç ekranı korumasını uygun boş oturumda cihazda doğrula; aktif dinleme/sohbette otomatik kaydırma yapılmamalı.
- [ ] Gerçek sağlayıcıyla Kayseri → konumu → nüfusu sohbeti. Sahte ağ testleri anlam doğrulaması değildir.
- [ ] Sesli Türkçe konuşma → model → seslendirme → tekrar dinleme uçtan uca testi.
- [ ] Sağlayıcı seçimi/değiştirme/iptal ekranının güvenli cihaz kontrolü; kullanıcı telefonu kullanırken otomatik dokunma yapma.

## Bilinen sınırlar ve çözüm yolu

- Bağlam artık geçici duraklatma ve giriş yöntemi değişiminde korunur. Android uygulama sürecini öldürürse geçmiş kaybolur; kalıcı şifreli geçmiş gereksinimini ayrıca tasarla.
- Şimdiki API istekleri son 4 turu yeniden taşır. Verimlilik için aktif konu + kısa özet + yakın mesajlar değerlendirilmeli; yanlış özet ve belirsiz göndermeler için test gerekir. Henüz uygulanmadı.
- Sunucu yanıt kimliği her sağlayıcıda aynı değil. OpenAI zincirde önceki tokenları yine ücretlendirir; Groq belgesinde previous_response_id desteklenmiyor. store=false izinsiz değiştirilmemeli.
- Telefon/WhatsApp kesintileri gerçek çağrıyla denenmedi. Ses odağı ve Android 12+ ses modu olayları kullanılacak; eski Android sürümlerinin olay desteği daha sınırlı.
- Tek cevap ekranı bütün konuşmayı göstermiyor; kaydırılabilir konuşma dökümü ve uzun metin düzeni ayrıca iyileştirilecek.
- Tıklayarak APK kurulumu güvenilir değil; yapılandırılmış Shizuku/rish kurulumu çalışıyor. Yeniden başlatmada Shizuku bağlantısını kontrol et.
- Disk dar; yeni büyük model/paket indirmeden önce boş alanı ölç.

## Ana hedefler — kapsamdan çıkarılmadı

- [ ] Sezai'ye seslenerek uyandırma ve telefon genelinde eller serbest kullanım. Yerel uyandırma sözcüğü, gerekli Android rol/izinleri ve ölçülmüş pil bütçesi tasarlanacak; yalnız uygulama görünür, odaklı ve boşta iken yerel uyanma dinlemesi var; uygulama kapalıyken dinleme yok.
  - 29 Ekim kullanıcı isteği: kapalıyken de "Sezai" deyince devreye girsin. Kullanıcı uygulama açıkken uyanma seçeneğini seçti; kapalıyken uyanma ayrıca tasarlanacak.
- [ ] Android'i sesle yönetme: uygulama açma, ses ayarı ve desteklenen telefon işlemleri; gereken yerde erişilebilirlik/varsayılan asistan rolü. Kullanıcı izinlerini aşmadan, yapılan işlemi doğrulayarak ilerle.
- [ ] Araç Bluetooth'u ile sesli yardım ve navigasyon: hedef seçme, rota, dönüş talimatları, yeniden yönlendirme ve görüşmelere öncelik.
- [ ] Araç güvenliği: hız, trafik/kavşak, hız sınırı/okul bölgesi, hız kamerası/koridoru; veri kaynağı, bölgesel kurallar ve doğruluk kontrolü gerekir.
- [ ] Yakındaki nöbetçi eczane, hastane, benzin istasyonu; konum izni ve güncel kaynaklarla doğrulama.

## Sohbetten sonra

- [ ] Kadın/erkek Türkçe sesleri kullanıcının dinleyerek eşlemesi.
- [ ] Alarm iptal komutunun gerçek kabul testi; yeni alarmı izinsiz/sessiz gece çalışmasında oluşturma.
- [ ] Arama, mesaj, müzik/radyo, YouTube, kamera/ekran, araç modu: CLAUDE.md sırasına ve kullanıcı önceliğine göre küçük aşamalar.
- [ ] Ticari sürüm için Sezai'ye özel release imzası, güvenli dağıtım ve sağlayıcı maliyet modeli.

## Çalışma sınırı

Saat 06.00 sınırı kaldırıldı. Her ana adımda `python3 scripts/calisma_kontrol.py`; herhangi bir kota penceresi %80'e ulaşırsa dur. Yeni beceri/paket ancak iş için gerekli olduğunda eklenir.

## 29 Ekim 2026 — uygulama ikonu
- Kullanıcı uygulama ikonunun basit Android simgesi olduğunu bildirdi. Manifestte android:icon tanımı ve launcher kaynağı yoktu.
- Yeşil (#176B52) zemin, krem S çizgisi içeren yerel vector ve adaptive ikon eklendi. Manifest icon/roundIcon aynı kaynağa bağlı. Ek kütüphane, animasyon, servis veya pil yükü yok.
- Değişen kaynaklar: AndroidManifest.xml; res/drawable/ikon_onplan.xml; res/mipmap-anydpi-v26/ic_launcher.xml; res/values/ikon.xml. Yedek: yedek/ikon-oncesi-20261029.tar.gz.
- Ubuntu proot ./build.sh başarılı. İlk hizalama/imza komut zinciri çıkış1 verdi; ara APK imza kontrolünde Missing META-INF/MANIFEST.MF görüldü. Ardından açık apksigner sign Signed döndü; zipalign -c 4 ve apksigner verify başarılı (v2/v3). APK manifestindeki icon/roundIcon kaynak kimlikleri aapt ile doğrulandı.
- /sdcard/Download/Sezai-debug.apk güncellendi, Shizuku/rish pm install -r: Success; lastUpdateTime=2026-10-29 19:28:27. Ana ekran ikonunun gerçek görünümü henüz görsel olarak doğrulanmadı. Ses/mikrofon testi yapılmadı; değişiklik yalnız ikon kaynaklarıdır.

## 29 Ekim 2026 — ikonun cihazda görsel kontrolü
- Kullanıcının ~ devam talimatıyla kurulu ikon doğrulandı. Shizuku/rish bağlantısı çalışıyor.
- Uygulama bilgileri ekranı package:com.sezai.assistant ile açıldı; /sdcard/Download/sezai-ikon-ayarlar.png ekran görüntüsü incelendi. Sezai adı yanında yeşil zemin ve krem S doğru, kırpılmadan görünüyor.
- Ana ekran görüntüsü girişiminde başlatıcı yerine başka uygulama görünüyordu; bu nedenle başlatıcı görünümü doğrulandı sayılmadı. Uygulama bilgileri ekranındaki gerçek Android adaptive ikon çizimi doğrulandı.
- Uygulama kodu/APK değişmedi; yeniden derleme veya ses testi yapılmadı. Bu adımda yalnız DURUM.md, KALAN_ISLER.md ve TEST_DURUMU.md güncellendi. Yeni pil yükü yok.

## 29 Ekim — uyanma düzeltmesi sonrası kalan doğrulama
- [x] Eksik gorunur=true ve yaşam döngüsü bağlantısı düzeltildi; MainActivity.kt ve SezaiUyanma.kt derlendi, imza/hizalama doğrulandı ve kuruldu.
- [x] İki gerçek ön plan/arka plan turunda uyanma motoru başlama/kaynak bırakma ve appops mikrofon açılış/kapanış doğrulandı. İlk eksik çıktı denemeleri başarısız sayıldı; dosya üzerinden son ölçüm geçti.
- [x] Aynı APK'da 38 sessiz regresyon geçti: dinleme9, sohbet21 (sahte ağ), görüşme8. Ayrıntılar TEST_DURUMU.md son kaydında.
- [ ] Kullanıcı hazır olduğunu bildirince gerçek Sezai çağrısı ve sonraki istek denenecek. Henüz insan sesi tanıma başarısı yok.
- [ ] Fiziksel kilit, yanlış tetikleme ve gerçek pil maliyeti ölçülecek. Ön planda mikrofon+Vosk artık gerçekten çalışıyor; arka planda kapandığı doğrulandı. TTS sırasında söz kesme ve kapalı uygulamadan uyanma ayrı açık işlerdir.
- Önceki 81e8a0d motor kabulü geçerli kanıt sayılmaz: o kaynakta gorunur bayrağı hiç true olmuyordu. Son iki tur doğrulama bu düzeltmeye aittir.


## 29 Ekim 2026 23.37 — devam ve sessiz regresyon
- Mevcut APK üzerinde dinleme9 + sohbet21 (sahte ağ) + görüşme8 =38 sessiz kontrol yeniden geçti. Uygulama kaynağı değişmedi.
- Gerçek insan sesiyle uygulama içi uyanma için hazır oluş yanıtı bekleniyor; henüz başarılı sayılmadı. TTS sırasında söz kesme, yanlış tetikleme, fiziksel kilit ve pil ölçümü bekliyor.


## 29 Ekim 23.39 — uyanma tetikleme kanıtı
- Gerçek mikrofon yolunda Sezai algılama ve MainActivity uyanma callback kaydı görüldü; Termux’a dönüşte mikrofon kapandı. Kullanıcının söylediği an/metin doğrulanmadı, yanlış tetikleme dışlanmadı.
- Sonraki kabul: kullanıcının gözlediği dinleme davranışı, uyanma sonrası isteğin ilk kelimeleri ve model yanıtı; yanlış tetikleme, pil ve kilit. TTS sırasında söz kesme henüz yok.


## 29 Ekim23.50 — söz kesme hâlâ açık
- Yeni birlikte testi1793306993313423058 SURE_DOLDU/çıkış1; kaynaklar bırakıldı. TTS+mikrofon çalışıyor, çağrı algılanmadı. İnsan çağrısı teyidi ve düşük-ses karşılaştırması gerekli; üretim değişmedi. Önceki uyanma ardından üç STT sonucu kaydı var, içerik/yanıt doğruluğu doğrulanmadı.


## 29 Ekim23.51 — yeni insan testi
- Kullanıcı önceki turda konuşmadığını açıkladı; önceki tur insan çağrısını kaçırma kanıtı değildir. Yeni1793307067533502144 birlikte testi de süre doldu/çıkış1; kullanıcı işe yaramadı dedi. Doygun örnek0; yalnız kırpılma açıklaması yeterli değil. Düşük hoparlör karşılaştırması ve motor/yankı teşhisi açık.


## 29 Ekim23.54 — düşük ses sonucu
- STREAM_MUSIC4→2 karşılaştırması1793307203533605321 süre doldu/çıkış1; eski4 doğrulanarak geri yüklendi. Söz kesme çözülmedi. TTS akışı/ses yönlendirmesi ve kayıt kaynağı-AEC oturumu incelenecek; düşük medya ayarından kesin yankı nedeni çıkarma.


## 29 Ekim23.58 — ses karşılaştırması düzeltmesi
- Yeni tanı APK derlendi/imzalandı/kuruldu; gerçek arka plan testi1793307469992191053 geçti. Asistanın volumeControlStream eşlemesi11 ve seviyesi15/15. Önceki medya3 üzerinde4→2 karşılaştırması TTS kısma kanıtı değildir.
- Sonraki kullanıcı testi doğru akış11 üzerinde geçici azaltma ve eski değeri geri yükleme ile yapılmalı. Henüz TTS üzerinde söz kesme başarısı yok. Tanı süre/cihaz türü alanları debug raporunda hazır.


## 30 Ekim00.01 — düşük asistan sesi henüz denenemedi
- Akış11 set5 komutu yine15 döndürdü; yeni tur1793307677455105401 düşük ses karşılaştırması değildir. Süre doldu/çıkış1,kaynaklar bırakıldı,eski15 doğrulandı.
- Sonraki teknik adım: debug cümlesine TTS PARAM_VOLUME uygulama; üretim sesini koru. Shell komutunun başarılı çıkışına güvenip sesin değiştiğini varsayma.


## 30 Ekim00.07 — doğrudan TTS katsayısı
- Cümleye özel KEY_PARAM_VOLUME ve debug --tts-genlik eklendi; derleme/imza/kurulum tamam. Normal çağrılar null parametreyle önceki davranışı korur.
- Katsayı0.2 turu1793307976850923985 süre doldu/çıkış1; tepe10950,doygun0,kaynaklar bırakıldı. Aynı APK38 sessiz regresyon geçti. İnsan çağrısı teyidi ve eş koşullarda sessiz-asistan karşılaştırması gerekli; söz kesme tamamlanmadı.


## Kullanıcının son0.2 denemesi teyidi
- Kullanıcı hem dur hem Sezai söyledi; son1793307976850923985 turu gerçek çağrıyı kaçırdı, söz kesme kabulü başarısız. Aynı düşük-ses denemesini yeni kanıt olmadan tekrarlama.
- Sonraki teknik ayrıştırma: debug kayıt kaynağı/AEC etkisi, aynı mesafede sessiz-asistan kontrolü. Bastırma/yankı/model olasılıkları henüz kesin kök neden değildir.


## 30 Ekim — araştırma sonrası yol
- Kaynaklı rapor: SOZ_KESME_ARASTIRMASI.md. Tekrarlanan düşük-ses testleri yerine hoparlör referanslı yerel AEC + temiz seste konuşma başlangıcı + çıkış kuyruğunu kesme prototipi değerlendirilecek.
- [ ] İlk küçük debug doğrulaması: Android TTS onAudioAvailable/onBeginSynthesis PCM biçim ve parça akışı; ham ses diske/ağa gitmez.
- [ ] WebRTC APM bağımsız ARM64/Termux derleme/bağımlılık, lisans/boyut ve pil uygunluğu; ardından senkron referanslı kayıt/çalma prototipi. Native paket henüz eklenmedi.
- [ ] Türkçe çağrı, kendi sesinden yanlış tetikleme, devam isteğinin ilk kelimeleri, bağlam, görüşme/kilit ve ölçülmüş enerji kabulü. Araştırma başarısı gerçek sesli başarı değildir.


## 30 Ekim00.37 — ilk referans adımı geçti
- [x] TTS onAudioAvailable/onBeginSynthesis: son APK mikrofonsuz katsayı0 test1793309848188549596 PASS;24kHz mono PCM16,132 parça132624 bayt. Ham ses tutulmadı. Aynı APK38 sessiz regresyon PASS.
- [ ] Yerel AEC bağımlılık/ARM64 uyumu ve kontrollü çalma-referans senkronu. Termux clang Android24 hedefli çalışıyor; NDK/cmake/ninja bulunmadı, paket kurulmadı. TTS24kHz/mikrofon16kHz uyumu gerekir. Söz kesme henüz tamamlanmadı.


## Sessiz bekleme ve kilit araştırması
- Kaynaklı rapor SESSIZ_DINLEME_KILIT_PIL.md. Düşük güç yalnız tasarım önerisi; mevcut Vosk pil maliyeti ölçülmedi.
- [ ] Ön planda söz kesme sonrası hafif Türkçe bekleme/enerji karşılaştırması.
- [ ] Açık kullanıcı başlatması ve bildirimli microphone FGS için ayrı tasarım; gerçek fiziksel kilit/uzun süre/izin-görüşme-kapatma kabulü. Mevcut kod kilitte dinlemeyi bırakır.
- [ ] SoundTrigger/varsayılan asistan yolu için gerçek donanım ve Türkçe model erişimi araştırması. Sistem servisinin varlığı yeterli kanıt değildir; rol değiştirilmedi.


## 30 Ekim00.47 — bağımsız AEC hazır
- [x] SpeexDSP1.2.1 yerel AndroidARM64 statik derleme ve kontrollü yapay yankı testi geçti; son test1793310420046423805. Tekrarlama python3 scripts/speex_yerel_dene.py, kaynak/lisans/kabul kaydı SPEEX_DENEY_KAYDI.md.
- [ ] Debug native köprü/APK yükleme ve kontrollü gerçek çalma/mikrofon referans hattı. CLI ikilisi Termux RUNPATH içerir; Android uygulama namespace uyumu ayrıca doğrulanmalı.
- [ ] Gerçek TTS kendi-sesinden tetikleme, dur/Sezai, ilk kelime/bağlam ve pil kabulü. Yapay58dB sonucu gerçek oda veya Türkçe başarı garantisi değildir.


## 30 Ekim00.57 — APK JNI doğrulandı
- [x] Debug APK native yükleme ve yapay AEC testi1793310897691974925 PASS;56KB kütüphane,özelRUNPATH yok,lisans/derleme kayıtları APK içinde. Aynı APK38 sessiz regresyon PASS.
- Derleme ön koşulu: Termux python3 scripts/speex_android_derle.py; sonra proot ./build.sh. preDebugBuild eski/eksik native çıktıyı reddeder.
- [ ] Gerçek mikrofon/kontrollü AudioTrack referans hattı ve örnekleme/çalma gecikmesi/genlik eşlemesi. JNI yapay başarı gerçek TTS üzerinde kesme değildir; insan çağrısı/yanlış tetikleme/devam isteği/pil/kilit açık.


## 30 Ekim05.27 — gerçek hat prototipi/ekran engeli
- [x] JNI kare/hız/kapatma ve24kHz yapay ikinci sinyal testi1793327159043386594 PASS; aynı APK38 sessiz regresyon geçti.
- [ ] Ekran-açık gerçek kayıt/çalma/senkron kabulü: ilk1793326719234322710 görünürlük kaybı; cihazDozing. Ön plan testi artık kapalı ekranda başlatılmaz. Arka plan testi gerçek mikrofon/native başlangıcını bekler; son koruma cihazda bekliyor.
- [ ]24→16kHz ses dönüşümü, çağrı ve devam isteği; gerçek yankı/insan/pil/kilit kabulü. GERCEK_AEC_HATTI.md ayrıntılı kayıt.
