#!/usr/bin/env python3
"""Gerçek cihazda JNI yükleme ve yapay AEC. Mikrofon, hoparlör ve ağ açılmaz."""
import json
import os
from pathlib import Path
import shlex
import subprocess
import time

rish = Path(__file__).resolve().parents[2] / 'rish/rish'
env = dict(os.environ, RISH_APPLICATION_ID='com.termux')
kimlik = str(time.time_ns())

def cihaz(komut):
    r = subprocess.run(['sh', str(rish), '-c', komut], env=env, capture_output=True,
                       text=True, timeout=20)
    if r.returncode:
        raise SystemExit('Cihaz komutu başarısız: ' + str(r.returncode))

cihaz('am broadcast --include-stopped-packages -n com.sezai.assistant/.YankiYerelTestAlicisi --es test_id ' + kimlik)
dosya = Path('/sdcard/Download/SEZAI_AEC_' + kimlik + '.json')
try:
    for _ in range(15):
        cihaz('run-as com.sezai.assistant cat files/yanki_yerel_test.json | /system/bin/cat > ' + shlex.quote(str(dosya)))
        try:
            rapor = json.loads(dosya.read_text())
        except (OSError, ValueError):
            rapor = {}
        if rapor.get('test_id') == kimlik:
            print(json.dumps(rapor, ensure_ascii=False, indent=2))
            if not (rapor.get('sonuc') == 'PASS' and rapor.get('akis_kare_boyutu_ve_kapatma') is True
                    and rapor.get('mikrofon_kullanildi') is False
                    and rapor.get('gercek_insan_sesi') is False
                    and 0.75 < rapor.get('akis24_yakin_katsayi', 0) < 1.25
                    and rapor.get('akis24_artik_azalma_db', 0) > 6
                    and rapor.get('yapay_yanki_azalma_db', 0) > 15
                    and abs(rapor.get('referanssiz_kontrol_db', 2)) < 1
                    and 0.75 < rapor.get('yapay_ikinci_ses_katsayisi', 0) < 1.25
                    and rapor.get('birlikte_artik_azalma_db', 0) > 6):
                raise SystemExit('Cihaz JNI/yapay AEC kabulü geçmedi')
            print('Cihaz JNI/yapay AEC: PASS; gerçek söz kesme kabulü değildir.')
            break
        time.sleep(0.3)
    else:
        raise SystemExit('Bu çalışmanın AEC raporu alınamadı')
finally:
    if dosya.exists():
        dosya.unlink()
