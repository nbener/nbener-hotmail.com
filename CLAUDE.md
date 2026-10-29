# SEZAI - KALICI PROJE TALİMATI

Bu dosya kalıcıdır. Her oturumda önce bunu oku, sonra çalış. Kullanıcıyla Türkçe konuş, kod yorumlarını Türkçe yaz.

## 1. Proje
Sezai bağımsız, kendi başına bir projedir; Zeki (com.zeki.assistant) ile ilişkisi yoktur, Zeki'nin kodu, anahtarı veya paket adı kullanılmaz. Sezai Android'de çalışan sıcak, insan gibi konuşan, güvenilir bir sesli yol arkadaşı ve her konuda bilgili yardımcıdır.
Hedef kitle: yaşlılar, çocuklar, gençler, aklında çok şey tutamayanlar.
Proje özeldir, satılacaktır. Asla public yapma, GitHub'a açık depo olarak koyma, kodu dışarı paylaşma.

## 2. Çalışma ortamı (KESİN KURAL)
- Her şey sadece Termux içinde, bu telefonda yapılır. Derleme araçları Termux içindeki Ubuntu proot ortamında kuruludur (bu ortam Termux'un içinde çalışır, başka platform değildir). GitHub Actions, bulut derleme, bilgisayar veya başka platform önerme.
- Kod bu telefonda derlenecek, kurulacak, çalıştırılacak ve test edilecek.
- Derleme araçlarını (java, kotlin, gradle, aapt2, d8, apksigner, adb) Ubuntu proot ortamında kontrol et. Hangisinin çalışıp çalışmadığını tahmin etme, komutla doğrula.
- Derleme komutu (Termux'tan): `proot-distro login ubuntu -- bash -c "cd /root/sezai && ./build.sh"`.
- Derleme yöntemi seçerken ARM/Termux uyumluluğunu önce test et. Çalışmayan yöntemde ısrar etme, hatayı olduğu gibi bildir ve seçenek sun.
- APK'yı derleyince imzala, termux-open ile kurulum ekranını aç. Test için mümkünse kablosuz adb (localhost) ile logcat oku. Bu mümkün değilse nedenini söyle.

## 3. Çalışma kuralları
1. Önce mevcut projeyi ve kodu analiz et. Mimariyi, teknolojileri, mevcut davranışı anlamadan değişiklik yapma.
2. Ben açıkça istemedikçe dosya silme, büyük refactor, migration, paket güncellemesi, mimari değişiklik yapma.
3. Sorun bildirilince önce kök nedeni bul. Belirtiyi gizleyen geçici çözüm uygulama.
4. Kod değiştirmeden önce hangi dosyaları neden değiştireceğini kısaca yaz.
5. Çalışan özellikleri koru. Gereksiz dosya ve kod değişikliğinden kaçın.
6. Emin olmadığın konuda varsayım yapma. Belirsizliği söyle, kod, config, log veya test ile doğrula.
7. Birden fazla çözüm varsa önce seçenekleri ve farklarını açıkla. Gereksiz yere en kapsamlısını uygulama.
8. Güvenlik, yetkilendirme, veri bütünlüğü, kişisel veri ve izinlerde özellikle dikkatli ol. Riskli değişikliği kendiliğinden uygulama.
9. Veri kaybına yol açabilecek işlemi (silme, sıfırlama, veritabanı şema değişikliği) açık onay olmadan yapma.
10. Değişiklikten sonra derle, kur, test et ve sonucu bildir.
11. İşlem başarısız olursa hatayı gizleme: hangi komut, hata mesajı, olası neden.
12. "Sadece analiz" dersem hiçbir dosyayı değiştirme, sistemi değiştiren komut çalıştırma.
13. Aynı bilgiyi bana tekrar sorma. Proje dosyalarından ve önceki sonuçlardan önce kendin bul.
14. "Daha modern/temiz" gerekçesiyle çalışan kodu değiştirme. Mevcut kod stilini ve mimariyi koru.
15. Doğruluk hızdan önemlidir.
16. Her iş sonunda kısa rapor ver: ne yapıldı, hangi dosyalar değişti, nasıl doğrulandı, kalan risk.
17. Bu dosya ile benim yeni talimatım çelişirse önce çelişkiyi söyle, güvenli olanı öner.

## 4. Sezai'nin kişiliği ve ses
- Sıcak, sabırlı, samimi, insan gibi konuşur. Kısa ve anlaşılır cümleler kurar. Yaşlıya ve çocuğa uygun, saygılı bir dil kullanır.
- Kadın ve erkek ses vardır, sesli komutla değişir.
- Her şey sesli komutla yapılabilir olmalı. Kullanıcı başka uygulamaya atlamamalı, işler uygulamanın içinde bitmeli.
- Konuşma tanıma ve ses motoru Sezai için ayrıca seçilir (Zeki'den bağımsız). Seçenekler (Vosk, sherpa-onnx, Piper, Android TTS vb.) Termux'ta denenip pil ve Türkçe kalitesine göre karar verilir, onayına sunulur.

## 5. Özellikler
Her özelliği tek tek, küçük adımlarla, her adımdan sonra derle-kur-test et. Kolaylardan başla, zorları sona bırak.

Genel:
- Sesli arama ve telefon görüşmesi, mesajları okuma, yeni mesaj bildirimi
- Alarm, YouTube arama, müzik ve internet radyosu (hepsi sesle)
- Güvenilir kaynaklardan web araştırması, güncel haberler ve manşetler, din ve dini rüya konularında bilgi
- Kamera analizi: "ön kameradan gördüklerini analiz et", "arka kamerayı analiz et"
- Ekran analizi: "ekranımda ne var, analiz et" (erişilebilirlik servisi veya ekran görüntüsü ile)

Araç modu:
- Araç Bluetooth'u üzerinden eller serbest, sesli navigasyon
- Anlık hız, önemli kavşak ve yoğun trafik uyarısı
- En yakın nöbetçi eczane, hastane, benzin istasyonu
- Hız sınırı ve okul bölgesi uyarısı, hız kamerası ve hız koridoru uyarısı
- Ekran sadece istenince gösterilir

## 6. Pil verimliliği (en yüksek öncelik)
Tüm uygulamada pil tüketimini düşük tut: gereksiz arka plan servisi, sürekli konum ve sürekli mikrofon dinlemesi yapma. Her yeni özellikte pil etkisini düşün ve raporda belirt.

## 7. Başlangıç görevi
Henüz kod yazma. Önce şunu yap:
1. ~/sezai içindeki mevcut durumu analiz et (Zeki başka bir projedir, yalnızca referans olarak bakılabilir).
2. Termux içindeki Ubuntu proot ortamında derleme araçlarının durumunu kontrol et ve neyin çalıştığını raporla.
3. Özellikler için küçükten büyüğe bir yol haritası öner, benim onayımı bekle.

## 8. İmza ve yedek (UNUTMA)
- Her APK derlemesinde: zipalign, ardından apksigner ile imzala, sonra `apksigner verify` ile doğrula. İmzasız APK'yı kurulum için sunma.
- Sezai'nin kendi imza anahtarı olacak (Zeki'nin anahtarıyla hiçbir ilişkisi yok, Zeki anahtarını kullanma). Anahtar kayıp olursa uygulama güncellenemez. Yedeği ~/sezai/yedek içinde tutulur (izin 600). Anahtarı ve şifresini asla paylaşma, repoya veya çıktıya yazma.
- Her önemli aşamadan önce kaynak kodun yedeğini al (tar.gz, ~/sezai/yedek). Silme, şema değişikliği veya büyük değişiklikten önce yedek almadan başlama.
- Derleme ortamı: `source ~/sezai/env.sh` (JDK 17, Gradle 8.10.2, Android platform 34 sabit).

## 9. Oturum sürekliliği ve devredilebilirlik
- Her oturumun başında DURUM.md oku. Her adımdan sonra git commit at ve DURUM.md'yi güncelle. Başka bir ajan devralabilsin diye hiçbir bilgiyi sadece sohbette bırakma.
- Git deposu yerel kalır: uzak depo ekleme, public yapma (bkz. Bölüm 1). Anahtar, şifre ve yedek/ klasörü git'e girmez (.gitignore).
- Derleme: ./build.sh (kurulum için ./build.sh kur).

## 10. Güncel kayıt ve doğrulama (kullanıcı talimatı)
- Her çalışma başlangıcında bu dosyayla birlikte DURUM.md, KALAN_ISLER.md ve TEST_DURUMU.md oku.
- Her anlamlı aşamanın sonunda ilerlemeyi, kalan işi, bilinen sorunu ve çözüm yolunu güncelle; yerel commit oluştur.
- Geçen testi, tarihini ve kapsamını yaz. Denenmemiş, başarısız veya sürmekte olan işi açıkça ayır; sahte ağ testini canlı yapay zekâ başarısı sayma.
- Kaynak değişince eski APK/test sonucu otomatik olarak yeni kaynağı doğrulamaz. Değişikliğe uygun derleme, imza, kurulum ve test yap.

## 11. Doğruluk — kullanıcının kesin talimatı
- Asla yalan söyleme, asla bilgi veya sonuç uydurma.
- Doğrulanan bilgiyi, çıkarımı ve bilinmeyeni açıkça ayır. Kanıt yoksa doğrulanmadığını söyle.
- Yapılmayan işlemi yapıldı, geçmeyen testi geçti, sahte ağ testini gerçek model/gerçek görüşme testi olarak sunma.
- Bir araç/derleme/test hatasını gizleme; sebebi doğrulanmadıysa tahmin olduğunu belirt.

## Kısa devam komutu
- Kullanıcı tek başına `~` veya `..` gönderirse anlamı: devam et, geliştir ve test et. Mevcut görev, proje kuralları ve kota sınırı geçerlidir.

## Telefon genelinde sesli yardım — kullanıcı önceliği
- Sezai kullanıcının telefon genelinde, telefona dokunmadan sesle kumanda edeceği yardımcısı olacak. Ekran okuma ve yorumlama yalnızca tek sohbet uygulamasıyla sınırlandırılmış bir hedef değildir; kullanıcının istediği açık uygulama/ekran kapsam içindedir.
- Kullanıcı ekran erişimini geliştirmeye açıkça izin verdi. Otomatik mesaj okuma ile açık istekle ekran inceleme ayrı davranışlardır. Android'in gereken kullanıcı izinleri ve mevcut teknik sınırlar dürüstçe belirtilir; henüz yapılmamış küresel sesli kumanda tamamlandı sayılmaz.

## Özgün karakter — tüm ajanlar için
- Karakter tasarımı, yasaklanan biçimler, pil/animasyon kuralları ve geri dönüş yolu KARAKTER_TASARIMI.md dosyasındadır. Claude/Anthropic, Codex ve devralan diğer ajanlar bu ortak kayda uyar. Kullanıcı bütün kararların ve gerçek test/hata sonuçlarının MD dosyalarında tutulmasını özellikle istedi.

## Konuşmanın arasına girme — kullanıcı talimatı
- Kullanıcı Sezai konuşurken “dur” veya “Sezai” diyerek sözünü kesip yeniden yönlendirmek istiyor. Bu, sohbeti bitirmek veya geçmişi silmek değildir. Sözü alınca dinlemeye geçer; takip mesajı aynı konuysa bağlamı korur, yeni konuysa son isteği esas alır.
- Hoparlör/TTS sesini kullanıcı komutu sayma. Eşzamanlı mikrofon/TTS, ses odağı ve yankı davranışını cihazda doğrula. Mevcut Android tanımayı kontrolsüz sürekli döngüye sokma; pil ve görüşme önceliği korunur. Tamamlanmayan eşzamanlı kesmeyi çalışıyor diye sunma.

- Söz alma ölçümleri, başarısız cihaz denemesi ve motor önerisi SOZ_KESME_TASARIMI.md içindedir; Claude/Anthropic ve Codex aynı kaydı kullanır. TTS sırasında söz kesme henüz çalışmıyor.
