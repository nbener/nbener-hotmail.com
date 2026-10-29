#!/usr/bin/env python3
# Sezai'yi açar; yalnızca başlangıç ekranı doğrulanırsa bir kez kaydırır. Ses/API çalıştırmaz.
import pathlib
import re
import shlex
import time
from cihaz_ui import ekran, uzak


def kendi_ekrani():
    kok = ekran()
    paketler = {n.get('package', '') for n in kok.iter('node')}
    if paketler - {'', 'com.sezai.assistant', 'com.android.systemui'}:
        return None
    ns = [n for n in kok.iter('node') if n.get('package') == 'com.sezai.assistant']
    if any(n.get('password') == 'true' for n in ns):
        return None
    metinler = {n.get('text', '') for n in ns}
    if not ('Sezai' in metinler or {'Yeni sohbet', 'Sohbet bağlantısı'} <= metinler):
        return None
    if not any(n.get('class') == 'android.widget.ScrollView' for n in ns):
        return None
    return ns

uzak('am start -n com.sezai.assistant/.MainActivity')
son = time.monotonic() + 20
ns = None
while time.monotonic() < son:
    ns = kendi_ekrani()
    if ns and any(n.get('class') == 'android.widget.EditText' for n in ns): break
    time.sleep(0.5)
else:
    raise SystemExit('Sezai ana ekranı doğrulanamadı; dokunma/kaydırma yapılmadı.')
def bos_baslangic(nodes):
    metinler = {n.get('text', '') for n in nodes}
    # Boş taslak tek başına yeterli değildir: dinleme veya mevcut sohbet açık olabilir.
    return ('Merhaba, ben Sezai.\nSize nasıl yardımcı olabilirim?' in metinler and
            'Sesli sohbet et' in metinler and 'Gönder' in metinler and
            all(n.get('text', '') in ('', 'Sezai’ye yazın')
                for n in nodes if n.get('class') == 'android.widget.EditText'))

if not bos_baslangic(ns):
    raise SystemExit('Boş başlangıç ekranı doğrulanmadı; dokunma/kaydırma yapılmadı.')
alan = next(n for n in ns if n.get('class') == 'android.widget.EditText')
if alan.get('text', '') not in ('', 'Sezai’ye yazın'):
    raise SystemExit('Taslak var; otomatik kaydırma yapılmadı.')
assert any(n.get('text') == 'Gönder' for n in ns), 'Gönder görünmedi'
assert any(n.get('class') == 'android.widget.CheckBox' for n in ns), 'Web seçeneği görünmedi'
print('PASS Yazı alanı, Gönder ve web seçeneği cihazda görünür.')

def dugmeler(nodes):
    sonuc = {}
    for n in nodes:
        if n.get('class') == 'android.widget.Button' and n.get('text') in ('Yeni sohbet', 'Sohbet bağlantısı'):
            sonuc[n.get('text')] = list(map(int, re.findall(r'\d+', n.get('bounds'))))
    return sonuc

def tam_gorunur(nodes, dugmeler):
    kaydirma = next(n for n in nodes if n.get('class') == 'android.widget.ScrollView')
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', kaydirma.get('bounds')))
    # UI XML sınırları kesilmiş olabilir; her düğmenin çevresinde görünür boşluk aranır.
    return len(dugmeler) == 2 and all(
        b[0] >= x1 and b[2] <= x2 and b[2] > b[0] and
        b[1] >= y1 + 16 and b[3] <= y2 - 16 and b[3] > b[1]
        for b in dugmeler.values())

d = dugmeler(ns)
if not tam_gorunur(ns, d):
    # Yalnızca doğrulanan kendi boş ekranımızda, tek kaydırma. Başka uygulamaya dokunulmaz.
    ns = kendi_ekrani()
    if not ns or not bos_baslangic(ns):
        raise SystemExit('Ekran değişti veya kullanım var; kaydırma yapılmadı.')
    alanlar = [n for n in ns if n.get('class') == 'android.widget.EditText']
    if any(n.get('text', '') not in ('', 'Sezai’ye yazın') for n in alanlar):
        raise SystemExit('Taslak değişti; kaydırma yapılmadı.')
    kaydirma = next(n for n in ns if n.get('class') == 'android.widget.ScrollView')
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', kaydirma.get('bounds')))
    x = (x1+x2)//2
    uzak(f'input swipe {x} {y2-120} {x} {y1+160} 450')
    time.sleep(0.7)
    ns = kendi_ekrani()
    if not ns: raise SystemExit('Ekran değişti; sonuç doğrulanmadı.')
    d = dugmeler(ns)
assert tam_gorunur(ns, d), 'Ayar düğmeleri tamamen görünmedi; başarı sayılmadı'
a, b = d['Yeni sohbet'], d['Sohbet bağlantısı']
assert a[1] == b[1] and a[3] == b[3] and a[3] > a[1], 'Düğme hizası eşit değil'
print('PASS Yeni sohbet ve bağlantı düğmeleri tamamen görünür ve eşit yükseklikte/hizada.')
p = pathlib.Path('/sdcard/Download/sezai-ekran-dogrulama-'+str(time.time_ns())+'.png')
uzak('screencap -p | /system/bin/cat > '+shlex.quote(str(p)))
assert p.exists() and p.stat().st_size > 1000
print('Görsel:',p)
