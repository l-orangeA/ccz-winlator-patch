#!/usr/bin/env python3
"""Long read-only probe covering idle -> auto-battle animation -> idle.

No input injection. The user clicks 自动战斗 manually during the run.
Judges long-tail smoothness with:
  * SurfaceFlinger --latency on the BLAST child layer (per-second new frames,
    frame-interval p50/p90/p99/max, >=80ms spikes)
  * dumpsys gfxinfo snapshots (Total frames, Janky frames, Missed Vsync,
    90/95/99th percentiles) - deltas over the run
  * /proc/<pid>/stat CPU cores per 10s segment
  * frameRateOverride (display refresh actually granted to the app)

Note: cpuX scaling_cur_freq and /proc/<pid>/sched nr_migrations are NOT
readable on this device without ftrace/root; they are reported as N/A.
"""
import collections
import json
import os
import re
import statistics
import subprocess
import sys
import time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "winlator11", "long_probe_20261109.json")


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


def gfx():
    s = txt(sh(["shell", "dumpsys gfxinfo com.winlatcn"]))
    d = {}

    def num(pat):
        m = re.search(pat, s)
        return int(m.group(1)) if m else None

    d["frames"] = num(r"Total frames rendered:\s*(\d+)")
    d["janky"] = num(r"Janky frames:\s*(\d+)")
    d["missed_vsync"] = num(r"Number Missed Vsync:\s*(\d+)")
    d["slow_ui"] = num(r"Number Slow UI thread:\s*(\d+)")
    d["p90"] = num(r"90th percentile:\s*(\d+)ms")
    d["p95"] = num(r"95th percentile:\s*(\d+)ms")
    d["p99"] = num(r"99th percentile:\s*(\d+)ms")
    return d


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


def stat_block(dd):
    if not dd:
        return {"n": 0}
    q = sorted(dd)
    return {
        "n": len(dd),
        "p50": round(q[int(len(q) * .5)], 2),
        "p90": round(q[int(len(q) * .9)], 2),
        "p99": round(q[min(len(q) - 1, int(len(q) * .99))], 2),
        "max": round(max(q), 2),
        "ge80": sum(1 for x in dd if x >= 80),
        "ge100": sum(1 for x in dd if x >= 100),
    }


def main(seconds=180, seg=10):
    layer = find_layer()
    if not layer:
        print("NO LAYER")
        return 2
    d = pids()
    print("LAYER", layer, flush=True)
    print("PIDS", d, flush=True)
    print("OVERRIDE_START", override(), flush=True)
    g0 = gfx()
    print("GFX_START", g0, flush=True)
    print("PROBE START %ds - click 自动战斗 now" % seconds, flush=True)

    sh(["shell", "dumpsys SurfaceFlinger --latency-clear"])
    t0 = {n: ticks(p) for n, p in d.items()}
    per_sec, all_ts, prev = [], [], 0
    segments = []
    seg_ts, seg_start = [], time.time()
    start = time.time()
    next_seg = seg
    while time.time() - start < seconds:
        ts = stamps(layer)
        new = [t for t in ts if t > prev]
        if ts:
            prev = max(prev, max(ts))
        all_ts.extend(new)
        seg_ts.extend(new)
        per_sec.append(len(new))
        el = time.time() - start
        if el >= next_seg:
            dd = [(seg_ts[i + 1] - seg_ts[i]) / 1e6 for i in range(len(seg_ts) - 1)]
            dd = [x for x in dd if 0 < x < 1000]
            now_ticks = {n: ticks(p) for n, p in d.items()}
            cores = {}
            for n, p in d.items():
                if now_ticks[n] is not None and t0.get(n) is not None:
                    cores[n] = round((now_ticks[n] - t0[n]) / seg / 100, 2)
            t0 = now_ticks
            s = {"t": round(el, 1), "frames": len(seg_ts),
                 "active_sec": sum(1 for x in per_sec[-seg:] if x > 0),
                 "intervals": stat_block(dd), "cores": cores}
            segments.append(s)
            print("SEG %s" % json.dumps(s, ensure_ascii=False), flush=True)
            seg_ts = []
            next_seg += seg
        time.sleep(max(0, 1 - (time.time() - start - int(el))))
    elapsed = time.time() - start
    dd = [(all_ts[i + 1] - all_ts[i]) / 1e6 for i in range(len(all_ts) - 1)]
    dd = [x for x in dd if 0 < x < 1000]
    g1 = gfx()
    result = {
        "elapsed": round(elapsed, 1),
        "per_sec": per_sec,
        "total_frames": len(all_ts),
        "active_sec": sum(1 for x in per_sec if x > 0),
        "fps_active": round(sum(per_sec) / max(1, sum(1 for x in per_sec if x > 0)), 1),
        "fps_wall": round(sum(per_sec) / elapsed, 1),
        "intervals": stat_block(dd),
        "gfx_start": g0, "gfx_end": g1,
        "gfx_delta": {k: (g1[k] - g0[k]) if (g1.get(k) is not None and g0.get(k) is not None) else None
                      for k in ("frames", "janky", "missed_vsync", "slow_ui")},
        "override_end": override(),
        "segments": segments,
    }
    open(OUT, "w", encoding="utf-8").write(json.dumps(result, ensure_ascii=False, indent=2))
    print("SUMMARY", json.dumps({k: v for k, v in result.items() if k != "segments"},
                                ensure_ascii=False), flush=True)
    print("SAVED", OUT, flush=True)
    print("DONE", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main(int(sys.argv[1]) if len(sys.argv) > 1 else 180))
