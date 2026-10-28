# Sezai derleme ortamı: sürümler sabit, her oturumda "source ~/sezai/env.sh"
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export GRADLE_HOME=$HOME/tools/gradle-8.10.2
export PATH=$JAVA_HOME/bin:$GRADLE_HOME/bin:$PATH
# Google'ın aapt2'si arm64 Linux'ta yok; sistemdeki aapt2 kullanılır
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Xmx1g -Dandroid.aapt2FromMavenOverride=/usr/bin/aapt2"
