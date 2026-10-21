# Build and release

## Standard Android project

Use JDK 17, Android SDK Platform 35, and Build-tools 35.0.0. Android Studio can install the SDK components. Set `ANDROID_HOME` or create an untracked `local.properties` with your SDK path.

The pinned toolchain is Android Gradle Plugin 8.9.2 and Gradle 8.11.1, matching the [AGP 8.9 compatibility requirements](https://developer.android.com/build/releases/agp-8-9-0-release-notes). The wrapper checks the distribution checksum. No third-party libraries are included at app runtime; JUnit and AndroidX are test-only dependencies. The isolated showcase module contains emulator artwork fixtures and is not linked into the launcher.

```bash
git clone https://github.com/junaid-devv/quietdeck-tv-launcher.git
cd quietdeck-tv-launcher
./gradlew lintDebug testDebugUnitTest assembleDebug
```

On Windows, use `.\gradlew.bat`. The first Gradle run needs internet access to download build tools and dependencies. The installed application does not.

Debug output: `app/build/outputs/apk/debug/app-debug.apk`. Its ID is `com.still.launcher.debug`; the release ID is `com.still.launcher`. Both use the same Java namespace, with an explicit activity name in the manifest.

## SDK-only build on Windows

For a small local build without Gradle:

```powershell
.\tools\build.ps1 -JavaHome 'C:\path\to\jdk-17' -SdkRoot 'C:\path\to\Android\sdk'
```

The script runs javac, AAPT, D8, zipalign and apksigner. It creates an ignored local development key and `dist/QuietDeck-TV-local.apk`. This path uses the release application ID, so it cannot coexist with the official release unless you uninstall one. Its independent signing key cannot update the official release. Prefer Gradle debug builds when testing alongside an installed release.

Build directories, APKs and keys are excluded from Git. The SDK-only script keeps generated files under `.local/` and `dist/` inside the repository.

## Signed release

`./gradlew assembleRelease` produces an unsigned release APK. Sign it with a private Android signing key using Android's [apksigner](https://developer.android.com/tools/apksigner), after alignment. Never commit the key or passwords.

The PowerShell build can also sign a release when `QUIETDECK_KEYSTORE` and `QUIETDECK_SIGNING_PASSWORD` are supplied privately:

```powershell
.\tools\build.ps1 -JavaHome 'C:\path\to\jdk-17' -Release -KeyAlias quietdeck
```

The key path is read from `QUIETDECK_KEYSTORE`. The password is passed to apksigner through the environment rather than embedded in the script. Official release signing is performed outside public CI. CI artifacts are development builds, not official update APKs.

Before publishing:

1. Update Gradle version fields, SDK-only build version fields, About text, changelog, and download links together.
2. Run debug/release lint, unit tests, device instrumentation and optimized APK/AAB generation.
3. Install the signed APK on a physical TV and exercise Home, app launching, favourites, app options, settings access, and rollback.
4. Verify APK signatures, alignment, package ID, version, and permissions.
5. Publish the APK with `SHA256SUMS.txt` and `SIGNING_CERTIFICATE.txt` in a GitHub Release.

The signing certificate must stay stable for updates. Public checksums and certificate fingerprints help users compare releases; the private key is never included in source or release assets.

## Play AAB and release evidence

```bash
./gradlew :app:lintDebug :app:lintRelease :app:testDebugUnitTest :app:assembleRelease :app:bundleRelease
```

Unsigned AAB: `app/build/outputs/bundle/release/app-release.aab`. Release output uses R8 and resource shrinking; archive `app/build/outputs/mapping/release/mapping.txt` for each release. Use official bundletool to validate the AAB, then sign it with the **upload key** using jarsigner. APKs use apksigner and the **app-signing key**. The two identities serve different purposes; see [publishing](../play/PUBLISHING.md). Keep all key material and passwords outside the repository.

CI builds once, then installs those exact debug/test APKs on Android TV API 23, API 30 and an API 35 16 KiB-page runtime. It installs isolated original artwork fixtures and uses `tools/run-device-tests.py`; direct instrumentation preserves screenshots that Gradle's test cleanup otherwise removes. The optimized release is separately signed with an ephemeral CI-only key and smoke-tested with five cold starts. That key never signs distributed APKs.

To collect a comparable diagnostic report on an available test device:

```bash
python tools/benchmark-device.py --serial YOUR_DEVICE_SERIAL --package com.still.launcher --samples 5 --output benchmark
```

This force-stops the selected app, starts it and records TotalTime, memory, graphics and runtime logs. It never discovers or connects to a device automatically. Capture steady-state and stress results on real hardware separately; emulator numbers are not user-device performance claims.

To regenerate branding, install Pillow in your own developer environment and run `python tools/generate-branding.py /path/to/font.ttf`. It derives store exports from the committed original logo concept and regenerates the lightweight vector companion. Store banners and runtime banners have different required sizes.
