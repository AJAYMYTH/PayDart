# ⚡ PayDart v1.0.0 Release Notes

> **Ultra-Lightweight, Lightning-Fast Native UPI QR Launcher for Android (<10ms).**
> 100% Offline • Zero Telemetry • Zero Ads • Under 5 MB

---

## 🚀 Overview

**PayDart v1.0.0** is the inaugural production release of the fastest UPI QR launcher on Android. Built for users tired of waiting 5–8 seconds for bloated payment apps to load splash screens, mutual fund advertisements, and loan popups before scanning a counter QR code.

With PayDart, you point, scan in **under 10 milliseconds**, and snap directly into your preferred payment app to complete your transaction with your secure UPI PIN.

---

## ✨ Key Highlights

### ⚡ Sub-10ms Camera & Scanning Engine
- **Pre-warmed Native ML Detector:** Asynchronously primes Google ML Kit's native C++ `libbarhopper_v3.so` engine on startup with a synthetic 1x1 bitmap, reducing first-scan latency from 450ms to 8ms.
- **Atomic Single-Frame Capture:** Discards duplicate video frames atomically (`STRATEGY_KEEP_ONLY_LATEST`), preventing camera thread thrashing and memory pressure.
- **Hardware Agnostic:** Optimized fallback resolution targeting (`FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER`) for low-end and flagship camera sensors alike.

### 🎯 Zero-Latency App Handoff
- **Bypasses Android Chooser Delays:** Avoids the system `createChooser` bottom sheet which typically adds 400–800ms of UI delay.
- **Instant Package Resolution:** Queries installed UPI apps using strict `<queries>` visibility declarations, automatically targeting single installed apps (e.g. PhonePe, GPay, Paytm) with zero user configuration.
- **Zero-Duration Window Transition:** Uses `overridePendingTransition(0, 0)` for an instantaneous leap into the target payment app.

### 🛡️ 100% Air-Gapped Privacy & Security
- **No Internet Permission:** Does not request `android.permission.INTERNET` in `AndroidManifest.xml`. Physical Linux kernel sandbox prevents transmitting data packets anywhere.
- **Volatile In-Memory Analysis:** Video frames are analyzed in temporary memory buffers and recycled immediately; zero images or payee records are stored on disk.
- **UPI PIN Safety:** PayDart never touches your bank credentials, PIN, or money. The financial transaction is authenticated and executed strictly inside your certified banking application.

### 🖼️ Android Photo Picker (Screenshot Scanning)
- Scan UPI QR codes from payment screenshots, WhatsApp, or gallery images.
- Uses Android's modern Photo Picker API, requiring **zero file storage permissions** (`READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES` are never requested).

### 🎨 Electric Cyber Viewfinder HUD
- Precision Cyber HUD reticle in Electric Cyan (`#00F0FF`) with corner crosshairs.
- Animated neon laser scan line with high-contrast targeting feedback.
- Floating bottom dock displaying detected payee name, VPA, and pre-filled amount.
- Subtle haptic feedback confirmation on successful lock.

---

## 📲 Verified Supported UPI Apps

- **Google Pay (Tez):** `com.google.android.apps.nbu.paisa.user`
- **PhonePe:** `com.phonepe.app`
- **Paytm:** `net.one97.paytm`
- **BHIM UPI:** `in.org.npci.upiapp`
- **CRED:** `com.dreamplug.androidapp`
- **Navi UPI:** `com.naviapp`
- **Amazon Pay:** `in.amazon.mShop.android.shopping`
- **Slice UPI:** `indwin.c3.shareme`
- *Any NPCI-compliant payment app registered for `upi://pay`*

---

## 📦 Download & Verification

### Direct Sideload:
Download `PayDart-v1.0.0-debug.apk` or `app-debug.apk` from [GitHub Releases](https://github.com/AJAYMYTH/PayDart/releases).

### Developer Quick Install (via ADB):
```bash
adb install -r PayDart-v1.0.0-debug.apk
```

### Cryptographic Verification:
```bash
# Linux / macOS
sha256sum PayDart-v1.0.0-debug.apk

# Windows PowerShell
Get-FileHash PayDart-v1.0.0-debug.apk -Algorithm SHA256
```

---

## 🌐 Project Links
- **Repository:** [https://github.com/AJAYMYTH/PayDart](https://github.com/AJAYMYTH/PayDart)
- **Official Website:** [https://ajaymyth.github.io/PayDart/](https://ajaymyth.github.io/PayDart/)
- **License:** Apache License 2.0
