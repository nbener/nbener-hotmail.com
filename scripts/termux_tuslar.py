#!/data/data/com.termux/files/usr/bin/python3
"""Yedeklenmiş Termux ek tuş düzenleri arasında geçiş yapar."""
import argparse
import ast
import json
import os
from pathlib import Path
import re
import subprocess
import sys

TERMUX = Path('/data/data/com.termux/files/home/.termux')
KAYIT = Path('/data/data/com.termux/files/home/.local/share/termux-panel')


def tus(key=None, display=None, macro=None, popup=None):
    sonuc = {'macro': macro} if macro else {'key': key}
    if display:
        sonuc['display'] = display
    if popup:
        sonuc['popup'] = popup
    return sonuc


KOMPAKT = [
    [tus('ESC', popup=tus(macro='CTRL c', display='Durdur')),
     tus('TAB', popup=tus(macro='CTRL r', display='Ara')),
     tus('CTRL', popup=tus(macro='CTRL z', display='Beklet')),
     tus('ALT', popup=tus(macro='ALT b', display='Geri')),
     tus('LEFT', popup='HOME'), tus('DOWN', popup='PGDN'),
     tus('UP', popup='PGUP'), tus('RIGHT', popup='END')],
    [tus(macro='cd SPACE ~/sezai SPACE && SPACE codex ENTER', display='Codex'),
     tus(macro='claude ENTER', display='Claude',
         popup=tus(macro='claude SPACE --continue ENTER', display='Devam')),
     tus(macro='panel ENTER', display='Panel',
         popup=tus(macro='tuslar SPACE tam ENTER', display='Tam')),
     tus('PASTE', display='Yapistir', popup=tus(macro='CTRL u', display='Sil')),
     tus('BKSP', display='DEL', popup=tus(macro='CTRL w', display='Sozcuk')),
     tus('ENTER', display='Enter', popup=tus(macro='CTRL j', display='Satir')),
     tus(macro='CTRL l', display='Temizle'),
     tus('KEYBOARD', display='Klavye', popup='DRAWER')]
]

TEK = [[
    tus('ESC', popup=tus(macro='CTRL c', display='Durdur')),
    tus('TAB', popup=tus(macro='CTRL r', display='Ara')),
    tus('CTRL', popup='ALT'),
    tus('LEFT', popup='UP'), tus('RIGHT', popup='DOWN'),
    tus('PASTE', display='Yapistir', popup='ENTER'),
    tus(macro='sesliyaz ENTER', display='Ses',
        popup=tus(macro='panel ENTER', display='Panel')),
    tus('KEYBOARD', display='Klavye', popup='DRAWER')
]]


def ayar_degistir(metin, ad, deger):
    desen = re.compile(r'^\s*' + re.escape(ad) + r'\s*=.*$', re.MULTILINE)
    satir = f'{ad} = {deger}'
    if desen.search(metin):
        return desen.sub(lambda _: satir, metin)
    return metin.rstrip() + '\n' + satir + '\n'


def cerceveli_etiketler(deger):
    # Yalnız etiket alanlarını değiştir; key/macro/popup davranışlarını koru.
    literal = r'''"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*' '''.rstrip()
    key_desen = re.compile(r'''(\{\s*(?:"key"|'key'|key)\s*:\s*)(''' + literal + r'''|[A-Z_]+)''')

    def eksik_etiket(eslesme):
        kalan = deger[eslesme.end():]
        if re.match(r'''\s*,\s*(?:"display"|'display'|display)\s*:''', kalan):
            return eslesme.group(0)
        ham = eslesme.group(2)
        ad = ast.literal_eval(ham) if ham.startswith(('"', "'")) else ham
        return eslesme.group(0) + ', display: ' + json.dumps(ad, ensure_ascii=True)

    deger = key_desen.sub(eksik_etiket, deger)
    display_desen = re.compile(r'''((?:"display"|'display'|\bdisplay)\s*:\s*)(''' + literal + ')')

    def etiket(eslesme):
        ad = ast.literal_eval(eslesme.group(2))
        if not (ad.startswith('[') and ad.endswith(']')):
            ad = '[' + ad + ']'
        return eslesme.group(1) + json.dumps(ad, ensure_ascii=True)

    return display_desen.sub(etiket, deger)


def uygula(profil):
    yedek = Path((KAYIT / 'aktif-yedek.txt').read_text().strip())
    asil = yedek / 'termux.properties'
    if not asil.is_file():
        raise RuntimeError('Özgün ayar yedeği bulunamadı; ayarlar değiştirilmedi.')
    mevcut = TERMUX / 'termux.properties'
    if profil == 'eski':
        metin = asil.read_text()
    else:
        metin = mevcut.read_text()
        if profil == 'cerceve':
            eslesme = re.search(r'^\s*extra-keys\s*=\s*(.*)$', metin, re.MULTILINE)
            if not eslesme or eslesme.group(1).rstrip().endswith('\\'):
                raise RuntimeError('Etkin tuş düzeni tek satır olarak okunamadı.')
            deger = cerceveli_etiketler(eslesme.group(1))
        elif profil in ('kompakt', 'tek'):
            deger = json.dumps(TEK if profil == 'tek' else KOMPAKT, ensure_ascii=True)
        else:
            eslesme = re.search(r'^\s*extra-keys\s*=\s*(.*)$', asil.read_text(), re.MULTILINE)
            if not eslesme or eslesme.group(1).rstrip().endswith('\\'):
                raise RuntimeError('Yedekteki tam tuş düzeni tek satır olarak okunamadı.')
            deger = eslesme.group(1)
        metin = ayar_degistir(metin, 'extra-keys', deger)
        metin = ayar_degistir(metin, 'extra-keys-text-all-caps', 'false')
    gecici = mevcut.with_name('termux.properties.panel-tmp')
    with gecici.open('w') as cikti:
        os.chmod(gecici, 0o600)
        cikti.write(metin)
        cikti.flush()
        os.fsync(cikti.fileno())
    gecici.replace(mevcut)
    sonuc = subprocess.run(['termux-reload-settings'], timeout=15, check=False)
    if sonuc.returncode:
        raise RuntimeError('Ayar kaydedildi fakat yeniden yükleme başarısız. termux-reload-settings komutunu tekrar dene.')
    print({'cerceve': 'Tuş etiketlerine [ ] eklendi; gerçek düğme kenarlığı değildir.',
           'tek': 'Tek satır: tuşları yukarı kaydırarak ek işlevleri açabilirsin.',
           'kompakt': 'Kompakt düzen: 2 satır. Yukarı kaydırılan tuşlar ek işlev açar.',
           'tam': 'Tam düzen: eski beş satırın bütün tuşları korundu.',
           'eski': 'Özgün termux.properties yedeği geri yüklendi.'}[profil])
    print('Tek: tuslar tek | İki: tuslar kompakt | Tam: tuslar tam | Özgün: tuslar eski')


def main():
    parser = argparse.ArgumentParser(description='Termux ek tuş düzeni')
    parser.add_argument('profil', nargs='?', choices=['tek', 'kompakt', 'tam', 'eski', 'cerceve'])
    args = parser.parse_args()
    profil = args.profil
    if not profil:
        if not sys.stdin.isatty():
            parser.print_help()
            return 0
        print('\n1  Kompakt: 2 satır\n2  Tam: eski 5 satır\n3  Özgün ayarları geri yükle\n4  Tek satır + Türkçe sesli yazma\n5  Etiketlere [ ] ekle\n0  Vazgeç')
        secim = input('Seçimin: ').strip()
        if secim == '0':
            return 0
        profil = {'1': 'kompakt', '2': 'tam', '3': 'eski', '4': 'tek', '5': 'cerceve'}.get(secim)
        if not profil:
            raise RuntimeError('Geçersiz seçim; ayarlar değiştirilmedi.')
    uygula(profil)
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, RuntimeError, subprocess.SubprocessError) as hata:
        print(f'İşlem tamamlanmadı: {hata}', file=sys.stderr)
        sys.exit(1)
    except (KeyboardInterrupt, EOFError):
        sys.exit(130)
