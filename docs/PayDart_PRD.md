# PayDart — Product Requirements Document (PRD)

**Project:** PayDart — Lightweight UPI QR Launcher
**Platform:** Android (native, Kotlin)
**Doc version:** v1.0
**Status:** Draft

---

## 1. Summary

PayDart is a minimal Android app that scans a UPI QR code and hands the payment URI off to the user's preferred UPI app (Google Pay, PhonePe, Paytm, BHIM, etc.). PayDart never touches money — it only reads, validates, and launches. Its entire reason to exist is speed: open app → point at QR → UPI app opens, with as little CPU/RAM/battery overhead as possible, so it stays smooth even on low- and mid-range devices where the stock camera or bigger scanner apps lag.

## 2. Problem Statement

Default camera apps and full-featured scanner apps are heavy — they do more processing than a UPI scan needs (multi-format barcode detection, high-res capture, filters, cloud lookups), which causes visible lag on budget devices. Users just want the fastest possible path from "see a QR" to "pay."

## 3. Goals

| # | Goal |
|---|------|
| G1 | Detect a UPI QR in 1–3 seconds under normal lighting |
| G2 | Keep CPU/RAM/battery footprint minimal at every stage |
| G3 | Launch the user's preselected UPI app automatically |
| G4 | Work fully offline until the UPI app is invoked |
| G5 | Remain smooth on low/mid-range Android hardware |
| G6 | Zero payment handling, zero stored payment data |

## 4. Non-Goals

- Processing or completing payments
- Storing credentials, PINs, or scan history (by default)
- Cloud OCR/AI, video recording, wallet features, user accounts
- Multi-format barcode scanning (EAN, UPC, Code128, etc.)
- Ads on the scanning screen
- Continuous frame processing after a successful scan

## 5. Target Users / Personas

**Primary — "Quick Scanner" (Priya, 24, budget Android phone):** Pays via UPI multiple times a day (shop counters, friends). Annoyed by GPay/PhonePe's slow camera cold-start and bloated home screens when all she wants is to scan and pay.

**Secondary — "Low-End Device User" (Ramesh, 45, entry-level phone):** Default camera app or scanner apps visibly lag/heat up his phone. Needs something that "just works" without stutter.

**Tertiary — "Privacy-Conscious User":** Doesn't want a scanner app uploading frames or storing QR/payment data anywhere.

## 6. Core User Flow

```
Open App → Camera Preview → Detect QR → Validate UPI URI →
Stop Camera Analysis → Launch Selected UPI App → User pays in that app
```

## 7. Functional Requirements

### 7.1 Scanning
- FR1: App opens directly to camera preview (no splash/onboarding friction).
- FR2: Detect only `Barcode.FORMAT_QR_CODE` — no other barcode formats.
- FR3: Use latest-frame-only analysis (`STRATEGY_KEEP_ONLY_LATEST`); never queue stale frames.
- FR4: Stop image analysis immediately upon a successful, valid UPI QR detection.
- FR5: Default camera resolution: 1280×720.
- FR6: User can change resolution (Auto / 480p / 720p / 1080p) in Settings; supported resolutions must be checked against actual device capabilities before offering them.
- FR7: Resolution changes stop → reconfigure → restart the camera pipeline; never auto-change resolution per failed frame.

### 7.2 UPI URI Handling
- FR8: Parse scanned content against the `upi://pay?...` scheme.
- FR9: Extract parameters: `pa` (payee VPA), `pn` (payee name), `am` (amount), `cu` (currency), `tn` (note), `tr` (transaction ref).
- FR10: Reject and show an error for any URI that isn't a valid UPI payment URI — never launch arbitrary/unsupported URIs.
- FR11: Optional lightweight confirmation preview before launch (toggleable in Settings: "Open immediately" vs "Confirm before opening").

### 7.3 UPI App Selection & Launch
- FR12: Settings screen to choose a preferred UPI app from installed apps (Google Pay, PhonePe, Paytm, BHIM, Other).
- FR13: Verify the selected app is installed before attempting launch; if not, show an error with a "Choose another app" action.
- FR14: Optional fallback to Android's system chooser for compatible UPI intent handlers.
- FR15: Pass the parsed UPI intent unmodified to the target app.

### 7.4 Settings
- FR16: Camera Resolution (Auto/480p/720p/1080p).
- FR17: Preferred UPI App picker.
- FR18: Scan behavior (open immediately vs confirm before opening).
- FR19: Vibration on detection toggle (default off or on — TBD by design, optional).

### 7.5 Permissions & Privacy
- FR20: Request only `android.permission.CAMERA`.
- FR21: No frame, image, or QR content is ever uploaded or persisted beyond the current scan session.
- FR22: No credentials, PINs, or banking info requested at any point.

### 7.6 Error Handling
- FR23: Camera permission denied → explanation + "Allow Camera" action.
- FR24: No QR detected after a reasonable interval → lightweight hint ("Move closer or improve lighting"), no auto-resolution changes or camera restarts.
- FR25: Invalid/non-UPI QR → "This QR code is not a supported UPI QR."
- FR26: Selected UPI app unavailable → error + "Choose another UPI app."

## 8. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | QR detected in 1–3s under normal lighting; no visible frame queue buildup |
| Resource usage | Minimal CPU/RAM; no bitmap copies per frame; no persistent frame buffers |
| Reliability | Works with printed QR, phone-screen QR, small/large QR, tilted QR, varied lighting |
| Offline | Fully functional (scan + validate + launch) without network access |
| Compatibility | Functions on low/mid-range Android devices (API level TBD in TRD) |
| Privacy | No analytics/telemetry on QR content or payment data by default |
| App size | Minimal dependency footprint |

## 9. Success Metrics

- **Detection speed:** ≥ 90% of scans detected within 3 seconds under normal indoor lighting.
- **Performance:** No dropped/janky frames reported during preview on mid-range test devices (e.g., Snapdragon 400-series class).
- **Reliability:** Successful detection across printed, screen-displayed, small, large, and slightly tilted QR codes in the test matrix (Section 13 of the source spec).
- **Launch success:** 100% of valid UPI URIs correctly hand off to an installed, selected UPI app that supports the UPI intent.
- **Footprint:** App size and idle memory usage stay within a lightweight budget (concrete numbers to be set once a baseline build exists).

## 10. MVP Scope (v1.0)

Camera permission · Camera preview · QR detection · UPI URI validation · UPI parameter parsing · Preferred UPI app selection · UPI intent launching · Camera resolution setting · Basic error messages · Lightweight UI.

Everything else (torch, zoom, scan history, multi-app auto-detect, accessibility polish) is post-MVP.

## 11. Future Improvements (Post-MVP, only if proven useful)

- Automatic device-specific resolution recommendation
- Torch button, zoom control
- Local-only scan history
- Multiple installed UPI app auto-detection
- Faster QR detection tuning
- Accessibility improvements

## 12. Open Questions

- Default state for the vibration-on-detection toggle?
- Minimum supported Android API level (affects CameraX/ML Kit compatibility — see TRD)?
- Should "confirm before opening" be the default scan behavior, or "open immediately"?
