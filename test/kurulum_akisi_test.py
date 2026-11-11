#!/usr/bin/env python3
"""build.sh kur: rish ile pm install akışını mock'larla test eder. Gerçek cihaz gerekmez."""
import os
import subprocess
import tempfile
from pathlib import Path

kok = Path(__file__).resolve().parents[1]
build_sh = kok / 'build.sh'
rish_bin = Path.home() / 'rish' / 'rish'

with tempfile.TemporaryDirectory() as gecici:
    d = Path(gecici)
    apk = d / 'test.apk'
    apk.write_bytes(b'\x50\x4b\x03\x04' + b'\x00' * 28)  # sahte APK (ZIP başlığı)
    sdcard = d / 'sdcard' / 'Download'
    sdcard.mkdir(parents=True)

    taklitler = {
        # Shizuku (rish) kontrolü ve kurulum
        'rish_ok': '#!/bin/sh\necho ok\n',
        'rish_fail': '#!/bin/sh\nexit 1\n',
        # zipalign ve apksigner doğrulama atlanır (APK sahte)
        'zipalign': '#!/bin/sh\nexit 0\n',
        'apksigner': '#!/bin/sh\necho "Verified using v2 scheme (APK Signature Scheme v2): true"\necho "Number of signers: 1"\n',
        'aapt2': '#!/bin/sh\necho "package: name=\'com.sezai.assistant\' versionCode=\'1\' versionName=\'0.0.1\'"\n',
        'sha256sum': '#!/bin/sh\necho "aabbcc  $1"\n',
        'gradle': '#!/bin/sh\nexit 0\n',
        'am': '#!/bin/sh\necho am_called >> "$TEST_DIR/am_log"\nexit 0\n',
        'termux-open': '#!/bin/sh\necho termux_open_called >> "$TEST_DIR/am_log"\nexit 0\n',
    }

    bin_dir = d / 'bin'
    bin_dir.mkdir()
    for ad, govde in taklitler.items():
        f = bin_dir / ad
        f.write_text(govde)
        f.chmod(0o755)

    # Rish binary takliti — RISH_APPLICATION_ID=com.termux ~/rish/rish -c '...' desenini karşılar
    rish_mock_dir = d / 'rish'
    rish_mock_dir.mkdir()

    def test_senaryo(ad, rish_govde, pm_govde, bekle_am):
        (d / 'am_log').unlink(missing_ok=True)
        rish_mock = rish_mock_dir / 'rish'
        rish_mock.write_text('#!/bin/sh\n' + rish_govde + '\n')
        rish_mock.chmod(0o755)
        pm_mock = bin_dir / 'pm'
        pm_mock.write_text('#!/bin/sh\n' + pm_govde + '\n')
        pm_mock.chmod(0o755)
        ortam = dict(
            os.environ,
            PATH=str(bin_dir) + ':' + os.environ['PATH'],
            HOME=gecici,
            TEST_DIR=gecici,
            ANDROID_HOME=gecici,
            ANDROID_SDK_ROOT=gecici,
            GRADLE_HOME=gecici,
            JAVA_HOME=gecici,
        )
        # env.sh yüklemesini atla: gradle sahte; APK yolunu build.sh içinden değiştirmeden çalıştır
        # Build.sh'i doğrudan çalıştır; APK değişkeni sahte APK'ya yönlendirilir
        betik = build_sh.read_text()
        betik = betik.replace(
            'source "$HOME/sezai/env.sh"', 'true'
        ).replace(
            'cd "$HOME/sezai/app"', f'cd {str(d)}'
        ).replace(
            'gradle --no-daemon -q :app:assembleDebug', 'true'
        ).replace(
            'APK="$HOME/sezai/app/app/build/outputs/apk/debug/app-debug.apk"',
            f'APK="{str(apk)}"'
        ).replace(
            'SDKART=/sdcard/Download/Sezai-debug.apk',
            f'SDKART={str(sdcard / "Sezai-debug.apk")}'
        ).replace(
            'RISH="$HOME/rish/rish"',
            f'RISH="{str(rish_mock_dir / "rish")}"'
        )
        sonuc = subprocess.run(
            ['bash', '-s', 'kur'], input=betik, text=True, capture_output=True,
            env=ortam, timeout=15
        )
        am_cagrildi = (d / 'am_log').exists()
        assert am_cagrildi == bekle_am, f'{ad}: am={am_cagrildi} bekle={bekle_am}\nstdout={sonuc.stdout}\nstderr={sonuc.stderr}'
        assert (sdcard / 'Sezai-debug.apk').exists(), f'{ad}: sdcard APK kopyalanmadı'
        print(f'PASS {ad}')

    # Shizuku aktif + pm başarılı → uygulama açılır
    test_senaryo('rish_aktif_kurulum_basarili',
                 rish_govde='echo ok',
                 pm_govde='echo Success; exit 0',
                 bekle_am=True)

    # Shizuku aktif + pm başarısız → GUI yükleyici açılır
    test_senaryo('rish_aktif_pm_basarisiz',
                 rish_govde='if echo "$@" | grep -q "pm install"; then exit 1; fi; echo ok',
                 pm_govde='exit 1',
                 bekle_am=True)

    # Shizuku yok → GUI yükleyici açılır
    test_senaryo('shizuku_kapali',
                 rish_govde='exit 1',
                 pm_govde='exit 0',
                 bekle_am=True)

print('3/3 kurulum akışı geçti; gerçek cihaz kurulumu değildir.')
