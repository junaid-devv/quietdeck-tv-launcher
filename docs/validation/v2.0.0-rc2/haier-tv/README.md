# Physical Haier TV smoke test

21 October 2026. Official signed rc2 APK, version code 21; source 8e00885aa991187e6b9440d33e56447e22390168. Model haierATVnippori, Android 11/API 30, low_ram=true, physical 1280 × 720, density 213, 4096-byte pages.

Passed: in-place upgrade from v1.1.0 without clearing data; existing YouTube, YouTube Music, Netflix and Prime Video favourites retained in order; proportional banners and visible labels; D-pad Apps navigation; Back; Settings and app-options dialogs; opening YouTube; Home returns to the existing QuietDeck default. No system launcher settings or app data were cleared. No test fixture apps were installed.

Five force-stop/am start -W TotalTime samples: 1058, 665, 490, 606, 481 ms, two-second settling between samples; median 606 ms. Settled PSS 56,937 KiB; this includes runtime memory and is not the artwork cache budget. No comparative speedup is claimed. Current launcher process-filtered AndroidRuntime error log had no fatal exception. Graphics measurements are diagnostic and include this limited session only.

Screenshots: [Home](../../../screenshots/v2-tv/home.png), [Apps](../../../screenshots/v2-tv/apps.png). Native screenshots, no fixture artwork or account-content captures.

Not covered: physical remote hardware buttons beyond injected Android key events, reboot/standby recovery, sustained memory pressure, full accessibility, other OEMs or Play-delivered updates. Automated CI results remain separately documented.
