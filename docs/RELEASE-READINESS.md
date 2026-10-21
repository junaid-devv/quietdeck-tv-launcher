# 2.0.0-rc1 release readiness

Prepared 21 October 2026. This page separates implemented engineering from external launch gates. An AAB that builds successfully is not a Google Play approval or a physical-device performance certification.

## Implemented

Native UI and per-control semantics; visible app options; configurable grid and labels; proportional artwork; local search, favourites, collections and recents; sampled wallpaper; high contrast; transactional bounded configuration import; standard Home selection; in-app privacy policy; no runtime libraries or requested permissions; optimized release APK/AAB and original branding.

## Automated evidence

See the candidate's [Android CI runs](https://github.com/junaid-devv/quietdeck-tv-launcher/actions/workflows/android.yml?query=branch%3Arelease%2F2.0-play-candidate). Each final run archives the exact build, unit/lint reports, instrumentation results, unaltered emulator screenshots, page size and release startup/memory evidence. Final artifact verification and results are recorded here when available.

The unit suite covers proportional image geometry and backup-input limits. Device instrumentation exercises legacy settings migration, transactional rejection, recent-list bounds, native search state restoration, ordinary Back and D-pad navigation, visible hide/restore actions, first-run dismissal, package change refresh and memory trimming. Activity recreation is not a full process-death test. CI startup samples also force-stop and launch the optimized release repeatedly.

The emulator screenshot fixtures are original non-functional Music, Cinema and Games apps installed separately by CI. They exercise square, tall and wide source artwork. They are not bundled in QuietDeck and are not streaming-service integrations.

## Physical performance remains pending

The original v1.1 low-RAM Haier check recorded one cold-start sample of 773 ms and an immediate PSS sample of 41,213 KiB. These were single diagnostic observations, not a stable performance baseline. No v2 comparison is valid until the same TV, app set, firmware and measurement procedure can be used.

Artwork budgets (2/4 MiB), sampled wallpaper and release file size are enforceable design facts. They do not establish total RAM, frame rate or key-to-highlight latency on a physical TV. Do not advertise a measured speedup from emulator timings.

## Before production publication

- Test the signed v2 APK on low-RAM TV hardware: first run, upgrades from v1.1, normal and default Home, reboot, standby/wake, repeated app switching, long labels, 720p/1080p/4K, rapid remote input and recovery.
- Test TalkBack, large system fonts, RTL layouts and OEM restrictions. Native controls provide semantics; full assistive-technology usability needs human verification.
- Review the support email, package ownership and final store declarations; verify the public privacy URL.
- Enroll in Play App Signing with compatible app-signing identity; inspect and install Play-delivered APKs from an internal test.
- Capture genuine final-version TV listing screenshots. Run the required closed test (12 opted-in testers continuously 14 days for this account), then apply for production access and TV review.

No Play Console submission or TV upgrade is implied by this candidate. See [publishing handoff](../play/PUBLISHING.md).
