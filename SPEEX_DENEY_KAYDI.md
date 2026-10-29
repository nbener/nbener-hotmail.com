# Yerel SpeexDSP referanslı yankı giderme deneyi

30 Ekim 2026. Araştırmadaki referanslı AEC yaklaşımının bağımsız ARM64 kontrolü; üretim ses motoru değişikliği değildir.

- Kaynak: [Xiph SpeexDSP 1.2.1](https://downloads.xiph.org/releases/speex/speexdsp-1.2.1.tar.gz).
- Arşiv SHA256: `8c777343e4a6399569c72abc38a95b24db56882c83dbdb6c6424a5f4aeb54d3d`. Yayıncının aynı HTTPS dağıtımındaki SHA256SUMS.txt listesiyle eşleşti; ayrı imza doğrulaması değildir.
- [COPYING](https://github.com/xiph/speexdsp/blob/SpeexDSP-1.2.1/COPYING) okundu. BSD biçimindeki kaynak/binary yeniden dağıtım bildirimleri korunmalıdır. Orijinal kaynak ve COPYING yerel arşiv/açılmış ağaçta korunur. Henüz APK'ya eklenmedi; eklenirse dağıtım bildirimleri APK varlıklarına da alınmalı.
- WebRTC APM de adaydır. Yerel CMake/Meson/NDK yokken küçük C derlemesiyle yaklaşımı sınamak için araştırmadaki SpeexDSP alternatifi seçildi. Bu karar motoru değiştirmez; gerçek telefon akustiğinde hangisinin uygun olduğu bilinmiyor.
- Termux Clang21.1.8 / Android24 hedefi ve Make ile statik kütüphane bu telefonda derlendi. Sistem paketi veya make install yok; masaüstü/bulut derlemesi yok.
- Yerel kaynak/ikili/log/JSON `yerel-model/speex-deneme/` altında ve mevcut gitignore kapsamındadır. Üçüncü taraf kaynak depoya veya dışarı yüklenmedi.

## Tekrarlama

Arşiv indirilmişken Termux'ta `python3 scripts/speex_yerel_dene.py`. Betik sabit SHA256'yı kontrol eder, yapılandırır/derler, ARM64 ELF başlığını doğrular, C deneyini çalıştırır ve bu çalışmaya ait raporu yerel klasöre yazar. Ağdan dosya indirmez ve APK kurmaz.

Deney16kHz/160 örnek(10ms) kare ve1600 örnek filtre kullanır. Sabit tohumlu yapay uzak sinyalden iki gecikmiş bileşenli yankı oluşturulur. Ayrı turlarda doğru referans, sıfır referans ve ikinci yapay sinyal eklenmesi ölçülür. Son2 saniyedeki yankı/artık enerjileri ve ikinci sinyale izdüşüm hesaplanır. Tüm sinyaller bellekte üretilir; hoparlör, mikrofon, ham ses dosyası ve ağ kullanılmaz.

Kabul: referansla yankı azalması>15dB; referanssız kontrolde mutlak azalma<1dB; ikinci sinyal izdüşüm katsayısı0.75–1.25; karışık turda artık azalması>6dB. Bu eşikler sentetik deney içindir, gerçek odada garanti değildir. Sabit sinüs karışımı gerçek konuşma, Türkçe tanıma veya çift konuşma veri kümesi sayılmaz.

İlk doğrudan yerel çalıştırma çıktı0: yankı58.167dB, referanssız0.247dB, ikinci sinyal katsayısı0.947, karışık artık9.190dB.36s toplam üretilen ses102.734ms thread değil süreç CPU süresinde işlendi. Bu değer gerçek zamanlı kayıt/çalma senkronu, enerji tüketimi veya uçtan uca gecikme değildir. Ana uygulamada JNI/native yükleme henüz sınanmadı.

Sonraki adım: Android debug bileşenine uygun native köprü, kontrollü AudioTrack çalma ve gecikmesi eşleştirilmiş referans/mikrofon hattı. TTS24kHz ile mikrofon16kHz örnekleme uyumu, ses katsayısı, görüşme ve kaynak bırakma şartları korunmalı. Gerçek kendi-sesinden yanlış tetikleme ve kullanıcının söz kesmesi ayrıca ölçülmeli.

Son betik turu: test_id1793310420046423805, çıktı0/PASS;58.167dB,referanssız0.247dB,ikinci sinyal0.947,karışık9.190dB,CPU108.246ms. ELF64 AArch64 ve /system/bin/linker64 kontrolü geçti. Android uygulama yüklemesi henüz yapılmadı; CLI Termux RUNPATH içeriyor.


## Debug APK JNI aşaması
Termux python3 scripts/speex_android_derle.py, sonra Ubuntu proot ./build.sh. Native kaynak/çıktı kontrolü debug derlemesini korur; release kaynaklarına eklenmez. İlkld.lld sistem kütüphane yolu hatası /system/lib64 eklenerek düzeltildi. Son56KBso özelRUNPATH içermez; SpeexCOPYING ve derlemeJSON debug assets içine alınır.

Son kurulu APK python3 test/yanki_yerel_test.py test1793310897691974925 PASS:58.167dB,referanssız0.247dB,ikinci sinyal0.947,karışık9.190dB,süreçCPU591.834ms. Gerçek mikrofon/hoparlör kullanılmadı. ELF segment16KB hizası fiziksel16KB cihaz desteğini doğrulamaz.38 sessiz regresyon aynıAPK üzerinde geçti. Gerçek kayıt/çalma referansı henüz bağlı değildir.
