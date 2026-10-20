# Build and release

## Standard Android project

Use JDK 17, Android SDK Platform 35, and Build-tools 35.0.0. Android Studio can install the SDK components. Set `ANDROID_HOME` or create an untracked `local.properties` with your SDK path.

The pinned toolchain is Android Gradle Plugin 8.9.2 and Gradle 8.11.1, matching the [AGP 8.9 compatibility requirements](https://developer.android.com/build/releases/agp-8-9-0-release-notes). The wrapper checks the distribution checksum. No third-party libraries are included at app runtime; JUnit is a test-only dependency.

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
2. Run lint, grid-navigation tests, and APK generation.
3. Install the signed APK on a physical TV and exercise Home, app launching, favourites, app options, settings access, and rollback.
4. Verify APK signatures, alignment, package ID, version, and permissions.
5. Publish the APK with `SHA256SUMS.txt` and `SIGNING_CERTIFICATE.txt` in a GitHub Release.

The signing certificate must stay stable for updates. Public checksums and certificate fingerprints help users compare releases; the private key is never included in source or release assets.
