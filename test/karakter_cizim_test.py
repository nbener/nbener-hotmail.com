#!/usr/bin/env python3
# Ekran uyandırmadan gerçek Android Canvas ile altı durağan ifade çizer. Ses/API yok.
import json
import os
import pathlib
import shlex
import subprocess
import time

rish = pathlib.Path(__file__).resolve().parents[2] / 'rish' / 'rish'
ortam = dict(os.environ, RISH_APPLICATION_ID='com.termux')
def uzak(komut):
    r = subprocess.run(['sh', str(rish), '-c', komut], env=ortam,
                       capture_output=True, text=True, timeout=25)
    if r.returncode: raise SystemExit('Cihaz komutu başarısız: '+str(r.returncode))
kimlik = str(time.time_ns())
uzak('am broadcast --include-stopped-packages -n com.sezai.assistant/.KarakterCizimTestAlicisi --es test_id '+shlex.quote(kimlik))
p = pathlib.Path('/sdcard/Download/SEZAI_KARAKTER_CIZIM_'+kimlik+'.json')
try:
    for _ in range(15):
        uzak('run-as com.sezai.assistant cat files/karakter_cizim_test.json | /system/bin/cat > '+shlex.quote(str(p)))
        try: rapor = json.loads(p.read_text())
        except (OSError, ValueError): rapor = {}
        if rapor.get('test_id') == kimlik:
            satirlar = rapor.get('rapor', [])
            print('\n'.join(satirlar))
            if len(satirlar) != 6 or any(not x.startswith('PASS ') for x in satirlar): raise SystemExit(1)
            gorsel = pathlib.Path('/sdcard/Download/sezai-karakter-duragan-'+kimlik+'.png')
            uzak('run-as com.sezai.assistant cat files/karakter-duragan.png | /system/bin/cat > '+shlex.quote(str(gorsel)))
            assert gorsel.exists() and gorsel.stat().st_size > 1000, 'Çizim aktarılamadı'
            print('Görsel:', gorsel)
            print('Sonuç: 6/6 başarılı; durağan Canvas, canlı animasyon testi değildir.')
            break
        time.sleep(.5)
    else: raise SystemExit('Bu çalışmanın çizim raporu alınamadı.')
finally:
    if p.exists(): p.unlink()
