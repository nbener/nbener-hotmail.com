#!/usr/bin/env python3
"""İndirilmiş SpeexDSP 1.2.1 ile telefonda mikrofonsuz yapay AEC deneyi.

Sistem paketi kurmaz, make install çalıştırmaz, APK'yı değiştirmez.
Arşiv yerel-model/speex-deneme altında bulunmalı; kaynak özeti sabittir.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import time

kok = Path(__file__).resolve().parents[1]
yerel = kok / 'yerel-model/speex-deneme'
arsiv = yerel / 'speexdsp-1.2.1.tar.gz'
kaynak = yerel / 'speexdsp-1.2.1'
beklenen_sha = '8c777343e4a6399569c72abc38a95b24db56882c83dbdb6c6424a5f4aeb54d3d'
for arac in ('clang', 'make', 'sh', 'readelf'):
    if not shutil.which(arac):
        raise SystemExit('Eksik yerel araç: ' + arac)
if not arsiv.is_file() or hashlib.sha256(arsiv.read_bytes()).hexdigest() != beklenen_sha:
    raise SystemExit('SpeexDSP 1.2.1 arşivi yok veya SHA256 eşleşmedi; derleme yapılmadı.')
if not kaynak.exists():
    with tarfile.open(arsiv) as t:
        t.extractall(yerel, filter='data')
kimlik = str(time.time_ns())
log = yerel / ('derleme-' + kimlik + '.log')
ortam = dict(os.environ, CONFIG_SHELL=shutil.which('sh'))

def calistir(komut, dizin):
    r = subprocess.run(komut, cwd=dizin, env=ortam, text=True, capture_output=True, timeout=180)
    with log.open('a') as f:
        f.write(r.stdout + r.stderr)
    if r.returncode:
        raise SystemExit('Komut başarısız: ' + repr(komut) + '\n' + (r.stdout + r.stderr)[-2000:])
    return r.stdout

calistir([shutil.which('sh'), './configure', '--build=aarch64-linux-android',
          '--host=aarch64-linux-android', '--disable-shared', '--enable-static',
          '--disable-examples', '--disable-neon', '--disable-sse',
          'CC=' + shutil.which('clang'), 'CFLAGS=-O2 -fPIC'], kaynak)
calistir(['make', '-j2', '-C', 'libspeexdsp'], kaynak)
ikili = yerel / 'speex_yanki_test'
calistir(['clang', '-O2', '-Wall', '-Wextra', '-Werror', '-I' + str(kaynak / 'include'),
          str(kok / 'test/speex_yanki_test.c'), str(kaynak / 'libspeexdsp/.libs/libspeexdsp.a'),
          '-lm', '-o', str(ikili)], kok)
elf = calistir(['readelf', '-h', str(ikili)], kok)
program = calistir(['readelf', '-l', str(ikili)], kok)
if 'AArch64' not in elf or '/system/bin/linker64' not in program:
    raise SystemExit('Derlenen dosya Android ARM64 hedefini doğrulamadı.')
rapor = json.loads(calistir([str(ikili)], kok))
if not (rapor.get('gecti') is True and rapor['yapay_yanki_azalma_db'] > 15
        and abs(rapor['referanssiz_kontrol_db']) < 1
        and 0.75 < rapor['yapay_ikinci_ses_katsayisi'] < 1.25
        and rapor['birlikte_artik_azalma_db'] > 6):
    raise SystemExit('Yapay AEC kabul ölçütleri geçmedi.')
rapor.update(test_id=kimlik, kaynak_sha256=beklenen_sha,
             mikrofon_kullanildi=False, gercek_insan_sesi=False, apk_degisti=False)
(yerel / ('sonuc-' + kimlik + '.json')).write_text(json.dumps(rapor, ensure_ascii=False, indent=2))
print(json.dumps(rapor, ensure_ascii=False, indent=2))
print('Yapay referanslı AEC: PASS. Gerçek söz kesme ve pil kabulü değildir.')
