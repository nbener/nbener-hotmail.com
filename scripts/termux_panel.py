#!/data/data/com.termux/files/usr/bin/python3
"""Telefonda, yalnızca kullanıcı başlattığında çalışan Türkçe kontrol paneli."""
import argparse
import datetime
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

PROJE = Path(__file__).resolve().parent.parent
SD = Path('/storage/1AA3-3380')
ARACLAR = 'git python3 node rg fd fzf jq yq tmux rsync zip 7zz cmake ninja sqlite3 htop ncdu ffmpeg magick tesseract pdftotext nvim shellcheck aria2c age gpg termux-clipboard-set'.split()


def calistir(komut, **kwargs):
    return subprocess.run(komut, check=True, **kwargs)


def durum():
    print('\nDEPOLAMA')
    for ad, yol in [('Telefon', Path('/data')), ('SD kart', SD)]:
        try:
            alan = shutil.disk_usage(yol)
            print(f'{ad}: {alan.free / 2**30:.2f} GiB boş / {alan.total / 2**30:.2f} GiB toplam')
        except OSError as hata:
            print(f'{ad}: erişilemiyor ({hata.strerror})')
    print(f'\nSezai kaynakları: {PROJE}')


def pil():
    if not shutil.which('termux-battery-status'):
        raise RuntimeError('Termux:API komut paketi bulunamadı.')
    sonuc = calistir(['termux-battery-status'], capture_output=True, text=True, timeout=15)
    veri = json.loads(sonuc.stdout)
    for ad, anahtar in [('Şarj (%)', 'percentage'), ('Sıcaklık (°C)', 'temperature'),
                         ('Durum', 'status'), ('Sağlık', 'health'), ('Güç bağlantısı', 'plugged')]:
        print(f'{ad}: {veri.get(anahtar, "bilinmiyor")}')


def araclar():
    eksik = []
    for ad in ARACLAR:
        var = shutil.which(ad) is not None
        print(f'{"VAR" if var else "EKSİK":6} {ad}')
        if not var:
            eksik.append(ad)
    print('\nBu kontrol komutun varlığını gösterir; tüm işlevlerinin test edildiği anlamına gelmez.')
    return int(bool(eksik))


def yedek():
    if not sys.stdin.isatty():
        raise RuntimeError('Şifreli yedek için paneli etkileşimli Termux terminalinde aç.')
    if not SD.is_dir() or SD.stat().st_dev == Path('/data').stat().st_dev:
        raise RuntimeError('Ayrı SD karta erişim doğrulanamadı.')
    if not shutil.which('age'):
        raise RuntimeError('age şifreleme aracı bulunamadı.')
    degisiklik = calistir(['git', '-C', str(PROJE), 'status', '--porcelain'], capture_output=True, text=True).stdout
    if degisiklik.strip():
        raise RuntimeError('Commit edilmemiş dosyalar var. Eksik yedek oluşturmamak için önce değişiklikleri inceleyip commit et.')
    commit = calistir(['git', '-C', str(PROJE), 'rev-parse', 'HEAD'], capture_output=True, text=True).stdout.strip()
    hedef = SD / 'Sezai-Sifreli-Yedekler'
    hedef.mkdir(exist_ok=True)
    zaman = datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    dosya = hedef / f'sezai-kaynak-{zaman}-{commit[:8]}.tar.gz.age'
    gecici = dosya.with_suffix('.age.partial')
    print('Son Git commitindeki kaynaklar şifreleniyor; imza anahtarı, modeller ve yerel veriler dahil değildir.')
    print('age yedek parolasını soracak. Parolayı güvenli sakla; kaybolursa yedek açılamaz.')
    try:
        # Açık kaynak arşivi yalnız Termux özel geçici klasöründe tutulur.
        with tempfile.TemporaryDirectory(prefix='sezai-panel-') as tmp:
            arsiv = Path(tmp) / 'kaynak.tar.gz'
            with arsiv.open('xb') as cikti:
                calistir(['git', '-C', str(PROJE), 'archive', '--format=tar.gz', commit], stdout=cikti)
            if shutil.disk_usage(SD).free < arsiv.stat().st_size + 16 * 2**20:
                raise RuntimeError('SD kartta yedek için yeterli boş alan yok.')
            calistir(['age', '-p', '-o', str(gecici), str(arsiv)])
        if not gecici.is_file() or gecici.stat().st_size == 0:
            raise RuntimeError('Şifreli yedek üretilemedi.')
        with gecici.open('rb') as akis:
            os.fsync(akis.fileno())
            ozet = hashlib.file_digest(akis, 'sha256').hexdigest()
        gecici.rename(dosya)
        dosya.with_suffix('.age.sha256').write_text(f'{ozet}  {dosya.name}\n')
        print(f'Şifreli yedek oluşturuldu: {dosya}')
        print('SHA-256 kaydı yazıldı. Geri yükleme bu işlemde denenmedi.')
    finally:
        if gecici.exists():
            gecici.unlink()


def derle(kur=False):
    if not shutil.which('proot-distro'):
        raise RuntimeError('Termux proot-distro bulunamadı; paneli Termux içinden çalıştır.')
    print('Sezai mevcut build.sh ile derleniyor; kurulum seçildiyse Shizuku/rish gerekir.', flush=True)
    calistir(['proot-distro', 'login', 'ubuntu', '--', 'bash', '-c',
              'cd /root/sezai && ./build.sh' + (' kur' if kur else '')])


def eylem(ad):
    if ad == 'masaustu':
        return calistir(['bash', str(PROJE / 'scripts/termux_masaustu.sh')]).returncode
    if ad == 'ses':
        return calistir([sys.executable, str(PROJE / 'scripts/termux_sesliyaz.py')]).returncode
    if ad == 'tuslar':
        return calistir([sys.executable, str(PROJE / 'scripts/termux_tuslar.py')]).returncode
    # Aynı anda iki derleme/yedek işleminin başlamasını önler.
    if ad in ('yedek', 'derle', 'kur'):
        kilit_yolu = Path(tempfile.gettempdir()) / f'sezai-panel-{os.getuid()}.lock'
        with kilit_yolu.open('a') as kilit:
            try:
                fcntl.flock(kilit, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as hata:
                raise RuntimeError('Başka bir panel derleme/yedek işlemi çalışıyor.') from hata
            if ad == 'yedek':
                return yedek()
            return derle(ad == 'kur')
    return {'durum': durum, 'pil': pil, 'araclar': araclar}[ad]()


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description='Türkçe Termux kontrol paneli')
    parser.add_argument('eylem', nargs='?', choices=['durum', 'pil', 'araclar', 'yedek', 'derle', 'kur', 'tuslar', 'ses', 'masaustu'])
    args = parser.parse_args()
    if args.eylem:
        return eylem(args.eylem) or 0
    if not sys.stdin.isatty():
        parser.print_help()
        return 0
    secimler = {'1': 'durum', '2': 'pil', '3': 'araclar', '4': 'yedek', '5': 'derle', '6': 'kur', '7': 'tuslar', '8': 'ses', '9': 'masaustu'}
    while True:
        renk = sys.stdout.isatty() and 'NO_COLOR' not in os.environ
        vurgu, son = ('\033[1;36m', '\033[0m') if renk else ('', '')
        print(f'\n{vurgu}╭────────────────────────────────╮\n│  TERMUX · TELEFON PANELİ        │\n╰────────────────────────────────╯{son}')
        try:
            print(f'  Telefon {shutil.disk_usage("/data").free / 2**30:.1f} GiB boş  ·  Kullanıma hazır')
        except OSError:
            pass
        print('\n  1  Depolama       2  Pil\n  3  Araç kontrolü  4  Şifreli yedek\n  5  Sezai derle    6  Derle ve kur\n  7  Tuş düzeni     8  Türkçe ses\n  9  Masaüstü       0  Çıkış')
        secim = input('Seçimin: ').strip()
        if secim == '0':
            return 0
        if secim not in secimler:
            print('0–9 arasında bir seçim yap.')
            continue
        try:
            eylem(secimler[secim])
        except (RuntimeError, OSError, ValueError, subprocess.SubprocessError) as hata:
            print(f'İşlem tamamlanmadı: {hata}', file=sys.stderr)
        input('\nMenüye dönmek için Enter...')


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (KeyboardInterrupt, EOFError):
        print('\nPanel kapatıldı.')
        sys.exit(130)
    except (RuntimeError, OSError, ValueError, subprocess.SubprocessError) as hata:
        print(f'İşlem tamamlanmadı: {hata}', file=sys.stderr)
        sys.exit(1)
