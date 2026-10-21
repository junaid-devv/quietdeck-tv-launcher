# 2.0.0-rc1 release readiness

Prepared 21 October 2026. This page separates implemented engineering from external launch gates. An AAB that builds successfully is not a Google Play approval or a physical-device performance certification.

## Implemented

Native UI and per-control semantics; visible app options; configurable grid and labels; proportional artwork; local search, favourites, collections and recents; sampled wallpaper; high contrast; transactional bounded configuration import; standard Home selection; in-app privacy policy; no runtime libraries or requested permissions; optimized release APK/AAB and original branding.

## Automated evidence

See the candidate's [Android CI runs](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/workflows/android.yml?query=branch%3Arelease%2F2.0-play-candidate). Each final run archives the exact build, unit/lint reports, instrumentation results, unaltered emulator screenshots, page size and release startup/memory evidence. Final artifact verification and results are recorded here when available.

The unit suite covers proportional image geometry and backup-input limits. Device instrumentation exercises legacy settings migration, transactional rejection, recent-list bounds, native search state restoration, ordinary Back and D-pad navigation, visible hide/restore actions, first-run dismissal, package change refresh and memory trimming. Activity recreation is not a full process-death test. CI startup samples also force-stop and launch the optimized release repeatedly.

The emulator screenshot fixtures are original non-functional Music, Cinema and Games apps installed separately by CI. They exercise square, tall and wide source artwork. They are not bundled in QuietDeck and are not streaming-service integrations.

### Signed candidate verification

The distributable APK is **127,903 bytes**; the upload AAB is **130,409 bytes**. Both use R8/resource-shrunk release output from source `e11615b6ffca83b31c7a8bb3760b204f0c21c5d0`, [build 39546042617](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/runs/39546042617). Subsequent test-harness changes do not modify product source.

- APK signature and zip alignment verified; signing certificate matches the existing v1.1 sideload identity.
- AAB uses a separate upload key and passes official bundletool 1.18.3 validation.
- A universal APK generated locally from the AAB verifies with the app-signing identity; its DEX matches the assembled release. This does not replace a Play-delivered install check.
- Package `com.still.launcher`, version code 20, minimum API 23, target API 35; no requested permissions, packaged native libraries or demonstration fixtures.
- Eleven local unit tests pass. Release lint has no errors and five reviewed warnings: target 35 is below the newest SDK (but exceeds the current TV submission minimum), three intentionally pinned API-23-compatible test dependencies, and fixed landscape orientation for this TV-only app.
- Store title/descriptions, icon, feature graphic, TV banner and the two unaltered 1080p screenshots pass the Play preflight tools. These checks validate files and text limits, not Play approval or discoverability.

Public release files include certificate fingerprints, checksums, R8 mapping, build provenance and verification reports. Private signing material is excluded. Preserve an offline backup of both private signing identities separately.

Current policy references: [TV quality requirements](https://developer.android.com/docs/quality-guidelines/tv-app-quality), [target API rules](https://support.google.com/googleplay/android-developer/answer/11926878), and [new personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465). Recheck these when submitting.

## Physical performance remains pending

The original v1.1 low-RAM Haier check recorded one cold-start sample of 773 ms and an immediate PSS sample of 41,213 KiB. These were single diagnostic observations, not a stable performance baseline. No v2 comparison is valid until the same TV, app set, firmware and measurement procedure can be used.

Artwork budgets (2/4 MiB), sampled wallpaper and release file size are enforceable design facts. They do not establish total RAM, frame rate or key-to-highlight latency on a physical TV. Do not advertise a measured speedup from emulator timings.

## Before production publication

- Test the signed v2 APK on low-RAM TV hardware: first run, upgrades from v1.1, normal and default Home, reboot, standby/wake, repeated app switching, long labels, 720p/1080p/4K, rapid remote input and recovery.
- Test TalkBack, large system fonts, RTL layouts and OEM restrictions. Native controls provide semantics; full assistive-technology usability needs human verification.
- Review the support email, package ownership and final store declarations; verify the public privacy URL.
- Enroll in Play App Signing with compatible app-signing identity; inspect and install Play-delivered APKs from an internal test.
- Review the supplied unaltered final-version Android TV emulator screenshots for listing use; physical TV captures are recommended during qualification. Run the required closed test (12 opted-in testers continuously 14 days for this account), then apply for production access and TV review.

No Play Console submission or TV upgrade is implied by this candidate. See [publishing handoff](../play/PUBLISHING.md).
