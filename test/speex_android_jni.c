/* Yalnız debug yapay deney köprüsü; mikrofon/ses/ağ/dosya erişimi yoktur. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <speex/speex_echo.h>
int sezai_yapay_aec_olc(double sonuc[5]);

typedef struct { SpeexEchoState *aec; int kare; } Oturum;

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
    if (!s || !s->aec) {
        free(s); hata(env, "java/lang/OutOfMemoryError", "AEC oturumu"); return 0;
    }
    speex_echo_ctl(s->aec, SPEEX_ECHO_SET_SAMPLING_RATE, &hz);
    return (jlong)(intptr_t)s;
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
    if (s) { speex_echo_state_destroy(s->aec); free(s); }
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
