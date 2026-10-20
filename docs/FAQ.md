# FAQ and troubleshooting

## Does this remove Google TV ads?

QuietDeck's own home screen contains no advertising or recommendation feed. It does not remove ads inside YouTube, streaming apps, or other installed software.

## Is it an Apple TV clone?

It is an independent Android TV launcher with a restrained grid and landscape artwork. It does not run tvOS, provide Apple services, or reproduce Apple's interface assets.

## Is root required?

The app itself does not require root. Sideloading uses Android's normal installation mechanisms. Changing the Home-button behavior depends on the TV; some firmware requires an ADB workaround, and others may restrict third-party launchers.

## Home still opens the original launcher

See [Home-button setup](INSTALL.md#make-it-your-home-screen). Confirm the original component and test before disabling anything. A successful `set-home-activity` response alone is not proof that Home changed.

## ADB shows unauthorized, offline, or cannot connect

- `unauthorized`: unlock/wake the TV and approve the computer's debugging prompt.
- `offline`: check the TV is awake and reconnect to the correct address.
- Connection failure: verify the TV's current IP, debugging mode and displayed port. USB debugging does not expose network ADB on every TV. Guest Wi-Fi isolation can block local connections.

## Some apps have generic icons or names

QuietDeck reads labels and artwork from each installed package. OEM shortcut apps sometimes supply placeholders. They can be hidden from the launcher or removed from favourites. Regular phone apps can appear, but may not be designed for a TV remote.

## My favourite disappeared

Hiding an app hides it from Home too. Use Appearance → Restore hidden apps. If it was removed from favourites, open Apps, hold OK or press Menu, and choose Add to favourites. Uninstalled apps cannot be launched.

## Inputs does not open the source selector

Manufacturers use different input settings interfaces. If the standard intent is unavailable, use your remote's Input/Source button. QuietDeck's Settings shortcut still opens Android settings where supported.

## Can I use my own wallpaper, channels, or widgets?

Version 1.1.0 offers three built-in backgrounds. Custom wallpapers, content rows, widgets, and a universal search feature are not implemented. See the [roadmap](ROADMAP.md).

## How do I update or uninstall?

Install a newer official APK with a matching signing certificate to retain local preferences. An independently signed build cannot update it. To uninstall, [restore a working original home first](INSTALL.md#restore-the-original-launcher), then uninstall QuietDeck through TV Settings or ADB.
