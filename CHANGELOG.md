# Changelog

## 2.0.0-rc1 — 2026-10-21

Release candidate; physical TV qualification and Play test-track distribution pending.

- Replaced the custom-drawn interface with native remote-friendly controls, a recycled grid and accessible app labels.
- Fixed stretched logos with proportional banner/icon fitting and FIT_CENTER display.
- Added global app-name search, recent apps, named collections, selectable favourites, tile density, label size, high contrast, local wallpaper and validated configuration export/import.
- Split metadata discovery, bounded lazy artwork loading, persistence and background drawing into separate owners.
- Removed full-screen background bitmaps and resume-time rediscovery; added 2/4 MiB artwork budgets and lifecycle trimming.
- Enabled R8/resource shrinking; added release AAB, emulator checks, benchmark tools and Play handoff documents.
- Added original QuietDeck Q logo, store graphics and public/in-app privacy policy.


## 1.1.0 — 2026-10-20

First public release under the **QuietDeck TV** name.

- Native Android TV Home and Apps screens with D-pad navigation.
- Favourites, reordering, hidden apps and local preference persistence.
- Three built-in landscape backgrounds.
- Settings and Inputs shortcuts, plus app information actions.
- Clearer names for generic Haier shortcuts and exclusion of known factory/service entries.
- ARGB8888 background rendering to avoid the earlier RGB565 banding.
- Standard Android Studio/Gradle project, navigation tests, CI configuration and an SDK-only Windows build path.
- Installation, rollback, privacy and device-compatibility documentation.

The application ID remains `com.still.launcher` for signed updates from the private Still prototype. No third-party app interface or firmware is modified.
