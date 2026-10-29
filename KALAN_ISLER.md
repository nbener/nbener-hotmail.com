# Kalan işler ve sorunlar

Her çalışma başlangıcında CLAUDE.md, DURUM.md ve TEST_DURUMU.md okunur. Tamamlanan iş, kanıt ve kalan riskle bu listeden güncel duruma taşınır.

## Öncelikli

- [ ] Gerçek web araştırması aracı bağlama: kullanıcı webden bak dediğinde model araca erişemedi. Mevcut uygulamada web aracı yok; telefon internetsiz diye raporlama. Sağlayıcı desteği, maliyet, kaynak bağlantısı ve gerçek sorgu testi gerekir.

- [ ] Görüşme önceliği gerçek kabul testi: APK kuruldu, sessiz testler 8/8 geçti. Gerçek telefon ve WhatsApp çağrısında doğrula. Otomatik mikrofon açılmamalı.
- [ ] Modern ekran son hizalama kontrolü: yeni tema kuruldu, ilk sürüm görsel olarak görüldü. Son iki düğme düzeltmesi kuruldu; son UI kontrolü aranan düğmeyi bulamadığı için geçmedi. Ekran durumunu doğrula, görünür düğmelerin hizasını kontrol et.
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
