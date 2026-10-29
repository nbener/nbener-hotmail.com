# Konuşma sırasında söz alma — ortak devir kaydı

Codex, Claude/Anthropic ve diğer ajanlar için. 29 Ekim 2026.

## İstenen davranış
Kullanıcı TTS sürerken “dur” veya “Sezai” diyerek sözü alır. Sezai susar ve isteğin devamını dinler. Takip sorusunda konu korunur; yeni konuda son istek esas alınır. Yalnız çağrı sohbeti bitirmez, geçmişi silmez. “Sohbeti bitir” ayrı komuttur.

## Kurulu uygulamadaki kapsam
SohbetKomutlari dur/Sezai çağrısını ve arkasındaki isteği ayırır. MainActivity mevcut tanıma sonucu geldiğinde yalnız çağrıda yeniden dinler, devamındaki isteği sohbet motoruna geçirir. SohbetMotoru yön değiştirme talimatı içerir. Bunlar TTS sırasında duyma özelliği değildir: üretimde TTS sırasında mikrofon hâlâ kapalıdır. Gerçek modelin her konu geçişini doğru yorumladığı doğrulanmadı.

## Cihazda bulunan engel
Android API31; varsayılan tanıma GoogleTTSRecognitionService. DUMP korumalı debug EszamanliSesTestActivity ile tek, en çok 12 saniyelik deneme yapıldı. Ham tanınan metin raporlanmaz; model API isteği yok. Üretime bağlı değildir.

Yeteneği okuma: `am start -W -n com.sezai.assistant/.EszamanliSesTestActivity --ez yalniz_bilgi true --es test_id 1793271921562238043`
Sonuç: `{"yerel_tanima_var":false,"test_id":"1793271921562238043","sonuc":"YETENEK_OKUNDU"}`.

Eşzamanlı deneme: `am start -W -n com.sezai.assistant/.EszamanliSesTestActivity --ez yerel false --es test_id 1793271941885500692`
Sonuç: `{"yerel_tanima_var":false,"yerel_istendi":false,"tts_basladi":true,"test_id":"1793271941885500692","sonuc":"SES_ODAGI_KESILDI"}`.

Test tamamlanamadı: tanıma başlatıldığında ses odağı kesildi; dinleme-hazır veya söz-alma-duyuldu doğrulanmadı. Android STT'yi TTS boyunca döngüye sokma ya da görüşme/odak korumasını kaldırma. EXTRA_PREFER_OFFLINE yerel çalışma garantisi değildir.

## Somut sonraki seçenek: ayrı yerel Türkçe motor denemesi
Vosk `vosk-model-small-tr-0.3`: resmî listede 35M, Türkçe doğruluk ölçümü TBD, Apache 2.0. Bu telefonda denenmedi; başarı/pil iddiası yok. Model ve ARM64 Android kütüphanesi indirilip deneme bileşeninde ölçülmeli. Yeni motor henüz eklenmedi. CLAUDE.md bölüm 4 motor kararının onaya sunulmasını ister.

Önerilen sınırlı kullanım: yalnız etkin sesli sohbetin TTS süresinde AudioRecord ile yerel tanıma; ekran/uygulama kapanışında, TTS bitişinde ve görüşmede kaynakları bırak. Sürekli arka plan mikrofonu açma. Cihaz destekliyorsa VOICE_COMMUNICATION/AcousticEchoCanceler denenebilir; yankıyı gerçekten ayırdığı ölçülmeden varsayılmaz. Kullanıcı çağrısından sonraki kelimeleri kaybetmeden aynı yakalama içinde isteği tamamla. Normal dinleme motorunu başlangıçta değiştirme.

Kabul: insan yokken Sezai kendi sesinden tetiklenmemeli (kendi cümlesinde dur/Sezai dahil); kullanıcı hoparlör üstüne konuşunca kesme gecikmesi ölçülmeli; çağrı ardından takip kelimeleri kaybolmamalı; aynı/yeni konu ve belirsiz istekler denenmeli. Kilit, arka plan, görüşme ve pil/RAM ölçülmeli. “Sezai” model sözlüğünde doğrulanmalı. Başarısızsa varsayılan akış korunur. Kütüphane/SDK/paket güncellemesi kendiliğinden yapılmaz.

Kaynaklar: https://alphacephei.com/vosk/models , https://alphacephei.com/vosk/android , https://developer.android.com/reference/android/speech/SpeechRecognizer .
