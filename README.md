This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

# Supported Platforms

Must have:

These platforms are the primary focus and must remain fully supported.
- Web (WasmJS)


Nice to have:

These platforms are currently working, but they are secondary because the web app is available to everyone. If support breaks or becomes too expensive to maintain, it is acceptable to drop the platform. When that happens, update this file to clearly mark the platform as unsupported or broken.
- Desktop (JVM)
- Android

# Business Logic Decisions

e.g ledgers, editing events after they happened, etc

TBD
