# Architecture and privacy

## Runtime

`MainActivity` owns a single native `HomeView`. Android Canvas draws a 1920 × 1080 reference layout scaled to the TV's reported display area. A locally generated ARGB8888 bitmap caches the background; app banners and icons are loaded through Android's package manager on a background executor.

`GridNavigator` contains the platform-independent grid movement used by the D-pad handler. JVM tests exercise empty grids, row boundaries, partial last rows, shrinking lists, and reachability across multi-page grids.

The app queries the standard `LEANBACK_LAUNCHER`, `LAUNCHER`, and `HOME` intents. It does not request broad `QUERY_ALL_PACKAGES` access. Installed home apps, settings entries, and known factory/service shortcuts are excluded. A package-change receiver refreshes the list after installs, removals, or changes.

## State

Android SharedPreferences stores favourite package IDs and their order, hidden package IDs, and the selected background. The preference file keeps the legacy name `still` to preserve upgrades from the original prototype. No viewing history or playback activity is recorded. Android application backup is disabled in the manifest.

## Permissions and data

The app requests **no Android permissions**, including no `INTERNET`, storage, microphone, camera, accessibility-service, or usage-access permission. There are no analytics, ad SDKs, network clients, or cloud endpoints in the application. Build-time downloads and GitHub's website are separate from the installed application.

App labels, icons and installed package identifiers are processed on the TV to render the launcher. They are not transmitted by QuietDeck. Other apps launched from the grid operate under their own policies.

## Accessibility and scope

Navigation is designed primarily for a D-pad. The selected item sends a basic accessibility announcement, but the Canvas UI does not yet expose a complete virtual-node tree for granular screen-reader navigation. Full accessibility support is open work, not a completed feature.

No firmware, package settings, or home-app preference is changed automatically by the APK. A user or installer must select the home app or explicitly apply an external ADB workaround.
