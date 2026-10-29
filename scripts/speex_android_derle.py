#!/usr/bin/env python3
"""Termux'ta debug JNI derler; --kontrol yalnız kaynak/çıktı özetlerini denetler."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

kok = Path(__file__).resolve().parents[1]
yerel = kok / 'yerel-model/speex-deneme'
kaynak = yerel / 'speexdsp-1.2.1'
android = yerel / 'android'
so = android / 'jni/arm64-v8a/libsezai_aec.so'
lisans = android / 'assets/third-party/speexdsp-COPYING.txt'
bilgi = android / 'derleme.json'
paket_bilgi = android / 'assets/third-party/sezai-aec-build.json'
arsiv = yerel / 'speexdsp-1.2.1.tar.gz'
pin = '8c777343e4a6399569c72abc38a95b24db56882c83dbdb6c6424a5f4aeb54d3d'
girdiler = [Path(__file__).resolve(), kok / 'scripts/speex_yerel_dene.py',
            kok / 'test/speex_yanki_test.c', kok / 'test/speex_android_jni.c', arsiv,
            kaynak / 'libspeexdsp/.libs/libspeexdsp.a', kaynak / 'COPYING']

def ozet(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def izler():
    return {str(p.relative_to(kok)): ozet(p) for p in girdiler + [so, lisans]}

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--kontrol', action='store_true')
a = p.parse_args()
try:
    if not arsiv.is_file() or ozet(arsiv) != pin:
        raise ValueError('SpeexDSP arşiv özeti eşleşmedi')
    if a.kontrol:
        kayit = json.loads(bilgi.read_text())
        if kayit.get('sha256') != izler() or paket_bilgi.read_bytes() != bilgi.read_bytes():
            raise ValueError('Native kaynak/çıktı eskimiş')
        print('Debug native kaynak/çıktı SHA256 kontrolü PASS')
        sys.exit(0)

    for arac in ('clang', 'ld.lld', 'readelf'):
        if not shutil.which(arac):
            raise ValueError('Eksik Termux aracı: ' + arac)

    def calistir(komut):
        subprocess.run(komut, cwd=kok, check=True, timeout=180)

    # Statik arşiv de bu telefonda aynı sabit kaynaktan üretilsin.
    calistir([sys.executable, str(kok / 'scripts/speex_yerel_dene.py')])
    if 'android' not in subprocess.check_output(['clang', '-dumpmachine'], text=True):
        raise ValueError('Clang Android hedefli değil')
    kok_arac = Path(shutil.which('clang')).resolve().parents[1]
    so.parent.mkdir(parents=True, exist_ok=True)
    lisans.parent.mkdir(parents=True, exist_ok=True)
    nesneler = []
    for ad in ('speex_android_jni', 'speex_yanki_test'):
        nesne = android / (ad + '.o')
        calistir(['clang', '-O2', '-Wall', '-Wextra', '-Werror', '-fPIC', '-fvisibility=hidden',
                  '-DSEZAI_KUTUPHANE', '-I' + str(kaynak / 'include'),
                  '-c', str(kok / ('test/' + ad + '.c')), '-o', str(nesne)])
        nesneler.append(str(nesne))
    # Clang sürücüsünün Termux özel RUNPATH'ini APK kütüphanesine taşımamak için
    # sistem C kitaplıklarıyla doğrudan bağla. Eksik sembolde derlemeyi durdur.
    calistir(['ld.lld', '-shared', '--no-undefined', '--exclude-libs', 'ALL',
              '-soname', 'libsezai_aec.so', '-z', 'now', '-z', 'relro',
              '-z', 'max-page-size=16384', '--hash-style=both',
              '-L' + str(kok_arac / 'lib'), '-L/system/lib64', str(kok_arac / 'lib/crtbegin_so.o'),
              *nesneler, str(kaynak / 'libspeexdsp/.libs/libspeexdsp.a'),
              '-lm', '-lc', '-ldl', str(kok_arac / 'lib/crtend_so.o'), '-o', str(so)])
    elf = subprocess.check_output(['readelf', '-h', str(so)], text=True)
    dinamik = subprocess.check_output(['readelf', '-d', str(so)], text=True)
    if 'AArch64' not in elf or 'RUNPATH' in dinamik or 'RPATH' in dinamik:
        raise ValueError('APK kütüphanesi mimari/yol kontrolü geçmedi')
    shutil.copyfile(kaynak / 'COPYING', lisans)
    metin = json.dumps({'surum': 'speexdsp-1.2.1', 'sha256': izler()}, indent=2)
    bilgi.write_text(metin)
    paket_bilgi.write_text(metin)
    print('Debug Android ARM64 JNI derlendi; özel RUNPATH yok:', so.relative_to(kok))
except (OSError, ValueError, subprocess.SubprocessError) as e:
    raise SystemExit('Native hazırlık başarısız: ' + str(e) +
                     '\nTermux içinde python3 scripts/speex_android_derle.py çalıştırın.')
