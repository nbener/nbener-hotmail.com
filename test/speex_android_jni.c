/* Yalnız debug yapay deney köprüsü; mikrofon/ses/ağ/dosya erişimi yoktur. */
#include <jni.h>
int sezai_yapay_aec_olc(double sonuc[5]);

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
