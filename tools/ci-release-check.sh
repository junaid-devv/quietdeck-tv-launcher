#!/usr/bin/env bash
set -euo pipefail
sdkmanager 'build-tools;35.0.0' > /dev/null
# Ephemeral CI signing identity, never used for distributed releases.
keytool -genkeypair -noprompt -keystore "$RUNNER_TEMP/quietdeck-ci.p12" -storepass ci-test-only -alias ci -keyalg RSA -keysize 2048 -validity 7 -dname "CN=QuietDeck CI Test Only"
"$ANDROID_HOME/build-tools/35.0.0/apksigner" sign --ks "$RUNNER_TEMP/quietdeck-ci.p12" --ks-pass pass:ci-test-only --ks-key-alias ci --out "$RUNNER_TEMP/quietdeck-ci.apk" ci-artifacts/app/build/outputs/apk/release/app-release-unsigned.apk
adb -s emulator-5554 install -r "$RUNNER_TEMP/quietdeck-ci.apk"
python3 tools/benchmark-device.py --serial emulator-5554 --output release-benchmark
