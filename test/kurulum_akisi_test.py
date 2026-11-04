#!/usr/bin/env python3
"""Kurulum hatasında uygulama açılmaz; gerçek cihaza bağlanmadan doğrular."""
import os
from pathlib import Path
import subprocess
import tempfile

betik = (Path(__file__).resolve().parents[1] / 'build.sh').read_text()
komut = betik.split("sh ./rish -c '", 1)[1].rsplit("'", 1)[0].replace("'\\''", "'")
with tempfile.TemporaryDirectory() as gecici:
    dizin = Path(gecici)
    taklitler = {
        'mktemp': 'echo "$TEST_DIR/gecici.apk"',
        'cp': 'exit "${CP_STATUS:-0}"',
        'pm': 'exit "${PM_STATUS:-0}"',
        'am': 'echo acildi >> "$TEST_DIR/acildi"',
        'rm': 'echo temizlendi >> "$TEST_DIR/temizlendi"',
    }
    for ad, govde in taklitler.items():
        dosya = dizin / ad
        dosya.write_text('#!/bin/sh\n' + govde + '\n')
        dosya.chmod(0o755)
    for ad, kopya, kurulum in [('kopya_hatasi', 1, 0), ('kurulum_hatasi', 0, 1), ('basarili', 0, 0)]:
        for isaret in ['acildi', 'temizlendi']:
            (dizin / isaret).unlink(missing_ok=True)
        ortam = dict(os.environ, PATH=gecici + ':' + os.environ['PATH'],
                     TEST_DIR=gecici, CP_STATUS=str(kopya), PM_STATUS=str(kurulum))
        sonuc = subprocess.run(['sh', '-c', komut], env=ortam, timeout=10)
        basarili = kopya == 0 and kurulum == 0
        assert (sonuc.returncode == 0) == basarili, ad
        assert (dizin / 'acildi').exists() == basarili, ad
        assert (dizin / 'temizlendi').exists(), ad
        print('PASS', ad)
print('3/3 yerel kurulum akışı geçti; gerçek cihaz kurulumu değildir.')
