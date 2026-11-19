import com.sezai.assistant.KomutYorumlayici;
import com.sezai.assistant.Komut;

public class YorumlaTest {
    public static void main(String[] args) {
        String[][] testler = {
            {"saat kaç", "SaatSor"},
            {"bugün günlerden ne", "TarihSor"},
            {"sabah yedi buçuğa alarm kur", "AlarmKur(saat=7, dakika=30)"},
            {"akşam altıda alarm kur", "AlarmKur(saat=18, dakika=0)"},
            {"saat 7:30 için alarm kur", "AlarmKur(saat=7, dakika=30)"},
            {"yarın sabah 6.45'e alarm ayarla", "AlarmKur(saat=6, dakika=45)"},
            {"gece on ikiye alarm kur", "AlarmKur(saat=0, dakika=0)"},
            {"sabah on bire alarm kur", "AlarmKur(saat=11, dakika=0)"},
            {"saat on iki otuza alarm kur", "AlarmKur(saat=12, dakika=30)"},
            {"akşam sekiz buçukta alarm kur", "AlarmKur(saat=20, dakika=30)"},
            {"beş dakikalık zamanlayıcı kur", "ZamanlayiciKur(saniye=300)"},
            {"on iki dakika zamanlayıcı kur", "ZamanlayiciKur(saniye=720)"},
            {"yirmi beş dakika zamanlayıcı kur", "ZamanlayiciKur(saniye=1500)"},
            {"bir buçuk saat sonrası için sayaç başlat", "ZamanlayiciKur(saniye=5400)"},
            {"otuz saniye zamanlayıcı kur", "ZamanlayiciKur(saniye=30)"},
            {"alarmı iptal et", "AlarmIptal"},
            {"bana bir fıkra anlat", "Bilinmiyor"},
            {"konumumu göster", "KonumGoster"},
            {"neredeyim", "KonumGoster"},
            {"Kayseri nin konumu nedir", "Bilinmiyor"},
            {"annemi ara", "AramaYap(hedef=anne)"},
            {"YouTube'da Tarkan ara", "YouTubeAra(sorgu=Tarkan)"},
            {"Tarkan Şımarık çal", "MuzikCal(sorgu=Tarkan Şımarık)"},
            {"feneri aç", "FenerAyar(ac=true)"},
            {"mesajlarımı oku", "MesajOku(adet=3)"},
            {"Kadıköy'e git", "NavigasyonBaslat(hedef=kadıköy)"},
            {"anneme toplantı iptal mesajı gönder: toplantı iptal", "MesajGonder(hedef=anne, metin=toplantı iptal)"},
            {"WhatsApp aç", "UygulamaAc(isim=whatsapp)"},
            {"haritayı aç", "UygulamaAc(isim=harita)"},
            {"YouTube'u aç", "UygulamaAc(isim=youtube)"},
            {"ayarları aç", "UygulamaAc(isim=ayarlar)"},
            {"bir buçukluk saat zamanlayıcı kur", "Bilinmiyor"},
            {"bir buçuğxyz saat zamanlayıcı kur", "Bilinmiyor"},
            {"bir buçuk dakika zamanlayıcı kur", "ZamanlayiciKur(saniye=90)"},
        };

        int hata = 0;
        for (String[] test : testler) {
            String cumle = test[0];
            String beklenen = test[1];
            Komut komut = KomutYorumlayici.INSTANCE.yorumla(cumle);
            String gelen = komut.toString();
            boolean dogru = gelen.contains(beklenen);
            System.out.println((dogru ? "PASS " : "FAIL ") + cumle);
            if (!dogru) {
                System.out.println("  Beklenen: " + beklenen + " Gelen: " + gelen);
                hata++;
            }
        }
        System.out.println("Sonuç: " + (testler.length - hata) + "/" + testler.length + " başarılı");
        System.exit(hata > 0 ? 1 : 0);
    }
}
