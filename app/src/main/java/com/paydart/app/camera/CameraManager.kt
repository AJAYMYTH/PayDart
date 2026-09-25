package com.paydart.app.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.paydart.app.scanner.QRScanner
import com.paydart.app.settings.CameraResolution
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraManager"

class CameraManager(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var analysisExecutor: ExecutorService? = null
    private var qrScanner: QRScanner? = null

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    private var currentResolution: CameraResolution = CameraResolution.RES_720P

    /**
     * Initializes CameraProvider and binds Preview and ImageAnalysis to lifecycle.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        resolution: CameraResolution,
        scanner: QRScanner,
        onInitialized: () -> Unit = {}
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        currentResolution = resolution
        qrScanner = scanner

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                onInitialized()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraX provider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val lifecycleOwner = currentLifecycleOwner ?: return
        val previewView = currentPreviewView ?: return
        val scanner = qrScanner ?: return

        if (analysisExecutor == null || analysisExecutor?.isShutdown == true) {
            analysisExecutor = Executors.newSingleThreadExecutor()
        }

        // 1. Build ResolutionSelector based on current resolution preference
        val resolutionSelector = buildResolutionSelector(currentResolution)

        // 2. Build Preview
        val previewBuilder = Preview.Builder()
        resolutionSelector?.let { previewBuilder.setResolutionSelector(it) }
        preview = previewBuilder.build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        // 3. Build ImageAnalysis with STRATEGY_KEEP_ONLY_LATEST (zero backlog)
        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        resolutionSelector?.let { analysisBuilder.setResolutionSelector(it) }

        imageAnalysis = analysisBuilder.build().also { analysis ->
            analysisExecutor?.let { executor ->
                analysis.setAnalyzer(executor, scanner)
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun buildResolutionSelector(res: CameraResolution): ResolutionSelector {
        val targetSize = if (res == CameraResolution.AUTO) {
            Size(1280, 720)
        } else {
            Size(res.width, res.height)
        }

        val strategy = ResolutionStrategy(
            targetSize,
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
        )
        return ResolutionSelector.Builder()
            .setResolutionStrategy(strategy)
            .build()
    }

    /**
     * Instantly pauses frame consumption in 0 microseconds without blocking.
     */
    fun pauseScanner() {
        qrScanner?.pause()
    }

    /**
     * Pauses the scanner immediately and clears the analyzer asynchronously
     * without blocking the UI or intent launching thread.
     */
    fun stopAnalysis() {
        qrScanner?.pause()
        analysisExecutor?.execute {
            try {
                imageAnalysis?.clearAnalyzer()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping analysis", e)
            }
        }
    }

    /**
     * Resumes analysis for continuous scanning.
     */
    fun resumeAnalysis() {
        val scanner = qrScanner ?: return
        val executor = analysisExecutor ?: return
        try {
            scanner.resume()
            imageAnalysis?.setAnalyzer(executor, scanner)
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming analysis", e)
        }
    }

    /**
     * Updates resolution and cleanly rebinds use cases without restarting the activity.
     */
    fun updateResolution(newResolution: CameraResolution) {
        if (currentResolution == newResolution) return
        currentResolution = newResolution
        bindCameraUseCases()
    }

    /**
     * Toggles the device camera torch/flashlight if supported.
     */
    fun setTorchEnabled(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun hasTorch(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() ?: false
    }

    /**
     * Checks supported resolutions on the back camera.
     */
    fun getSupportedResolutions(): List<CameraResolution> {
        val cam = camera ?: return CameraResolution.entries.toList()
        return try {
            val camera2Info = Camera2CameraInfo.from(cam.cameraInfo)
            val streamMap = camera2Info.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = streamMap?.getOutputSizes(ImageFormat.YUV_420_888) ?: emptyArray()

            val supported = mutableListOf(CameraResolution.AUTO)
            for (res in listOf(CameraResolution.RES_480P, CameraResolution.RES_720P, CameraResolution.RES_1080P)) {
                val matches = outputSizes.any { size ->
                    (size.width == res.width && size.height == res.height) ||
                            (size.width == res.height && size.height == res.width)
                }
                if (matches) {
                    supported.add(res)
                }
            }
            supported
        } catch (e: Exception) {
            CameraResolution.entries.toList()
        }
    }

    /**
     * Releases executor and camera bindings.
     */
    fun release() {
        stopAnalysis()
        analysisExecutor?.shutdown()
        analysisExecutor = null
        cameraProvider?.unbindAll()
        qrScanner?.close()
    }
}
