#!/usr/bin/env python3
"""Read-only frame-rate / CPU probe for the currently running battle.

No input injection, no container restart, no clicks. Just:
  dumpsys SurfaceFlinger --latency-clear + --latency <layer>  (per-second new frames)
  /proc/<pid>/stat CPU ticks, Cpus_allowed_list
  dumpsys power / display (wakefulness, frameRateOverride)
Use while a battle is already running (user opened it manually).
"""
import collections
import os
import statistics
import subprocess
import sys
import time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True,
                          env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def find_layer():
    out = txt(sh(["shell", "dumpsys SurfaceFlinger --list"]))
    lines = [l.strip() for l in out.splitlines()
             if "SurfaceView[" in l and "XServerDisplayActivity" in l]
    # The BLAST child layer carries the actual frame timestamps. The parent
    # SurfaceView layer (#NNN without "(BLAST)") returns an all-zero buffer.
    hits = [l for l in lines if "(BLAST)" in l] or lines
    if not hits:
        return None
    i = hits[0].find("SurfaceView[")
    j = hits[0].find(" parentId")
    return hits[0][i:j] if i >= 0 and j > i else hits[0]


def stamps(layer):
    raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "%s"' % layer]))
    out = []
    for line in raw.splitlines():
        p = line.split()
        if len(p) == 3:
            try:
                v = tuple(int(x) for x in p)
            except ValueError:
                continue
            # SurfaceFlinger pads the ring buffer with LONG_MAX (9223372036854775807)
            # sentinel rows. Without the upper bound, max(ts) sticks at LONG_MAX
            # and every later frame is counted as "already seen" -> fake 0 fps.
            if 1e12 < v[1] < 1e18:
                out.append(v[1])
    return out


def pids():
    d = {}
    for n in ("Ekd5.exe", "com.winlatcn", "wineserver", "libtinyproot.so"):
        o = txt(sh(["shell", "pidof " + n])).split()
        if o:
            d[n] = o[0]
    return d


def ticks(p):
    s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
    try:
        f = s.rsplit(")", 1)[1].split()
        return int(f[11]) + int(f[12])
    except (IndexError, ValueError):
        return None


def main(seconds=12):
    print("wake=%s" % (txt(sh(["shell", "dumpsys power"]))
                       .split("mWakefulness=")[-1].split()[0] if
                       "mWakefulness=" in txt(sh(["shell", "dumpsys power"])) else "?"), flush=True)
    layer = find_layer()
    print("LAYER", layer, flush=True)
    if not layer:
        print("NO LAYER"); return 2
    d = pids()
    print("PIDS", d, flush=True)
    allowed = {n: txt(sh(["shell", "grep Cpus_allowed_list /proc/%s/status" % p]))
               .strip().split(":")[-1].strip() for n, p in d.items()}
    print("AFFINITY", allowed, flush=True)

    sh(["shell", "dumpsys SurfaceFlinger --latency-clear"])
    t0 = {n: ticks(p) for n, p in d.items()}
    per_sec, all_ts, prev = [], [], 0
    start = time.time()
    while time.time() - start < seconds:
        ts = stamps(layer)
        new = [t for t in ts if t > prev]
        if ts:
            prev = max(prev, max(ts))
        all_ts.extend(new)
        per_sec.append(len(new))
        print("  t=%.1f +%d frames" % (time.time() - start, len(new)), flush=True)
        time.sleep(1)
    elapsed = time.time() - start
    print("MOTION free / per_sec=%s avg=%.1f" % (per_sec, sum(per_sec) / len(per_sec)), flush=True)
    if len(all_ts) > 1:
        dd = [(all_ts[i + 1] - all_ts[i]) / 1e6 for i in range(len(all_ts) - 1)]
        dd = [x for x in dd if 0 < x < 1000]
        if dd:
            b = collections.Counter("<20" if x < 20 else "<40" if x < 40 else
                                    "<80" if x < 80 else ">=80" for x in dd)
            print("FPS frames=%d avg=%.1f median=%.2f p90=%.2f max=%.1f %s" % (
                len(dd) + 1, 1000 / (sum(dd) / len(dd)), statistics.median(dd),
                sorted(dd)[int(len(dd) * .9)], max(dd), dict(b)), flush=True)
    for n, p in d.items():
        t1 = ticks(p)
        if t1 is not None and t0.get(n) is not None:
            print("  %-14s %.2f core" % (n, (t1 - t0[n]) / elapsed / 100), flush=True)
    disp = txt(sh(["shell", "dumpsys display"]))
    for line in disp.splitlines():
        if "frameRateOverride" in line:
            import re
            m = re.search(r"frameRateOverride \{[^}]*\}", line)
            if m:
                print("OVERRIDE", m.group(0), flush=True)
                break
    print("DONE", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main(int(sys.argv[1]) if len(sys.argv) > 1 else 12))
