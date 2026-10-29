#!/usr/bin/env python3
# Sentetik görsel durumlar; gerçek mikrofon, TTS ve API başlatmaz. Sistem ayarını değiştirmez.
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
    if r.returncode:
        raise SystemExit('Karakter cihaz komutu başarısız: '+str(r.returncode))
    return r

kimlik = str(time.time_ns())
uzak('am start -W -f 0x18000000 -n com.sezai.assistant/.KarakterOnizlemeActivity --es test_id '+shlex.quote(kimlik))
time.sleep(1.2)
gorsel = pathlib.Path('/sdcard/Download/sezai-karakter-onizleme-'+kimlik+'.png')
uzak('screencap -p | /system/bin/cat > '+shlex.quote(str(gorsel)))
assert gorsel.exists() and gorsel.stat().st_size > 1000, 'Önizleme görseli alınamadı'
print('Görsel:', gorsel, flush=True)
rapor_dosyasi = pathlib.Path('/sdcard/Download/SEZAI_KARAKTER_TEST_'+kimlik+'.json')
try:
    for _ in range(18):
        uzak('run-as com.sezai.assistant cat files/karakter_test_sonuc.json | /system/bin/cat > '+shlex.quote(str(rapor_dosyasi)))
        try: rapor = json.loads(rapor_dosyasi.read_text())
        except (OSError, ValueError): rapor = {}
        if rapor.get('test_id') == kimlik:
            satirlar = rapor.get('rapor', [])
            print('\n'.join(satirlar))
            if len(satirlar) != 6 or any(not x.startswith('PASS ') for x in satirlar):
                raise SystemExit('Karakter testi tamamlanmadı veya başarısız.')
            print('Sonuç: 6/6 başarılı; sentetik görsel/kare planlama testi.')
            break
        time.sleep(.7)
    else: raise SystemExit('Bu çalışmanın karakter test raporu alınamadı.')
finally:
    if rapor_dosyasi.exists(): rapor_dosyasi.unlink()
