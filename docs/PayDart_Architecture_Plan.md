# PayDart — Architecture Plan

**Project:** PayDart — Lightweight UPI QR Launcher
**Doc version:** v1.0

---

## 1. Architectural Style

Simple, single-module **MVVM-lite** app — no multi-module split needed at this size (keep the "keep the number of libraries/moving parts small" principle from the source spec). One `app` module, layered internally by responsibility.

```
UI (Compose)  →  ViewModel  →  Domain/Use-cases  →  Camera / Scanner / UPI / Settings layers
```

No repository/network layer is needed since the app is offline-first with no backend.

## 2. Module / Package Structure

```
com.example.lightupi (paydart)
│
├── MainActivity.kt                 — hosts NavHost, requests permission
│
├── scanner/
│   ├── QRScanner.kt                — wraps ML Kit BarcodeScanner, exposes Flow<Barcode?>
│   └── UPIParser.kt                — parses/validates raw QR string → UpiPaymentRequest
│
├── camera/
│   └── CameraManager.kt            — binds CameraX use cases (Preview + ImageAnalysis),
│                                      handles resolution rebind, lifecycle-aware
│
├── upi/
│   ├── UpiPaymentRequest.kt        — data class (pa, pn, am, cu, tn, tr)
│   ├── UpiAppRegistry.kt           — known UPI app name → package name map
│   └── UPIIntentLauncher.kt        — installed-check + Intent construction/launch
│
├── settings/
│   ├── SettingsManager.kt          — DataStore read/write for resolution, preferred app,
│   │                                  scan behavior, vibration
│   └── SettingsModels.kt           — enums: CameraResolution, ScanBehavior
│
├── ui/
│   ├── ScannerScreen.kt            — camera preview + overlay states (permission/error/hint)
│   ├── ConfirmPaymentSheet.kt      — optional confirm-before-launch modal
│   ├── SettingsScreen.kt
│   └── AppPickerScreen.kt
│
├── viewmodel/
│   └── ScannerViewModel.kt         — orchestrates CameraManager + QRScanner + UPIParser +
│                                      UPIIntentLauncher + SettingsManager; exposes UI state
│
└── AndroidManifest.xml
```

This mirrors the source spec's suggested structure (Section 15) with a `viewmodel/` layer added for clean state management under Compose.

## 3. Component Responsibilities

| Component | Responsibility | Does NOT do |
|---|---|---|
| `CameraManager` | Bind/rebind `Preview` + `ImageAnalysis` use cases to lifecycle; apply selected resolution; expose latest `ImageProxy` frames | Parse QR content; know about UPI at all |
| `QRScanner` | Run ML Kit on incoming frames, emit detected QR string once, then stop | Validate UPI-specific structure |
| `UPIParser` | Validate scheme/host, extract `pa/pn/am/cu/tn/tr` | Touch the camera, touch Intents |
| `SettingsManager` | Persist/read user preferences via DataStore | Any camera or UPI logic |
| `UpiAppRegistry` | Map friendly names ↔ package names | Launching intents |
| `UPIIntentLauncher` | Check install status, build and fire `Intent.ACTION_VIEW`, handle `ActivityNotFoundException` | Parsing raw QR strings |
| `ScannerViewModel` | Coordinate the above; hold `StateFlow<ScannerUiState>` for Compose to render | Any direct CameraX/ML Kit API calls (delegated to `CameraManager`/`QRScanner`) |
| `ScannerScreen` (Compose) | Render preview + state (idle/scanning/error/permission) | Business logic — reads state, dispatches events only |

## 4. Data Flow (Runtime)

```
CameraX Frame (ImageProxy, latest-only)
        ↓
CameraManager → QRScanner.analyze(frame)
        ↓ (Flow emission on detection)
ScannerViewModel.onQrDetected(rawString)
        ↓
UPIParser.parseUpiUri(rawString)
        ↓                              ↘ (null → invalid)
   UpiPaymentRequest                 ScannerUiState.Error("Not a supported UPI QR")
        ↓
CameraManager.stopAnalysis()   // immediate, before any further work
        ↓
SettingsManager.scanBehavior?
   ├── OPEN_IMMEDIATELY → UPIIntentLauncher.launch(request)
   └── CONFIRM_FIRST    → ScannerUiState.ConfirmPending(request) → user taps Confirm →
                            UPIIntentLauncher.launch(request)
        ↓
Intent.ACTION_VIEW(upi://...) → target UPI app
        ↓
PayDart returns to idle ScannerUiState (ready to scan again if reopened)
```

## 5. State Model

```kotlin
sealed interface ScannerUiState {
    data object PermissionRequired : ScannerUiState
    data object Scanning : ScannerUiState              // live preview, actively analyzing
    data object NoQrHint : ScannerUiState               // no detection after N seconds
    data class ConfirmPending(val req: UpiPaymentRequest) : ScannerUiState
    data class Error(val message: String, val recoverable: Boolean) : ScannerUiState
    data object Idle : ScannerUiState                    // post-launch resting state
}
```

`ScannerViewModel` exposes this as a single `StateFlow<ScannerUiState>`; `ScannerScreen` is a pure function of this state (plus the live `PreviewView` surface from `CameraManager`, which is bound imperatively via `AndroidView` in Compose since CameraX's preview surface isn't a Compose-native construct).

## 6. Lifecycle & Resource Management

- `CameraManager` binds to the Activity/Fragment `LifecycleOwner` via CameraX's `ProcessCameraProvider.bindToLifecycle(...)` — camera automatically releases on stop/destroy, no manual teardown needed in most cases.
- `ImageAnalysis.clearAnalyzer()` is called the instant a valid QR is confirmed, per TRD §3.3 — this is the primary CPU-saving mechanism and must not be delayed by any async work (parsing/validation happens after analysis is already stopped).
- On resolution change (Settings), the ViewModel triggers `CameraManager.rebind(newResolution)`, which unbinds and rebinds only the affected use cases — the `LifecycleOwner` binding itself isn't recreated.

## 7. Error & Edge-Case Handling (Architectural)

| Case | Where handled |
|---|---|
| Permission denial | `MainActivity` permission callback → `ScannerViewModel.onPermissionResult()` → `PermissionRequired` state |
| Invalid/non-UPI QR | `UPIParser` returns null → ViewModel emits `Error`, resumes scanning after dismiss (analysis is NOT stopped for invalid QRs — only for valid ones, so scanning continues live) |
| Preferred app missing | `UPIIntentLauncher.isAppInstalled()` check before `startActivity` → `Error(recoverable = true)` with action routing to `AppPickerScreen` |
| `ActivityNotFoundException` | Caught in `UPIIntentLauncher`, surfaced as the same recoverable error |
| Unsupported resolution requested | `CameraManager` filters `SettingsScreen` options against `CameraInfo` capabilities before they're ever selectable |

## 8. Why This Shape

- **No repository/network layer:** there's no backend, no auth, no remote data — adding one would violate the "keep dependencies small" principle for no benefit.
- **Single module:** app is small enough that multi-module (`:core`, `:feature-scanner`, etc.) would add build complexity without a real payoff at this scope; revisit only if the app grows substantially post-MVP.
- **Flow/StateFlow over LiveData:** idiomatic for Compose + Kotlin-first stack, avoids extra lifecycle-observer boilerplate.
- **DataStore over SharedPreferences:** async-safe, avoids main-thread disk I/O for the few settings this app has.
