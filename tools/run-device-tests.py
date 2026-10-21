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
parser.add_argument('--display-checks', action='store_true')
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
(args.output/'system-log.txt').write_text(adb('shell', 'logcat', '-d'), encoding='utf-8')
subprocess.run(['adb', '-s', args.serial, 'pull',
    '/sdcard/Android/data/com.still.launcher.debug/files/screenshots', str(args.output/'screenshots')], check=False)
(args.output/'page-size.txt').write_text(adb('shell', 'getconf', 'PAGE_SIZE'), encoding='utf-8')
if not re.search(r'OK \(\d+ tests?\)', result) or 'FAILURES' in result or 'INSTRUMENTATION_FAILED' in result:
    print(result)
    raise SystemExit('Instrumentation did not pass')
print(result)
if args.display_checks:
    try:
        for label, size, density, scale in [('720p','1280x720','213','1.0'), ('4k','3840x2160','640','1.0'),
                                           ('large-text-contrast','1920x1080','320','1.3')]:
            adb('shell','wm','size',size)
            adb('shell','wm','density',density)
            adb('shell','settings','put','system','font_scale',scale)
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
        subprocess.run(['adb','-s',args.serial,'pull','/sdcard/Android/data/com.still.launcher.debug/files/screenshots',str(args.output/'screenshots')],check=False)
