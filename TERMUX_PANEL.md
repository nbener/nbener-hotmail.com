# Türkçe Termux kontrol paneli

Termux terminalinde `panel` yaz. Menüde boş alan, pil, araç kontrolü,
SD karta şifreli kaynak yedeği, Sezai derleme ve derleyip kurma seçenekleri vardır.
Doğrudan kullanım: `panel durum`, `panel pil`, `panel araclar`, `panel yedek`,
`panel derle`, `panel kur`, `panel ses`.

## Telefona uygun görünüm ve tuşlar

`panel` renkli, dar ekranlara uygun menüyü açar. Menüde **7 — Tuş düzeni**
seçeneği bulunur. Lacivert tema değiştirilmedi.

- `tuslar tek`: en az alan kaplayan tek satır; Ses düğmesi de buradadır.
  CTRL yukarı kaydırılınca ALT, sol ok yukarı kaydırılınca yukarı ok,
  sağ ok yukarı kaydırılınca aşağı ok, Yapıştır yukarı kaydırılınca Enter,
  Ses yukarı kaydırılınca Panel açılır. Gerçek yatay kayan sayfalar değildir.
- `tuslar kompakt`: iki satır. Ok tuşlarını yukarı
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

## Türkçe sesli yazma

Kabuk istemindeyken tek satırdaki **Ses** düğmesine bas veya `sesliyaz` yaz.
Türkçe konuşma penceresi açılır. Sonuç panoya alınır; **Yapıştır** düğmesiyle
aktarılır, kendiliğinden komut olarak çalıştırılmaz veya gönderilmez.
İptal/hata/boş sonuç durumunda pano korunur. Android gerekli mikrofon iznini
henüz vermediysen izin sorabilir. Tanıma motoru internet kullanabilir.

Bu telefonda sistem dili tr-TR, Gboard etkin dili tr-TR ve Gboard mikrofon
izni doğrulandı. Dialog API cihaz dilini kullanır. Kurulu SpeechToText API
en-US diline sabit olduğu için kullanılmadı. Başka bir cihazda veya sistem
dili değişince Türkçe tanıma yeniden doğrulanmalıdır.

Codex/Claude gibi bir programın içindeyken doğrudan yazdırmak için Gboard'un
kendi mikrofonunu kullan. Ek tuş satırındaki Ses bir kabuk makrosudur;
çalışan programa komut göndermek amacıyla kullanılmamalıdır. Ses aracı yalnız
istendiğinde tek dikte alır; sürekli mikrofon servisi eklenmedi.

Dayanak: [DialogAPI 0.53.0](https://github.com/termux/termux-api/blob/v0.53.0/app/src/main/java/com/termux/api/apis/DialogAPI.java),
[Gboard sesli yazma](https://support.google.com/gboard/answer/2781851?hl=tr).

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
