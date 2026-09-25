package com.paydart.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsModelsTest {

    @Test
    fun cameraResolution_fromName_returnsMatchingEnumOrFallback() {
        assertEquals(CameraResolution.RES_720P, CameraResolution.fromName("RES_720P"))
        assertEquals(CameraResolution.RES_1080P, CameraResolution.fromName("res_1080p"))
        assertEquals(CameraResolution.AUTO, CameraResolution.fromName("AUTO"))
        assertEquals(CameraResolution.RES_480P, CameraResolution.fromName("RES_480P"))
        // Fallback to 720p
        assertEquals(CameraResolution.RES_720P, CameraResolution.fromName(null))
        assertEquals(CameraResolution.RES_720P, CameraResolution.fromName("UNKNOWN_RES"))
    }

    @Test
    fun scanBehavior_fromName_returnsMatchingEnumOrFallback() {
        assertEquals(ScanBehavior.OPEN_IMMEDIATELY, ScanBehavior.fromName("OPEN_IMMEDIATELY"))
        assertEquals(ScanBehavior.CONFIRM_FIRST, ScanBehavior.fromName("confirm_first"))
        // Fallback to OPEN_IMMEDIATELY
        assertEquals(ScanBehavior.OPEN_IMMEDIATELY, ScanBehavior.fromName(null))
        assertEquals(ScanBehavior.OPEN_IMMEDIATELY, ScanBehavior.fromName("INVALID_BEHAVIOR"))
    }
}
