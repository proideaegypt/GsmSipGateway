# GSM SIP Gateway

Android app that auto-answers incoming GSM/SIM calls and bridges them to FreePBX via SIP.

## Build via GitHub Actions
1. Go to **Actions** tab
2. Click **Build APK** → **Run workflow**
3. Download APK from **Artifacts**

## Build Locally
```bash
npm install --legacy-peer-deps
cd android && ./gradlew assembleRelease
```
APK: `android/app/build/outputs/apk/release/app-release.apk`

## FreePBX Setup
- Create PJSIP Extension: `android_gsm1`
- Inbound Route DID = SIM phone number
- App config: FreePBX IP, port 5060, username, password, bridge extension
