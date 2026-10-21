# QuietDeck v2 emulator captures

These PNGs are unaltered screenshots of 2.0.0-rc1, captured with Android UI Automation on the Android TV API 30 x86 emulator. Home uses three original demonstration apps: Quiet Cinema (wide), Quiet Music (square) and Quiet Games (tall). They are non-functional artwork fixtures installed separately; they are not included in the production APK/AAB. Other visible apps belong to the emulator image and are not QuietDeck integrations.

- `default-*`: 1920 × 1080, 320 dpi, normal font scale.
- `720p-*`: 1280 × 720, 213 dpi, normal font scale.
- `large-text-contrast-*`: 1920 × 1080, 320 dpi, 1.3 font scale, high contrast enabled. The app grid scrolls to further rows.
- `4k-output-*`, when present: 3840 × 2160 output override with the TV's 320 dpi logical density. This Android TV runtime keeps an app surface at 1920 × 1080; these are not native 4K-rendering captures.

The two files under `play/en-US/graphics/tvScreenshots/` are exact copies of default Home and Apps, with no frame, overlay, resize or retouching. Both passed the Play image-set preflight (format, dimensions, alpha and count). Physical-device behavior and manufacturer Home integration still need qualification.

Source build: e11615b6ffca83b31c7a8bb3760b204f0c21c5d0. Capture evidence: https://github.com/junaid-devv/quietdeck-tv-launcher/actions/runs/39546042617

Later CI-only changes correct the 2160p output test density and allow the Google APIs 16 KiB emulator's first-boot services to settle. Product source is unchanged.
