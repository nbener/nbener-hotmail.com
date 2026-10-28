#!/usr/bin/env python3
# Termux'tan: python3 scripts/durum_bildir.py 'Yorumlama testleri: 20/20 başarılı.'
import shlex
import os
from pathlib import Path
import subprocess
import sys

if len(sys.argv) != 2 or not sys.argv[1].strip():
    raise SystemExit('Kullanım: python3 scripts/durum_bildir.py "Durum mesajı"')
mesaj = sys.argv[1].strip()[:300]
komut = 'am broadcast --include-stopped-packages -n com.sezai.assistant/.GelistirmeDurumAlicisi --es mesaj ' + shlex.quote(mesaj)
rish = Path(__file__).resolve().parents[2] / 'rish' / 'rish'
sonuc = subprocess.run(['sh', str(rish), '-c', komut], timeout=30,
                       env=dict(os.environ, RISH_APPLICATION_ID='com.termux'))
raise SystemExit(sonuc.returncode)
