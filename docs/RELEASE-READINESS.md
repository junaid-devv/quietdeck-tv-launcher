# 2.0.0-rc2 release readiness

Prepared 21 October 2026. This candidate restores the original QuietDeck Home design while preserving the native accessibility, app organization and performance work. It is not a production-approved Play release.

## Home design

The Home screen returns to compact Home / Apps / Appearance / Settings tabs, the original blue mountain scenery, a date and time-based greeting, small favourites with labels below the artwork, and three small shortcuts. Search, collections, recents and visible app options move into Apps and Settings. They do not occupy the Home surface. Native D-pad focus remains distinct from the selected-tab underline.

## Engineering retained

Background metadata discovery; recycled native grid cells; proportional artwork; bounded 2/4 MiB artwork caching; local customization; sampled wallpaper; high contrast; bounded transactional backup import; standard Home selection; no requested permissions, third-party runtime libraries or native libraries; R8 and resource shrinking. The mountain scenery is drawn with cached paths/shaders instead of a full-screen bitmap.

## Qualification

The TV remains unavailable. No v2 physical-TV installation, reboot/standby, OEM Home integration, accessibility qualification or Play-delivered update test has been performed. Cache budgets and APK size do not establish total RAM or physical-device responsiveness. Do not infer a physical speedup from emulator timings.

The original v1.1 Haier check recorded a single cold launch of 773 ms and immediate PSS of 41,213 KiB. Those observations are not a v2 performance baseline.

Before production: qualify low-RAM TV hardware, recovery and upgrades; test TalkBack/RTL/large fonts; provide the support email; verify package ownership and Play App Signing; install Play's internal-test delivery; complete the newer personal account's 12 continuously opted-in testers for 14 days and apply for production access. See [publishing handoff](../play/PUBLISHING.md).

Current official references: [TV quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality), [target API rules](https://support.google.com/googleplay/android-developer/answer/11926878), [personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465).

## Final automated validation

Source `8e00885aa991187e6b9440d33e56447e22390168`: [push run](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/runs/39574392581) and [independent PR run](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/runs/39574399356). Release APK **131,999 bytes**, upload AAB **134,259 bytes**, version code 21, package `com.still.launcher`, min API 23 / target 35.

| Runtime | Budget | Device checks | Optimized release launches |
| --- | --- | --- | --- |
| Android TV API 23 x86 | 1 GiB / 128 MiB heap | 11 passed | 5 passed |
| Android TV API 30 x86 | 1 GiB / 128 MiB heap | 11 passed + 3 display captures | 5 passed |
| Google APIs API 35 x86_64 | 4 GiB / 256 MiB heap | 11 passed; 16,384-byte pages verified | 5 passed |

The new device check verifies that search, collections and app options stay off Home while remaining available on Apps. Other tests cover native D-pad focus/Back, first-run guidance, legacy settings, transactional imports, recents, package changes, hide/restore, recreation and memory trimming. Eleven geometry/input-validation unit tests and debug/release lint pass. Five reviewed lint warnings remain: current target versus newest SDK, three API-23-compatible test dependency pins and TV-only landscape orientation.

API 35 is a generic Google APIs image with TV hardware, not TV firmware or a low-RAM qualification. CI settles its first-boot system workload and acknowledges its phone-only immersive tutorial. API 23/30 retain 1 GiB RAM. Display captures cover 720p, 1080p, large fonts/high contrast and 2160p output with a 1080p logical surface. [Screenshots and context](screenshots/v2-emulator/README.md).

APK signatures/alignment, preserved app-signing identity, no-permission/native-library checks and official bundletool validation pass. A bundle-generated local universal APK verifies with the app identity and has matching DEX. AAB uses the separate upload key. Actual Play delivery remains untested. Private keys are excluded from public files.

Raw instrumentation results and release startup observations are under [validation](validation/v2.0.0-rc2/) and in the prerelease evidence ZIP. Shared-runner emulator timings do not establish physical TV performance.
