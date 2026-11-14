#!/usr/bin/env python3
"""Final long read-only probe: idle -> auto-battle -> idle.

Adds two safeguards over phone_long_probe.py:
  * screenshot byte-size sampling every 5s (tells us whether the picture is
    actually changing even when SurfaceFlinger --latency reports 0 frames)
  * re-resolve the BLAST layer every 10s (surface may be recreated)
No input injection.
"""
import hashlib
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
OUT = os.path.join(BASE, "winlator11", "final_probe_20261109.json")


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True,
                          env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def find_layer():
    out = txt(sh(["shell", "dumpsys SurfaceFlinger --list"]))
    lines = [l.strip() for l in out.splitlines()
             if "SurfaceView[" in l and "XServerDisplayActivity" in l]
    hits = [l for l in lines if "(BLAST)" in l] or lines
    if not hits:
        return None
    i = hits[0].find("SurfaceView[")
    j = hits[0].find(" parentId")
    return hits[0][i:j] if i >= 0 and j > i else hits[0]


def stamps(layer):
    raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "' + layer + '"']))
    out = []
    for line in raw.splitlines():
        p = line.split()
        if len(p) == 3:
            try:
                v = tuple(int(x) for x in p)
            except ValueError:
                continue
            if 1e12 < v[1] < 1e18:  # filter LONG_MAX sentinel
                out.append(v[1])
    return out


def screen():
    b = sh(["exec-out", "screencap -p"], 60).stdout
    return len(b), hashlib.md5(b).hexdigest()[:8] if b else ""


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


def override():
    s = txt(sh(["shell", "dumpsys display"]))
    m = re.search(r"frameRateOverride \{[^}]*\}", s)
    return m.group(0) if m else "none"


def intervals(ts):
    d = [(ts[i + 1] - ts[i]) / 1e6 for i in range(len(ts) - 1)]
    d = [x for x in d if 0 < x < 1000]
    if not d:
        return {"n": 0}
    q = sorted(d)
    return {"n": len(d), "p50": round(q[len(q) // 2], 2),
            "p90": round(q[int(len(q) * .9)], 2),
            "p99": round(q[min(len(q) - 1, int(len(q) * .99))], 2),
            "max": round(max(d), 2),
            "ge80": sum(1 for x in d if x >= 80),
            "ge100": sum(1 for x in d if x >= 100)}


def main(seconds=180, seg=10):
    layer = find_layer()
    if not layer:
        print("NO LAYER")
        return 2
    d = pids()
    print("LAYER", layer, flush=True)
    print("PIDS", d, flush=True)
    print("OVERRIDE", override(), flush=True)
    print("PROBE START %ds (no input injected)" % seconds, flush=True)

    sh(["shell", "dumpsys SurfaceFlinger --latency-clear"])
    t0 = {n: ticks(p) for n, p in d.items()}
    per_sec, all_ts, prev = [], [], 0
    seg_ts = []
    screens = []
    segments = []
    start = time.time()
    next_seg, next_screen = seg, 5
    while time.time() - start < seconds:
        el = time.time() - start
        ts = stamps(layer)
        new = [t for t in ts if t > prev]
        if ts:
            prev = max(prev, max(ts))
        all_ts.extend(new)
        seg_ts.extend(new)
        per_sec.append(len(new))

        if el >= next_screen:
            n, h = screen()
            screens.append({"t": round(el, 1), "bytes": n, "md5": h})
            print("  SCREEN t=%.1f bytes=%d md5=%s" % (el, n, h), flush=True)
            next_screen += 5

        if el >= next_seg:
            nl = find_layer()
            if nl and nl != layer:
                print("  LAYER CHANGED %s -> %s" % (layer, nl), flush=True)
                layer = nl
                sh(["shell", "dumpsys SurfaceFlinger --latency-clear"])
                prev = 0
            now_ticks = {n: ticks(p) for n, p in d.items()}
            cores = {n: round((now_ticks[n] - t0[n]) / seg / 100, 2)
                     for n, p in d.items()
                     if now_ticks[n] is not None and t0.get(n) is not None}
            t0 = now_ticks
            recent = [s["bytes"] for s in screens[-2:]] if len(screens) >= 2 else []
            s = {"t": round(el, 1), "frames": len(seg_ts),
                 "active_sec": sum(1 for x in per_sec[-seg:] if x > 0),
                 "intervals": intervals(seg_ts), "cores": cores,
                 "screen_bytes": recent}
            segments.append(s)
            print("SEG %s" % json.dumps(s, ensure_ascii=False), flush=True)
            seg_ts = []
            next_seg += seg
        time.sleep(0.4)
    elapsed = time.time() - start
    sizes = [s["bytes"] for s in screens]
    result = {
        "elapsed": round(elapsed, 1),
        "total_frames": len(all_ts),
        "active_sec": sum(1 for x in per_sec if x > 0),
        "fps_active": round(sum(per_sec) / max(1, sum(1 for x in per_sec if x > 0)), 1),
        "per_sec": per_sec,
        "intervals": intervals(all_ts),
        "screen_bytes_min": min(sizes) if sizes else None,
        "screen_bytes_max": max(sizes) if sizes else None,
        "screen_md5_unique": len(set(s["md5"] for s in screens)),
        "screen_samples": screens,
        "override_end": override(),
        "segments": segments,
    }
    open(OUT, "w", encoding="utf-8").write(
        json.dumps(result, ensure_ascii=False, indent=2))
    print("SUMMARY", json.dumps({k: v for k, v in result.items()
                                 if k not in ("segments", "screen_samples", "per_sec")},
                                ensure_ascii=False), flush=True)
    print("SAVED", OUT, flush=True)
    print("DONE", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main(int(sys.argv[1]) if len(sys.argv) > 1 else 180))
