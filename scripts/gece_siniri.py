#!/usr/bin/env python3
# Kullanıcının kota sınırında CPU kilidini ve geçici pil istisnasını bırakır.
# Geliştirme yapmaz; yalnızca %80 kota sınırını izler.
import datetime
import fcntl
import json
import os
import pathlib
import subprocess
import time

kok = pathlib.Path(__file__).resolve().parent.parent
kilit = open('/data/data/com.termux/files/usr/tmp/sezai-gece-siniri.lock', 'w')
try:
    fcntl.flock(kilit, fcntl.LOCK_EX | fcntl.LOCK_NB)
except BlockingIOError:
    raise SystemExit('Sınır gözcüsü zaten çalışıyor.')
kilit.write(str(os.getpid())); kilit.flush()
oturum = os.environ.get('CODEX_THREAD_ID', '')
oturumlar = pathlib.Path(os.environ['CODEX_HOME'])/'sessions'
dosyalar = list(oturumlar.rglob('*'+oturum+'*.jsonl')) if oturum else []
print('Kota gözcüsü başladı; saat sınırı yok, %80 kullanımda durur.', flush=True)
while True:
    sebep = None
    son = None
    for dosya in dosyalar:
        with dosya.open('rb') as kaynak:
            kaynak.seek(max(0, dosya.stat().st_size-2097152))
            for satir in kaynak:
                try:
                    veri = json.loads(satir)
                except (ValueError, UnicodeError):
                    continue
                p = veri.get('payload', {})
                if veri.get('type') == 'event_msg' and p.get('type') == 'token_count' and p.get('rate_limits'):
                    son = p['rate_limits']
    yuzdeler = [son[p]['used_percent'] for p in ('primary','secondary') if son and son.get(p)]
    if not yuzdeler:
        sebep = sebep or 'Kota doğrulanamadı'
    elif max(yuzdeler) >= 80:
        sebep = '%80 kota sınırı'
    if sebep:
        (kok/'calisma_durdu.json').write_text(json.dumps({
            'sebep': sebep, 'zaman': datetime.datetime.now(datetime.timezone.utc).isoformat()
        }, ensure_ascii=False))
        ortam = dict(os.environ, RISH_APPLICATION_ID='com.termux')
        rish = kok.parent/'rish'/'rish'
        islemler = [
            ['termux-wake-unlock'],
            ['sh',str(rish),'-c','cmd deviceidle whitelist -com.termux'],
            ['python3',str(kok/'scripts/durum_bildir.py'), 'Çalışma durdu: '+sebep+'.']
        ]
        for komut in islemler:
            try:
                subprocess.run(komut, env=ortam, timeout=35)
            except (OSError, subprocess.TimeoutExpired):
                print('Bir kapanış işlemi tamamlanamadı:',komut[0],flush=True)
        print('Duruldu:',sebep,flush=True)
        break
    time.sleep(15)
