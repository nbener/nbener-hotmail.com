#!/usr/bin/env python3
# Termux'tan çalıştırılır. Yalnızca yorumlar; alarm veya arama başlatmaz.
import pathlib
import os
import re
import shlex
import subprocess
import time

kok = pathlib.Path(__file__).resolve().parent
cumleler = [satir.split('|', 1) for satir in
            (kok / 'cihaz_yorumla_cumleler.txt').read_text().splitlines() if satir]
cumleler += [
    ('bir buçukluk saat zamanlayıcı kur', 'Bilinmiyor'),
    ('bir buçuğxyz saat zamanlayıcı kur', 'Bilinmiyor'),
    ('bir buçuk dakika zamanlayıcı kur', 'ZamanlayiciKur(saniye=90)'),
]
isaret = 'SEZAI_TEST_' + str(time.time_ns())
komutlar = ['log -t SezaiTest ' + shlex.quote(isaret)]
for cumle, _ in cumleler:
    komutlar += ['am force-stop com.sezai.assistant',
                 'am start -n com.sezai.assistant/.MainActivity --es yorumla '
                 + shlex.quote(cumle) + ' >/dev/null', 'sleep 3']
# rish uzun çıktılarda karakter kaybedebiliyor; sonuç dosyasını doğrudan oku.
cikti = pathlib.Path('/sdcard/Download/' + isaret + '.txt')
komutlar += ["logcat -d -v raw -s Sezai:I SezaiTest:I '*:S' | grep -E '^YORUM |^SEZAI_TEST_' > " + shlex.quote(str(cikti))]
rish = kok.parent.parent / 'rish' / 'rish'
sonuc = subprocess.run(['sh', str(rish), '-c', '; '.join(komutlar)],
                       text=True, capture_output=True, timeout=300,
                       env=dict(os.environ, RISH_APPLICATION_ID='com.termux'))
if sonuc.returncode:
    raise SystemExit('Cihaz bağlantısı başarısız: ' + sonuc.stderr)
try:
    gunluk = cikti.read_text()
finally:
    if cikti.exists():
        cikti.unlink()
if isaret not in gunluk:
    raise SystemExit('Bu testin günlük işareti bulunamadı; sonuç doğrulanamadı.')
gunluk = gunluk.rsplit(isaret, 1)[1]
hata = 0
for cumle, beklenen in cumleler:
    kayitlar = re.findall(re.escape("YORUM '" + cumle + "' -> ") + r'([^\n]*)', gunluk)
    dogru = bool(kayitlar) and beklenen in kayitlar[-1]
    print(('PASS ' if dogru else 'FAIL ') + cumle, flush=True)
    if not dogru:
        print('  Beklenen:', beklenen, 'Gelen:', kayitlar or 'günlük yok', flush=True)
        hata += 1
print(f'Sonuç: {len(cumleler)-hata}/{len(cumleler)} başarılı', flush=True)
raise SystemExit(bool(hata))
