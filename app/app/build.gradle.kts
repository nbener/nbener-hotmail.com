plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
dependencies {
    // Yalnız sınırlı yerel ses denemesi; üretim motoru değişmez.
    implementation("com.alphacephei:vosk-android:0.3.75@aar")
    implementation("net.java.dev.jna:jna:5.18.1@aar")
}
android {
    namespace = "com.sezai.assistant"
    compileSdk = 34
    sourceSets.getByName("debug").jniLibs.srcDir("../../yerel-model/speex-deneme/android/jni")
    sourceSets.getByName("debug").assets.srcDir("../../yerel-model/speex-deneme/android/assets")
    defaultConfig {
        applicationId = "com.sezai.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.1"
        ndk { abiFilters += "arm64-v8a" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// Native dosya ayrı Termux adımıyla üretilir; eski/eksik dosya ile APK üretme.
val yerelAecDogrula by tasks.registering(Exec::class) {
    workingDir("../..")
    commandLine("python3", "scripts/speex_android_derle.py", "--kontrol")
}
tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(yerelAecDogrula) }
