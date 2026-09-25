package com.paydart.app.settings

enum class CameraResolution(val title: String, val width: Int, val height: Int) {
    AUTO("Auto (Default)", 0, 0),
    RES_480P("480p (SD - 854x480)", 854, 480),
    RES_720P("720p (HD - 1280x720)", 1280, 720),
    RES_1080P("1080p (FHD - 1920x1080)", 1920, 1080);

    companion object {
        fun fromName(name: String?): CameraResolution {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: RES_720P
        }
    }
}

enum class ScanBehavior(val title: String, val description: String) {
    OPEN_IMMEDIATELY("Open immediately", "Launch UPI app instantly when QR is scanned"),
    CONFIRM_FIRST("Confirm before opening", "Review payee name & amount before opening UPI app");

    companion object {
        fun fromName(name: String?): ScanBehavior {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OPEN_IMMEDIATELY
        }
    }
}

data class AppSettings(
    val cameraResolution: CameraResolution = CameraResolution.RES_720P,
    val preferredPackage: String? = null,
    val scanBehavior: ScanBehavior = ScanBehavior.OPEN_IMMEDIATELY,
    val vibrationEnabled: Boolean = true
)
