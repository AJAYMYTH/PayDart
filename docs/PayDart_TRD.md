# PayDart — Technical Requirements Document (TRD)

**Project:** PayDart — Lightweight UPI QR Launcher
**Doc version:** v1.0
**Status:** Draft

---

## 1. Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Camera | CameraX |
| QR/Barcode detection | Google ML Kit Barcode Scanning (on-device) |
| App-to-app handoff | Android `Intent` |
| Build system | Gradle (Kotlin DSL) |
| Local persistence | `DataStore` (Preferences) for settings only |

**Minimum SDK:** Recommend API 24 (Android 7.0) for broad low/mid-range device coverage — CameraX and ML Kit both support this; confirm against current CameraX/ML Kit min-SDK requirements at implementation time.
**Target/Compile SDK:** Latest stable at build time.

## 2. QR Detection Configuration

```kotlin
val options = BarcodeScannerOptions.Builder()
    .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // ONLY QR — no EAN/UPC/Code128/PDF417/DataMatrix/Aztec
    .build()
val scanner = BarcodeScanning.getClient(options)
```

Rationale: restricting formats reduces per-frame detector work, which is the single biggest lever on low-end CPUs.

## 3. Camera Pipeline (CameraX)

### 3.1 Resolution
- Default target resolution: **1280×720**.
- User-selectable: Auto / 480p (854×480) / 720p (1280×720) / 1080p (1920×1080).
- Before exposing an option, query `Camera2CameraInfo` / `CameraCharacteristics` (via CameraX's `CameraInfo`) to confirm the device actually supports that stream configuration. Never present a resolution the device can't deliver.

### 3.2 Frame strategy
```kotlin
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(selectedResolution)
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
```
- `STRATEGY_KEEP_ONLY_LATEST` guarantees no frame queue buildup — the analyzer always gets the freshest frame, dropping stale ones instead of processing a backlog.
- Analyzer callback converts `ImageProxy` → ML Kit `InputImage` directly (no intermediate Bitmap copy).
- **Always call `imageProxy.close()`** in a `finally` block regardless of success/failure to avoid stalling the CameraX pipeline.

### 3.3 Stop-on-detect
```
onSuccess(barcodes) {
    if (validQrFound) {
        imageAnalysis.clearAnalyzer()   // or unbind analysis use case
        // proceed to parse + launch
    }
}
```
Analysis must stop as close to detection time as possible — no further frames should reach the detector after a valid UPI QR is found.

### 3.4 Resolution change flow
1. Persist new setting (DataStore).
2. Unbind current `ImageAnalysis`/`Preview` use cases from lifecycle.
3. Rebuild `ImageAnalysis` with new target resolution.
4. Rebind to `ProcessCameraProvider`.
5. Resume scanning.

Never trigger this cycle automatically on a failed-frame basis — only on explicit user setting change.

## 4. UPI URI Parsing & Validation

### 4.1 Expected format
```
upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=<currency>&tn=<note>&tr=<ref>
```

### 4.2 Validation rules
- Scheme must be exactly `upi`.
- Host/path must be `pay` (`upi://pay?...`).
- `pa` (payee VPA) is required; must match a VPA-like pattern (`handle@bank`).
- `am`, `cu` are optional but if present must be well-formed (numeric amount; ISO currency code).
- Reject anything that doesn't match — **never launch an arbitrary URI**, even if it superficially resembles one. This is the primary security control in the app.

### 4.3 Implementation sketch
```kotlin
data class UpiPaymentRequest(
    val payeeVpa: String,
    val payeeName: String?,
    val amount: String?,
    val currency: String?,
    val note: String?,
    val txnRef: String?
)

fun parseUpiUri(raw: String): UpiPaymentRequest? {
    val uri = Uri.parse(raw)
    if (uri.scheme != "upi" || uri.host != "pay") return null
    val pa = uri.getQueryParameter("pa") ?: return null
    if (!pa.matches(Regex("^[\\w.\\-]+@[\\w.\\-]+$"))) return null
    return UpiPaymentRequest(
        payeeVpa = pa,
        payeeName = uri.getQueryParameter("pn"),
        amount = uri.getQueryParameter("am"),
        currency = uri.getQueryParameter("cu"),
        note = uri.getQueryParameter("tn"),
        txnRef = uri.getQueryParameter("tr")
    )
}
```

## 5. UPI App Launch

### 5.1 Preferred app → package mapping
Maintain a small static map of known UPI apps to package names (Google Pay, PhonePe, Paytm, BHIM). Store the user's chosen package name in DataStore.

### 5.2 Installed-app check
```kotlin
fun isAppInstalled(pm: PackageManager, packageName: String): Boolean =
    try { pm.getPackageInfo(packageName, 0); true } catch (e: PackageManager.NameNotFoundException) { false }
```

### 5.3 Launch intent
```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUpiUri)).apply {
    setPackage(preferredPackageName) // omit to use system chooser fallback
}
try {
    context.startActivity(intent)
} catch (e: ActivityNotFoundException) {
    // show "app unavailable" error state
}
```
- If no preferred app is set, or the preferred app fails the installed check, fall back to `Intent.ACTION_VIEW` without `setPackage()` so Android shows the system chooser of UPI-capable apps.
- Do not modify query parameters of `rawUpiUri` before handing off — pass the scanned URI through as-is once validated.

## 6. Settings Persistence

Use Jetpack `DataStore<Preferences>` (not SharedPreferences) for:
- `camera_resolution` (enum: AUTO/480P/720P/1080P)
- `preferred_upi_package` (string)
- `scan_behavior` (enum: OPEN_IMMEDIATELY/CONFIRM_FIRST)
- `vibration_enabled` (boolean)

No other data is persisted. No scan history, no QR content, by default.

## 7. Permissions

`AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.any" android:required="true" />
```
Nothing else. Runtime permission requested via standard Compose `ActivityResultContracts.RequestPermission()` flow, with the rationale UI specified in the PRD (FR23).

## 8. Performance Budget

| Metric | Target |
|---|---|
| Cold start → camera preview visible | < 1s on mid-range hardware |
| QR detection latency | 1–3s under normal lighting |
| Frame analyzer backlog | 0 (enforced by `STRATEGY_KEEP_ONLY_LATEST`) |
| Per-frame allocations | No new Bitmap per frame; reuse `InputImage` from `ImageProxy` directly |
| Post-detection CPU | Drops to idle immediately (analyzer cleared) |
| APK size | Keep dependency set minimal — CameraX + ML Kit barcode module only, no full ML Kit bundle |

## 9. Error Handling Matrix

| Condition | Detection point | User-facing result |
|---|---|---|
| Camera permission denied | On launch / permission callback | Rationale screen + "Allow Camera" |
| No QR found (timeout heuristic) | Analyzer, no detection after N seconds | Lightweight hint, no auto camera restart |
| QR found but not UPI-shaped | `parseUpiUri()` returns null | "Not a supported UPI QR" message |
| Preferred app not installed | `isAppInstalled()` check before launch | "Selected UPI app is unavailable" + chooser action |
| `ActivityNotFoundException` on launch | `startActivity()` catch | Same as above, fallback to system chooser if enabled |

## 10. Testing Requirements

- **Device matrix:** at least one low-end (2–3GB RAM), one mid-range, one flagship device.
- **QR matrix:** printed QR, phone-screen QR, small QR, large QR, slightly tilted QR, bright light, indoor light.
- **URI matrix:** valid full-parameter UPI URI, minimal (`pa` only) URI, malformed URI, non-UPI URI (e.g. a random `https://` QR) — confirm rejection.
- **App-availability matrix:** preferred app installed / not installed / no preferred app set (system chooser path).
- **Resolution-switch test:** verify camera cleanly rebinds without crash or frozen preview when the user changes resolution mid-session.

## 11. Dependencies (indicative)

```kotlin
// build.gradle.kts (app)
implementation("androidx.camera:camera-core:<latest-stable>")
implementation("androidx.camera:camera-camera2:<latest-stable>")
implementation("androidx.camera:camera-lifecycle:<latest-stable>")
implementation("androidx.camera:camera-view:<latest-stable>")
implementation("com.google.mlkit:barcode-scanning:<latest-stable>")
implementation("androidx.datastore:datastore-preferences:<latest-stable>")
implementation("androidx.compose.ui:ui:<latest-stable>")
implementation("androidx.compose.material3:material3:<latest-stable>")
```
Pin exact versions at implementation time; keep the dependency list to exactly this set unless a Future Improvement (PRD §11) requires more.
