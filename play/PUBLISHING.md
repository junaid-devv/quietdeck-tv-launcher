# Google Play publishing handoff

This is a **release candidate**, not a production-approved Play release. Do not describe emulator checks as physical TV certification or closed testing.

## Prepared in this repository

- Native Android TV app, API 23 minimum / target 35; TV-only feature declaration.
- R8 optimization and resource shrinking for release APK/AAB.
- English title, short description and full description under `en-US/`.
- Original logo, 512×512 RGBA store icon, 1024×500 feature graphic, 1280×720 store TV banner. Runtime TV banner is separately 320×180.
- In-app privacy policy and public HTML source in `docs/privacy.html`.
- Unit tests and emulator remote/lifecycle/persistence checks; see `docs/RELEASE-READINESS.md` for results and limits.

## Owner actions before the first upload

1. Confirm `com.still.launcher` is available to this Play account. The legacy package preserves sideloaded updates; Play ownership has not been verified.
2. Supply a public developer support email and review the listing, privacy policy, content rating, target audience and Data safety responses. Do not invent an email address. The issue tracker is available for project support, but it does not replace Play's required contact email.
3. Confirm a live, publicly accessible privacy-policy URL. The prepared URL is `https://junaid-devv.github.io/quietdeck-tv-launcher/privacy.html`; verify it loads before entering it in Console.
4. Enroll in Play App Signing. Preserve the existing APK app-signing identity if sideload-to-Play updates are required. Follow the Console's key-import instructions privately. The AAB should be signed with a separate upload key. A public certificate is safe to share; private keys and passwords must never enter Git or issue reports.
5. Upload the upload-key-signed AAB to **internal testing** first. Review App Bundle Explorer, device exclusions, TV opt-in, pre-launch reports and generated APK signatures. Install the Play-delivered build on a physical TV; verify updates from the sideloaded version and settings migration.
6. Review the genuine, unaltered Android TV emulator captures supplied with the candidate. They show the current app with separately installed original artwork fixtures, not streaming integrations. Play requires accurate current-version TV screenshots; its TV-G4 rule does not specify physical hardware. Capturing the final setup on physical TVs is recommended during qualification. Do not use v1.1 screenshots for v2.

## Personal account created after 13 November 2023

Run a closed test with at least **12 testers opted in continuously for 14 days**, collect meaningful feedback, fix defects, then apply for production access. This is a real participation requirement; CI tests and a prepared AAB do not fulfill it. Check the current Console instructions when starting the test.

## Proposed Data safety responses — review against the actual build

The launcher has no INTERNET permission, network client, ads, accounts or third-party runtime SDKs. No user data is collected or shared off-device by QuietDeck. Installed app metadata, configuration and recent app identifiers are processed locally. User-selected file providers may independently use cloud services; exported files remain user-controlled. The privacy policy explains this distinction.

- Ads: **No**.
- Account sign-in / app access restrictions: none.
- Data collected or shared by this app: proposed **No**, subject to owner review of the final binary and dependencies.
- Account deletion: no app accounts exist. Local data can be removed by clearing storage or uninstalling; exported files require user deletion.
- Content rating and target audience: answer the Console questionnaire truthfully; do not infer a rating from the absence of ads.

## Release operation

Use a new, increasing versionCode for every accepted upload. Candidate code is 20. If 20 has already been consumed, increase it before rebuilding. Store artifacts are under `play/en-US/graphics/`; repository APKs and keys are ignored. Keep R8 mapping files per released build. Use staged rollout after approval, monitor Android vitals and feedback, and halt rollout for startup crashes, focus traps or launcher recovery failures.

Official references: [TV quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality), [TV distribution](https://developer.android.com/training/tv/publishing/distribute), [TV target API requirements](https://developer.android.com/google/play/requirements/target-sdk), [personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465), [user data and privacy](https://support.google.com/googleplay/android-developer/answer/10144311), [Play App Signing](https://developer.android.com/studio/publish/app-signing).
