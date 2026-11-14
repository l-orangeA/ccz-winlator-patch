#!/usr/bin/env python3
"""Clean boot-phase timing: kill leftover proot children first.

Key discovery (2026-11-09 23:1x): `am force-stop com.winlatcn` does NOT kill
the proot children (wineserver / winedevice / Ekd5.exe). Repeated "restarts"
stacked multiple shell instances, which looked like an unfixable freeze.
`run-as com.winlatcn pkill -f ...` does kill them (same uid).

Phases measured:
  wineserver  - container/Wine server up
  ekd5        - shell exe process
  unpack      - hwbp_stats shows arms=1600 (VProtect unpack finished)
  title       - screenshot > 900 KB twice
"""
import json
import os
import re
import subprocess
import sys
import time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
BASE = os.path.dirname(os.path.abspath(__file__))
LABEL = sys.argv[1] if len(sys.argv) > 1 else "clean"


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def pidof(n):
    return txt(sh(["shell", "pidof " + n])).strip()


def shot():
    return len(sh(["exec-out", "screencap -p"], 60).stdout)


def hwbp():
    return txt(sh(["shell", "run-as com.winlatcn cat files/imagefs/home/xuser-1/hwbp_stats.txt"])).strip()


def clean():
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", "run-as com.winlatcn sh -c 'pkill -f Ekd5.exe; pkill -f wineserver; pkill -f winedevice.exe'"])
    time.sleep(2)
    left = [n for n in ("wineserver", "Ekd5.exe") if pidof(n)]
    sh(["shell", "run-as com.winlatcn rm -f files/imagefs/home/xuser-1/hwbp_stats.txt"])
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    return left


def main():
    left = clean()
    print("CLEAN left=%s" % (left or "none"), flush=True)
    t0 = time.time()
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
    marks = {}
    hits = 0
    last = -99
    last_hw = ""
    while time.time() - t0 < 200:
        now = time.time() - t0
        if "ws" not in marks and pidof("wineserver"):
            marks["ws"] = round(now, 1)
            print("  t=%.1f wineserver" % now, flush=True)
        if "ekd" not in marks and pidof("Ekd5.exe"):
            marks["ekd"] = round(now, 1)
            print("  t=%.1f ekd5" % now, flush=True)
        h = hwbp()
        if h and h != last_hw:
            last_hw = h
            print("  t=%.1f hwbp: %s" % (now, h), flush=True)
            if "arms=1600" in h and "unpack" not in marks:
                marks["unpack"] = round(now, 1)
                print("  t=%.1f UNPACK DONE (arms=1600)" % now, flush=True)
        if now - last >= 2:
            n = shot()
            last = now
            if n > 900000:
                hits += 1
                print("  t=%.1f title-ish %d" % (now, n), flush=True)
                if hits >= 2:
                    marks["title"] = round(now, 1)
                    break
            else:
                hits = 0
        time.sleep(0.5)
    elapsed = time.time() - t0
    res = {"label": LABEL, "elapsed": round(elapsed, 1), "marks": marks, "hwbp_last": last_hw}
    out = os.path.join(BASE, "winlator11", "boot_clean_%s.json" % LABEL)
    open(out, "w", encoding="utf-8").write(json.dumps(res, ensure_ascii=False, indent=2))
    print("RESULT", json.dumps(res, ensure_ascii=False), flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
