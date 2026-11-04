#!/data/data/com.termux/files/usr/bin/python3
"""Kullanıcının isteğiyle bir kez dikte alır; sonucu yalnız panoya yazar."""
import json
import shutil
import subprocess
import sys
import unicodedata


def main():
    if '--help' in sys.argv or '-h' in sys.argv:
        print('sesliyaz: Türkçe cihaz dilinde sesli yazma penceresi açar; sonucu panoya alır.\nSes düğmesi kabuk isteminde kullanılır; yazı kendiliğinden çalıştırılmaz.')
        return 0
    if len(sys.argv) != 1:
        raise RuntimeError('Kullanım: sesliyaz')
    for komut in ['termux-dialog', 'termux-clipboard-set']:
        if not shutil.which(komut):
            raise RuntimeError(f'{komut} eksik. Termux:API gerekir.')
    print('Türkçe sesli yazma açılıyor. Bitince metni Yapıştır düğmesiyle aktar.', flush=True)
    # Dialog API cihaz dilini kullanır; bu telefonda tr-TR doğrulandı.
    # SpeechToText API bu kurulu sürümde en-US diline sabitlendiği için kullanılmaz.
    r = subprocess.run(['termux-dialog', 'speech', '-t', 'Türkçe sesli yazma',
                        '-i', 'Türkçe konuş; iptal etmek için Vazgeç düğmesine dokun.'],
                       capture_output=True, text=True, timeout=90, check=True)
    veri = json.loads(r.stdout)
    if veri.get('error'):
        raise RuntimeError('Ses tanıma tamamlanmadı: ' + str(veri['error']))
    if veri.get('code') == -2:
        print('İptal edildi; pano korunuyor.')
        return 0
    metin = veri.get('text', '')
    if not isinstance(metin, str):
        raise RuntimeError('Ses tanıma metin döndürmedi.')
    metin = ' '.join(''.join(c for c in metin if not unicodedata.category(c).startswith('C')).split())
    if not metin:
        raise RuntimeError('Konuşma algılanmadı; pano korunuyor.')
    subprocess.run(['termux-clipboard-set'], input=metin, text=True, check=True, timeout=15)
    print('Konuşma metni panoya alındı. Yapıştır düğmesiyle aktar; otomatik gönderilmedi.')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as hata:
        print(f'İşlem tamamlanmadı: {hata}', file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print('\nİptal edildi.')
        sys.exit(130)
