# 2.0.0-rc1 release readiness

Prepared 21 October 2026. This page separates implemented engineering from external launch gates. An AAB that builds successfully is not a Google Play approval or a physical-device performance certification.

## Implemented

Native UI and per-control semantics; visible app options; configurable grid and labels; proportional artwork; local search, favourites, collections and recents; sampled wallpaper; high contrast; transactional bounded configuration import; standard Home selection; in-app privacy policy; no runtime libraries or requested permissions; optimized release APK/AAB and original branding.

## Automated evidence

See the candidate's [Android CI runs](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/workflows/android.yml?query=branch%3Arelease%2F2.0-play-candidate). Each final run archives the exact build, unit/lint reports, instrumentation results, unaltered emulator screenshots, page size and release startup/memory evidence. Final artifact verification and results are recorded here when available.

The unit suite covers proportional image geometry and backup-input limits. Device instrumentation exercises legacy settings migration, transactional rejection, recent-list bounds, native search state restoration, ordinary Back and D-pad navigation, visible hide/restore actions, first-run dismissal, package change refresh and memory trimming. Activity recreation is not a full process-death test. CI startup samples also force-stop and launch the optimized release repeatedly.

The emulator screenshot fixtures are original non-functional Music, Cinema and Games apps installed separately by CI. They exercise square, tall and wide source artwork. They are not bundled in QuietDeck and are not streaming-service integrations.

### Signed candidate verification

The distributable APK is **127,903 bytes**; the upload AAB is **130,410 bytes**. Both use R8/resource-shrunk release output from source `d6812fbbae9974b7fcd80fad8a01ab3620307105`, [build 39570159230](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/runs/39570159230). This build passes the complete device matrix. Its product code matches the earlier candidate; the build metadata records the final tested commit.

- APK signature and zip alignment verified; signing certificate matches the existing v1.1 sideload identity.
- AAB uses a separate upload key and passes official bundletool 1.18.3 validation.
- A universal APK generated locally from the AAB verifies with the app-signing identity; its DEX matches the assembled release. This does not replace a Play-delivered install check.
- Package `com.still.launcher`, version code 20, minimum API 23, target API 35; no requested permissions, packaged native libraries or demonstration fixtures.
- Eleven local unit tests pass. Release lint has no errors and five reviewed warnings: target 35 is below the newest SDK (but exceeds the current TV submission minimum), three intentionally pinned API-23-compatible test dependencies, and fixed landscape orientation for this TV-only app.
- Store title/descriptions, icon, feature graphic, TV banner and the two unaltered 1080p screenshots pass the Play preflight tools. These checks validate files and text limits, not Play approval or discoverability.

Public release files include certificate fingerprints, checksums, R8 mapping, build provenance and verification reports. Private signing material is excluded. Preserve an offline backup of both private signing identities separately.

Current policy references: [TV quality requirements](https://developer.android.com/docs/quality-guidelines/tv-app-quality), [target API rules](https://support.google.com/googleplay/android-developer/answer/11926878), and [new personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465). Recheck these when submitting.

### Final CI matrix

[Push run 39570159230](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/runs/39570159230) and [independent PR run 39570218024](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/runs/39570218024) both pass all jobs.

| Runtime | Configuration | Device suite | Optimized release smoke |
| --- | --- | --- | --- |
| Android TV API 23 x86 | 1 GiB RAM / 128 MiB heap | 10 passed | 5 launches passed |
| Android TV API 30 x86 | 1 GiB RAM / 128 MiB heap | 10 passed + 3 display captures | 5 launches passed |
| Google APIs API 35 x86_64 | 4 GiB RAM / 256 MiB heap; verified 16,384-byte pages | 10 passed | 5 launches passed |

API 35 is a generic Google APIs image with a TV hardware profile, not Android TV firmware or a low-RAM qualification. Its first-boot services receive a settling period; its phone-specific immersive-mode tutorial is acknowledged by the CI setup. Android TV uses the original 1 GiB test configuration.

API 30 captures cover 720p, 1080p, large fonts/high contrast and 2160p output with a 1080p logical app surface. See [unaltered screenshots and context](screenshots/v2-emulator/README.md). The initial 640-dpi output override produced a cropped test setup and was corrected to the TV's 320-dpi logical density before qualification.

For reproducibility, the push-run median cold-launch observations were 1,070 ms (API 23), 931 ms (API 30) and 1,038 ms (API 35), from five force-stop/`am start -W` samples including first launch. These are shared-runner emulator observations, not speed claims or physical-TV acceptance thresholds. Raw samples, memory snapshots and instrumentation results are preserved with the prerelease.

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
