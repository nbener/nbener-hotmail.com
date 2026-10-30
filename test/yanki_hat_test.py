#!/usr/bin/env python3
"""Sınırlı debug kayıt/çalma/AEC hattı. Varsayılan çıkış sessiz, mikrofon GERÇEKTİR."""
import argparse
import json
import os
from pathlib import Path
import shlex
import subprocess
import time

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--kip', choices=['sessiz', 'yanki'], default='sessiz')
p.add_argument('--genlik', type=float, default=0.2)
p.add_argument('--arka-plan', action='store_true')
a = p.parse_args()
if not 0 <= a.genlik <= 0.3: p.error('--genlik 0 ile 0.3 arasında olmalı')
if a.kip == 'sessiz': a.genlik = 0.0
rish = Path(__file__).resolve().parents[2] / 'rish/rish'
env = dict(os.environ, RISH_APPLICATION_ID='com.termux')

def cihaz(komut):
    r = subprocess.run(['sh', str(rish), '-c', komut], env=env, text=True,
                       capture_output=True, timeout=20)
    if r.returncode: raise SystemExit('Cihaz komutu başarısız: ' + str(r.returncode))
    return r.stdout

kimlik = str(time.time_ns())
guc = cihaz('dumpsys power | grep mWakefulness=')
if 'mWakefulness=Awake' not in guc:
    raise SystemExit('Ön plan testi başlatılmadı: ekran Awake değil veya durumu doğrulanamadı. Ekran zorla açılmadı.')
print('Test:', kimlik, a.kip, 'çıkış katsayısı:', a.genlik, flush=True)
cihaz('am start -W -f 0x18000000 -n com.sezai.assistant/.YankiHatTestActivity --es kip ' + a.kip +
      ' --es test_id ' + kimlik + ' --ef genlik ' + str(a.genlik))
dosya = Path('/sdcard/Download/SEZAI_AEC_HAT_' + kimlik + '.json')
hazir_dosya = Path('/sdcard/Download/SEZAI_AEC_HAT_HAZIR_' + kimlik + '.json')
try:
    if a.arka_plan:
        hazir_son = time.monotonic() + 28
        while time.monotonic() < hazir_son:
            cihaz('run-as com.sezai.assistant cat files/yanki_hat_hazir.json | /system/bin/cat > ' + shlex.quote(str(hazir_dosya)))
            try: hazir = json.loads(hazir_dosya.read_text())
            except (OSError, ValueError): hazir = {}
            if hazir.get('test_id') == kimlik and hazir.get('mikrofon_basladi') and hazir.get('native_kare', 0) > 0:
                time.sleep(0.5)
                cihaz('input keyevent KEYCODE_HOME')
                break
            time.sleep(0.3)
        else: raise SystemExit('Arka plana geçilmedi: gerçek mikrofon/native kare başlangıcı alınamadı')
    son = time.monotonic() + 50
    while time.monotonic() < son:
        cihaz('run-as com.sezai.assistant cat files/yanki_hat_test.json | /system/bin/cat > ' + shlex.quote(str(dosya)))
        try: rapor = json.loads(dosya.read_text())
        except (OSError, ValueError): rapor = {}
        if rapor.get('test_id') == kimlik:
            print(json.dumps(rapor, ensure_ascii=False, indent=2))
            if not rapor.get('kaynaklar_birakildi') or rapor.get('kisisel_ses_saklandi') is not False:
                raise SystemExit('Kaynak bırakma/ham ses politikası geçmedi')
            if (rapor.get('korelasyon_ornek', -1) != rapor.get('native_kare', 0) * 30
                    or not 0 <= rapor.get('mikrofon_tepe', -1) <= 32768
                    or rapor.get('mikrofon_kirpilan_ornek', -1) < 0):
                raise SystemExit('Gecikme tanısı/tepe ölçümü eksik veya kare sayısıyla uyumsuz')
            if a.arka_plan:
                if not (rapor.get('sonuc') == 'GORUNURLUK_KAYBI' and rapor.get('mikrofon_basladi')
                        and rapor.get('native_kare', 0) > 0): raise SystemExit('Gerçek kayıt sonrası arka plan kesintisi doğrulanmadı')
            elif not (rapor.get('sonuc') == 'HAT_BITTI' and rapor.get('mikrofon_basladi')
                      and rapor.get('audiotrack_basladi') and rapor.get('native_kare', 0) > 0
                      and rapor.get('zaman_eslesen_kare', 0) > 0 and rapor.get('yazma_bitti')
                      and rapor.get('calinan_ornek', 0) >= rapor.get('tts_pcm_ornek', 1)
                      and abs(rapor.get('cikis_genlik', -1) - a.genlik) < 0.00001):
                raise SystemExit('Gerçek kayıt/çalma zamanlama hattı geçmedi')
            print('Kayıt/çalma hattı PASS; yankı ayrımı/insan sözü kesme kabulü değildir.')
            break
        time.sleep(0.4)
    else: raise SystemExit('Bu çalışmanın hat raporu alınamadı')
finally:
    if dosya.exists(): dosya.unlink()
    if hazir_dosya.exists(): hazir_dosya.unlink()
    odak = cihaz('dumpsys window | grep mCurrentFocus')
    if 'com.sezai.assistant/' in odak:
        cihaz('am start -W -n com.termux/.app.TermuxActivity')
