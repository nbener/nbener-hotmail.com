#!/usr/bin/env python3
# Gerçek API/arama kotası kullanır: sabit ve kişisel veri içermeyen TEK sorgu.
# Kullanıcının kendi anahtarı uygulama içinde kullanılır, betiğe/rapora çıkmaz.
import json
import os
import pathlib
import shlex
import subprocess
import time

rish = pathlib.Path(__file__).resolve().parents[2] / 'rish' / 'rish'
ortam = dict(os.environ, RISH_APPLICATION_ID='com.termux')
def calistir(komut):
    return subprocess.run(['sh', str(rish), '-c', komut], capture_output=True,
                          text=True, timeout=20, env=ortam)

kimlik = str(time.time_ns())
r = calistir('am start -n com.sezai.assistant/.WebCanliTestActivity --es test_id '+shlex.quote(kimlik))
if r.returncode:
    raise SystemExit('Canlı test ekranı açılamadı.')
p = pathlib.Path('/sdcard/Download/SEZAI_WEB_CANLI_'+kimlik+'.json')
try:
    for _ in range(60):
        calistir('run-as com.sezai.assistant cat files/web_canli_test_sonuc.json | /system/bin/cat > '+shlex.quote(str(p)))
        try:
            rapor = json.loads(p.read_text())
        except (OSError, ValueError):
            rapor = {}
        if rapor.get('test_id') == kimlik:
            print('Canlı test:', rapor.get('sonuc'))
            print(rapor.get('mesaj', ''))
            # Yanıt almak, kaynak bilgisinin doğruluğunu tek başına kanıtlamaz.
            raise SystemExit(0 if rapor.get('sonuc') == 'YANIT_ALINDI' else 2)
        time.sleep(1)
    raise SystemExit('Canlı test sonucu alınamadı; başarı sayılmadı.')
finally:
    if p.exists(): p.unlink()
