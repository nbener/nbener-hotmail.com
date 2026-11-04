# Türkçe Termux kontrol paneli

Termux terminalinde `panel` yaz. Menüde boş alan, pil, araç kontrolü,
SD karta şifreli kaynak yedeği, Sezai derleme ve derleyip kurma seçenekleri vardır.
Doğrudan kullanım: `panel durum`, `panel pil`, `panel araclar`, `panel yedek`,
`panel derle`, `panel kur`.

## Telefona uygun görünüm ve tuşlar

`panel` renkli, dar ekranlara uygun menüyü açar. Menüde **7 — Tuş düzeni**
seçeneği bulunur. Lacivert tema değiştirilmedi.

- `tuslar kompakt`: günlük kullanım için iki satır. Ok tuşlarını yukarı
  kaydırınca HOME/END/PGUP/PGDN açılır. Codex, Claude, Panel, yapıştırma,
  silme, Enter, temizleme ve klavye düğmeleri ikinci satırdadır.
- `tuslar tam`: önceki beş satırın bütün tuşlarını ve makrolarını getirir.
- `tuslar eski`: özgün `termux.properties` dosyasını birebir geri yükler.

Komut yazan Codex/Claude/Panel düğmelerini kabuk istemindeyken kullan;
başka bir program açıksa makro metni o programa gider. Yalnız tuş gönderen
ESC, CTRL, oklar gibi düğmeler etkin programda çalışır.

Özgün `.termux` klasörü, kabuk başlangıç dosyaları ve kısayolların yedeği
`~/.local/share/termux-panel/gorunum-yedek-*` içindedir. Tam yolu
`~/.local/share/termux-panel/aktif-yedek.txt` dosyasında bulunur.
`SHA256.json` yedek dosyalarının özetlerini tutar. Bu özel yedek Git'e veya
SD karta kopyalanmaz. `tuslar eski` yalnız tuş ayar dosyasını geri yükler;
diğer yedek dosyaları da gerektiğinde elle geri alınabilir.

İki satır, önceki beş satıra göre satır sayısını %60 azaltır; ekranda
kazanılan gerçek piksel alanı cihaza bağlıdır. Android'in Gboard harf
klavyesinin yüksekliği ayrı bir ayardır; bu işlem Gboard ayarını değiştirmez.

Panel yalnız açıldığında çalışır; arka plan servisi, otomatik başlangıç,
mikrofon dinlemesi veya wake lock eklemez. Derleme mevcut Ubuntu proot
`/root/sezai/build.sh` betiğini kullanır; APK imza/hizalama kontrolü oradadır.
Kurulum için mevcut Shizuku/rish bağlantısı gerekir.

## Ana ekran

Android ana ekranında boş yere uzun bas → Widget'lar → Termux:Widget ekle.
Kısayollar: Termux Panel, Pil Bilgisi, Bos Alan, Sezai Derle.
Android başlatıcısına widget ekleme kullanıcı tarafından yapılır.
Kısayollar `~/.shortcuts` içinde, terminal komutu `~/bin/panel` içindedir.
Panel kaynağı bu depodaki `scripts/termux_panel.py` dosyasıdır.

## Şifreli SD kart yedeği

Hedef: `/storage/1AA3-3380/Sezai-Sifreli-Yedekler`.
`age` parola sorar; parola kaydedilmez. Kaybolursa yedek açılamaz.
Yedek yalnız son Git commitindeki dosyaları içerir; `.git`, imza anahtarı,
yerel modeller, mevcut `yedek/` arşivleri ve uygulama kullanıcı verileri dahil değildir.
Commit edilmemiş değişiklik varsa işlem durur. Var olan yedekler silinmez.
Yanındaki SHA-256 kaydı şifreli dosyanın bütünlüğünü kontrol etmek içindir;
şifre çözme veya geri yükleme testi yerine geçmez.

Geri açma (Termux özel alanına, yeterli boş alan olduğunda):
`age -d -o ~/kaynak.tar.gz /storage/1AA3-3380/Sezai-Sifreli-Yedekler/DOSYA.tar.gz.age`
Arşivi önce incele; mevcut projenin üzerine doğrudan açma.

## Doğrulama kapsamı

Panel eklemek Android uygulamasını değiştirmez. Menü, durum, pil ve araç
kontrolleri ile kaynak yedeğinin şifreleme/çözme yolu ayrı doğrulanır.
Panel üzerinden yeni APK derlendiği veya fiziksel Widget düğmelerinin
çalıştığı ancak gerçekten denenirse raporlanır.
