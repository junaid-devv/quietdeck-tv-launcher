# Compatibility and qualification

The release declares Android TV 6.0 / API 23 minimum and target API 35, with required Leanback support and no touchscreen requirement. Java-only bytecode has no bundled native ABI restriction. These declarations describe packaging support; device qualification is recorded separately.

| Environment | Version | Evidence / limits |
| --- | --- | --- |
| Physical Haier Android 11, low-RAM, 720p | v1.1.0 | Earlier installation, launch, favourites, grid and Home return checks; not v2 qualification |
| Android TV API 23 x86 emulator, 1 GiB | v2 candidate | Instrumentation results linked in release readiness |
| Android TV API 30 x86 emulator, 1 GiB | v2 candidate | Instrumentation results linked in release readiness |
| Android API 35 x86_64 16 KiB-page image, TV display profile | v2 candidate | Modern runtime check; this image is not certified Google TV firmware |
| Physical Haier Android 11, low-RAM, 720p | v2.0.0-rc2 | Signed upgrade, preserved favourites, real artwork, D-pad navigation, Settings, YouTube launch and default Home return pass; median startup 606 ms across five samples |
| Other Android TV / Google TV | v2 candidate | Physical qualification pending |

TV manufacturers control Home selection, input settings, installed-app visibility, storage providers and standby behavior. A working standard intent on one device does not establish support on others. Optional features show an explanation when unavailable. The launcher can still open apps without replacing the Home button.

Please report model, Android version, app version, display resolution, remote type, reproduction steps and sanitized logs in a [device result](https://github.com/junaid-devv/quietdeck-tv-launcher/issues/new/choose). Never post ADB pairing codes, account credentials or signing keys.
