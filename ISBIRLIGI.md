# Claude ↔ Codex iş birliği panosu

Kural: Claude (bulut, yalnız okuma/analiz/kayıt) ve Codex (Termux, derleme/cihaz) yalnız bu dosya ve diğer kayıt dosyaları üzerinden haberleşir.
Her iki taraf da CLAUDE.md kurallarına uyar. Gönderim: `scripts/github_aktar.sh`. Başlamadan önce: `git pull --rebase origin claude/sezai-voice-assistant-7wtvse`.
Kullanıcıdan teknik bilgi istenmez; kullanıcı yalnızca fiziksel işleri (ekranı açık tutma, canlı "dur/Sezai" deme) yapar.

## Kullanıcının hedefi (05.11.2026)
Temiz, imzalı, hizalı ve doğrulanmış bir APK. "Temiz": commitli kaynaktan, çalışma ağacı temizken derlenmiş; her adımın kanıtı kayıtta.

## GÖREV 1 — Codex'e (temiz APK) — durum: BEKLİYOR
Kaynak kod DEĞİŞTİRİLMEZ. Yalnız derle, doğrula, kur, kaydet.
1. Başlangıç: `git status --short` boş olmalı (değilse neyin değiştiğini yaz, devam etme). `git rev-parse --short HEAD` kaydet.
2. Derle: `proot-distro login ubuntu -- bash -c "cd /root/sezai && ./build.sh"`. Başarısızsa komutu ve hata mesajını olduğu gibi yaz.
3. Doğrula: `zipalign -c 4`, `apksigner verify` (v2/v3), APK SHA256. İmzasız/doğrulanmamış APK sunulmaz.
4. Mevcut kaynakla yeniden çalıştırılabilen testler (TEST_DURUMU.md "Doğrulama komutları"): sohbet, dinleme, görüşme, yorumlama. Sonuçları sayılarıyla yaz; sahte ağ/yapay sesi gerçek kabul diye yazma.
5. Kurulum: `./build.sh kur` (Shizuku köprüsü çalışıyorsa). /sdcard/Download/Sezai-debug.apk aynı SHA256 olmalı.
6. Not düş: imza şu an DEBUG anahtarıdır; release anahtarı oluşturulmadı (şifreyi kullanıcı belirler). Anahtarı kendiliğinden oluşturma.
7. Sonucu aşağıdaki "Codex → Claude" bölümüne ve DURUM.md/TEST_DURUMU.md'ye yaz; yerel commit; `scripts/github_aktar.sh`.

Bilinen sınır (kullanıcıya dürüst söylenecek): temiz APK mevcut kaynağı derler. TTS sırasında "dur/Sezai" söz kesme BAŞARISIZ/açık kalır; temiz derleme bunu düzeltmez.

## GÖREV 2 — Söz kesme karşılaştırması — durum: GÖREV 1 SONRASI
Aynı hoparlör/ses düzeyi/kayıt kaynağı sabit; tek değişken sırayla:
1. TTS kapalı, insan yok: yanlış tetik (sıfır beklenir).
2. TTS kapalı, insan "dur"×5 ve "Sezai"×5 (normal ses düzeyi): dar ve serbest tanıyıcı ayrı sayılarla.
3. TTS açık, insan yok: TTS'in kendi sesiyle yanlış tetik.
4. TTS açık, insan "dur"×5 ve "Sezai"×5: asıl hedef.
Sayılarla raporla (5 denemede kaç). Kullanıcı her denemeyi söylediğini teyit eder. Kulaklık takılı olmayacak. Karar ölçütü: tur 2'de ikisi iyi, tur 4'te biri belirgin düşükse yankı sorunu (SOZ_KESME_TASARIMI.md seçenek B/C/D); tur 2'de serbest de kötüyse sorun serbest tanıyıcıdır. Seçenek seçimi kullanıcıya aittir; kod değiştirmeden önce dosyaları ve nedeni yaz.

## Codex → Claude (sonuçlar buraya, tarihle)
_(henüz kayıt yok)_

## Claude → Codex (notlar buraya, tarihle)
- 05.11.2026: Görevler yukarıda. Kayıtlarda kullandığın "doğrulanan / çıkarım / bilinmeyen" ayrımını koru; saatlik kontrolümde yeni commit'i okuyup özetleyeceğim.
