# Contributing to PayDart

First off, thank you for considering contributing to PayDart! Every contribution—from bug reports to architectural enhancements—helps make PayDart faster and more reliable.

---

## 🛠️ Development Setup

1. **Prerequisites:**
   - Android Studio Ladybug (2024.2+) or later
   - JDK 17
   - Android SDK with platform `android-35`

2. **Fork & Clone:**
   ```bash
   git clone https://github.com/AJAYMYTH/PayDart.git
   cd PayDart
   ```

3. **Verify the Build:**
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```

---

## 📜 Development Guidelines

- **Speed Over Everything:** PayDart's primary value proposition is instant QR detection and zero-lag redirection. Any pull request that adds heavy dependencies, blocking IPC calls on the scan path, or perceptible latency will not be accepted.
- **Privacy & Offline First:** PayDart must remain 100% offline. Do not add `android.permission.INTERNET` or third-party analytics libraries.
- **Kotlin & Compose Idioms:** Follow official Android architecture guidelines. Use unidirectional data flow, Kotlin Flow, and Compose Material 3 components.
- **Maintain Test Coverage:** Ensure existing unit tests in `UPIParserTest` and `SettingsModelsTest` pass, and write new unit tests for any new business logic.

---

## 🔀 Submitting Pull Requests

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feature/awesome-speedup
   ```
2. Commit your changes following [Conventional Commits](https://www.conventionalcommits.org/):
   ```bash
   git commit -m "perf: optimize camera pipeline resolution negotiation"
   ```
3. Push to your branch and open a Pull Request against `main`.
4. Ensure CI checks pass.
