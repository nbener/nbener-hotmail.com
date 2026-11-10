#!/bin/bash
# Ubuntu proot içinde: bash test/sessiz_komut_jvm.sh
set -euo pipefail
cd "$(dirname "$0")/.."
source ./env.sh
kutuphane="$GRADLE_HOME/lib"
gecici=$(mktemp -d /root/sezai/test/.sessiz-jvm-XXXXXX)
trap 'rm -rf -- "$gecici"' EXIT
java -Dfile.encoding=UTF-8 -cp "$kutuphane/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -classpath "$kutuphane/kotlin-stdlib-1.9.24.jar:$kutuphane/annotations-24.0.1.jar" \
  -d "$gecici/test.jar" \
  app/app/src/main/java/com/sezai/assistant/KomutYorumlayici.kt \
  test/sessiz_komut_jvm.kt
java -Dfile.encoding=UTF-8 -cp "$gecici/test.jar:$kutuphane/kotlin-stdlib-1.9.24.jar" com.sezai.assistant.Sessiz_komut_jvmKt
