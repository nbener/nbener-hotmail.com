#!/bin/bash
# Sezai derleme betiği. Kullanım:
#   ./build.sh          -> debug APK derler
#   ./build.sh kur      -> derler, /sdcard/Download/Sezai-debug.apk'ya kopyalar ve kurulum ekranını açar
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
if command -v zipalign >/dev/null 2>&1; then zipalign -c 4 "$APK"; else echo "zipalign bulunamadı, hizalama kontrolü atlandı (AGP zaten hizalar)"; fi
apksigner verify --verbose "$APK"
aapt2 dump badging "$APK" | grep -E "^package"
sha256sum "$APK"
if [ "${1:-}" = "kur" ]; then
  SDKART=/sdcard/Download/Sezai-debug.apk
  cp "$APK" "$SDKART"
  echo "APK kopyalandı: $SDKART"
  RISH="$HOME/rish/rish"
  # Shizuku çalışıyorsa rish ile sessiz kurulum; yoksa GUI yükleyici
  if RISH_APPLICATION_ID=com.termux "$RISH" -c 'echo ok' >/dev/null 2>&1; then
    BOY=$(wc -c < "$APK")
    echo "Rish ile kuruluyor (Shizuku aktif)..."
    if RISH_APPLICATION_ID=com.termux "$RISH" -c "pm install -r -S $BOY" < "$APK"; then
      echo "Kuruldu."
      am start -n com.sezai.assistant/.MainActivity >/dev/null 2>&1 || true
    else
      echo "Rish kurulum başarısız. GUI yükleyici açılıyor."
      termux-open "$SDKART" 2>/dev/null || \
        am start -a android.intent.action.VIEW -d "file://$SDKART" \
          -t "application/vnd.android.package-archive" --grant-read-uri-permission 2>/dev/null || true
    fi
  else
    echo "Shizuku çalışmıyor. GUI yükleyici açılıyor."
    termux-open "$SDKART" 2>/dev/null || \
      am start -a android.intent.action.VIEW -d "file://$SDKART" \
        -t "application/vnd.android.package-archive" --grant-read-uri-permission 2>/dev/null || true
  fi
fi
