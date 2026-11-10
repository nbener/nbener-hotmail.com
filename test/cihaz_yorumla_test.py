#!/usr/bin/env python3
# Termux'tan çalıştırılır. Yalnızca yorumlar; alarm veya arama başlatmaz.
import pathlib
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

def am(komut):
    return subprocess.run(['/system/bin/sh', '-c', komut], capture_output=True, text=True, timeout=15)

# Zaman damgası: logcat bu andan itibaren filtrelenecek
zaman_damgasi = time.strftime('%m-%d %H:%M:%S.000')

for cumle, _ in cumleler:
    am('am start -n com.sezai.assistant/.MainActivity --es yorumla ' + shlex.quote(cumle))
    time.sleep(3)

# Logcat'ten bu turdan YORUM satırlarını topla
logcat_sonuc = subprocess.run(
    ['/system/bin/logcat', '-d', '-T', zaman_damgasi, '-v', 'raw', '-s', 'Sezai:I', '*:S'],
    capture_output=True, text=True, timeout=30)
gunluk = logcat_sonuc.stdout

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
