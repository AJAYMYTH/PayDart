package com.paydart.app.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.paydart.app.camera.CameraManager
import com.paydart.app.scanner.QRScanner
import com.paydart.app.scanner.UPIParser
import com.paydart.app.settings.AppSettings
import com.paydart.app.settings.CameraResolution
import com.paydart.app.settings.ScanBehavior
import com.paydart.app.settings.SettingsManager
import com.paydart.app.upi.LaunchResult
import com.paydart.app.upi.UPIIntentLauncher
import com.paydart.app.upi.UpiAppRegistry
import com.paydart.app.upi.UpiPaymentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

sealed interface ScannerUiState {
    data object PermissionRequired : ScannerUiState
    data object Scanning : ScannerUiState
    data object NoQrHint : ScannerUiState
    data class ConfirmPending(val request: UpiPaymentRequest) : ScannerUiState
    data class Error(val message: String, val isRecoverable: Boolean = true) : ScannerUiState
    data object Idle : ScannerUiState
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    val settingsManager = SettingsManager(application)
    val cameraManager = CameraManager(application)

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val settingsState: StateFlow<AppSettings> = settingsManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    val qrScanner = QRScanner { rawQr ->
        onQrScanned(rawQr)
    }

    private var activeActivity: WeakReference<Activity>? = null
    private var singleInstalledAppPackage: String? = null
    private var noDetectionHintJob: Job? = null
    private var errorDismissJob: Job? = null

    private val vibrator: Vibrator? by lazy {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        startNoDetectionTimer()
        detectSingleInstalledApp()
    }

    fun attachActivity(activity: Activity) {
        activeActivity = WeakReference(activity)
    }

    fun detachActivity() {
        activeActivity = null
    }

    private fun detectSingleInstalledApp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val installed = UpiAppRegistry.getInstalledUpiApps(getApplication())
                if (installed.size == 1) {
                    singleInstalledAppPackage = installed[0].packageName
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun startNoDetectionTimer() {
        noDetectionHintJob?.cancel()
        noDetectionHintJob = viewModelScope.launch {
            delay(8000)
            if (_uiState.value is ScannerUiState.Scanning) {
                _uiState.value = ScannerUiState.NoQrHint
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            _uiState.value = ScannerUiState.Scanning
            startNoDetectionTimer()
            cameraManager.resumeAnalysis()
        } else {
            _uiState.value = ScannerUiState.PermissionRequired
        }
    }

    fun onQrScanned(rawQr: String) {
        val currentState = _uiState.value
        if (currentState !is ScannerUiState.Scanning && currentState !is ScannerUiState.NoQrHint) {
            return
        }
        handleValidScannedValue(rawQr)
    }

    /**
     * Scans and processes a QR code from a static image URI selected from device storage/gallery.
     */
    fun scanImageUri(uri: Uri) {
        viewModelScope.launch {
            cameraManager.pauseScanner()
            noDetectionHintJob?.cancel()

            try {
                val context = getApplication<Application>()
                val inputImage = InputImage.fromFilePath(context, uri)

                qrScanner.processImage(
                    inputImage = inputImage,
                    onSuccess = { barcodes ->
                        val rawValue = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                        if (rawValue != null) {
                            handleValidScannedValue(rawValue)
                        } else {
                            _uiState.value = ScannerUiState.Error(
                                message = "No QR code found in selected image.",
                                isRecoverable = true
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = ScannerUiState.Error(
                            message = "Failed to analyze image: ${e.localizedMessage ?: "Unknown error"}",
                            isRecoverable = true
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(
                    message = "Could not load image: ${e.localizedMessage ?: "Invalid file"}",
                    isRecoverable = true
                )
            }
        }
    }

    private fun handleValidScannedValue(rawQr: String) {
        val request = UPIParser.parse(rawQr)
        if (request == null) {
            // Invalid or non-UPI QR: show error and auto-dismiss after 2s
            _uiState.value = ScannerUiState.Error(
                message = "This QR code is not a supported UPI payment QR.",
                isRecoverable = true
            )
            errorDismissJob?.cancel()
            errorDismissJob = viewModelScope.launch {
                delay(2000)
                if (_uiState.value is ScannerUiState.Error) {
                    resumeScanning()
                }
            }
            return
        }

        // VALID UPI QR DETECTED:
        // 1. Immediately pause scanner in memory (0 microseconds)
        cameraManager.pauseScanner()
        noDetectionHintJob?.cancel()

        // 2. Launch UPI Intent IMMEDIATELY! (Zero lag before redirection)
        when (settingsState.value.scanBehavior) {
            ScanBehavior.OPEN_IMMEDIATELY -> {
                launchUpiIntent(request)
            }
            ScanBehavior.CONFIRM_FIRST -> {
                _uiState.value = ScannerUiState.ConfirmPending(request)
            }
        }

        // 3. Trigger haptic feedback asynchronously (never delays intent launch)
        if (settingsState.value.vibrationEnabled) {
            triggerVibrationAsync()
        }

        // 4. Asynchronously clear analyzer to drop CPU to idle
        cameraManager.stopAnalysis()
    }

    fun launchUpiIntent(request: UpiPaymentRequest) {
        val launchContext: Context = activeActivity?.get() ?: getApplication<Application>()
        val result = UPIIntentLauncher.launch(
            context = launchContext,
            request = request,
            preferredPackage = settingsState.value.preferredPackage,
            fallbackPackage = singleInstalledAppPackage
        )

        when (result) {
            is LaunchResult.Success -> {
                _uiState.value = ScannerUiState.Idle
            }
            is LaunchResult.Error -> {
                _uiState.value = ScannerUiState.Error(
                    message = result.message,
                    isRecoverable = result.canFallbackToChooser
                )
            }
        }
    }

    fun confirmPayment(request: UpiPaymentRequest) {
        launchUpiIntent(request)
    }

    fun cancelPayment() {
        resumeScanning()
    }

    fun dismissError() {
        errorDismissJob?.cancel()
        resumeScanning()
    }

    fun resumeScanning() {
        _uiState.value = ScannerUiState.Scanning
        cameraManager.resumeAnalysis()
        startNoDetectionTimer()
    }

    fun toggleTorch() {
        val newTorchState = !_isTorchOn.value
        _isTorchOn.value = newTorchState
        cameraManager.setTorchEnabled(newTorchState)
    }

    fun setCameraResolution(resolution: CameraResolution) {
        viewModelScope.launch {
            settingsManager.updateResolution(resolution)
            cameraManager.updateResolution(resolution)
        }
    }

    fun setPreferredPackage(packageName: String?) {
        viewModelScope.launch {
            settingsManager.updatePreferredPackage(packageName)
        }
    }

    fun setScanBehavior(behavior: ScanBehavior) {
        viewModelScope.launch {
            settingsManager.updateScanBehavior(behavior)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateVibration(enabled)
        }
    }

    private fun triggerVibrationAsync() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        noDetectionHintJob?.cancel()
        errorDismissJob?.cancel()
        cameraManager.release()
        activeActivity = null
    }
}
