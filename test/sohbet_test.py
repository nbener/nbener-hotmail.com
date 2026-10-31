#!/usr/bin/env python3
# Termux'tan sessiz cihaz testi. Gerçek API, mikrofon veya kullanıcı anahtarı kullanılmaz.
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
r = calistir('am broadcast --include-stopped-packages -n com.sezai.assistant/.SohbetTestAlicisi --es test_id '+shlex.quote(kimlik))
if r.returncode:
    raise SystemExit('Test başlatılamadı.')
cikti = pathlib.Path('/sdcard/Download/SEZAI_SO_TEST_'+kimlik+'.txt')
try:
    for _ in range(12):
        calistir('run-as com.sezai.assistant cat files/sohbet_test_sonuc.txt | /system/bin/cat > '+shlex.quote(str(cikti)))
        satirlar = cikti.read_text().splitlines() if cikti.exists() else []
        if satirlar and satirlar[0] == 'TEST_ID '+kimlik:
            rapor = satirlar[1:]
            if len(rapor) < 31:
                time.sleep(0.3)
                continue
            print('\n'.join(rapor))
            if len(rapor) != 31 or any(not s.startswith('PASS ') for s in rapor):
                raise SystemExit(1)
            print('Sonuç: 31/31 başarılı')
            break
        time.sleep(0.3)
    else:
        raise SystemExit('Bu çalışmanın test sonucu alınamadı.')
finally:
    if cikti.exists():
        cikti.unlink()
