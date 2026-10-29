# Yerel Vosk debug denemesi — üçüncü taraf kayıtları

- com.alphacephei:vosk-android:0.3.75: Apache License 2.0. Kaynak/lisans: https://github.com/alphacep/vosk-api/blob/master/COPYING . Maven POM ve orijinal AAR korunur.
- net.java.dev.jna:jna:5.18.1: JNA'nın LGPL 2.1 veya Apache 2.0 çift lisansı; Apache 2.0 seçeneği esas alınır. Orijinal lisans/NOTICE: https://github.com/java-native-access/jna/tree/5.18.1 . Paket içindeki lisans bildirimleri korunur.
- vosk-model-small-tr-0.3: resmî model listesinde Apache 2.0, 35M, Türkçe doğruluk TBD. https://alphacephei.com/vosk/models . Model yerel dosyada; repoya veya dışarı aktarılmadı. ZIP README yalnız “Turkish model” içeriyor.

Bu kayıt resmî lisans metinlerinin yerine geçmez. Deneme debug kaynak kümesine bağlıdır; release sürümüne model/motor eklenmedi. Debug APK assets/third-party içinde Vosk COPYING, JNA LICENSE/AL2.0 ve model kaynak kaydı bulunur. Release entegrasyonunda ilgili tüm NOTICE bildirimleri ayrıca kontrol edilmelidir.
