# Security & Privacy Policy

PayDart is built with a security-first, offline-first philosophy. We treat user privacy and device safety with paramount importance.

---

## 🛡️ Core Security Architecture & Guarantees

1. **Zero Payment Handling:**
   PayDart **never** asks for, handles, processes, or stores:
   - Bank account numbers, IFSC codes, or debit/credit card credentials
   - UPI PINs, MPINs, or ATM PINs
   - One-Time Passwords (OTPs) or two-factor authentication tokens
   - Payment credentials or account balances

2. **Strict Protocol Validation (`UPIParser`):**
   PayDart parses raw QR strings against a strict validation engine:
   - Validates that the scheme is strictly `upi://pay`
   - Validates that the payee VPA (`pa`) strictly matches a legitimate handle pattern (`^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z0-9.\-_]{2,64}$`)
   - Validates that optional amounts are well-formed positive decimals
   - Rejects arbitrary web URLs (`http://`, `https://`), shell commands, SMS triggers, or phishing payloads. **PayDart will never launch an arbitrary intent.**

3. **100% Offline & Zero Network Permissions:**
   PayDart declares **no `android.permission.INTERNET`** permission in its manifest. The app is incapable of making HTTP/HTTPS connections, transmitting analytics, or exfiltrating scanned QR data.

4. **Zero-Persistence Policy:**
   Camera preview frames and scanned QR strings reside only transiently in RAM during the active scan session and are dropped immediately upon completion or app dismissal. No camera frames or QR payloads are ever written to disk or device databases.

---

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Reporting a Vulnerability

If you discover a security vulnerability within PayDart, please do **NOT** open a public issue. Instead, follow responsible disclosure:

1. Send an email to **ajaymyth.dev@gmail.com** with the subject `[SECURITY VULNERABILITY] PayDart`.
2. Please provide:
   - Description of the vulnerability and its potential impact
   - Step-by-step reproduction steps or proof-of-concept
   - The device model and Android OS version tested
3. You will receive an initial response within **48 hours** confirming receipt of the report.
4. We will coordinate remediation and a release before any public disclosure.
