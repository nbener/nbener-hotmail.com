#!/usr/bin/env python3
# Kullanıcı saat sınırını kaldırdı; kota en fazla %80.
# Yalnızca mevcut oturumun kullanım sayacını okur; mesaj veya anahtar yazdırmaz.
import datetime
import json
import os
import pathlib

durdu = pathlib.Path(__file__).resolve().parent.parent / 'calisma_durdu.json'
if durdu.exists():
    raise SystemExit('DUR: Önceden çalışma sınırı işaretlendi. Yeni kullanıcı talimatı olmadan sürdürme.')
oturum = os.environ.get('CODEX_THREAD_ID')
kok = os.environ.get('CODEX_HOME')
if not oturum or not kok:
    raise SystemExit('DUR: Bu oturumun kota bilgisi bulunamıyor.')
dosyalar = list((pathlib.Path(kok)/'sessions').rglob('*'+oturum+'*.jsonl'))
son = None
for dosya in dosyalar:
    with dosya.open() as kaynak:
        for satir in kaynak:
            try:
                veri = json.loads(satir)
            except ValueError:
                continue
            p = veri.get('payload', {})
            if veri.get('type') == 'event_msg' and p.get('type') == 'token_count' and p.get('rate_limits'):
                son = p['rate_limits']
if not son:
    raise SystemExit('DUR: Kota sayacı okunamadı; sınır doğrulanamıyor.')
yuzdeler = [son[p]['used_percent'] for p in ('primary', 'secondary') if son.get(p)]
if not yuzdeler:
    raise SystemExit('DUR: Kullanım yüzdesi bulunamadı.')
print('Kota kullanımı:', ', '.join('%'+str(y) for y in yuzdeler))
if max(yuzdeler) >= 80:
    raise SystemExit('DUR: %80 kota sınırına ulaşıldı.')
print('Devam edilebilir; saat sınırı yok, kota sınırı %80.')
