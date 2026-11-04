#!/bin/bash
# Sezai derleme betiği. Kullanım:
#   ./build.sh          -> debug APK derler
#   ./build.sh kur      -> derler, Shizuku/rish ile telefona kurar ve açar
set -euo pipefail
if [ "$#" -gt 1 ] || { [ "$#" -eq 1 ] && [ "$1" != "kur" ]; }; then
  echo "Kullanım: $0 [kur]" >&2
  exit 2
fi
source "$HOME/sezai/env.sh"
cd "$HOME/sezai/app"
gradle --no-daemon -q :app:assembleDebug
APK="$HOME/sezai/app/app/build/outputs/apk/debug/app-debug.apk"
# Gradle debug APK'sını imzalar; hizalama ve imza geçmeden dağıtma.
zipalign -c 4 "$APK"
apksigner verify --verbose "$APK"
aapt dump badging "$APK" | grep -E "^package"
sha256sum "$APK"
if [ "${1:-}" = "kur" ]; then
  cp "$APK" /sdcard/Download/Sezai-debug.apk
  cd "$HOME/rish"
  export RISH_APPLICATION_ID=com.termux
  sh ./rish -c 'set -e; hedef=$(mktemp /data/local/tmp/sezai-XXXXXX.apk); trap '\''rm -f "$hedef"'\'' EXIT; cp /sdcard/Download/Sezai-debug.apk "$hedef"; pm install -r "$hedef"; am start -W -n com.sezai.assistant/.MainActivity'
fi
