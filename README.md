# App Risk Auditor

An on-device Android app that scans the apps installed on your phone, groups what each one can access into nine categories, and gives each app a 0 to 100 risk score with plain-language reasons. It helps a non-technical user answer three questions:

1. Which of my apps can do the most harm?
2. Why is each one flagged?
3. What can I do about it right now?

Everything runs on the device and works offline. No root, no ADB, and no data leaves the phone.

## Features
- **App inventory:** name, icon, package, version, install and update dates, installer source (Play Store, other store, system, or sideloaded) and target SDK. A toggle includes system apps.
- **Permissions:** declared versus granted for each app, with protection level (normal, dangerous, signature, special).
- **Nine categories:** special access, SMS, contacts and call logs, camera and microphone, location, phone and identity, storage and media, background and persistence, network. Shown per app and as a device-wide summary.
- **Special access detection:** enabled accessibility services, active device admins, enabled notification listeners, and declared or granted overlay, install-packages and usage-access.
- **Explainable risk score:** four bands (Low, Medium, High, Critical), reasons sorted by points. See `RUBRIC.md` for every weight and rule.
- **Combination rules and context mismatch**, for example accessibility plus overlay, or a flashlight app that can read texts.
- **Detail screen:** "Why flagged" panel, permissions by category, and buttons for app settings, uninstall, and the relevant special-access settings.
- **Change tracking:** a baseline scan, then NEW and UPDATED badges, "Changes since last scan" for added sensitive permissions, and notifications for newly risky apps.
- **Search, filters and sorting** on the home screen.

## Privacy
All scanning is local. The app declares `QUERY_ALL_PACKAGES` because it must see every installed app. It has no analytics, no network calls and no accounts.

## Build and run
Requirements: Android Studio (current stable), JDK 17 or later bundled with it, and a device or emulator on Android 8.0 (API 26) or newer.

1. Clone the repository and open it in Android Studio.
2. Let Gradle sync finish.
3. Select a device and press Run, or build an APK with `./gradlew assembleDebug`.
4. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests with `./gradlew testDebugUnitTest`.

## Project structure
- `data/scanner`: `AppScanner` (package manager queries) and `SpecialAccessDetector`
- `domain`: `RiskEngine`, `RiskWeights`, `ContextMismatch`, `PermissionCatalog`, `DeviceSummary`, `PermissionDiff`
- `ui`: home and detail screens
- `app/src/test`: unit tests for the scoring engine, device summary and permission diff

## Documents
- `RUBRIC.md`: one-page scoring rubric
- `DEMO.md`: three-minute demo script

## Notes
- No real malware samples are used or distributed. The scoring was tested with unit-test profiles and a harmless fixture app that only declares permissions.
- Hash lookup, manifest checks, an ML model and PDF export are not included.
