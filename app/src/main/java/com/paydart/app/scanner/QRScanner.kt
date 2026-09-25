package com.paydart.app.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance QR-only barcode analyzer using Google ML Kit.
 *
 * Configured specifically for [Barcode.FORMAT_QR_CODE] to eliminate unnecessary detector
 * overhead on low- and mid-range devices. Uses atomic pause flags to drop subsequent frames
 * in 0 microseconds upon first successful detection.
 */
class QRScanner(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(options)
    private val isPaused = AtomicBoolean(false)

    init {
        // Pre-warm the native ML Kit Barcode C++ library (libbarhopper_v3.so) on a background thread
        // so the first camera frame detects instantly with 0ms cold-start penalty.
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            try {
                val dummyBitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
                val dummyImage = InputImage.fromBitmap(dummyBitmap, 0)
                scanner.process(dummyImage)
            } catch (e: Exception) {
                // Ignore warmup failure
            }
        }
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (!isPaused.get()) {
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            // Atomically pause so subsequent in-flight frames discard in 0ms
                            if (isPaused.compareAndSet(false, true)) {
                                onQrDetected(rawValue)
                            }
                            break
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Ignore detector errors, analyzer will continue with next frame
            }
            .addOnCompleteListener {
                // Must always close ImageProxy to allow CameraX to deliver the next frame
                imageProxy.close()
            }
    }

    fun processImage(
        inputImage: InputImage,
        onSuccess: (List<Barcode>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        scanner.process(inputImage)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    fun close() {
        scanner.close()
    }
}
