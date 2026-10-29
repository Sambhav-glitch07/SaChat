# SaChat — Android messaging starter

SaChat is an original-branded Android starter for account-based one-to-one messaging. It uses Kotlin, Firebase Authentication (email/password), Cloud Firestore and GitHub Actions to build a debug APK.

## UI direction
The Android UI now uses an original dark glassmorphism treatment: translucent layered panels, thin reflective borders, rounded cards, mint highlights, and a familiar chat list/direct-message structure. It is inspired by common chat-app patterns, not a copy of Discord branding or assets.

## Included
- Create account and sign in with email/password
- Cloud-synced user directory
- One-to-one text messages with live Firestore updates
- Message history, simple chat list and search
- Firestore security rules starter
- GitHub Actions APK build workflow

## Not included yet
This is a working starter, not a 1:1 copy of WhatsApp and not production-ready. It does not yet include phone-number/OTP login, end-to-end encryption, media uploads, group chats, voice/video calls, push notifications, message read receipts, contact sync, or account recovery UI. Do not use it for sensitive conversations until you have added and audited end-to-end encryption and hardened the backend.

## 1. Create a Firebase project
1. Open https://console.firebase.google.com/ in your browser and create a project named `SaChat`.
2. Add an **Android app** with package name exactly `in.sachat.app`.
3. Download `google-services.json`.
4. Put that file in this project at `app/google-services.json` (same level as `app/build.gradle.kts`). Do not commit service-account credentials. The client Firebase config file is expected for Android, but keep server/admin credentials private.
5. In Firebase Console → Authentication → Sign-in method, enable **Email/Password**.
6. In Firestore Database, create a database. Start in production mode.
7. Open Firestore Rules and paste the contents of `firestore.rules`, then publish.

## 2. Build from your Android phone with GitHub Actions
1. Create a new GitHub repository, e.g. `SaChat`.
2. Upload all project files and folders from this ZIP to the repository root. Keep `.github/workflows/build-apk.yml`.
3. Upload `google-services.json` into `app/`.
4. Commit to the `main` branch.
5. Open the repository's **Actions** tab. Select **Build SaChat APK** and tap **Run workflow** (or let the push trigger it).
6. When the workflow completes successfully, open the run, scroll to **Artifacts**, and download `SaChat-debug-apk`.
7. Extract the ZIP artifact and install `app-debug.apk`. Android may ask you to allow installs from your browser/file manager.

## 3. Test real messaging
- Install the same build on two phones, or use two Android devices/emulators.
- Create a different SaChat account on each device.
- Each account should appear in the People list. Tap a person to open the chat and send a message.
- Both devices need internet access and must use the same Firebase project.

## Security notes
- Never put Firebase Admin SDK credentials, private keys, or any server secret into the Android app or GitHub repository.
- Firestore rules are essential: without them, user data may be exposed. The included rules are a baseline and need testing before production.
- The starter currently lets authenticated users read the public user directory (name/email). Consider reducing exposed profile data and using a Cloud Function or a more controlled discovery model.
- Email/password authentication does not automatically verify email ownership. Enable email verification and enforce it before production use.
- Firestore messages are not end-to-end encrypted by this starter. Firebase encryption at rest/in transit is not the same as end-to-end encryption.
- Consider App Check, abuse/rate limits, account deletion, reporting/blocking, privacy policy, and Play Store policy compliance before release.

## Build locally
Requires JDK 17 and Gradle 8.9:
```bash
gradle assembleDebug
```
The APK output is `app/build/outputs/apk/debug/app-debug.apk`.

## Troubleshooting
- **Missing google-services.json**: make sure it is at `app/google-services.json`, not the repository root.
- **No people appear**: create another account on the same Firebase project.
- **Firestore permission denied**: publish `firestore.rules` and confirm the signed-in user's profile exists.
- **Build fails at Google Services plugin**: ensure the Android package in Firebase is exactly `in.sachat.app` and the downloaded JSON belongs to that app.
