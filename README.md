# NOTICE BOARD

Independent Android application for institution-based notices and campus communication.

## Current application structure

The current source includes:
- Google sign-in integration through Android Credential Manager and Firebase Authentication.
- Institution onboarding and saved institution selection.
- Campus dashboard and repository/view-model layers.
- Firestore-backed campus data.
- User preferences and appearance settings.
- Jetpack Compose UI.

## Android build

The project uses the Gradle Kotlin DSL, a version catalog at `gradle/libs.versions.toml`, Kotlin Compose, and Android Gradle Plugin configuration in the app module.

A GitHub Actions workflow builds the debug APK on pushes to `main` and on manual dispatch. The APK is uploaded as a workflow artifact named `NOTICE-BOARD-debug-apk`.

Expected output: `app/build/outputs/apk/debug/app-debug.apk`

## Configuration and security

No Firebase project configuration file or production credentials are committed in this repository. The build pipeline must remain able to compile without private credentials. Authentication and Firestore runtime connectivity require valid institution/backend configuration and must be tested after that configuration is supplied.

Never commit service-account keys, signing keystores, OAuth client secrets, or private institution data.

## Project boundary

This repository is the independent NOTICE BOARD project. Do not import or merge source code, credentials, databases, or assets from BLDEACET, GRAPE APP, or earlier notice-board projects.
