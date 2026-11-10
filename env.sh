# Sezai derleme ortamı: sürümler sabit, her oturumda "source ~/sezai/env.sh"
# Termux native derleme: proot gerekmez
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export GRADLE_HOME=$HOME/tools/gradle-8.10.2
export PATH=$JAVA_HOME/bin:$GRADLE_HOME/bin:$PATH
# aapt2 Termux pkg olarak kurulu
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Xmx1g -Dandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2"
