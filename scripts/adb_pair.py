#!/usr/bin/env python3
"""ADB wireless pairing (Android 11+ SPAKE2) — Termux native TLS sorunu bypass."""
import hashlib, hmac, socket, ssl, struct, sys, os
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.x509 import CertificateBuilder, NameAttribute, random_serial_number
from cryptography.x509.oid import NameOID
from cryptography import x509
import datetime, tempfile
from spake2 import SPAKE2_A

# ADB pairing packet: version(1) + type(1) + payload_size(4 BE) + payload
SPAKE2_MSG      = 0
SPAKE2_VERIFIER = 1
PEER_INFO       = 2

def send_pkt(sock, ptype, payload):
    hdr = struct.pack('>BBL', 1, ptype, len(payload))
    sock.sendall(hdr + payload)

def recv_pkt(sock):
    hdr = recvall(sock, 6)
    ver, ptype, size = struct.unpack('>BBL', hdr)
    return ptype, recvall(sock, size)

def recvall(sock, n):
    buf = b''
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError(f'EOF after {len(buf)}/{n} bayt')
        buf += chunk
    return buf

def gen_cert(key):
    sub = x509.Name([NameAttribute(NameOID.COMMON_NAME, 'adb-pairing-termux')])
    now = datetime.datetime.utcnow()
    return (CertificateBuilder()
        .subject_name(sub).issuer_name(sub)
        .public_key(key.public_key())
        .serial_number(random_serial_number())
        .not_valid_before(now)
        .not_valid_after(now + datetime.timedelta(days=3650))
        .sign(key, hashes.SHA256()))

def pair(host, port, pin):
    # RSA anahtar + sertifika
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    cert = gen_cert(key)
    with tempfile.TemporaryDirectory() as td:
        kf = os.path.join(td, 'k.pem')
        cf = os.path.join(td, 'c.pem')
        open(kf, 'wb').write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.TraditionalOpenSSL, serialization.NoEncryption()))
        open(cf, 'wb').write(cert.public_bytes(serialization.Encoding.PEM))

        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        ctx.load_cert_chain(cf, kf)
        ctx.set_ciphers('DEFAULT@SECLEVEL=0')
        # TLS 1.2 veya 1.3 — Android sürümüne göre değişir
        ctx.minimum_version = ssl.TLSVersion.TLSv1_2

        raw = socket.create_connection((host, port), timeout=10)
        tls = ctx.wrap_socket(raw, server_hostname=host)
        print(f'TLS bağlantısı: {tls.version()} cipher={tls.cipher()[0]}')

        # Sunucu sertifikasını al
        server_cert_der = tls.getpeercert(binary_form=True)

        # SPAKE2_A (istemci/Alice)
        pin_bytes = pin.encode('utf-8')
        sp = SPAKE2_A(pin_bytes)
        msg = sp.start()
        print(f'SPAKE2 mesajı: {len(msg)} bayt gönderiliyor')
        send_pkt(tls, SPAKE2_MSG, msg)

        ptype, payload = recv_pkt(tls)
        if ptype != SPAKE2_MSG:
            raise ValueError(f'Beklenen SPAKE2_MSG (0), gelen {ptype}')
        print(f'SPAKE2 yanıtı: {len(payload)} bayt alındı')

        shared_key = sp.finish(payload)
        print(f'Paylaşılan anahtar türetildi: {len(shared_key)} bayt')

        # Verifier: HMAC-SHA256(shared_key, client_cert_der)
        my_cert_der = cert.public_bytes(serialization.Encoding.DER)
        verifier = hmac.new(shared_key, my_cert_der, hashlib.sha256).digest()
        send_pkt(tls, SPAKE2_VERIFIER, verifier)

        ptype, server_verifier = recv_pkt(tls)
        if ptype != SPAKE2_VERIFIER:
            raise ValueError(f'Beklenen SPAKE2_VERIFIER (1), gelen {ptype}')

        expected = hmac.new(shared_key, server_cert_der, hashlib.sha256).digest()
        if not hmac.compare_digest(server_verifier, expected):
            raise ValueError('Sunucu verifier uyuşmuyor — yanlış PIN?')
        print('Verifier doğrulandı')

        # Peer info: sertifika DER
        send_pkt(tls, PEER_INFO, my_cert_der)
        ptype, _ = recv_pkt(tls)
        if ptype != PEER_INFO:
            raise ValueError(f'Beklenen PEER_INFO (2), gelen {ptype}')

        print('✓ Eşleştirme başarılı!')
        # Sertifikayı ADB keys dizinine kaydet
        adb_dir = os.path.expanduser('~/.android')
        os.makedirs(adb_dir, exist_ok=True)
        adb_key = os.path.join(adb_dir, 'adbkey')
        if not os.path.exists(adb_key):
            os.system(f'adb keygen {adb_key} 2>/dev/null')
        tls.close(); raw.close()
        return True

if __name__ == '__main__':
    if len(sys.argv) != 4:
        print(f'Kullanım: {sys.argv[0]} HOST PORT PIN')
        sys.exit(1)
    host, port, pin = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    try:
        pair(host, port, pin)
    except Exception as e:
        print(f'HATA: {e}')
        sys.exit(1)
