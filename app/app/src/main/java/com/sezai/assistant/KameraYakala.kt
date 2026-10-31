package com.sezai.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size

// Tek kare çeker: kamera yalnız çekim süresince açık kalır, görüntü diske yazılmaz, yalnız bellekte döner.
// Varsayılan arka kamera. Önizleme yüzeyi yok; ışık/odak oturması için kısa süre küçük bir akış çalışır.
class KameraYakala(private val context: Context) {
    private val anaIs = Handler(Looper.getMainLooper())

    // sonuc ana iş parçacığında: (JPEG baytları veya null, kullanıcıya söylenecek hata/boş)
    @SuppressLint("MissingPermission")
    fun cek(on: Boolean, sonuc: (ByteArray?, String) -> Unit) {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            sonuc(null, "Kamera izni verilmemiş. Uygulamadan izin verebilirsiniz."); return
        }
        val yonetici = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val hedef = if (on) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        val id = try {
            yonetici.cameraIdList.firstOrNull { yonetici.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == hedef }
        } catch (e: Exception) { Log.w("Sezai", "KAMERA liste hata: ${e.javaClass.simpleName}"); null }
        if (id == null) { sonuc(null, if (on) "Ön kamerayı bulamadım." else "Arka kamerayı bulamadım."); return }
        val ozellik = yonetici.getCameraCharacteristics(id)
        val harita = ozellik.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val jpegBoyut = harita?.getOutputSizes(ImageFormat.JPEG)?.filter { maxOf(it.width, it.height) <= 1600 }
            ?.maxByOrNull { it.width * it.height } ?: Size(1280, 960)
        val onizlemeBoyut = harita?.getOutputSizes(ImageFormat.YUV_420_888)?.filter { maxOf(it.width, it.height) <= 640 }
            ?.maxByOrNull { it.width * it.height } ?: Size(640, 480)
        val donus = ozellik.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        val is_ = HandlerThread("sezai-kamera").also { it.start() }
        val h = Handler(is_.looper)
        val jpegOkuyucu = ImageReader.newInstance(jpegBoyut.width, jpegBoyut.height, ImageFormat.JPEG, 2)
        val onizleme = ImageReader.newInstance(onizlemeBoyut.width, onizlemeBoyut.height, ImageFormat.YUV_420_888, 2)
        var kamera: CameraDevice? = null
        var oturum: CameraCaptureSession? = null
        var bitti = false

        fun bitir(veri: ByteArray?, mesaj: String) {
            synchronized(this) { if (bitti) return; bitti = true }
            try { oturum?.close() } catch (_: Exception) {}
            try { kamera?.close() } catch (_: Exception) {}
            try { jpegOkuyucu.close(); onizleme.close() } catch (_: Exception) {}
            is_.quitSafely()
            Log.i("Sezai", "KAMERA bitti ${if (veri != null) "bayt=${veri.size}" else "hata"}")
            anaIs.post { sonuc(veri, mesaj) }
        }

        anaIs.postDelayed({ bitir(null, "Kamera zamanında yanıt vermedi.") }, 10000)
        onizleme.setOnImageAvailableListener({ r -> try { r.acquireLatestImage()?.close() } catch (_: Exception) {} }, h)
        jpegOkuyucu.setOnImageAvailableListener({ r ->
            try {
                val resim = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                val tampon = resim.planes[0].buffer
                val bayt = ByteArray(tampon.remaining()); tampon.get(bayt); resim.close()
                bitir(bayt, "")
            } catch (e: Exception) { bitir(null, "Resim okunamadı.") }
        }, h)

        try {
            yonetici.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) {
                    kamera = c
                    try {
                        @Suppress("DEPRECATION")
                        c.createCaptureSession(listOf(onizleme.surface, jpegOkuyucu.surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) {
                                oturum = s
                                try {
                                    val on_ = c.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                        addTarget(onizleme.surface)
                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                    }.build()
                                    s.setRepeatingRequest(on_, null, h)
                                    // Pozlama ve odak oturması için kısa bekleme, sonra tek kare
                                    h.postDelayed({
                                        try {
                                            val kare = c.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                                addTarget(jpegOkuyucu.surface)
                                                set(CaptureRequest.JPEG_ORIENTATION, donus)
                                                set(CaptureRequest.JPEG_QUALITY, 85.toByte())
                                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                            }.build()
                                            s.capture(kare, null, h)
                                        } catch (e: Exception) { bitir(null, "Resim çekilemedi.") }
                                    }, 1200)
                                } catch (e: Exception) { bitir(null, "Kamera oturumu başlatılamadı.") }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) { bitir(null, "Kamera hazırlanamadı.") }
                        }, h)
                    } catch (e: Exception) { bitir(null, "Kamera oturumu açılamadı.") }
                }
                override fun onDisconnected(c: CameraDevice) { kamera = c; bitir(null, "Kamera bağlantısı kesildi.") }
                override fun onError(c: CameraDevice, kod: Int) { kamera = c; Log.w("Sezai", "KAMERA hata kodu=$kod"); bitir(null, "Kamera açılamadı. Başka bir uygulama kullanıyor olabilir.") }
            }, h)
        } catch (e: Exception) {
            Log.w("Sezai", "KAMERA acilamadi: ${e.javaClass.simpleName}")
            bitir(null, "Kamera açılamadı.")
        }
    }
}
