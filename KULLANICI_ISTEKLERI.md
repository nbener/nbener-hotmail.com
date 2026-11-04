# Kullanıcı istekleri — kalıcı ana liste (her oturumda oku, her işte güncelle)

Kullanıcı talimatı (31 Ekim 2026): İstediği HER ŞEY buraya ve KALAN_ISLER.md'ye yazılır, tekrar anlatması gerekmez. İstekler tek tek, parça parça yapılır; bir seferde bitmiyorsa her seferinde bir parça eklenir. Eksik yetenekleri ajan kendisi bulur ve ekler. Kaynak olarak açık kaynak projeler, GitHub ve developer.android.com taranır (lisansa dikkat: GPL kod gömülmez, bkz. ACIK_KAYNAK_ARASTIRMASI.md). Doğruluk kuralı geçerli: yapılmayan iş yapıldı denmez.

Durum: [x] cihazda doğrulandı · [~] kodlandı/kurulu, kullanıcı veya cihaz testi bekliyor · [ ] yapılmadı · [?] karar/bilgi gerekli

## Sesli konuşma ve uyanma
- [x] Ekran kilitli ve kapalıyken "Sezai" deyince uyan, dinle, sohbet et, takip sorularına cevap ver (31 Ekim, olay günlüğüyle doğrulandı; açma/kapama düğmesi "Kilitliyken de dinle").
- [ ] "Hey Sezai" söyleyişi (şu an yalnız "Sezai" sözü; "hey" tanınmayabilir). Uyanma sözcüğü seçenekleri ve yanlış tetikleme ölçümü.
- [~] Konuşurken "dur" / "Sezai" deyince kes (söz kesme v1: yalnız arka plan servisi, Android AEC + serbest Vosk; test bekliyor). Üretimde uygulama içi akışta YOK.
- [ ] Söz kesme v2: ChatGPT/Claude sesli görüşmesi gibi doğal, çift yönlü konuşma. Yüksek sesli/yakın konuşma gelince dinle, konuyla ilgiliyse anla ve tepki ver, alakasızsa anlatmaya devam et, sesi tamamen kapatma (cümle sınırında duraklat/devam). Aday: Silero VAD (MIT, doğrulanmadı).
- [x] Seçilen ses: 11 numara (tr-tr-x-tmc-network, erkek, ağ gerektirir). [ ] Çevrimdışı güçlü erkek ses alternatifi; internetsizken geri dönüş.
- [x] Konuşma sesi çubuğu (TTS düzeyi); medya sesi %60 korunur, ses düşürme istenmedi.
- [?] Konuşmacıya uyum: çocuk/yetişkin, kadın/erkek sesine göre dil ve ses. Teknik olarak mümkün, güvenilirlik sınırlı, cinsiyet varsayımı riskli; önerilen: sözlü beyan/ayar. Karar bekliyor.

## Zekâ ve kişilik
- [ ] Sesli kopilot: zeki, anlayışlı, ileriyi gören, niyeti ve bağlamı kavrayan, geçmişe bakıp konu bütünlüğü kuran, ne ve niçin yaptığını gerektiğinde açıklayan, insansı. Sistem istemi, aktif konu+özet bağlamı, kalıcı (şifreli) geçmiş.
- [ ] Mod ve yetenek mimarisi: hangi moda geçeceğini bilen (genel/araç/gece/çocuk…), tüm sesli komutlara yanıt veren tek yetenek kaydı. Niyet tanıma ile yürütme ayrı katman.
- [ ] Kullanıcı tekrar anlatmak zorunda kalmasın: hedefi ve nedeni çıkar, gerekli yetenekleri eksiksiz ekle.

## Telefon yetenekleri
- [x] İzinler verildi (31 Ekim, doğrulandı): konum, mikrofon, bildirim, arama, telefon durumu, kişiler, SMS, arama kaydı, kamera, Bluetooth. Yeni özellik için gereken yeni izin eklenir, kullanıcıya bildirilir.
- [~] Konum: "neredeyim" (kilitliyken servis de yerelde çözer; test bekliyor).
- [~] Kamera: "kamerayı aç ve resim çek / ne görüyorsun" → arka kamera (ön belirtilirse ön), tek kare, görmeyen birine yardım eder gibi sesli yorum, hiçbir şey sorulmadan. Groq görsel modeli adı doğrulanmadı (qwen/qwen3.8-27b), ayarlanabilir. Kilitliyken kamera erişimi cihazda denenmedi.
- [ ] Sürekli/aralıklı kamera anlatımı (yürürken çevreyi tarif).
- [ ] Sesli arama ve telefon görüşmesi; mesaj okuma/yazma/gönderme; yeni mesaj bildirimi sesle (kilitliyken kişisel veri okuma kararı: okunmaz, kullanıcı değiştirirse güncelle).
- [ ] Telefon numarası/kişi bilgisi sorguları ("kimi aradım", "numaram ne").
- [ ] Alarm/zamanlayıcı (kodlu, arka plandan Intent denenmedi), hatırlatıcı, takvim.
- [ ] Müzik, internet radyosu, YouTube arama (hepsi sesle).
- [ ] Hava durumu, güncel haber/manşet, güvenilir web araştırması, din ve dini rüya bilgisi.
- [ ] Ekran analizi ve telefonu sesle yönetme (uygulama aç, ses/parlaklık ayarı); erişilebilirlik izni kullanıcıda.
- [ ] Wi-Fi/Bluetooth/fener/rahatsız etme gibi sistem ayarlarını sesle yönet.

## Araç kılavuzu (A'dan Z'ye, akıllı araç rehberi)
- [ ] Araç algılama: "arabadayım" sözü + araç Bluetooth'u (BluetoothClass) + GPS hız artışı; GPS kapalıysa denetle/açtır; yalnız araç modunda sürekli konum.
- [ ] Hedefi sesle söyleyince Google Haritalar navigasyonunu başlat (google.navigation:q=). Arka plandan/kilitliyken başlatma kısıtı denenmedi.
- [ ] Sezai katmanı: anlık hız (GPS), hız sınırı (OSM Overpass), hız aşımı uyarısı, okul bölgesi, hız kamerası/koridoru (veri kaynağı ve Türkiye kapsamı araştırılacak), dönemeç/kavşak/şerit, yoğun trafik uyarısı — sesle, araç Bluetooth'u üzerinden.
- [ ] En yakın nöbetçi eczane, hastane, benzin istasyonu (sesle, konuma göre).
- [ ] Görüşmelere öncelik: telefon çalarken navigasyon sesi kısılır/susar.

## Pil, güvenlik, dayanıklılık
- [ ] Pil ölçümü (30 dk temiz gözlem), yanlış tetikleme oranı, Samsung pil optimizasyonu, yeniden başlatma sonrası davranış, görüşme sırasında davranış.
- [ ] Kilitliyken kişisel veri koruması; isteğe bağlı PIN/ses doğrulama.

## Ajan önerileri (kullanıcının "eksikleri sen bul" talimatıyla)
- [ ] "Neler yapabilirsin?" sesli yardım ve ilk kurulum sihirbazı (izinler, ses seçimi, anahtar).
- [ ] Acil durum: "112'yi ara", acil kişiye konum mesajı, düşme/kaza algılama (yaşlılar ve araç için).
- [ ] İlaç/randevu hatırlatıcı, günlük özet ("günaydın" özeti: hava, takvim, haber).
- [ ] Çevrimdışı mod: temel komutlar internetsiz çalışsın, ağ sesi yoksa yerel sese geri dön.
- [ ] Yaşlı/çocuk kolaylığı: yavaş konuşma, tekrar, basit dil, büyük yazı.
- [ ] Kullanım ve hata raporu (yerel, paylaşılmaz), tek dokunuşla "bugün ne oldu".
- [ ] Ticari sürüm: Sezai'ye özel release imzası, lisans denetimi (GPL yok), sağlayıcı maliyet modeli.


## 1 Kasım devam aşaması
- `~`: mevcut hedefleri geliştir ve test et talimatı sürdürüldü. Arka plan servisinde eski oturum geri çağrı koruması eklendi; derleme/imza/kurulum ve48 sessiz regresyon geçti. Servis yaşam döngüsü fiziksel kabulü, ekran yardımcısı ve gerçek söz kesme açık (DURUM.md).


## 01.11.2026 — ekran inceleme komut bağlantısı (2. parça)
- `~` devamıyla son kayıtlar incelendi; kota %0/%55, %80 sınırının altında. Yedek: yedek/ekran-komut-baglantisi-oncesi-20261101.tar.gz.
- SohbetKomutlari.ekranInceleMi: ekranımda/ekranda ne var, ekranı/ekranımı incele/analiz et; hitap ve lütfen desteği. MainActivity normal komut ve sesli sohbet yolunda bunu API öncesinde yakalar, sohbeti duraklatıp dış pencere metnini EkranIncelemeActivity'ye bellekte devreder. İzin yoksa açıklama gösterir. Mevcut Yorumla ve oku adımı korunur; otomatik ağ gönderimi eklenmedi. Arka plan servisine henüz bağlanmadı.
- Debug EkranTestAlicisi'na olumlu ve olumsuz/alıntı komut kontrolleri eklendi; test/ekran_test.py artık 11 sonuç bekler.
- Ubuntu proot ./build.sh çıkış 0; zipalign -c 4 PASS; Gradle debug imzalı APK apksigner verify --verbose PASS (v2=true, v3=false). APK: app/app/build/outputs/apk/debug/app-debug.apk. Python AST ve git diff --check PASS.
- KURULUM/CİHAZ TESTİ YAPILAMADI: yapılandırılmış rish ile id komutu çıkış1, `Server is not running`. Shizuku çalışmıyor. Yeni 11 ekran kontrolü ve önceki 48 sessiz regresyon bu APK'da çalıştırılmadı; eski sonuçlar yeni kaynak kabulü değildir. Download APK güncellenmedi, uygulama açılmadı, ekran metni toplanmadı, ses/mikrofon başlatılmadı.
- Sonraki: Shizuku yeniden başlatılınca imzalı APK'yı kur (Activity'yi otomatik açmadan), ekran11 ve mevcut48 sessiz test; sonra kullanıcıyla erişilebilirlik izni ve gerçek dış pencere/komut kabulü. Pencere listesi arkadaki uygulamayı sunmayabilir; cihaz kanıtı olmadan telefon geneli ekran yardımı tamamlandı sayılmaz. Söz kesme/pil/kilit işleri açık.
- Pil: yeni servis, sürekli sorgu, wake lock veya mikrofon döngüsü yok; ekran okuma yalnız açık komutta. Pil ölçülmedi.


## 01.11.2026 — ekran komutlarının bağımsız JVM doğrulaması
- Yeni `~` devamı: kota %4/%55. rish `id` çıkış1: `Server is not running`; `adb devices -l` listesi boş. ADB yerel sunucusu komutla başladı; kablosuz hata ayıklama açılmadı, bağlantı/izin değiştirilmedi. Kurulum ve cihaz testleri hâlâ bekliyor.
- test/ekran_komut_jvm.kt ve .sh eklendi: mevcut Gradle içindeki Kotlin derleyicisiyle gerçek KomutYorumlayici.kt ve SohbetKomutlari.kt yerel JVM için derlenir. Yeni paket/ağ/Android/anahtar kullanılmaz; geçici test çıktısı temizlenir.
- Yerel JVM 21/21 PASS: olumlu ekran komutları, Türkçe büyük harf/noktalama/hitap, olumsuz/alıntı ifadeler ve geçmiş/bitirme/söz alma ayrımı. Bu sonuç gerçek erişilebilirlik penceresi, Activity yönlendirmesi veya sesli kabulü değildir. bash sözdizimi ve diff kontrolü başarılı.
- Üretim kaynağı/APK değişmedi; yeniden APK derlenmedi/kurulmadı. Yeni mikrofon, servis veya pil yükü yok; pil ölçülmedi. Sonraki adım: kullanıcı Shizuku'yu başlattığında önceki imzalı APK'yı kur, ekran11 ve regresyon48 cihaz kontrolünü çalıştır; sonra gerçek dış pencere kabulü.


## 01.11.2026 — bağlantı geldi, ekran komutlu APK kuruldu
- `~` devamında rish id başarılı: uid2000(shell). ADB listesi boş; cihaz işlemleri yapılandırılmış Shizuku üzerinden yapıldı. Kota %7/%56.
- Mevcut APK tekrar zipalign -c 4 ve apksigner verify --verbose kontrolünden geçti (v2=true). Kaynak değişmedi, önceki başarılı derlemenin APK'sı kullanıldı. `pm install -r -S 11708326` stdin aktarımı Success; Activity otomatik açılmadı. Download kopyası güncellenmedi.
- KURULU SON APK: ekran11/11, sohbet31/31, dinleme9/9, görüşme8/8; toplam59/59 PASS. Her betik yeni test_id ile kendi raporunu doğruladı. Gerçek ekran içeriği, insan sesi, API ve telefon görüşmesi kabulü değildir; sentetik metin/sahte ağ/ses/odak olayları ve Android sistem yetenek kontrolüdür.
- `settings get secure enabled_accessibility_services` listesinde Sezai yok: ekran okuyucu izni etkin değil. Android erişilebilirlik ayarlarında kullanıcı Sezai ekran okuyucusunu açmalı; sonra gerçek dış pencere/ekranımda ne var kabulü yapılmalı. İzin otomatik değiştirilmedi, ekran içeriği toplanmadı.
- Yeni üretim kaynak değişikliği, servis, wake lock veya mikrofon döngüsü yok. Testte ana ekran/mikrofon/hoparlör başlatılmadı. Pil ölçülmedi. Arka plan komut bağlantısı, gerçek söz kesme ve diğer MD hedefleri açık.


## 01.11.2026 — Sezai ekran yardımcısı izni etkin
- Kullanıcı hizmeti açtığını bildirdi; Shizuku üzerinden enabled_accessibility_services sorgusunda com.sezai.assistant/com.sezai.assistant.EkranOkuyucuServisi bulundu. Android erişilebilirlik etkinleştirmesi doğrulandı.
- Doğru arayüz adı: Sezai ekran yardımcısı. Önceki Zeki/Sezai karışıklığı kullanıcıya düzeltildi; diğer hizmetlerin ayarları değiştirilmedi.
- Gerçek dış pencere metni, sesli komut ve yorumlama henüz denenmedi. Sonraki: başka uygulama açıkken erişilebilirlik Sezai düğmesiyle açık istekli ekran inceleme, ardından Sezai öndeyken sesli komut/dış pencere kabulü. Kaynak/APK değişmedi, ekran içeriği toplanmadı, yeni mikrofon/ses/pil testi yapılmadı.


## 01.11.2026 — gerçek komutla inceleme ekranı açılış kontrolü
- `~` devamı kota %10/%56. Mevcut MainActivity test girişiyle `am start ... --es komut "ekranımda ne var"` çalıştırıldı; Activity başlatma çıkış0.
- İlk uiautomator XML'ini rish standart çıktısından okuma başarısız: XML ayrıştırılamadı; sebep kesinleşmedi. Mevcut test/cihaz_ui.py dosya aktarımıyla yeni döküm alındı ve ayrıştırıldı. Geçici XML dosyaları yardımcı tarafından temizlendi.
- Gerçek UI: Sezai paketinde13 düğüm, Ekranda ne var? başlığı=true, Yorumla ve oku düğmesi enabled=true. Mevcut kod düğmeyi yalnız boş olmayan metinde etkinleştirir: bu koşulda dış pencere yolunun boş olmayan metin döndürdüğü çıkarılır. Beklenen boş/metin açıklaması önekleri XML'de eşleşmedi; açıklama metni kabulü başarılı sayılmadı. Hangi uygulama/pencere metninin geldiği, doğruluğu ve kapsamı doğrulanmadı; tam ekran görsel analizi değildir.
- Yorumla ve oku tıklanmadı, ekran içeriği rapora/sohbete yazılmadı, test bu içerikle API çağrısı başlatmadı. İnsan sesiyle tetikleme denenmedi. MainActivity gerçek açılışı yapıldı; ayarlı uyanma/arka plan dinlemesinin açılıştaki etkisi ayrıca ölçülmedi, bu deneme mikrofon açılmadı iddiası taşımaz.
- Kaynak/APK değişmedi. Önceki59 sessiz kontrol bu tur tekrarlanmadı. Sonraki: kullanıcı açık uygulama ile alınan metni karşılaştırmalı; ardından isterse Yorumla ve oku ile model kabulü. Arka plan sesli ekran komutu, söz kesme ve pil ölçümü açık.


## 01.11.2026 — ekran yardımcısı
- [~] "Ekranımda ne var": dış uygulama metni okunuyor (cihazda doğrulandı, uygulama içi komut). Arka plandan sesle kullanım kodlandı, insan sesi kabulü bekliyor.
- [?] Not: kullanıcı "note oku" dedi; hangi dosya/not kastedildiği belirsiz, yanıt bekleniyor.


## 02.11.2026 — karakter, ses, görünüm istekleri
- "Resim tıklanınca dinleme modu olsun": karakter dokunuşu dinlemeyi başlatır (kodlandı, kuruldu; cihazda dokunuş denenmedi, mikrofon açtığı için).
- "Gece modu gündüz rengi" ve "gece gündüz rengi nerde": ana ekrana Gece/Gündüz görünüm düğmesi eklendi (kullanıcı seçimi). Görsel doğrulama yapılmadı; diğer ekranlar açık renkli.
- "Sesi %120 / %200 / %250 yap": yazılım kazancı (PCM + AudioTrack) en çok %250. Kullanıcı: rakam artsa da ses yükselmiyor. Duyulabilirlik doğrulanmadı.
- "Neler yapabilirsin" yardım komutu eklendi.
- "note oku" / "md yada hatırlama dosyasını oku": hangi not kastedildi belirsiz, yanıt bekleniyor.


## 02.11.2026 — sesli not, varsayılan gece, bildirim ve kumanda istekleri
- "Sesli not kaydedeyim, 'son notu' deyince okusun": "not al ..." / "son notu oku" / "notlarımı oku" kodlandı (NotDefteri.kt, yalnız uygulama özel klasörü).
- "Varsayılan gece modunda başlasın": gece varsayılan yapıldı.
- "Sezai açıldığında bildirim çubuğunda bildirimi görünsün": uygulama açıkken sessiz bildirim kodlandı (servis yok).
- "Sezai başladı, mümkün olan çok özellik bildirim çubuğundan açılır/kapanır, elle müdahale/tıklama olsun": bildirim düğmeleri — AÇIK İŞ.
- "İcondaki tüm özelliklere sesle kumanda olsun": AÇIK İŞ (ses komutu kapsamı genişletilecek).
- "Sesle kendini geliştirecek, öğrenecek, öğrendiğini hatırlayacak": AÇIK İŞ; tanım netleşmeli (kullanıcının öğrettiği bilgi/tercihlerin kalıcı hafızası; uygulamanın kendi kodunu değiştirmesi kapsam dışı/güvensiz).
- "Tamamen sessiz pasif moda getirecek buton ve sesle kumanda": AÇIK İŞ.


## 04.11.2026 — yanıtsız seslenme
- [ ] “Seslendim Sezai cevap vermedi”: canlı dinleme/servis/hata nedenini bul ve düzelt. Shizuku kapalı, ADB cihaz yok; gerçek cihaz tanılaması ve insan sesi kabulü bekliyor.


## 04.11.2026 — emoji okuma ve konuşurken söz kesme
- [~] Emoji adları (“ağzı açık gülen yüz” vb.) seslendirilmesin: ortak TTS filtresi ve model talimatı eklendi, JVM 8/8 geçti; yeni APK kurulumu ve kulakla kabul bekliyor.
- [~] Sezai konuşurken “Sezai” veya “dur” deyince susup kullanıcıyı dinlesin, konu korunsun: ortak TTS söz kesme bağlantısı ve tek işçili model yaşam döngüsü; ön plan/arka plan mikrofon devri. Derleme ve insan sesi kabulü sürüyor.
- Kullanıcı açıkça düzelt, yeniden yükle, çalıştır talimatı verdi.

- 04.11 son durum: emoji filtresi imzalı APK ile yüklü (JVM8/8; kulakla kabul açık). “Dur/Sezai” insan kabulü BAŞARISIZ; bitmedi.48 sessiz kontrol ve yapay JNI geçti. Ham mikrofon/AGC karşılaştırması kilit açılması bekliyor; kullanıcı aynı isteği yeniden anlatmak zorunda değil.


## 04.11.2026 — olgunlaştırma ve ses yok
- [~] Eksikleri tamamla, test et, projeyi olgunlaştır: sürüm doğrulama/kurulum hata kontrolü düzeltildi; yerel testler geçti, cihaz köprüsü bekleniyor.
- [ ] “Sesi hiç çıkmıyor”: öncelikli gerçek cihaz tanısı ve düzeltme. Sessiz tercih/mute/TTS/odak nedeni henüz doğrulanmadı; Shizuku bağlantısı gerekli.


## 04.11.2026 — konuşma geçmişi görünmüyor/okunmuyor
- [~] Kullanıcı sesli sohbet yazılarının ve konuşma geçmişinin görünmediğini, sesli okunmadığını bildirdi. Kodda arka plan yanıtının döküme yazılmaması, yeni Activity dökümünün ayrı olması ve ilk motor hazırlığında silinmesi doğrulandı. Ortak sınırlı bellek ve servis kayıt bağlantısı eklendi; disk kalıcılığı eklenmedi.
- [~] Geçmiş penceresi metin zemini gece/gündüz paletine bağlandı. Sessiz mod/hazır olmayan TTS açıklanır; gerçek cihaz sesi kabulü açık. Shizuku bu tur da Server is not running döndü.


## 04.11.2026 — kilitliyken hiç dinlemiyor/cevap vermiyor
- [ ] Kullanıcı art arda ekran kilitliyken Sezai'nin ses almadığını/cevap vermediğini bildirdi. Gerçek kilitli kullanım kabulü BAŞARISIZ; çalışıyor sayılmaz.
- [~] Kaynakta mikrofon servisi onResume içinde durdurulup onStop içinde yeniden başlatılıyordu. Başlatma görünürlük bitmeden onPause'a taşındı; sessiz/arka_plan tercihleri korunur. Derleme sürüyor; gerçek nedenin cihaz loguyla doğrulanması ve kilitli insan sesi kabulü açık. Shizuku bu tur exit1 Server is not running.

- 05.11 00:00 son durum: yeni APK derlendi,imza/hizalama doğrulandı,kurulum isteği gönderildi. Gerçek kilitli dinleme/yanıt kabulü hâlâ BAŞARISIZ/açık; Shizuku bağlantısı bekleniyor.


## 05.11.2026 — kapsamlı ve cep telefonuna uygun Termux
- Kullanıcı pano/Termux:API kurulumu, ek araçlar, uygun büyük dosyaların SD karta taşınması ve Türkçe kontrol paneli istedi. API/pano çalıştı; araçlar kuruldu; 15 büyük indirme dosyası SD'ye SHA256 doğrulamasıyla taşındı (kayıt SD/Termux-Tasinan-20261105/tasima-kaydi.json).
- “Süper, kullanışlı, görsel ve pratik Termux; klavye daha az yer tutsun, bütün özellikler korunsun, şimdiki güzel hali yedeklensin”: özgün görünüm özel alanda yedeklendi; lacivert tema korundu; 2 satırlı kompakt düzen ve eski 5 satırlı tam düzen arasında geçiş, tek komutla özgün ayara dönüş hazır.
- Türkçe panel ve dört ana ekran kısayolu hazır; Widget kuruldu. Android başlatıcısına widget ekleme ve fiziksel kullanım kabulü açık. Gboard yüksekliği ayrı ayardır; değiştirilmedi.


## 05.11.2026 — tek satır ve Türkçe ses
- [x] “Yap bakayım, ayrıca sesli klavye de ekle TR olsun”: tek satır8tuş ve Ses/pano dikte aracı kuruldu; önceki profiller/yedekler korundu. Mevcut Gboard Türkçe.
- [ ] Gerçek Türkçe insan sesiyle Ses düğmesi/dikte ve Gboard mikrofonu kullanım kabulü. Sürekli mikrofon açılmadı.
