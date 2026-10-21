"""Collect repeatable startup and memory evidence on an explicitly selected device.

Force-stops the selected app between samples. Use only on a test device.
Emulator results are diagnostic and must not be advertised as physical TV results.
"""
import argparse
import json
from pathlib import Path
import re
import statistics
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--package', default='com.still.launcher')
parser.add_argument('--samples', type=int, default=5)
parser.add_argument('--output', type=Path, default=Path('benchmark'))
args = parser.parse_args()
if not 3 <= args.samples <= 20: parser.error('Use 3–20 samples')
if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+', args.package): parser.error('Invalid package')
args.output.mkdir(parents=True, exist_ok=True)
def adb(*command):
    return subprocess.run(['adb','-s',args.serial,*command],check=True,capture_output=True,text=True).stdout

samples = []
for index in range(args.samples):
    adb('shell','am','force-stop',args.package)
    output = adb('shell','am','start','-W','-n',args.package+'/com.still.launcher.MainActivity')
    (args.output/f'start-{index+1}.txt').write_text(output,encoding='utf-8')
    match = re.search(r'TotalTime:\s*(\d+)',output)
    if not match or 'Error:' in output: raise SystemExit('App did not start: '+output)
    samples.append(int(match.group(1)))
    time.sleep(2)
memory = adb('shell','dumpsys','meminfo',args.package)
(args.output/'memory.txt').write_text(memory,encoding='utf-8')
(args.output/'graphics.txt').write_text(adb('shell','dumpsys','gfxinfo',args.package),encoding='utf-8')
crashes = adb('shell','logcat','-d','-s','AndroidRuntime:E')
(args.output/'runtime-errors.txt').write_text(crashes,encoding='utf-8')
if args.package in crashes and 'FATAL EXCEPTION' in crashes: raise SystemExit('Runtime crash found; inspect evidence')
result = {'package':args.package,'model':adb('shell','getprop','ro.product.model').strip(),
    'sdk':adb('shell','getprop','ro.build.version.sdk').strip(),
    'low_ram':adb('shell','getprop','ro.config.low_ram').strip(),
    'page_size':adb('shell','getconf','PAGE_SIZE').strip(),
    'cold_start_ms':samples,'median_cold_start_ms':statistics.median(samples),
    'method':'am force-stop + am start -W, TotalTime; 2s settling; no installed-app data clearing'}
if not result['page_size'].isdigit(): result['page_size']='unknown (getconf unavailable)'
(args.output/'summary.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result,indent=2))
