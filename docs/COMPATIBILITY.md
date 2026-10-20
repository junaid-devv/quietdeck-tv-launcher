# Device compatibility

QuietDeck TV declares Android 6.0/API 23 as its minimum and targets Android 15/API 35. This describes its build configuration, not a claim that every TV or Android version has been tested.

| Device / software | Status | Evidence and limits |
| --- | --- | --- |
| Haier Android TV, model reported as `haierATVnippori` | Physically tested | Android 11/API 30; reported 1280 × 720 output, density 213. Native home rendering, app discovery, D-pad movement, app-grid scrolling, Menu options, favourites persistence across a cold launch, app launching, and Home return were exercised. |
| Haier stock Android TV Home | Workaround verified | Default-home selection alone did not override the system launcher. Disabling its package for user 0 allowed QuietDeck to handle Home. Package retained for restoration. |
| Other Android TV manufacturers / boxes | Unverified | API-compatible in principle; input settings, app artwork and Home-button behavior vary. |
| Google TV devices | Unverified | May accept sideloading but enforce different Home-launcher restrictions. No universal Home override is promised. |
| Samsung Tizen / LG webOS | Unsupported | Not Android application platforms. |

No TV reboot was performed in the original test session. Post-reboot behavior and the physical remote's long-press timing still need device reports. The Menu-key route to app options was tested.

## Share a device result

Use the [device compatibility form](https://github.com/junaid-devv/quietdeck-tv-launcher/issues/new?template=device-report.yml). Include the brand/model, Android version, app version, what works, and whether a Home workaround was needed. Do not post pairing codes, private IP addresses, serial numbers, account details, or screenshots containing personal information.
