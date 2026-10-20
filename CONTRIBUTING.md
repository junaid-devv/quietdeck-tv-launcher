# Contributing

Compatibility reports, focused bug fixes, accessibility improvements and documentation corrections are welcome.

1. Read the [architecture](docs/ARCHITECTURE.md) and [roadmap](docs/ROADMAP.md).
2. Open an issue for larger behavior changes so the scope can be discussed.
3. Use JDK 17 and run `./gradlew lintDebug testDebugUnitTest assembleDebug`.
4. Test UI changes with a D-pad and provide the device/API level. A successful build alone is not proof of TV compatibility.
5. Submit a pull request describing the problem, resulting behavior and validation.

Keep the launcher useful offline. Avoid accounts, advertising, analytics or new runtime dependencies without discussing a specific need. Keep signing files, local SDK paths, private IP addresses and personal screenshots out of commits. Existing public APIs start at Android 6.0/API 23; guard newer calls appropriately.

Report broader device results through the compatibility template rather than marking untested devices as supported. See [SECURITY.md](SECURITY.md) for vulnerability reporting.

By contributing, you agree that your contribution is provided under this repository's MIT license. Be respectful and constructive in issues and reviews.
