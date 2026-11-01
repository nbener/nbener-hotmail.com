#!/bin/bash
# Yorumlayıcı testi (yan etkisiz). Önce ./build.sh kur ile kurulu olmalı.
# test/yorumla_cumleler.txt: "cümle|beklenen çıktı parçası". Çıkış kodu: hata sayısı.
set -uo pipefail
cd "$HOME/sezai"
cut -d'|' -f1 test/yorumla_cumleler.txt > /sdcard/Download/sezai_cumleler.txt
cat > /sdcard/Download/sezai_yorumla.sh <<'IC'
logcat -c
# am komutları döngünün girişini okumasın diye cümleler 3 numaralı dosya tanıtıcısından okunur
while IFS= read -r c <&3; do
  # am komutu aralıklı başarısız olabiliyor; günlükte cümle görünene kadar en çok 3 kez dene
  for deneme in 1 2 3; do
    am force-stop com.sezai.assistant </dev/null
    am start -n com.sezai.assistant/.MainActivity --es yorumla "$c" </dev/null >/dev/null 2>&1
    sleep 3
    if logcat -d </dev/null | grep -qF "YORUM '$c' ->"; then break; fi
  done
done 3< /sdcard/Download/sezai_cumleler.txt
logcat -d | grep -E "Sezai +: YORUM" | cut -c33-
IC
cd "$HOME/rish"; export RISH_APPLICATION_ID=com.termux
# rish satır sonlarını bazen birleştirir; her YORUM kaydını ayrı satıra böl
SONUC=$(sh ./rish -c 'sh /sdcard/Download/sezai_yorumla.sh' 2>&1 | tr -d '\r' | sed 's/ Sezai   : YORUM/\nYORUM/g' | grep '^YORUM')
rm -f /sdcard/Download/sezai_cumleler.txt /sdcard/Download/sezai_yorumla.sh
cd "$HOME/sezai"
HATA=0
while IFS='|' read -r cumle beklenen; do
  satir=$(printf '%s\n' "$SONUC" | grep -F "YORUM '$cumle' ->" | head -1)
  if [[ "$satir" == *"$beklenen"* ]]; then echo "PASS  $cumle"; else echo "FAIL  $cumle  (beklenen: $beklenen)  gelen: ${satir:-yok}"; HATA=$((HATA+1)); fi
done < test/yorumla_cumleler.txt
echo "Hata sayısı: $HATA"; exit $HATA
