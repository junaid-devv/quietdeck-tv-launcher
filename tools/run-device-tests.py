"""Run installed instrumentation without deleting screenshot evidence afterward.

Intended for CI emulators. Explicit serial required; never discovers or connects TVs.
"""
import argparse
from pathlib import Path
import re
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--artifacts', type=Path, required=True)
parser.add_argument('--output', type=Path, default=Path('device-evidence'))
parser.add_argument('--display-checks', action='store_true')
parser.add_argument('--expect-16k', action='store_true')
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)

def adb(*command):
    return subprocess.run(['adb', '-s', args.serial, *command], check=True, capture_output=True, text=True).stdout

def pull_screenshots():
    listing = subprocess.run(['adb','-s',args.serial,'shell','run-as','com.still.launcher.debug','ls','files/screenshots'],
                             capture_output=True,text=True)
    if listing.returncode:
        (args.output/'screenshot-extraction.txt').write_text(listing.stdout+listing.stderr,encoding='utf-8')
        return 0
    folder=args.output/'screenshots'; folder.mkdir(exist_ok=True)
    count=0
    for name in listing.stdout.splitlines():
        name=name.strip()
        if not re.fullmatch(r'[a-z0-9-]+\.png',name): continue
        data=subprocess.run(['adb','-s',args.serial,'exec-out','run-as','com.still.launcher.debug','cat','files/screenshots/'+name],
                            check=True,capture_output=True).stdout
        if not data.startswith(b'\x89PNG\r\n\x1a\n'): raise SystemExit('Invalid screenshot stream')
        (folder/name).write_bytes(data); count+=1
    return count

# The Google APIs 16 KiB image starts many system services after boot_completed.
# Let that first-boot workload settle before instrumenting the app. The 1 GiB
# Android TV images deliberately retain their smaller, representative budget.
if args.expect_16k:
    time.sleep(45)
    (args.output/'pre-test-memory.txt').write_text(adb('shell', 'dumpsys', 'meminfo'), encoding='utf-8')
    # This is a Google APIs phone image with TV hardware, not TV firmware.
    # Acknowledge its system immersive-mode tutorial before app UI tests.
    adb('shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed')

app = next(args.artifacts.rglob('app-debug.apk'))
test = next(args.artifacts.rglob('app-debug-androidTest.apk'))
adb('install', '-r', str(app))
adb('install', '-r', str(test))
for apk in sorted(args.artifacts.rglob('showcase-*-debug.apk')):
    adb('install', '-r', str(apk))
result = adb('shell', 'am', 'instrument', '-w', '-r',
             'com.still.launcher.debug.test/androidx.test.runner.AndroidJUnitRunner')
(args.output/'instrumentation.txt').write_text(result, encoding='utf-8')
(args.output/'runtime-log.txt').write_text(adb('shell', 'logcat', '-d', '-s', 'AndroidRuntime:E', 'TestRunner:I'), encoding='utf-8')
(args.output/'system-log.txt').write_text(adb('shell', 'logcat', '-d'), encoding='utf-8')
screenshots=pull_screenshots()
page_size=adb('shell', 'getconf', 'PAGE_SIZE').strip()
(args.output/'page-size.txt').write_text(page_size, encoding='utf-8')
if args.expect_16k and page_size != '16384': raise SystemExit('Expected a real 16 KiB-page runtime')
if not re.search(r'OK \(\d+ tests?\)', result) or 'FAILURES' in result or 'INSTRUMENTATION_FAILED' in result:
    print(result)
    raise SystemExit('Instrumentation did not pass')
print(result)
if screenshots < 2: raise SystemExit('Missing required development screenshots')
if args.display_checks:
    try:
        # TV non-native-4K windows retain a 1080p logical surface. Keep the
        # TV's 320 dpi logical density when exercising a 2160p output mode.
        for label, size, density, scale in [('720p','1280x720','213','1.0'), ('4k-output','3840x2160','320','1.0'),
                                           ('large-text-contrast','1920x1080','320','1.3')]:
            adb('shell','wm','size',size)
            adb('shell','wm','density',density)
            adb('shell','settings','put','system','font_scale',scale)
            (args.output/f'{label}-display.txt').write_text(adb('shell','dumpsys','display'),encoding='utf-8')
            capture=adb('shell','am','instrument','-w','-r','-e','class',
                'com.still.launcher.LauncherDeviceTest#captureRealEmulatorScreensAndTrimMemory',
                '-e','captureLabel',label,'-e','captureContrast',str(scale!='1.0').lower(),
                'com.still.launcher.debug.test/androidx.test.runner.AndroidJUnitRunner')
            (args.output/f'{label}-test.txt').write_text(capture,encoding='utf-8')
            if not re.search(r'OK \(1 test\)',capture): raise SystemExit('Display check failed: '+capture)
    finally:
        adb('shell','wm','size','reset')
        adb('shell','wm','density','reset')
        adb('shell','settings','put','system','font_scale','1.0')
        pull_screenshots()
