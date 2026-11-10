#!/usr/bin/env python3
# Termux'tan sessiz cihaz testi. Gerçek API, mikrofon veya kullanıcı anahtarı kullanılmaz.
import pathlib
import shlex
import subprocess
import time

def am(komut):
    return subprocess.run(['/system/bin/sh', '-c', komut], capture_output=True, text=True, timeout=20)

kimlik = str(time.time_ns())
r = am('am broadcast --include-stopped-packages -n com.sezai.assistant/.GorusmeTestAlicisi --es test_id ' + shlex.quote(kimlik))
if r.returncode:
    raise SystemExit('Test başlatılamadı: ' + r.stderr)
cikti = pathlib.Path('/sdcard/Download/SEZAI_GO_TEST_' + kimlik + '.txt')
try:
    for _ in range(40):
        if cikti.exists():
            satirlar = cikti.read_text().splitlines()
            if satirlar and satirlar[0] == 'TEST_ID ' + kimlik:
                rapor = satirlar[1:]
                if len(rapor) < 8:
                    time.sleep(0.3)
                    continue
                print('\n'.join(rapor))
                if len(rapor) != 8 or any(not s.startswith('PASS ') for s in rapor):
                    raise SystemExit(1)
                print('Sonuç: 8/8 başarılı')
                break
        time.sleep(0.3)
    else:
        raise SystemExit('Bu çalışmanın test sonucu alınamadı.')
finally:
    if cikti.exists():
        cikti.unlink()
