#!/data/data/com.termux/files/usr/bin/bash
# Masaüstü yalnız bu komut açıkken çalışır; otomatik başlatma eklemez.
set -euo pipefail

for arac in termux-x11 dbus-run-session xfce4-session flock python3; do
    if ! command -v "$arac" >/dev/null 2>&1; then
        echo "Eksik araç: $arac" >&2
        exit 1
    fi
done

exec 9>"${TMPDIR:-/data/data/com.termux/files/usr/tmp}/sezai-masaustu.lock"
if ! flock -n 9; then
    echo "Bu başlatıcıyla açılmış bir masaüstü zaten çalışıyor." >&2
    exit 1
fi
x11_canli() {
    python3 - <<'PY'
import os, socket, sys
s = socket.socket(socket.AF_UNIX)
s.settimeout(0.5)
try:
    s.connect(os.path.join(os.environ.get('TMPDIR', '/data/data/com.termux/files/usr/tmp'), '.X11-unix/X1'))
except OSError:
    sys.exit(1)
finally:
    s.close()
PY
}
if x11_canli; then
    echo ":1 ekranı kullanımda; mevcut oturuma dokunulmadı." >&2
    exit 1
fi

gunluk=$(mktemp "${TMPDIR:-/data/data/com.termux/files/usr/tmp}/masaustu-XXXXXX.log")
termux-x11 :1 >"$gunluk" 2>&1 &
sunucu_pid=$!
temizle() {
    # Yalnız bu betiğin başlattığı X11 sürecini kapatır.
    kill "$sunucu_pid" 2>/dev/null || true
    wait "$sunucu_pid" 2>/dev/null || true
}
trap temizle EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

hazir=false
for ((deneme = 0; deneme < 50; deneme++)); do
    if ! kill -0 "$sunucu_pid" 2>/dev/null; then
        echo "X11 başlatılamadı. Özel günlük: $gunluk" >&2
        exit 1
    fi
    if x11_canli; then
        hazir=true
        break
    fi
    sleep 0.2
done
if [[ "$hazir" != true ]]; then
    echo "X11 10 saniyede hazır olmadı. Özel günlük: $gunluk" >&2
    exit 1
fi
# Normal uygulama açma yolu çalışmazsa mevcut Shizuku/rish köprüsünü dene.
if ! termux-am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity 2>/dev/null; then
    betik_dizini=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
    rish_yolu="$betik_dizini/../../rish/rish"
    if [[ -r "$rish_yolu" ]]; then
        if ! RISH_APPLICATION_ID=com.termux sh "$rish_yolu" -c 'am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity'; then
            echo "Termux:X11 uygulamasını uygulama listesinden elle aç."
        fi
    else
        echo "Termux:X11 uygulamasını uygulama listesinden elle aç."
    fi
fi
echo "Masaüstü açılıyor. Bitirince XFCE menüsünden oturumu kapat veya bu terminalde Ctrl+C kullan."
DISPLAY=:1 dbus-run-session -- xfce4-session
