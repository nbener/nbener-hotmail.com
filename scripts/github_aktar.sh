#!/usr/bin/env bash
# Sezai: değişiklikleri YALNIZCA private depodaki tek dala güvenle gönderir.
# Kullanım (Termux): cd ~/sezai && scripts/github_aktar.sh
# Token'ı bu betik saklamaz; Git'in kimlik yardımcısına bırakılır (önerilen: credential.helper cache).
set -euo pipefail

DAL="claude/sezai-voice-assistant-7wtvse"
DEPO="https://github.com/nbener/nbener-hotmail.com"

cd "$(dirname "$0")/.."

# 1) Yalnızca izin verilen depoya gönder
if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin "$DEPO"
fi
if [ "$(git remote get-url origin)" != "$DEPO" ]; then
  echo "HATA: origin beklenen depo değil: $(git remote get-url origin)" >&2
  exit 1
fi

# 2) Commitlenmemiş değişiklik uyarısı (gönderilen şey commitlenmiş olandır)
if [ -n "$(git status --porcelain)" ]; then
  echo "UYARI: commitlenmemiş değişiklik var; yalnızca commitli olanlar gönderilir." >&2
fi

# 3) Geçmişin TAMAMINDA dosya adı denetimi (push tüm geçmişi gönderir)
yasak='(\.jks|\.keystore|\.pem|\.key|local\.properties|\.env|\.apk|\.aab)$|^(yedek|yerel-model)/'
bulunan=$(git log --all --name-only --format= | grep -E "$yasak" | sort -u || true)
if [ -n "$bulunan" ]; then
  echo "DURDU: geçmişte yasak dosya adı var (içerik gösterilmedi):" >&2
  echo "$bulunan" | head -20 >&2
  exit 2
fi

# 4) Anahtar benzeri metin denetimi (vosk- gibi kelimeleri yanlış alarm saymak için sol sınır şart)
if git grep -I -q -E '(^|[^A-Za-z0-9])(sk-[A-Za-z0-9_-]{20,}|AIza[0-9A-Za-z_-]{30,}|gsk_[A-Za-z0-9]{20,})' $(git rev-list --all) -- . 2>/dev/null; then
  echo "DURDU: anahtar benzeri metin bulundu. Değer gösterilmedi; elle incele." >&2
  exit 3
fi

# 5) Önce uzaktaki ilerlemeyi al (force YOK); ayrışma varsa durup bildir
git fetch origin "$DAL" 2>/dev/null || true
if git rev-parse -q --verify "origin/$DAL" >/dev/null; then
  if ! git merge-base --is-ancestor "origin/$DAL" HEAD; then
    echo "DURDU: uzak dalda sende olmayan commit var. Önce: git pull --rebase origin $DAL" >&2
    exit 4
  fi
fi

git push origin "HEAD:$DAL"
echo "TAMAM: $(git rev-parse --short HEAD) -> $DAL"
