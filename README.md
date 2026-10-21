<p align="center"><img src="docs/branding/quietdeck-mark.svg" width="88" height="88" alt="QuietDeck logo"></p>
<h1 align="center">QuietDeck TV Launcher</h1>
<p align="center"><strong>Your apps. A quieter home.</strong></p>
<p align="center"><a href="https://github.com/junaid-devv/quietdeck-tv-launcher/actions/workflows/android.yml"><img alt="Android build and tests" src="https://github.com/junaid-devv/quietdeck-tv-launcher/actions/workflows/android.yml/badge.svg?branch=release%2F2.0-play-candidate"></a> <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/license-MIT-blue.svg"></a> <img alt="Android TV 6.0 and later" src="https://img.shields.io/badge/Android_TV-6.0%2B-3ddc84"></p>

**QuietDeck is a free, open-source Android TV launcher with favourites, app search and personal collections.** A calm home screen, native remote controls, and local customization without ads, accounts, analytics or internet permission.

**2.0.0-rc2 is a release candidate.** The original calm Home design is restored: compact Home / Apps / Appearance / Settings navigation, a time-based greeting, favourites with labels below their banners and three small shortcuts. Search, collections and recent apps live on Apps and Settings. The native accessibility and performance work remains. A physical Haier Android 11 smoke test now passes; broader TV qualification and Google Play testing remain required. The stable v1.1.0 physical-device results do not certify this new build.

[Downloads and release notes](https://github.com/junaid-devv/quietdeck-tv-launcher/releases) Ãƒâ€šÃ‚Â· [Install and restore](docs/INSTALL.md) Ãƒâ€šÃ‚Â· [Build](docs/BUILD.md) Ãƒâ€šÃ‚Â· [Readiness and testing](docs/RELEASE-READINESS.md) Ãƒâ€šÃ‚Â· [Play handoff](play/PUBLISHING.md)

## A focused Android TV home

- **Organize your apps:** choose and reorder favourites, create named collections, hide apps and restore them later.
- **Find and return:** search installed visible apps by name, or revisit up to 12 apps opened through QuietDeck. Clear recent activity anytime.
- **Proportional artwork:** landscape banners, square icons and tall artwork fit inside tiles without stretching.
- **Make it readable:** four, five or six columns; three app-label sizes; high contrast; native focusable controls and dialogs.
- **Local appearance:** three original vector scenes or a sampled local wallpaper through the Android document picker.
- **Portable configuration:** export and restore favourites, hidden apps, collections and appearance. Recent activity and wallpaper stay out of backups.
- **Small native runtime:** Java and framework widgets, no third-party runtime libraries, a recycled grid and bounded artwork caching. Release builds use R8 and resource shrinking.
- **Clear setup:** first-run guidance, optional default-Home selection and visible app-options access for remotes without a Menu button.

QuietDeck changes your home screen. Streaming content and subscriptions come from your installed apps. Default Home, Inputs and file-picker support depend on the manufacturer.

## Screenshots

![QuietDeck v2 Home with proportional artwork](docs/screenshots/v2-tv/home.png)

![QuietDeck v2 app library](docs/screenshots/v2-tv/apps.png)

The two images above are actual v2.0.0-rc2 captures from a physical 1280 × 720 Haier Android 11 TV. New-version captures and their exact device/runtime context are documented in [release readiness](docs/RELEASE-READINESS.md). Development emulator images use original fixture apps to exercise square, tall and wide artwork; those fixtures are not included in the launcher. The old Haier captures under `docs/screenshots/` show **v1.1**, not v2. They must not be used in a v2 Play listing.

## Try it safely

1. Download the version you intend to test from [Releases](https://github.com/junaid-devv/quietdeck-tv-launcher/releases), and check its checksum and certificate fingerprint.
2. Follow [installation](docs/INSTALL.md), then open QuietDeck as an ordinary app to check remote navigation and app launching.
3. Choose your favourites. Use Settings for appearance, import/export and optional default-Home selection.
4. Verify Home-button behavior and [restore instructions](docs/INSTALL.md#restore-the-original-launcher) before depending on it as your only launcher.

Requires **Android TV 6.0 / API 23 or later**. The declared API floor is not a promise that every manufacturer's firmware is qualified. See [compatibility](docs/COMPATIBILITY.md).

## Remote controls

| Control | Action |
| --- | --- |
| Arrows | Move between native controls and app tiles; scroll app rows |
| OK / Enter | Open the selected item |
| Hold OK, Menu or Info | Open app options; Apps has an App options button; Home exposes it through Settings |
| Back | Clear search, then return to Home; leave an ordinary launch when already on Home |
| Home | Open your chosen default Home app, subject to TV firmware |

## Build and contribute

Use JDK 17 and Android SDK 35 / Build-tools 35.0.0:

```bash
./gradlew :app:lintDebug :app:lintRelease :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

On Windows use `gradlew.bat`. Debug builds use `com.still.launcher.debug` and coexist with the release. The production package remains `com.still.launcher`, preserving signed updates and preferences from the original Still prototype. The isolated `showcase` module is only for emulator artwork fixtures.

[Build and signing](docs/BUILD.md) Ãƒâ€šÃ‚Â· [Architecture](docs/ARCHITECTURE.md) Ãƒâ€šÃ‚Â· [FAQ](docs/FAQ.md) Ãƒâ€šÃ‚Â· [Roadmap](docs/ROADMAP.md) Ãƒâ€šÃ‚Â· [Contributing](CONTRIBUTING.md) Ãƒâ€šÃ‚Â· [Changelog](CHANGELOG.md)

## Privacy and support

The app requests no Android permissions and sends no data to a server. Installed app metadata, preferences and recent activity are processed locally. Read the in-app privacy policy or [public policy source](docs/privacy.html). File providers, Android, Google Play and other apps have their own policies.

[Report an issue or device result](https://github.com/junaid-devv/quietdeck-tv-launcher/issues/new/choose) Ãƒâ€šÃ‚Â· [Private security reports](SECURITY.md)

## License

[MIT](LICENSE), Ãƒâ€šÃ‚Â© 2026 Muhammad Junaid. Original QuietDeck branding is included; third-party app artwork belongs to its owners. QuietDeck is independent of Google, Apple, TV manufacturers and streaming services.
