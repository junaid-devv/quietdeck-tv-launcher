"""Run installed instrumentation without deleting screenshot evidence afterward.

Intended for CI emulators. Explicit serial required; never discovers or connects TVs.
"""
import argparse
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--artifacts', type=Path, required=True)
parser.add_argument('--output', type=Path, default=Path('device-evidence'))
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)

def adb(*command):
    return subprocess.run(['adb', '-s', args.serial, *command], check=True, capture_output=True, text=True).stdout

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
subprocess.run(['adb', '-s', args.serial, 'pull',
    '/sdcard/Android/data/com.still.launcher.debug/files/screenshots', str(args.output/'screenshots')], check=False)
(args.output/'page-size.txt').write_text(adb('shell', 'getconf', 'PAGE_SIZE'), encoding='utf-8')
if not re.search(r'OK \(\d+ tests?\)', result) or 'FAILURES' in result or 'INSTRUMENTATION_FAILED' in result:
    print(result)
    raise SystemExit('Instrumentation did not pass')
print(result)
