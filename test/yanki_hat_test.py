#!/usr/bin/env python3
"""Sınırlı debug kayıt/çalma/AEC hattı. Varsayılan çıkış sessiz, mikrofon GERÇEKTİR."""
import argparse
import json
import os
import re
from pathlib import Path
import shlex
import subprocess
import time

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--kip', choices=['sessiz', 'yanki'], default='sessiz')
p.add_argument('--genlik', type=float, default=0.2)
p.add_argument('--cikis', choices=['asistan', 'medya'], default='asistan')
p.add_argument('--referans-gecikme-ms', type=int, default=0)
p.add_argument('--cagri-tanisi', action='store_true', help='Temiz16kHz seste dur/Sezai bayrakları; çalmayı kesmez')
p.add_argument('--arka-plan', action='store_true')
p.add_argument('--ekran-bekle', type=int, default=0, metavar='SANIYE',
               help='Ekranın kullanıcı tarafından açılmasını en çok 60 saniye bekle; ekranı açmaz')
a = p.parse_args()
if not 0 <= a.ekran_bekle <= 60: p.error('--ekran-bekle 0 ile 60 arasında olmalı')
if not 0 <= a.referans_gecikme_ms <= 200: p.error('--referans-gecikme-ms 0 ile 200 arasında olmalı')
if not 0 <= a.genlik <= 0.3: p.error('--genlik 0 ile 0.3 arasında olmalı')
if a.kip == 'sessiz': a.genlik = 0.0
rish = Path(__file__).resolve().parents[2] / 'rish/rish'
env = dict(os.environ, RISH_APPLICATION_ID='com.termux')

def cihaz(komut, iki_akis=False):
    r = subprocess.run(['sh', str(rish), '-c', komut], env=env, text=True,
                       capture_output=True, timeout=20)
    if r.returncode: raise SystemExit('Cihaz komutu başarısız: ' + str(r.returncode))
    return r.stdout + r.stderr if iki_akis else r.stdout

kimlik = str(time.time_ns())
ekran_son = time.monotonic() + a.ekran_bekle
if a.ekran_bekle:
    print('Ekranın açılması bekleniyor (en çok', a.ekran_bekle, 'saniye). Ekran zorla açılmaz.', flush=True)
guc = cihaz('dumpsys power | grep mWakefulness=')
while 'mWakefulness=Awake' not in guc and time.monotonic() < ekran_son:
    time.sleep(min(1, max(0, ekran_son - time.monotonic())))
    if time.monotonic() >= ekran_son: break
    guc = cihaz('dumpsys power | grep mWakefulness=')
if 'mWakefulness=Awake' not in guc:
    raise SystemExit('Ön plan testi başlatılmadı: ekran Awake değil veya durumu doğrulanamadı. Ekran zorla açılmadı.')
if a.kip == 'yanki':
    akis = 3 if a.cikis == 'medya' else 11
    ses = cihaz('cmd media_session volume --stream ' + str(akis) + ' --get', iki_akis=True)
    duzey = re.search(r'volume is (\d+) in range \[(\d+)\.\.(\d+)\]', ses)
    if not duzey or int(duzey.group(1)) == 0:
        raise SystemExit('Sesli test başlatılmadı: ' + a.cikis + ' ses düzeyi sıfır veya doğrulanamadı. Ses ayarı değiştirilmedi.')
    print(a.cikis, 'sistem ses düzeyi:', duzey.group(1), '/', duzey.group(3), flush=True)
print('Test:', kimlik, a.kip, 'çıkış katsayısı:', a.genlik, flush=True)
cihaz('am start -W -f 0x18000000 -n com.sezai.assistant/.YankiHatTestActivity --es kip ' + a.kip +
      ' --es test_id ' + kimlik + ' --ef genlik ' + str(a.genlik) + ' --es cikis ' + a.cikis +
      ' --ei referans_gecikme_ms ' + str(a.referans_gecikme_ms) +
      ' --ez cagri_tanisi ' + str(a.cagri_tanisi).lower())
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
            if a.cagri_tanisi:
                tani = rapor.get('cagri_tanisi', {})
                if not (tani.get('tanici_birakildi') is True and tani.get('tanici_ornek', 0) > 0
                        and tani.get('tanici_ornek') == rapor.get('tanima16_ornek')
                        and tani.get('cozumlenen_ornek') == tani.get('tanici_ornek')
                        and type(tani.get('dur_duyuldu')) is bool and type(tani.get('sezai_duyuldu')) is bool):
                    raise SystemExit('Temiz ses/tanıyıcı aktarımı doğrulanmadı')
            if (rapor.get('cikis_turu') != a.cikis
                    or rapor.get('referans_gecikme_ms') != a.referans_gecikme_ms
                    or rapor.get('audiotrack_usage') != (1 if a.cikis == 'medya' else 16)):
                raise SystemExit('İstenen AudioTrack kullanımı doğrulanmadı')
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
