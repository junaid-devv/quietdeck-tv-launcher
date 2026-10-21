# Install QuietDeck TV on Android TV

For v2, select the explicitly marked `2.0.0-rc1` prerelease rather than the latest stable v1.1 download. Use its filename in the commands below. Test it as an ordinary app before changing Home. Candidate qualification is documented in [release readiness](RELEASE-READINESS.md).

## Download and verify

Get the APK and `SHA256SUMS.txt` from the project's [GitHub Releases](https://github.com/junaid-devv/quietdeck-tv-launcher/releases/latest). The checksum detects a damaged or mismatched download; it is not an independent guarantee of trust.

```bash
sha256sum -c SHA256SUMS.txt
```

On Windows: `Get-FileHash .\QuietDeck-TV-v1.1.0.apk -Algorithm SHA256` and compare it with the published checksum.

You can use your TV's supported APK installer, enabling installation for that installer when Android asks. The steps below use [Android Debug Bridge (ADB)](https://developer.android.com/tools/adb), included in Android SDK Platform Tools.

## Enable debugging on your own TV

1. Open Settings → Device Preferences/System → About.
2. Select Build repeatedly until Developer options are enabled.
3. Open Developer options and enable the available debugging option.
4. Keep the TV awake and approve your computer when Android displays its debugging prompt.

On the tested Haier Android 11 TV, enabling **USB debugging** also exposed network ADB. This is manufacturer-specific. A TV may instead offer Network debugging, require an initial USB connection, or support Wireless debugging with pairing.

## Connect over Wi-Fi

Both devices must be able to reach each other on the local network. Use the TV's actual address; `TV_IP` below is a placeholder.

For a TV that exposes classic network ADB:

```bash
adb connect TV_IP:5555
adb devices -l
```

For supported Wireless debugging with pairing, use the pairing address/code displayed on the TV, followed by the **separate connection address** displayed by that page:

```bash
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:CONNECTION_PORT
```

The device must be listed as `device`, rather than `unauthorized` or `offline`, before installation. An open network connection alone does not authorize installation. Keep ADB on a trusted local network; do not forward its port on your router.

## Install and try it first

Replace `TV_SERIAL` with the full serial shown by `adb devices`, including the port where applicable.

```bash
adb -s TV_SERIAL install --no-incremental -r QuietDeck-TV-v1.1.0.apk
adb -s TV_SERIAL shell am start -n com.still.launcher/.MainActivity
```

Check the app grid, Settings shortcut, remote navigation, and app launching. A matching release signing certificate is required to update an existing installation. An independently signed local build cannot update the official APK in place.

## Make it your home screen

First try the TV's Home-app picker or QuietDeck's **Settings → Choose default Home** action. ADB can also request a home-app change:

```bash
adb -s TV_SERIAL shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s TV_SERIAL shell cmd package set-home-activity --user 0 com.still.launcher/.MainActivity
adb -s TV_SERIAL shell input keyevent 3
```

Record the original home component before changing it. The command may report success while the TV still opens a higher-priority system launcher.

### Tested Haier / Android TV Home workaround

On the tested Haier Android 11 firmware, the original component was `com.google.android.tvlauncher/.MainActivity`, with a higher priority than third-party launchers. After confirming QuietDeck worked, the following made Home open it:

```bash
adb -s TV_SERIAL shell pm disable-user --user 0 com.google.android.tvlauncher
adb -s TV_SERIAL shell cmd package set-home-activity --user 0 com.still.launcher/.MainActivity
adb -s TV_SERIAL shell input keyevent 3
```

This disables the original launcher **for user 0**; it does not uninstall it. These package names are specific to the tested configuration. Do not apply them blindly to another TV. Google TV can use a different system launcher and may enforce additional restrictions; that configuration is not verified here.

## Restore the original launcher

For the tested configuration:

```bash
adb -s TV_SERIAL shell pm enable --user 0 com.google.android.tvlauncher
adb -s TV_SERIAL shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
adb -s TV_SERIAL shell am start -n com.google.android.tvlauncher/.MainActivity
```

You can also re-enable Android TV Home under Settings → Apps → See all apps → Show system apps → Android TV Home → Enable, where your TV provides that option.

**Restore a working home app before uninstalling QuietDeck.** Then use TV Settings → Apps → QuietDeck TV → Uninstall, or:

```bash
adb -s TV_SERIAL uninstall com.still.launcher
```

## After setup

You can turn debugging off once installation and testing are complete. QuietDeck itself does not need ADB or internet access to run. Firmware updates can change the Home-app behavior; repeat the compatibility checks if an update restores the stock home screen.
