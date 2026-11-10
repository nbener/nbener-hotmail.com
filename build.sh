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
  cp "$APK" /sdcard/Download/Sezai-debug.apk
  echo "APK kopyalandı: /sdcard/Download/Sezai-debug.apk"
  # Kurulum ekranını aç; başarısız olursa kullanıcıya yönlendirme yap
  if am start -a android.intent.action.VIEW \
       -d "file:///sdcard/Download/Sezai-debug.apk" \
       -t "application/vnd.android.package-archive" \
       --grant-read-uri-permission 2>/dev/null; then
    echo "Kurulum ekranı açıldı."
  else
    echo "Kurulum ekranı açılamadı."
    echo "İndir klasörünü açıp Sezai-debug.apk dosyasına dokunun."
    am start -a android.intent.action.VIEW \
      -d "content://com.android.providers.downloads.documents/document/last_modified" \
      2>/dev/null || true
  fi
fi
