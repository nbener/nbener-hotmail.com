/* Mikrofon/ağ/dosya sesi kullanmadan referanslı AEC'yi sınar.
 * Yapay ikinci sinyal gerçek Türkçe konuşma kabulünün yerine geçmez. */
#include <speex/speex_echo.h>
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <time.h>

enum { HZ = 16000, KARE = 160, TUR = 1200, GECMIS = 512 };
typedef struct { double yankili, artik, yakin, capraz; } Olcum;

static double saat(void) {
    struct timespec t;
    clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &t);
    return t.tv_sec + t.tv_nsec / 1e9;
}

static Olcum dene(int referansVar, int ikinciSes) {
    SpeexEchoState *s = speex_echo_state_init(KARE, 1600);
    if (!s) return (Olcum){NAN, NAN, NAN, NAN};
    int hz = HZ;
    speex_echo_ctl(s, SPEEX_ECHO_SET_SAMPLING_RATE, &hz);
    int16_t hoparlor[KARE], mikrofon[KARE], cikis[KARE];
    int16_t gecmis[GECMIS] = {0};
    double yakin[KARE];
    uint32_t rastgele = 123456789;
    double yumusak = 0;
    Olcum o = {0};
    for (int tur = 0; tur < TUR; ++tur) {
        for (int i = 0; i < KARE; ++i) {
            int n = tur * KARE + i;
            rastgele = 1664525u * rastgele + 1013904223u;
            double gurultu = ((int)(rastgele >> 16) - 32768) * 0.10;
            yumusak = 0.65 * yumusak + 0.35 * gurultu;
            int16_t uzak = (int16_t)yumusak;
            gecmis[n % GECMIS] = uzak;
            double yanki = 0.55 * gecmis[(n + GECMIS - 80) % GECMIS]
                         + 0.20 * gecmis[(n + GECMIS - 160) % GECMIS];
            yakin[i] = ikinciSes && tur >= 1000
                ? 700 * sin(2 * 3.141592653589793 * 317 * n / HZ)
                    + 400 * sin(2 * 3.141592653589793 * 719 * n / HZ) : 0;
            mikrofon[i] = (int16_t)(yanki + yakin[i]);
            hoparlor[i] = referansVar ? uzak : 0;
        }
        speex_echo_cancellation(s, mikrofon, hoparlor, cikis);
        if (tur >= 1000) for (int i = 0; i < KARE; ++i) {
            double yanki = mikrofon[i] - yakin[i];
            double artik = cikis[i] - yakin[i];
            o.yankili += yanki * yanki;
            o.artik += artik * artik;
            o.yakin += yakin[i] * yakin[i];
            o.capraz += cikis[i] * yakin[i];
        }
    }
    speex_echo_state_destroy(s);
    return o;
}

int sezai_yapay_aec_olc(double sonuc[5]) {
    double bas = saat();
    Olcum temiz = dene(1, 0), kontrol = dene(0, 0), birlikte = dene(1, 1);
    double azalma = 10 * log10((temiz.yankili + 1) / (temiz.artik + 1));
    double kontrolDb = 10 * log10((kontrol.yankili + 1) / (kontrol.artik + 1));
    double birlikteDb = 10 * log10((birlikte.yankili + 1) / (birlikte.artik + 1));
    double koruma = birlikte.capraz / birlikte.yakin;
    int gecti = isfinite(azalma) && azalma > 15 && fabs(kontrolDb) < 1
        && isfinite(koruma) && koruma > 0.75 && koruma < 1.25 && birlikteDb > 6;
    sonuc[0] = azalma; sonuc[1] = kontrolDb; sonuc[2] = koruma;
    sonuc[3] = birlikteDb; sonuc[4] = (saat() - bas) * 1000;
    return gecti ? 0 : 1;
}

#ifndef SEZAI_KUTUPHANE
int main(void) {
    double sonuc[5];
    int kod = sezai_yapay_aec_olc(sonuc);
    printf("{\"yapay_yanki_azalma_db\":%.3f,\"referanssiz_kontrol_db\":%.3f,"
           "\"yapay_ikinci_ses_katsayisi\":%.3f,\"birlikte_artik_azalma_db\":%.3f,"
           "\"islem_cpu_ms\":%.3f,\"uretilen_toplam_ses_ms\":36000,\"gecti\":%s}\n",
           sonuc[0], sonuc[1], sonuc[2], sonuc[3], sonuc[4], kod == 0 ? "true" : "false");
    return kod;
}
#endif
