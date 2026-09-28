# Avito Assistant v0.1

MVP Android app that works locally with Android notification access.

## What it does
- Watches notifications that look like they came from Avito.
- Keeps a local history on the phone.
- Filters by keywords and optional maximum price.
- Highlights new messages automatically.
- Sends a high-priority Avito Assistant notification for matching items/messages.
- Reuses the original notification tap action when available, so tapping can open Avito.

## Setup on phone
1. Install the APK.
2. Open Avito Assistant.
3. Tap "Включить доступ к уведомлениям".
4. Enable Avito Assistant in Android notification access settings.
5. In Avito itself, save searches and enable Avito notifications.
6. In Avito Assistant, set keywords and a maximum price.

No Avito password is stored by this app.

## Build
This repository includes `.github/workflows/build-apk.yml`.
Push it to GitHub and the workflow will produce `AvitoAssistant_v0.1.apk`.
