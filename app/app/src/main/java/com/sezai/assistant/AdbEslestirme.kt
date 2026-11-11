package com.sezai.assistant

import android.util.Log
import java.io.*
import java.math.BigInteger
import java.net.Socket
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.*

/**
 * ADB Wireless Pairing implementasyonu (Android 11+ SPAKE2 protokolü).
 * Termux'un adb binary'si BoringSSL uyumsuzluğu nedeniyle TLS 1.3 pairing yapamıyor.
 * Bu sınıf Android'in kendi TLS stack'ini kullanarak doğrudan pairing yapar.
 */
object AdbEslestirme {

    private const val ETIKET = "Sezai"

    // SPAKE2 sabit kimlik string'leri (ADB protokolü sabit, null yok)
    private val KIMLIK_A = "adb pair client".toByteArray(Charsets.UTF_8) // 15 bayt
    private val KIMLIK_B = "adb pair server".toByteArray(Charsets.UTF_8) // 15 bayt

    // P-256 (secp256r1) eğrisi parametreleri
    private val P256_P = BigInteger(
        "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16
    )
    private val P256_A = BigInteger(
        "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16
    )
    private val P256_B = BigInteger(
        "5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16
    )
    private val P256_N = BigInteger(
        "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16
    )
    private val P256_GX = BigInteger(
        "6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16
    )
    private val P256_GY = BigInteger(
        "4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16
    )

    // SPAKE2 M noktası (sıkıştırılmış, ADB sabit)
    private val SPAKE2_M_COMPRESSED = hexToBytes(
        "02886e2f97ace46e55ba9dd7242579f2993b64e16ef3dcab95afd497333d8fa12f"
    )

    // SPAKE2 N noktası (sıkıştırılmış, ADB sabit)
    private val SPAKE2_N_COMPRESSED = hexToBytes(
        "03d8bbd6c639c62937b04d997f38c3770719c629d7014d49a24b4f98baa1292b49"
    )

    // PairingPacket tip sabitleri
    private const val TIP_SPAKE2_MSG = 0.toByte()
    private const val TIP_SPAKE2_VERIFIER = 1.toByte()
    private const val TIP_PEER_INFO = 2.toByte()
    private const val PROTOKOL_SURUMU = 1.toByte()

    // Termux ADB public key yolu
    private const val TERMUX_ADB_KEY_YOL = "/data/data/com.termux/files/home/.android/adbkey.pub"

    /**
     * P-256 eğrisindeki bir nokta. null koordinatlar sonsuz noktayı (identity) temsil eder.
     */
    private data class EkNokta(val x: BigInteger?, val y: BigInteger?) {
        companion object {
            val SONSUZ = EkNokta(null, null)
        }

        fun sonsuzdaMi() = x == null
    }

    /**
     * P-256 eğrisinde nokta toplama (affine koordinat).
     */
    private fun noktaToplama(p: EkNokta, q: EkNokta): EkNokta {
        if (p.sonsuzdaMi()) return q
        if (q.sonsuzdaMi()) return p

        val px = p.x!!; val py = p.y!!
        val qx = q.x!!; val qy = q.y!!

        // P == Q ise iki katını al
        if (px == qx) {
            if (py != qy) return EkNokta.SONSUZ  // P == -Q
            return noktaIkiKat(p)
        }

        // m = (qy - py) / (qx - px) mod P
        val m = (qy - py).multiply((qx - px).modInverse(P256_P)).mod(P256_P)
        val rx = (m * m - px - qx).mod(P256_P)
        val ry = (m * (px - rx) - py).mod(P256_P)
        return EkNokta(rx.let { if (it < BigInteger.ZERO) it + P256_P else it },
                       ry.let { if (it < BigInteger.ZERO) it + P256_P else it })
    }

    /**
     * P-256 eğrisinde nokta iki katlama.
     */
    private fun noktaIkiKat(p: EkNokta): EkNokta {
        if (p.sonsuzdaMi()) return p
        val x = p.x!!; val y = p.y!!
        if (y == BigInteger.ZERO) return EkNokta.SONSUZ

        // m = (3*x^2 + a) / (2*y) mod P
        val m = (BigInteger.valueOf(3) * x * x + P256_A)
            .multiply((BigInteger.valueOf(2) * y).modInverse(P256_P)).mod(P256_P)
        val rx = (m * m - x - x).mod(P256_P)
        val ry = (m * (x - rx) - y).mod(P256_P)
        return EkNokta(rx.let { if (it < BigInteger.ZERO) it + P256_P else it },
                       ry.let { if (it < BigInteger.ZERO) it + P256_P else it })
    }

    /**
     * P-256 eğrisinde skaler çarpma: k * P (double-and-add).
     */
    private fun skalarCarpma(k: BigInteger, p: EkNokta): EkNokta {
        var sonuc = EkNokta.SONSUZ
        var ekleyecek = p
        var kalan = k.mod(P256_N)
        while (kalan > BigInteger.ZERO) {
            if (kalan.testBit(0)) {
                sonuc = noktaToplama(sonuc, ekleyecek)
            }
            ekleyecek = noktaIkiKat(ekleyecek)
            kalan = kalan.shiftRight(1)
        }
        return sonuc
    }

    /**
     * P-256 noktası: sıkıştırılmış 33 bayttan açılır (02/03 prefix).
     */
    private fun sikistirmaAc(bytes: ByteArray): EkNokta {
        require(bytes.size == 33) { "Sıkıştırılmış nokta 33 bayt olmalı" }
        val prefix = bytes[0].toInt() and 0xFF
        require(prefix == 0x02 || prefix == 0x03) { "Geçersiz prefix: $prefix" }

        val x = BigInteger(1, bytes.copyOfRange(1, 33))
        // y^2 = x^3 + ax + b mod P
        val y2 = (x.pow(3) + P256_A * x + P256_B).mod(P256_P)

        // Modüler karekök (P ≡ 3 mod 4 için: y = y2^((P+1)/4) mod P)
        val y = y2.modPow((P256_P + BigInteger.ONE).divide(BigInteger.valueOf(4)), P256_P)

        // Prefix'e göre doğru y'yi seç
        val yFinal = if ((y.testBit(0)) == (prefix == 0x03)) y else P256_P - y
        return EkNokta(x, yFinal)
    }

    /**
     * P-256 noktasını sıkıştırılmamış 65 bayta çevirir (04 prefix + x + y).
     */
    private fun noktaSikistirmaSiz(p: EkNokta): ByteArray {
        val sonuc = ByteArray(65)
        sonuc[0] = 0x04
        val xBytes = bigIntTo32Bayt(p.x!!)
        val yBytes = bigIntTo32Bayt(p.y!!)
        System.arraycopy(xBytes, 0, sonuc, 1, 32)
        System.arraycopy(yBytes, 0, sonuc, 33, 32)
        return sonuc
    }

    /**
     * BigInteger'ı tam 32 bayta çevirir (sıfır dolgulu, işaretsiz).
     */
    private fun bigIntTo32Bayt(n: BigInteger): ByteArray {
        val ham = n.toByteArray()
        return when {
            ham.size == 32 -> ham
            ham.size > 32 -> ham.copyOfRange(ham.size - 32, ham.size) // öndeki sıfır baytı at
            else -> ByteArray(32).also { System.arraycopy(ham, 0, it, 32 - ham.size, ham.size) }
        }
    }

    /**
     * SPAKE2 w skaleri: SHA-512(len8LE(idA)+idA+len8LE(idB)+idB+len8LE(pin)+pin) → ilk 32 bayt → mod n
     */
    private fun spake2WSkaleriHesapla(pinBytes: ByteArray): BigInteger {
        val md = MessageDigest.getInstance("SHA-512")
        // len8LE = 8 bayt little-endian uzunluk
        md.update(uzunlukLE8(KIMLIK_A.size.toLong())); md.update(KIMLIK_A)
        md.update(uzunlukLE8(KIMLIK_B.size.toLong())); md.update(KIMLIK_B)
        md.update(uzunlukLE8(pinBytes.size.toLong())); md.update(pinBytes)
        val hash = md.digest()
        return BigInteger(1, hash.copyOfRange(0, 32)).mod(P256_N)
    }

    /**
     * 64-bit değeri 8 bayt little-endian olarak verir.
     */
    private fun uzunlukLE8(uzunluk: Long): ByteArray {
        val b = ByteArray(8)
        for (i in 0..7) b[i] = ((uzunluk shr (8 * i)) and 0xFF).toByte()
        return b
    }

    /**
     * PairingPacket gönderir: version(1) + type(1) + payload_size(4 BE) + payload
     */
    private fun paketGonder(cikis: OutputStream, tip: Byte, veri: ByteArray) {
        val baslik = ByteArray(6)
        baslik[0] = PROTOKOL_SURUMU
        baslik[1] = tip
        // payload boyutu: 4 bayt big-endian
        val boyut = veri.size
        baslik[2] = ((boyut shr 24) and 0xFF).toByte()
        baslik[3] = ((boyut shr 16) and 0xFF).toByte()
        baslik[4] = ((boyut shr 8) and 0xFF).toByte()
        baslik[5] = (boyut and 0xFF).toByte()
        cikis.write(baslik)
        cikis.write(veri)
        cikis.flush()
        Log.d(ETIKET, "ADB paket gönderildi: tip=$tip boyut=$boyut")
    }

    /**
     * PairingPacket okur. Tip ve veriyi döner. Versiyon/tip uyumsuzluğunda hata fırlatır.
     */
    private fun paketOku(giris: InputStream): Pair<Byte, ByteArray> {
        val baslik = ByteArray(6)
        var okunan = 0
        while (okunan < 6) {
            val n = giris.read(baslik, okunan, 6 - okunan)
            if (n < 0) throw EOFException("Bağlantı kapandı (başlık okunurken)")
            okunan += n
        }
        val versiyon = baslik[0]
        val tip = baslik[1]
        val boyut = ((baslik[2].toInt() and 0xFF) shl 24) or
                    ((baslik[3].toInt() and 0xFF) shl 16) or
                    ((baslik[4].toInt() and 0xFF) shl 8) or
                    (baslik[5].toInt() and 0xFF)
        if (versiyon != PROTOKOL_SURUMU) {
            throw IOException("Beklenmeyen paket versiyonu: $versiyon (beklenen: $PROTOKOL_SURUMU)")
        }
        if (boyut < 0 || boyut > 65536) {
            throw IOException("Geçersiz paket boyutu: $boyut")
        }
        val veri = ByteArray(boyut)
        var veriOkunan = 0
        while (veriOkunan < boyut) {
            val n = giris.read(veri, veriOkunan, boyut - veriOkunan)
            if (n < 0) throw EOFException("Bağlantı kapandı (veri okunurken)")
            veriOkunan += n
        }
        Log.d(ETIKET, "ADB paket alındı: tip=$tip boyut=$boyut")
        return Pair(tip, veri)
    }

    /**
     * Her bağlantı için kendinden imzalı RSA-2048 TLS sertifikası oluşturur.
     * Android'in kendi crypto provider'ı kullanılır.
     */
    private fun tlsBaglantisiOlustur(host: String, port: Int): Triple<SSLSocket, ByteArray, ByteArray> {
        // 1. Yeni RSA-2048 anahtar çifti oluştur
        val anahtarUreteci = KeyPairGenerator.getInstance("RSA")
        anahtarUreteci.initialize(2048, SecureRandom())
        val anahtarCifti = anahtarUreteci.generateKeyPair()

        // 2. Kendinden imzalı sertifika oluştur (X.509 v3)
        // Android'de BouncyCastle veya sun.security olmadığı için minimal ASN.1 oluşturuyoruz
        // Alternatif: Android Keystore yerine runtime-generated self-signed cert
        // org.conscrypt veya android.net.SSLCertificateSocketFactory kullan
        val sertifikaDER = kucukX509SertifikaOlustur(anahtarCifti)

        // 3. KeyStore'a yükle
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        val sertifika = java.security.cert.CertificateFactory
            .getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(sertifikaDER)) as X509Certificate
        ks.setKeyEntry("adb_key", anahtarCifti.private, CharArray(0), arrayOf(sertifika))

        // 4. TrustManager: karşı tarafın her sertifikasını kabul et (self-signed)
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        // 5. SSLContext kur
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, CharArray(0))

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, arrayOf(trustManager), SecureRandom())

        val sf = sslContext.socketFactory
        val socket = sf.createSocket(host, port) as SSLSocket

        // TLS 1.3 zorla
        socket.enabledProtocols = arrayOf("TLSv1.3")

        // Handshake başlat
        socket.startHandshake()

        // Sunucu sertifikasını al (verifier doğrulaması için DER)
        val sunucuSertDER = socket.session.peerCertificates[0].encoded

        Log.d(ETIKET, "ADB TLS handshake tamamlandı: ${socket.session.protocol}")

        return Triple(socket, sertifikaDER, sunucuSertDER)
    }

    /**
     * Minimal kendinden imzalı X.509 RSA sertifikası oluşturur (DER formatı).
     * Android API 28+ için org.conscrypt mevcuttur; biz Android'in Conscrypt'ini kullanıyoruz.
     * Ancak sertifika oluşturma için android.security.keystore yerine runtime üretim yapıyoruz.
     *
     * Not: Android API 28+ üzerinde android.security.X509Certificate API'si mevcut.
     * Daha basit yol: Conscrypt'in SelfSignedCertificate sınıfı veya BouncyCastle.
     * Termux/Android uyumlu en minimal yol: Java'nın sun.security.x509 kullanımı.
     * ARM Android'de sun.security.x509 paketleri mevcut olabilir; yoksa elle ASN.1 oluştururuz.
     */
    private fun kucukX509SertifikaOlustur(anahtarCifti: KeyPair): ByteArray {
        // Android'de sun.security.x509 genellikle mevcut değil.
        // Minimal DER kodlu self-signed X.509 v1 sertifika elle oluşturalım.
        // Sertifika içeriği: tbsCertificate imzalanır, SHA256withRSA kullanılır.

        val subjectPublicKeyInfo = anahtarCifti.public.encoded // SubjectPublicKeyInfo DER
        val seriNo = BigInteger(64, SecureRandom())

        // TBSCertificate bileşenleri
        val versiyon = derTag(0xA0, derInteger(ByteArray(1) { 0x00 })) // v1=0
        val seriNoDer = derInteger(seriNo.toByteArray())
        val imzaAlgId = derSequence(
            derOid(byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B, 0x05, 0x00))
            // SHA256withRSA OID = 1.2.840.113549.1.1.11, params NULL
        )
        val konuIssuer = derSequence(ByteArray(0)) // Boş distinguished name
        val gecerlilikBaslangic = derUTCTime("700101000000Z")
        val gecerlilikBitis = derUTCTime("991231235959Z")
        val gecerlilik = derSequence(gecerlilikBaslangic + gecerlilikBitis)
        val tbs = derSequence(
            versiyon + seriNoDer + imzaAlgId + konuIssuer + gecerlilik + konuIssuer + subjectPublicKeyInfo
        )

        // TBSCertificate imzala
        val imzalayici = Signature.getInstance("SHA256withRSA")
        imzalayici.initSign(anahtarCifti.private)
        imzalayici.update(tbs)
        val imzaBytes = imzalayici.sign()

        // Certificate = SEQUENCE { tbsCertificate, signatureAlgorithm, signatureValue }
        val imzaBitString = ByteArray(1) { 0x00 } + imzaBytes // bit string: 0 unused bits
        val sertifika = derSequence(
            tbs + imzaAlgId + derTag(0x03, imzaBitString)
        )
        return sertifika
    }

    // --- Minimal DER yardımcı fonksiyonları ---

    private fun derTag(tag: Int, icerik: ByteArray): ByteArray =
        byteArrayOf(tag.toByte()) + derUzunluk(icerik.size) + icerik

    private fun derSequence(icerik: ByteArray): ByteArray = derTag(0x30, icerik)

    private fun derInteger(deger: ByteArray): ByteArray {
        // INTEGER: negatif bit set ise öne 0x00 ekle
        val temiz = if (deger.isNotEmpty() && (deger[0].toInt() and 0x80) != 0)
            byteArrayOf(0x00) + deger else deger
        return derTag(0x02, temiz)
    }

    private fun derOid(oidBytes: ByteArray): ByteArray = derTag(0x06, oidBytes)

    private fun derUTCTime(zaman: String): ByteArray =
        derTag(0x17, zaman.toByteArray(Charsets.UTF_8))

    private fun derUzunluk(n: Int): ByteArray = when {
        n < 0x80 -> byteArrayOf(n.toByte())
        n < 0x100 -> byteArrayOf(0x81.toByte(), n.toByte())
        else -> byteArrayOf(0x82.toByte(), ((n shr 8) and 0xFF).toByte(), (n and 0xFF).toByte())
    }

    /**
     * HMAC-SHA256 hesaplar.
     */
    private fun hmacSha256(anahtar: ByteArray, veri: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(anahtar, "HmacSHA256"))
        return mac.doFinal(veri)
    }

    /**
     * Termux ADB public key'ini okur ve PEER_INFO payload'u için hazırlar.
     * ~/.android/adbkey.pub: "base64(RSAPublicKey) kullanici@makina" formatı
     * Android ADB PEER_INFO formatı: bu base64 string + null terminator, UTF-8
     */
    private fun adbPeerInfoOlustur(): ByteArray {
        val dosya = File(TERMUX_ADB_KEY_YOL)
        if (!dosya.exists()) {
            Log.w(ETIKET, "ADB public key bulunamadı: $TERMUX_ADB_KEY_YOL")
            // Boş/placeholder peer info — pairing yine de tamamlanabilir ama bağlantı kurulamaz
            return ByteArray(1) // sadece null terminator
        }
        val icerik = dosya.readText(Charsets.UTF_8).trim()
        Log.d(ETIKET, "ADB public key okundu (ilk 20 karakter): ${icerik.take(20)}...")
        // PEER_INFO payload: UTF-8 string + null terminator
        return (icerik + "\u0000").toByteArray(Charsets.UTF_8)
    }

    /**
     * Ana eşleştirme fonksiyonu. Arka plan thread'de çağrılmalıdır.
     *
     * @param host     ADB daemon'ın IP adresi
     * @param port     Pairing portu (Android'in gösterdiği ~5 haneli port)
     * @param pin      6 haneli PIN kodu
     * @param onSonuc  Sonuç callback'i: (basarili: Boolean, mesaj: String)
     */
    fun eslestir(host: String, port: Int, pin: String, onSonuc: (Boolean, String) -> Unit) {
        Thread {
            try {
                Log.i(ETIKET, "ADB_PAIR başlıyor: $host:$port")
                val basari = eslestirIc(host, port, pin)
                val mesaj = if (basari) "ADB_PAIR BASARILI: $host:$port" else "ADB_PAIR BASARISIZ: $host:$port"
                Log.i(ETIKET, mesaj)
                onSonuc(basari, mesaj)
            } catch (e: Exception) {
                val mesaj = "ADB_PAIR HATA: ${e.javaClass.simpleName}: ${e.message}"
                Log.e(ETIKET, mesaj, e)
                onSonuc(false, mesaj)
            }
        }.apply {
            name = "AdbEslestirme"
            isDaemon = true
            start()
        }
    }

    /**
     * Eşleştirme protokolünü yürütür. Başarı durumunda true döner.
     */
    private fun eslestirIc(host: String, port: Int, pin: String): Boolean {
        // 1. TLS bağlantısı kur (self-signed cert, her iki taraf)
        val (socket, bizimSertDER, sunucuSertDER) = tlsBaglantisiOlustur(host, port)

        socket.use {
            val giris = socket.inputStream
            val cikis = socket.outputStream

            // 2. PIN'i bayta çevir
            val pinBytes = pin.toByteArray(Charsets.UTF_8)

            // 3. SPAKE2 w skaleri hesapla
            val w = spake2WSkaleriHesapla(pinBytes)
            Log.d(ETIKET, "SPAKE2 w hesaplandı (mod n)")

            // 4. G noktası (P-256 generator)
            val G = EkNokta(P256_GX, P256_GY)

            // 5. İstemci rastgele skaleri x üret
            val guvenliRastgele = SecureRandom()
            var x: BigInteger
            do {
                x = BigInteger(256, guvenliRastgele).mod(P256_N)
            } while (x == BigInteger.ZERO)

            // 6. M noktasını sıkıştırmadan aç
            val M = sikistirmaAc(SPAKE2_M_COMPRESSED)
            val N = sikistirmaAc(SPAKE2_N_COMPRESSED)

            // 7. İstemci mesajı X = x*G + w*M (sıkıştırılmamış 65 bayt)
            val xG = skalarCarpma(x, G)
            val wM = skalarCarpma(w, M)
            val X = noktaToplama(xG, wM)
            val X_bytes = noktaSikistirmaSiz(X)
            Log.d(ETIKET, "SPAKE2 X nokta hesaplandı (65 bayt)")

            // 8. SPAKE2_MSG gönder (istemci mesajı)
            paketGonder(cikis, TIP_SPAKE2_MSG, X_bytes)

            // 9. Sunucunun SPAKE2_MSG'sini al
            val (tip1, Y_bytes) = paketOku(giris)
            if (tip1 != TIP_SPAKE2_MSG) {
                throw IOException("Beklenen SPAKE2_MSG (0), alınan: $tip1")
            }
            if (Y_bytes.size != 65) {
                throw IOException("Sunucu SPAKE2 mesajı 65 bayt olmalı, alınan: ${Y_bytes.size}")
            }
            Log.d(ETIKET, "Sunucu SPAKE2_MSG alındı")

            // 10. Sunucunun noktasını ayrıştır: Y (sıkıştırılmamış)
            val Y = EkNokta(
                BigInteger(1, Y_bytes.copyOfRange(1, 33)),
                BigInteger(1, Y_bytes.copyOfRange(33, 65))
            )

            // 11. K_AB = x * (Y - w*N)
            // Y - w*N = Y + (-w*N) = Y + (w * (-N)) [negatif nokta: y koordinatı negat]
            val wN = skalarCarpma(w, N)
            val wNNegatif = EkNokta(wN.x, if (wN.y != null) P256_P - wN.y else null)
            val YmwN = noktaToplama(Y, wNNegatif)
            val K_AB = skalarCarpma(x, YmwN)
            val K_AB_bytes = noktaSikistirmaSiz(K_AB)
            Log.d(ETIKET, "SPAKE2 K_AB hesaplandı")

            // 12. Transcript key = SHA-512(...)
            val md = MessageDigest.getInstance("SHA-512")
            val SPAKE2_APP_DATA = "SPAKE2 app data".toByteArray(Charsets.UTF_8)
            md.update(uzunlukLE8(SPAKE2_APP_DATA.size.toLong())); md.update(SPAKE2_APP_DATA)
            md.update(uzunlukLE8(KIMLIK_A.size.toLong())); md.update(KIMLIK_A)
            md.update(uzunlukLE8(KIMLIK_B.size.toLong())); md.update(KIMLIK_B)
            md.update(X_bytes)   // istemci X (sıkıştırılmamış)
            md.update(Y_bytes)   // sunucu Y (sıkıştırılmamış)
            md.update(K_AB_bytes) // paylaşılan K_AB (sıkıştırılmamış)
            val transkriptHash = md.digest() // 64 bayt SHA-512

            // 13. shared_key = transkript[32:64] (son 32 bayt)
            val sharedKey = transkriptHash.copyOfRange(32, 64)
            Log.d(ETIKET, "SPAKE2 shared_key türetildi")

            // 14. Bizim verifier'ımız: HMAC-SHA256(key=sharedKey, data=bizimSertDER)
            val bizimVerifier = hmacSha256(sharedKey, bizimSertDER)
            paketGonder(cikis, TIP_SPAKE2_VERIFIER, bizimVerifier)
            Log.d(ETIKET, "Verifier gönderildi")

            // 15. Sunucunun verifier'ını al ve doğrula
            val (tip2, sunucuVerifier) = paketOku(giris)
            if (tip2 != TIP_SPAKE2_VERIFIER) {
                throw IOException("Beklenen SPAKE2_VERIFIER (1), alınan: $tip2")
            }
            val beklenenSunucuVerifier = hmacSha256(sharedKey, sunucuSertDER)
            if (!sunucuVerifier.contentEquals(beklenenSunucuVerifier)) {
                Log.e(ETIKET, "Sunucu verifier doğrulaması BAŞARISIZ — PIN yanlış veya MITM")
                return false
            }
            Log.d(ETIKET, "Sunucu verifier doğrulandı")

            // 16. PEER_INFO gönder (Termux ADB public key)
            val peerInfo = adbPeerInfoOlustur()
            paketGonder(cikis, TIP_PEER_INFO, peerInfo)
            Log.d(ETIKET, "PEER_INFO gönderildi")

            // 17. Sunucunun PEER_INFO'sunu al (opsiyonel, bağlantıyı kapatabilir)
            try {
                val (tip3, _) = paketOku(giris)
                Log.d(ETIKET, "Sunucu PEER_INFO alındı: tip=$tip3")
            } catch (e: EOFException) {
                Log.d(ETIKET, "Sunucu PEER_INFO sonrası bağlantıyı kapattı (normal)")
            }

            return true
        }
    }

    // --- Yardımcı: hex string'i byte array'e çevir ---
    private fun hexToBytes(hex: String): ByteArray {
        val temiz = hex.replace(" ", "")
        return ByteArray(temiz.length / 2) { i ->
            temiz.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
