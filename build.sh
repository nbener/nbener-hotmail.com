#!/bin/bash
# Sezai derleme betiği. Kullanım:
#   ./build.sh          -> debug APK derler
#   ./build.sh kur      -> derler, Shizuku/rish ile telefona kurar ve açar
set -euo pipefail
source "$HOME/sezai/env.sh"
cd "$HOME/sezai/app"
gradle --no-daemon -q :app:assembleDebug
APK="$HOME/sezai/app/app/build/outputs/apk/debug/app-debug.apk"
aapt dump badging "$APK" | grep -E "^package"
if [ "${1:-}" = "kur" ]; then
  cp "$APK" /sdcard/Download/Sezai-debug.apk
  cd "$HOME/rish"
  export RISH_APPLICATION_ID=com.termux
  sh ./rish -c 'cp /sdcard/Download/Sezai-debug.apk /data/local/tmp/sezai.apk && pm install -r /data/local/tmp/sezai.apk; rm -f /data/local/tmp/sezai.apk; am start -n com.sezai.assistant/.MainActivity'
fi
