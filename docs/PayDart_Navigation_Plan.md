# PayDart — Navigation Plan

**Project:** PayDart — Lightweight UPI QR Launcher
**Doc version:** v1.0

---

## 1. Screen Inventory

| Screen | Purpose | Entry points |
|---|---|---|
| `ScannerScreen` | Default/home screen — camera preview + QR scanning | App launch (always the start destination) |
| `PermissionRationaleScreen` (state, not a route) | Explains why camera access is needed | Shown in-place on `ScannerScreen` when permission is denied |
| `ConfirmPaymentSheet` | Optional preview of parsed UPI details before launch | Modal over `ScannerScreen`, only if "Confirm before opening" is enabled |
| `SettingsScreen` | Camera resolution, preferred UPI app, scan behavior, vibration | Gear icon on `ScannerScreen` |
| `AppPickerScreen` (or sheet) | Choose preferred UPI app from installed apps | From `SettingsScreen` → "Preferred UPI App" |
| `ErrorState` (state, not a route) | Invalid QR / app unavailable / no QR detected hints | Inline overlay on `ScannerScreen` |

There is intentionally **no bottom nav, no drawer, no multi-tab structure** — this matches the "no unnecessary navigation" principle in the source spec (Section 10).

## 2. Navigation Graph

```
NavHost(startDestination = "scanner")
│
├── "scanner"                     (ScannerScreen)
│     ├── permission denied → inline rationale UI (same route, different state)
│     ├── QR detected + confirm-mode → present "confirm_sheet" as modal
│     ├── QR detected + immediate-mode → launch external UPI Intent directly (no route change)
│     └── gear icon → navigate("settings")
│
├── "confirm_sheet"               (ConfirmPaymentSheet, modal bottom sheet)
│     ├── Confirm → launch external UPI Intent, dismiss sheet, return to "scanner"
│     └── Cancel → dismiss sheet, resume scanning on "scanner"
│
├── "settings"                    (SettingsScreen)
│     ├── back → "scanner"
│     └── "Preferred UPI App" row → navigate("app_picker")
│
└── "app_picker"                  (AppPickerScreen)
      ├── select app → save to DataStore, back → "settings"
      └── back (no selection) → "settings"
```

## 3. Compose Navigation Sketch

```kotlin
NavHost(navController = navController, startDestination = "scanner") {
    composable("scanner") {
        ScannerScreen(
            onOpenSettings = { navController.navigate("settings") },
            onNeedsConfirmation = { navController.navigate("confirm_sheet") }
        )
    }
    dialog("confirm_sheet") { // or bottomSheet via accompanist/material3 sheet
        ConfirmPaymentSheet(
            onConfirm = { launchUpiIntent(); navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }
    composable("settings") {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onPickApp = { navController.navigate("app_picker") }
        )
    }
    composable("app_picker") {
        AppPickerScreen(
            onAppSelected = { pkg -> saveSelection(pkg); navController.popBackStack() },
            onBack = { navController.popBackStack() }
        )
    }
}
```

## 4. State-Driven UI (not routes)

To keep the graph flat, these are handled as **UI state within `ScannerScreen`**, not separate destinations:
- Camera permission rationale / denied state
- "QR not detected — move closer" hint
- "Not a supported UPI QR" error
- "Selected UPI app unavailable" error + inline "Choose another app" action (deep-links to `app_picker` when tapped)

This avoids extra back-stack entries for what are really just transient overlays on the scanner.

## 5. Deep Links / External Entry

- No external deep links into the app are required for MVP (PayDart is always opened directly by the user, not launched via another app's intent).
- Outbound only: PayDart launches the selected UPI app via `Intent.ACTION_VIEW` with the scanned `upi://` URI (see TRD §5).

## 6. Back-Stack Behavior

- `scanner` is the single root; `settings` and `app_picker` push onto the stack normally with standard back behavior.
- `confirm_sheet` is a modal — back/cancel dismisses it and returns focus to the live camera preview without re-navigating.
- After a successful UPI app launch, PayDart does **not** navigate anywhere — it simply resumes the idle scanner state in case the user returns to it (e.g., via Recents), per the "stop, don't wait" principle.
