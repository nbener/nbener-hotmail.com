# Konuşurken söz kesme: kaynaklı teknik araştırma

30 Ekim 2026. Kullanıcı kapsamlı web araştırması istedi. Bu belge çözüm araştırmasıdır; cihazda başarı veya yeni motor entegrasyonu kanıtı değildir. ChatGPT'nin yayımlanmamış uygulama içi mimarisi bilinmiyor.

## Sonuç

Yöntem var: eşzamanlı kayıt/çalma, hoparlör referansıyla yankı giderme, temizlenmiş girişte konuşma başlangıcı algılama, çıkış kuyruğunu kesme ve kullanıcı isteğini aynı oturumda tamamlama. Yalnız Vosk'un dur/Sezai metnini üretmesini beklemek mevcut denemede başarı sağlamadı. Telefonun bu işi yapamayacağı sonucu çıkmaz.

## Resmî kaynaklarda bulunanlar

1. [WebRTC APM kaynak kodu](https://webrtc.googlesource.com/src/+/refs/heads/main/api/audio/audio_processing.h): mikrofon/capture ve hoparlör/render olmak üzere iki ses yönüyle çalışır. `ProcessStream` mikrofonu, `ProcessReverseStream` çalınan referansı alır. Yaklaşık 10 ms PCM kareleri kullanılır. [APM açıklaması](https://webrtc.googlesource.com/src/+/refs/heads/main/modules/audio_processing/g3doc/audio_processing_module.md) modülün WebRTC içinde veya bağımsız kullanılabildiğini açıklar. Dolayısıyla yerel ses temizleme için zorunlu sunucu yoktur. ARM64 paketinin bu telefonda kullanılabilirliği henüz sınanmadı.

2. [SpeexDSP yankı giderme kılavuzu](https://www.speex.org/docs/manual/speex-manual/node7.html): mikrofon sinyali ile hoparlöre gönderilen sinyal birlikte işlenir. Referansın zamanlaması önemlidir; kırpılma gibi doğrusal olmayan bozulma giderimi zorlaştırır. Bu, WebRTC dışında yerel bir alternatif sunar; Sezai'de derlenip ölçülmedi. Gürültü azaltma ile yankı giderme aynı işlem değildir.

3. [Android AcousticEchoCanceler](https://developer.android.com/reference/android/media/audiofx/AcousticEchoCanceler): efekt, AudioRecord oturum kimliğine bağlanır. [WebRTC Android ses modülü](https://webrtc.googlesource.com/src/+/refs/heads/main/sdk/android/api/org/webrtc/audio/JavaAudioDeviceModule.java) donanım AEC kullanımını seçilebilir tutar. [100ms Android belgesi](https://github.com/100mslive/100ms-docs/blob/main/docs/android/v2/how-to-guides/configure-your-device/microphone/echo-cancellation.mdx) bazı cihazlarda donanım yerine yazılım AEC gerektiğini belirtir. Bu kaynaklar bizim cihazda donanım AEC'nin bozuk olduğunu kanıtlamaz; kontrollü karşılaştırma gerekçesidir.

4. [OpenAI Realtime kesinti belgesi](https://developers.openai.com/api/docs/guides/realtime-conversations): VAD ile kullanıcı konuşması algılanınca devam eden yanıt iptal edilebilir. WebSocket istemcisi ses çalmayı durdurmalı ve duyulmamış yanıt kısmını bağlamdan çıkarmalıdır; WebRTC/SIP sunucusu çıkış tamponunu yönetir. Bu belgelenmiş API davranışıdır, ChatGPT Android uygulamasının iç yapısına dair kanıt değildir. Realtime'a geçmek mevcut Groq metin+Android TTS akışından ayrı tasarımdır; hesap, veri aktarımı ve maliyet koşulları incelenmeden geçilmez.

5. [Gemini Live önerileri](https://ai.google.dev/gemini-api/docs/live-api/best-practices): kesinti bildiriminde istemcinin çıkış ses tamponunu hemen atması gerekir; giriş küçük parçalarla aktarılmalıdır. Yalnız model isteğini iptal etmek hoparlörde sıraya alınmış sesi kendiliğinden susturmaz.

6. [LiveKit kesinti ayarları](https://docs.livekit.io/agents/logic/turns/tuning/): konuşma süresi, sözcük sayısı, VAD/adaptif yaklaşım ve yanlış kesintiden geri dönme ayrı ayarlardır. [Pipecat konuşma başlangıcı stratejileri](https://reference-server.pipecat.ai/en/latest/api/pipecat.turns.user_start.html) konuşma başlangıcı ile yazı çözümlemesini ayrı bileşenlerde ele alır. Çıkarım: hızlı kesme için tam kelime çözümlemesini beklemek zorunlu değildir. Ama ham mikrofon genliğiyle kesmek kendi TTS'sinin tetiklemesine yol açabilir; önce yankı ayrımı gerekir.

7. [Android TTS ses geri çağrısı](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener#onAudioAvailable(java.lang.String,%20byte%5B%5D)): üretilen ses parçaları uygulamaya alınabilir; parça geldiğinde henüz çalınmamış olabilir. Sezai'nin Android TTS sesini referans olarak elde etmek için somut giriş noktasıdır. Sadece callback eklemek senkron AEC kurmaz: örnekleme/kanal biçimi, gerçek çalma zamanı ve tampon gecikmesi yönetilmelidir. Kontrollü AudioTrack çıkışı ayrı prototip olarak değerlendirilmeli.

8. [Vosk doğruluk teşhisi](https://alphacephei.com/vosk/accuracy) ses kalitesi, sözlük, model koşulları ve kare kaybını ayrı nedenler sayar. [Vosk uyarlama](https://alphacephei.com/vosk/adaptation) uygun dinamik modellerde sınırlı sözlüğü destekler. Bizim söz kesme denemesi tam sözlük kullanıyor; ön plan uyanma motoru sınırlı Sezai sözlüğü kullanıyor. Sınırlı sözlük kıyası mümkündür ama yankıyı çözmez ve yanlış tetikleme kontrolü ister.

## Yeni motoru hemen indirmeme gerekçesi

[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) Android/ARM64 ve anahtar sözcük algılama sunuyor. Ancak [açılan hazır KWS modeli](https://k2-fsa.github.io/sherpa/onnx/kws/pretrained_models/index.html) Çince/İngilizce; Türkçe dur/Sezai kalitesi kanıtlanmadı. [openWakeWord](https://github.com/dscripka/openWakeWord) hazır desteğini İngilizce olarak açıklıyor. [Porcupine Model API dil listesinde](https://picovoice.ai/docs/model-api/porcupine/) Türkçe yok. Bunları hazır Türkçe çözüm gibi sunmak yanlış olur. ASR/uyandırma motorunu değiştirmek tek başına hoparlör referansını sağlamaz.

## Sezai koduyla karşılaştırma ve önerilen sıra

- Doğrulanan: debug testte TTS ve mikrofon eşzamanlı başladı; kullanıcı hem dur hem Sezai söyledi, düşük0.2 TTS katsayısında da algılanmadı. Üretimde TTS sırasında mikrofon hâlâ kapalıdır. Debug başarı elde edilse bile MainActivity entegrasyonu ayrıca gereklidir.
- Kodda uygulama düzeyinde hoparlör PCM referansını AEC'ye veren hat yok. Android AEC/HAL'in kendi referansı yoktur denemez; bizim kontrolümüzde ölçülmedi. Kaynak seçimi ve AEC etkisi ayrıştırılmadı. Kesin kök neden henüz yok.
- İlk küçük prototip: yalnız debug'da TTS PCM biçim/parça callback'lerinin geldiğini, ham sesi diske/ağa yazmadan doğrula. Aynı zamanda kayıt/çalma zamanlamasını ölç. Bu kabul insan gerektirmeden yapılabilir.
- Sonra yerel WebRTC APM için ARM64/Termux uyumlu bağımlılık ve derleme yolunu doğrula. Native paket eklemeden lisans/boyut ve yerel araç gereksinimlerini incele. Masaüstü/bulut derleme önerilmez.
- Kontrollü çıkış referansı + mikrofon temizleme hattını ayrı debug bileşeninde kur; ardından temiz seste VAD ve kısa ön tamponla kullanıcı başlangıcını yakala. VAD, bir kelimenin dur/Sezai olduğunu söylemez; her konuşmada kesme ile yalnız çağrıda kesme ürün davranışları ayrı değerlendirilir.
- Çıkış kuyruğunu anında durdur, model isteğini iptal et, aynı kayıt içindeki devam isteğini tamamla. TTS bitişi/yeni oturum callback yarışlarını ve konu bağlamını koru. Görüşme önceliğini geçersiz kılma; MODE_IN_COMMUNICATION eklemek mevcut mod korumasıyla çelişebilir, doğrudan eklenmez.

## Kabul ve enerji sınırı

Kullanıcı yokken TTS içindeki dur/Sezai kelimelerinde yanlış tetikleme olmamalı. Kullanıcı konuşmaya başladığında kesme gecikmesi gerçek söyleme anına göre ölçülmeli; takip isteğinin ilk kelimeleri kaybolmamalı. Aynı/yeni konu, görüşme, arka plan ve kilit denenmeli. Yakın geçmişte ses tamponu kaybı kanıtlanmadı; CPU süresi bunu tek başına göstermez. Yalnız etkin sesli oturumda işleme, ekran/uygulama kapanınca kaynak bırakma korunur. APM/VAD'nin gerçek CPU/pil maliyeti ölçülmeden düşük pil iddiası yapılmaz.

Araştırma sırasında APK veya üretim kodu değişmedi; yeni paket kurulmadı, ses testi başlatılmadı. Başarı vaadi yerine uygulanabilir, kaynaklı yol belirlendi.
