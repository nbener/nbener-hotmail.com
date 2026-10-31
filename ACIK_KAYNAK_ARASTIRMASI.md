# Açık kaynak ve yöntem araştırması (31 Ekim 2026)

Proje özel ve satılacak: GPL/AGPL kod ürüne GÖMÜLMEZ; yalnızca tasarım fikri gözlemlenir. Apache-2.0/MIT/BSD kullanılabilir (lisans metni korunur). Aşağıdaki "doğrulanmadı" ibaresi, web aramasının özetine dayanır, depo/lisans dosyası açılıp teyit edilmedi.

## Kaynaklar ve karar
| Konu | Bulgu | Lisans | Karar |
|---|---|---|---|
| OsmAnd (navigasyon, sesli yönlendirme, hız sınırı uyarısı) | Çevrimdışı rota, sesli yönlendirme, isteğe bağlı hız sınırı gösterimi; kaynak GitHub'da. Wikipedia: https://en.wikipedia.org/wiki/OsmAnd | GPLv3 (Wikipedia'ya göre) | Kod KOPYALANMAZ. Yalnız davranış örneği. |
| Organic Maps | Bu aramada doğrulanamadı; bellekten Apache-2.0 olduğu düşünülüyor | DOĞRULANMADI | Depo/LICENSE dosyası açılmadan kullanılmaz. |
| OSM hız sınırı | Overpass sorgusu biçimi: `way[maxspeed](around:R,lat,lon); out tags;` (en yakın yol kendimiz seçilmeli; düğüm üzerindeki maxspeed anlamsız). https://help.openstreetmap.org/questions/68159/get-maxspeed-from-lat-lon-values | ODbL (veri) | Araç modunda internetle; Türkiye'de etiket doluluğu test edilmeli. |
| Hız kamerası | `highway=speed_camera` düğüm etiketi OSM'de var; Türkiye kapsamı bu aramada doğrulanmadı | ODbL | Kapsam ölçülmeden güvenilir uyarı sözü verilmez. |
| Silero VAD | ~2 MB, 32 ms/512 örnek akışlı konuşma algısı, ONNX Runtime ile Android'de çalışır (https://soniqo.audio/guides/vad/android); MIT (üçüncü taraf kopyadaki başlığa göre) | MIT (doğrulanmadı) | Söz kesme v2'de enerji eşiği yerine aday. ONNX Runtime bağımlılığı ve boyut kararı gerekir. |
| openWakeWord | Apache-2.0 olduğu ima edildi, resmî depodan doğrulanmadı | DOĞRULANMADI | Vosk "sezai" yerine düşük güçlü uyanma sözcüğü adayı; Türkçe modeli yok, eğitim gerekir. |
| Dicio (Android asistan) | Yerelde çalışan, beceri (skill) mimarili Kotlin asistanı: arama, hava, navigasyon, zamanlayıcı, medya. https://sourceforge.net/mirror/dicio-assistant/profile/ | GPLv3 | Kod KOPYALANMAZ; beceri mimarisi fikri alınır. |
| Rhasspy | Uyanma→konuşma tanıma→niyet→niyet yürütme→TTS boru hattı. https://rhasspy.readthedocs.io/en/latest/about/ | MIT | Mimari fikir: ayrı niyet tanıma ve yürütme katmanı. |
| Camera2 kamerasız yakalama | Resmi doküman: JPEG için ImageReader yüzeyi yeterli (https://developer.android.com/reference/android/hardware/camera2/package-summary). Kamera ön plan servis türü: FOREGROUND_SERVICE_CAMERA / FOREGROUND_SERVICE_TYPE_CAMERA (https://developer.android.com/develop/background-work/services/fg-service-types). | Android API | Kullanıldı. Arka planda/kilitliyken kamera kısıtı Android 14'te araştırmadan KESİN söylenemez, cihazda denenecek. |
| Araç algılama | Bluetooth sınıfı `BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO` ile bağlı cihaz denetlenir (Stack Overflow özeti); araçlar sınıfı tutarsız bildirir, tek başına yetmez. Etkinlik tanıma (IN_VEHICLE) bu aramada doğrulanamadı. | Android API | Araç modu: sözlü "arabadayım" + Bluetooth sınıfı + GPS hızı birlikte. |
| Groq görsel model | Resmî sayfa `qwen/qwen3.8-27b` (en çok 3 görsel, görsel başı 2048 token); önbellek kopyası ve aramada llama-4-scout/qwen3.6 geçti: çelişki. https://console.groq.com/docs/vision | — | Model adı ayarlanabilir; ilk canlı istekte erişim doğrulanır. |

## Kararlar
1. Hız, hız sınırı, uyarılar: Sezai katmanı (GPS + OSM). Rota ve dönüş talimatı Google Haritalar'da (intent `google.navigation:q=`).
2. Sesli yeteneklerde niyet tanıma ve yürütme ayrı katman olacak (Rhasspy fikri); yetenek kaydı tek yerde (Dicio fikri), kod kopyalanmadan.
3. Kamera: Camera2 + ImageReader, tek kare, saklanmaz.
