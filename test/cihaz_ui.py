# Cihaz UI yardımcıları. Her döküm benzersizdir; eski XML başarı sayılmaz.
import os
import pathlib
import shlex
import subprocess
import time
import xml.etree.ElementTree as ET

rish = pathlib.Path(__file__).resolve().parents[2] / 'rish' / 'rish'
ortam = dict(os.environ, RISH_APPLICATION_ID='com.termux')

def uzak(komut):
    r = subprocess.run(['sh', str(rish), '-c', komut], capture_output=True,
                       text=True, timeout=30, env=ortam)
    if r.returncode:
        raise RuntimeError('Cihaz komutu başarısız; çıkış kodu '+str(r.returncode))
    return r.stdout

def ekran():
    ad = 'sezai-ui-'+str(time.time_ns())+'.xml'
    yerel = pathlib.Path('/sdcard/Download') / ad
    cihaz = '/data/local/tmp/'+ad
    try:
        uzak('uiautomator dump '+shlex.quote(cihaz)+' >/dev/null && cp '+
             shlex.quote(cihaz)+' '+shlex.quote(str(yerel)))
        kok = ET.fromstring(yerel.read_text())
        if kok.tag != 'hierarchy':
            raise RuntimeError('Beklenen UI XML yapısı alınamadı.')
        return kok
    finally:
        if yerel.exists(): yerel.unlink()
        try: uzak('rm -f '+shlex.quote(cihaz))
        except RuntimeError: pass
