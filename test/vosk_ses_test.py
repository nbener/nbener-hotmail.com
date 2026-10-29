#!/usr/bin/env python3
"""Sınırlı cihaz denemesi. Varsayılan bilgi; --kip ses modları gerçek mikrofon açar."""
import argparse
import json
import os
from pathlib import Path
import shlex
import subprocess
import time

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--kip', choices=['bilgi', 'dinle', 'birlikte', 'yanki'], default='bilgi')
p.add_argument('--tts-genlik', type=float, default=1.0, help='Yalnız deneme cümlesi için 0–1 ses katsayısı; sistem sesini değiştirmez')
p.add_argument('--arka-plan', action='store_true', help='Dinle kipinde 3 saniye sonra Home ile gerçek yaşam döngüsünü dene')
a = p.parse_args()
if not 0 <= a.tts_genlik <= 1: p.error('--tts-genlik 0 ile 1 arasında olmalı')
if a.arka_plan and a.kip != 'dinle': p.error('--arka-plan yalnız --kip dinle ile kullanılır')
rish = Path(__file__).resolve().parents[2] / 'rish/rish'
env = dict(os.environ, RISH_APPLICATION_ID='com.termux')
def uzak(komut):
    r = subprocess.run(['sh', str(rish), '-c', komut], env=env, capture_output=True, text=True, timeout=20)
    if r.returncode:
        raise SystemExit('Cihaz komutu başarısız: '+str(r.returncode))
    return r.stdout + r.stderr

kimlik = str(time.time_ns())
print('Test:', kimlik, a.kip, flush=True)
print(uzak('am start -W -f 0x18000000 -n com.sezai.assistant/.VoskSesTestActivity --es kip '+a.kip+' --es test_id '+kimlik+' --ef tts_genlik '+str(a.tts_genlik)), flush=True)
if a.arka_plan:
    time.sleep(3)
    uzak('input keyevent KEYCODE_HOME')
paylasilan = Path('/sdcard/Download/SEZAI_VOSK_'+kimlik+'.json')
try:
    son = time.monotonic()+48
    while time.monotonic() < son:
        uzak('run-as com.sezai.assistant cat files/vosk_ses_test.json | /system/bin/cat > '+shlex.quote(str(paylasilan)))
        try: rapor = json.loads(paylasilan.read_text())
        except (OSError, ValueError): rapor = {}
        if rapor.get('test_id') == kimlik:
            # Rapor yalnız durum/bayrak/sayısal ölçüm içerir; tanınan metin yok.
            print(json.dumps(rapor, ensure_ascii=False, indent=2))
            if abs(rapor.get('tts_istenen_genlik', -1) - a.tts_genlik) > 0.00001:
                raise SystemExit('Bu APK istenen TTS genlik parametresini doğrulamadı')
            if not rapor.get('kaynaklar_birakildi') or not rapor.get('model_acildi'):
                raise SystemExit('Model/kaynak bırakma doğrulanamadı')
            if a.kip == 'bilgi' and rapor.get('sonuc') != 'MODEL_HAZIR':
                raise SystemExit('Model hazır değil')
            beklenen = ['GORUNURLUK_KAYBI'] if a.arka_plan else ['DENEME_TAMAMLANDI', 'SOZ_ALMA_DUYULDU', 'SURE_DOLDU']
            if a.kip != 'bilgi' and (not rapor.get('mikrofon_basladi') or rapor.get('sonuc') not in beklenen):
                raise SystemExit('Ses denemesi tamamlanamadı')
            if a.kip == 'birlikte' and not (rapor.get('soz_alma_duyuldu') and rapor.get('tts_sirasinda')):
                raise SystemExit('TTS sırasında söz alma doğrulanmadı; süre dolması başarı değildir')
            if a.kip == 'dinle' and not a.arka_plan and not rapor.get('soz_alma_duyuldu'):
                raise SystemExit('Dur/Sezai tanıması doğrulanmadı; süre dolması başarı değildir')
            break
        time.sleep(.8)
    else: raise SystemExit('Bu denemenin raporu alınamadı; başarı sayılmaz')
finally:
    if paylasilan.exists(): paylasilan.unlink()
