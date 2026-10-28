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
1. TAMAMLANDI: Yorumlayıcı cihazda 20/20 yan etkisiz testten geçti. Test: Termux içinde `python3 test/cihaz_yorumla_test.py`.
2. Gerçek alarm/zamanlayıcı testi (sistem saati uygulamasında göründüğünü doğrula, test alarmlarını iptal et).
3. Kullanıcıdan: Google ses numaralarının kadın/erkek eşlemesi, "Dinle" gerçek konuşma testi, internetsiz çalışma şart mı sorusu.
4. Sonra Aşama 1: sesli arama (rehber araması, aramadan önce onay, test sırasında gerçek arama YAPMA), müzik/radyo, YouTube.
5. Sezai imza anahtarı + release derleme.

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
