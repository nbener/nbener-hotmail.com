/* Yalnız debug yapay deney köprüsü; mikrofon/ses/ağ/dosya erişimi yoktur. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <speex/speex_echo.h>
#include <speex/speex_resampler.h>
int sezai_yapay_aec_olc(double sonuc[5]);

typedef struct { SpeexEchoState *aec; SpeexResamplerState *ornekleyici; int kare; } Oturum;

static void hata(JNIEnv *env, const char *tur, const char *metin) {
    jclass c = (*env)->FindClass(env, tur);
    if (c) (*env)->ThrowNew(env, c, metin);
}

JNIEXPORT jlong JNICALL
Java_com_sezai_assistant_YankiYerel_olustur(JNIEnv *env, jobject nesne, jint hz) {
    (void)nesne;
    if (hz != 16000 && hz != 24000 && hz != 48000) {
        hata(env, "java/lang/IllegalArgumentException", "AEC örnekleme hızı"); return 0;
    }
    Oturum *s = calloc(1, sizeof(*s));
    if (s) { s->kare = hz / 100; s->aec = speex_echo_state_init(s->kare, hz / 10); }
    int kod = RESAMPLER_ERR_SUCCESS;
    if (s && s->aec) s->ornekleyici = speex_resampler_init(1, hz, 16000, 3, &kod);
    if (!s || !s->aec || !s->ornekleyici || kod != RESAMPLER_ERR_SUCCESS) {
        if (s && s->aec) speex_echo_state_destroy(s->aec);
        if (s && s->ornekleyici) speex_resampler_destroy(s->ornekleyici);
        free(s); hata(env, "java/lang/OutOfMemoryError", "AEC oturumu"); return 0;
    }
    speex_echo_ctl(s->aec, SPEEX_ECHO_SET_SAMPLING_RATE, &hz);
    return (jlong)(intptr_t)s;
}

JNIEXPORT jint JNICALL
Java_com_sezai_assistant_YankiYerel_donustur(JNIEnv *env, jobject nesne, jlong kimlik,
        jshortArray giris, jshortArray cikis) {
    (void)nesne;
    Oturum *s = (Oturum *)(intptr_t)kimlik;
    if (!s) { hata(env, "java/lang/IllegalStateException", "AEC kapalı"); return 0; }
    if (!giris || !cikis || (*env)->GetArrayLength(env, giris) != s->kare || (*env)->GetArrayLength(env, cikis) != 160) {
        hata(env, "java/lang/IllegalArgumentException", "Örnekleme kare boyutu"); return 0;
    }
    jshort veri[480], sonuc[160];
    (*env)->GetShortArrayRegion(env, giris, 0, s->kare, veri);
    if ((*env)->ExceptionCheck(env)) return 0;
    spx_uint32_t alinan = s->kare, uretilen = 160;
    int kod = speex_resampler_process_int(s->ornekleyici, 0, veri, &alinan, sonuc, &uretilen);
    if (kod != RESAMPLER_ERR_SUCCESS || alinan != (spx_uint32_t)s->kare) {
        hata(env, "java/lang/IllegalStateException", "Ses örneği kaybı"); return 0;
    }
    (*env)->SetShortArrayRegion(env, cikis, 0, uretilen, sonuc);
    return uretilen;
}

JNIEXPORT jint JNICALL
Java_com_sezai_assistant_YankiYerel_orneklemeGecikmesi(JNIEnv *env, jobject nesne, jlong kimlik) {
    (void)nesne;
    Oturum *s = (Oturum *)(intptr_t)kimlik;
    if (!s) { hata(env, "java/lang/IllegalStateException", "AEC kapalı"); return 0; }
    return speex_resampler_get_output_latency(s->ornekleyici);
}

JNIEXPORT void JNICALL
Java_com_sezai_assistant_YankiYerel_isle(JNIEnv *env, jobject nesne, jlong kimlik,
        jshortArray mikrofon, jshortArray referans, jshortArray cikis) {
    (void)nesne;
    Oturum *s = (Oturum *)(intptr_t)kimlik;
    if (!s) { hata(env, "java/lang/IllegalStateException", "AEC kapalı"); return; }
    if (!mikrofon || !referans || !cikis || (*env)->GetArrayLength(env, mikrofon) != s->kare
            || (*env)->GetArrayLength(env, referans) != s->kare || (*env)->GetArrayLength(env, cikis) != s->kare) {
        hata(env, "java/lang/IllegalArgumentException", "AEC kare boyutu"); return;
    }
    jshort giris[480], ref[480], sonuc[480];
    (*env)->GetShortArrayRegion(env, mikrofon, 0, s->kare, giris);
    if ((*env)->ExceptionCheck(env)) return;
    (*env)->GetShortArrayRegion(env, referans, 0, s->kare, ref);
    if ((*env)->ExceptionCheck(env)) return;
    speex_echo_cancellation(s->aec, giris, ref, sonuc);
    (*env)->SetShortArrayRegion(env, cikis, 0, s->kare, sonuc);
}

JNIEXPORT void JNICALL
Java_com_sezai_assistant_YankiYerel_birak(JNIEnv *env, jobject nesne, jlong kimlik) {
    (void)env; (void)nesne;
    Oturum *s = (Oturum *)(intptr_t)kimlik;
    if (s) { speex_resampler_destroy(s->ornekleyici); speex_echo_state_destroy(s->aec); free(s); }
}

JNIEXPORT jdoubleArray JNICALL
Java_com_sezai_assistant_YankiYerel_yapayOlc(JNIEnv *env, jobject nesne) {
    (void)nesne;
    double sonuc[5];
    if (sezai_yapay_aec_olc(sonuc) != 0) {
        jclass hata = (*env)->FindClass(env, "java/lang/IllegalStateException");
        if (hata) (*env)->ThrowNew(env, hata, "Yapay AEC kabul ölçütü geçmedi");
        return NULL;
    }
    jdoubleArray dizi = (*env)->NewDoubleArray(env, 5);
    if (dizi) (*env)->SetDoubleArrayRegion(env, dizi, 0, 5, sonuc);
    return dizi;
}
