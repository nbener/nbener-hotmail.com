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

- [ ] Sezai'ye seslenerek uyandırma ve telefon genelinde eller serbest kullanım. Yerel uyandırma sözcüğü, gerekli Android rol/izinleri ve ölçülmüş pil bütçesi tasarlanacak; şu an sürekli dinleme yok.
  - 29 Ekim kullanıcı isteği: kapalıyken de "Sezai" deyince devreye girsin. Pil/gizlilik çelişkisi nedeniyle seçenekler kullanıcıya sunuldu, onay bekliyor.
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
