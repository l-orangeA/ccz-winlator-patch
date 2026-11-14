#!/usr/bin/env python3
"""Boot-phase timing for the 8.0 production container on the phone.

Measures, from the `am start` call:
  t_activity   XServerDisplayActivity displayed (logcat)
  t_wineserver wineserver process appears
  t_ekd5       Ekd5.exe process appears
  t_title      title screen detected twice (screenshot > 900 KB)

Usage: python phone_boot_timing.py [label]
Writes winlator11/boot_timing_<label>.json
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
LABEL = sys.argv[1] if len(sys.argv) > 1 else "run"


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True,
                          env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def pidof(name):
    return txt(sh(["shell", "pidof " + name])).strip()


def shot_bytes():
    return len(sh(["exec-out", "screencap -p"], 60).stdout)


def main():
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; '
                  'rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    sh(["logcat", "-c"])

    t0 = time.time()
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
    marks = {}
    title_hits = 0
    last_shot = -99
    seq = []
    while time.time() - t0 < 240:
        now = time.time() - t0
        if "wineserver" not in marks and pidof("wineserver"):
            marks["wineserver"] = round(now, 1)
            seq.append(("wineserver", marks["wineserver"]))
            print("  t=%.1f wineserver up" % now, flush=True)
        if "ekd5" not in marks and pidof("Ekd5.exe"):
            marks["ekd5"] = round(now, 1)
            seq.append(("ekd5", marks["ekd5"]))
            print("  t=%.1f Ekd5.exe up" % now, flush=True)
        if now - last_shot >= 2:
            n = shot_bytes()
            last_shot = now
            if n > 900000:
                title_hits += 1
                print("  t=%.1f title-ish shot=%d (hit %d)" % (now, n, title_hits), flush=True)
                if title_hits >= 2:
                    marks["title"] = round(now, 1)
                    seq.append(("title", marks["title"]))
                    break
            else:
                if title_hits:
                    print("  t=%.1f lost title (shot=%d)" % (now, n), flush=True)
                title_hits = 0
        time.sleep(0.5)
    elapsed = time.time() - t0

    # activity displayed from logcat
    log = txt(sh(["logcat", "-d", "-v", "time"]))
    m = re.search(r"(\d\d:\d\d:\d\d\.\d+).*Displayed com\.winlatcn/\.XServerDisplayActivity", log)
    print("ACTIVITY_LOG", m.group(1) if m else "not found", flush=True)

    res = {"label": LABEL, "elapsed": round(elapsed, 1), "marks": marks, "seq": seq}
    out = os.path.join(BASE, "winlator11", "boot_timing_%s.json" % LABEL)
    open(out, "w", encoding="utf-8").write(json.dumps(res, ensure_ascii=False, indent=2))
    print("RESULT", json.dumps(res, ensure_ascii=False), flush=True)
    print("SAVED", out, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
