# SEZAI - DURUM (her adımdan sonra güncelle, her oturumda ilk bunu oku)

Son güncelleme: 2026-10-28

## Ne yapıldı
- Sezai bağımsız projedir (Zeki ile ilişkisi yok). Paket adı: com.sezai.assistant. Kurallar: CLAUDE.md
- Araç zinciri kuruldu ve doğrulandı (proot Ubuntu 26.04 aarch64): JDK 17, Gradle 8.10.2 (~/tools), Android platform 34 (~/android-sdk), aapt2/apksigner/zipalign (apt). SDK lisansı kullanıcı onayıyla kabul edildi.
- Disk açıldı: Download'dan ggml-large-v3 ve eski loglar silindi (kullanıcı yetkisiyle). Boş alan ~3-4 GB; /sdcard ayrı kart değil, aynı bölüm.
- Boş "Merhaba, ben Sezai" APK'sı derlendi, Shizuku/rish ile kuruldu, ekranda doğrulandı.
- Ses altyapısı kararı: Android TTS/STT + KonusmaMotoru/DinlemeMotoru arayüzleri. Piper/espeak-ng GPL lisans riski nedeniyle ertelendi (satış öncesi lisans kontrolü şart). Google TTS varsayılan: 5 yerel Türkçe ses (ama, cfs, efu, mfm, tmc), cinsiyet API'de yok, kullanıcı kulakla belirleyecek. Samsung TTS: 1 kadın ses.
- Kod (app/app/src/main/java/com/sezai/assistant): SesArayuzleri, AndroidKonusma, AndroidDinleme, MainActivity (test ekranı: Konuş/Dinle/Sesi değiştir), KomutYorumlayici (Türkçe cümle -> komut), KomutCalistirici (saat, tarih, alarm kur/iptal, zamanlayıcı).
- Git deposu başlatıldı (yerel, uzak depo yok).

## Derleme nasıl yapılır
- `cd ~/sezai && ./build.sh` debug APK derler (ilk derleme ~3 dk).
- `./build.sh kur` derler, Shizuku/rish ile telefona kurar ve açar.
- Ortam: `source ~/sezai/env.sh` (JDK 17, Gradle 8.10.2, aapt2 override).
- Telefona kurulum: tıklayarak kurulum çalışmadı; pm /sdcard'ı okuyamaz, bu yüzden dosya /data/local/tmp'ye kopyalanıp rish ile kurulur (build.sh bunu yapar). Shizuku kapalıysa önce Shizuku uygulamasından başlatılmalı. rish kopyası ~/rish, RISH_APPLICATION_ID=com.termux gerekir.
- Test: rish ile `am start ... --es yorumla "cümle"` (yalnızca yorumla), `--es komut "cümle"` (çalıştır), `logcat -d | grep Sezai`.
- İmza: şu an debug anahtarı. Release için Sezai'nin KENDİ anahtarı oluşturulacak (şifreyi kullanıcı belirler), apksigner verify zorunlu, yedek ~/sezai/yedek (git'e girmez).

## Sıradaki adım
1. Yorumlayıcıyı yan etkisiz cümlelerle test et (parse_test), hataları düzelt.
2. Gerçek alarm/zamanlayıcı testi (sistem saati uygulamasında göründüğünü doğrula, test alarmlarını iptal et).
3. Kullanıcıdan: Google ses numaralarının kadın/erkek eşlemesi, "Dinle" gerçek konuşma testi, internetsiz çalışma şart mı sorusu.
4. Sonra Aşama 1: sesli arama (rehber araması, aramadan önce onay, test sırasında gerçek arama YAPMA), müzik/radyo, YouTube.
5. Sezai imza anahtarı + release derleme.

## Bilinen sorunlar
- Tıklayarak APK kurulumu başlamıyor (neden belirsiz); Shizuku gerekli, telefon yeniden başlayınca Shizuku durabilir.
- Yorumlayıcı henüz test edilmedi; ilk parse testi boş çıktı (YORUM günlüğü TTS hazır olana kadar bekliyordu, düzeltilecek).
- Android konuşma tanıma çoğu durumda internet ister; çevrimdışı ihtiyacı belirsiz.
- Disk dar (~3-4 GB boş). Derleme önbelleği ~1,1 GB.
- Pil: sürekli dinleme yok (mikrofon yalnızca butonla). WakeService gibi arka plan servisi eklenmedi; eklenirse pil ölçülmeli.
- Kadın/erkek ses eşlemesi henüz yok.
