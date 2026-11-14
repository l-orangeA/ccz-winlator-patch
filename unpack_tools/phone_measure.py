#!/usr/bin/env python3
"""Enter battle on the phone and measure the frame-rate / affinity baseline.

State detection uses screenshot byte-size bands calibrated from
fps_probe_120.txt (same phone, same save):
  title   ~971-974 KB | dialog/confirm ~634-643 KB | scene ~2.57 MB
  loading/other <200 KB
This replaces auto_load.py's missing no1.png / tap8.png references.
Run phone_boot.py first (title screen must already be up).
"""
import collections
import os
import statistics
import subprocess
import sys
import threading
import time
from PIL import Image

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
BASE = os.path.dirname(os.path.abspath(__file__))
TMP = os.path.join(BASE, "phone_measure_state.png")
SHOTS = os.path.join(BASE, "winlator11")


def sh(a, t=90, inp=None):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True,
                          env=ENV, timeout=t, input=inp)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def grab(save=None):
    b = sh(["exec-out", "screencap -p"], 60).stdout
    open(TMP, "wb").write(b)
    if save:
        open(os.path.join(SHOTS, save), "wb").write(b)
    return b


def state(save=None):
    b = grab(save)
    n = len(b)
    if n < 10000:
        s = "off"
    elif n > 1100000:
        s = "scene"
    elif 900000 < n < 1100000:
        s = "title"
    elif 500000 < n < 800000:
        s = "dialog"
    elif n < 200000:
        s = "loading"
    else:
        s = "other"
    return s, n


def tap(x, y, gap=1.2):
    sh(["shell", "input tap %d %d" % (x, y)])
    time.sleep(gap)


def layer():
    out = txt(sh(["shell", "dumpsys SurfaceFlinger --list"]))
    for line in out.splitlines():
        if "SurfaceView[" in line and "(BLAST)" in line and "XServerDisplayActivity" in line:
            i = line.find("SurfaceView[")
            j = line.find(" parentId")
            return (line[i:j] if i >= 0 and j > i else line).strip()
    return None


def fps(seconds=10):
    name = layer()
    if not name:
        print("no layer")
        return None
    print("LAYER", name, flush=True)
    rows = []
    end = time.time() + seconds
    while time.time() < end:
        raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "' + name + '"']))
        rr = []
        for line in raw.splitlines():
            p = line.split()
            if len(p) == 3:
                try:
                    v = tuple(int(x) for x in p)
                except ValueError:
                    continue
                if 1e12 < v[1] < 1e18:  # filter LONG_MAX sentinel
                    rr.append(v[1])
        rows = rr
        time.sleep(2)
    if len(rows) < 2:
        print("no rows")
        return None
    d = [(rows[i + 1] - rows[i]) / 1e6 for i in range(len(rows) - 1)]
    d = [x for x in d if 0 < x < 1000]
    span = sum(d) / 1000
    b = collections.Counter("<20" if x < 20 else "<40" if x < 40 else
                            "<80" if x < 80 else ">=80" for x in d)
    print("FPS frames=%d span=%.2fs fps=%.1f median=%.2f p90=%.2f max=%.1f %s" % (
        len(d) + 1, span, len(d) / span if span else 0, statistics.median(d),
        sorted(d)[int(len(d) * .9)], max(d), dict(b)), flush=True)
    return len(d) / span if span else None


def motion_worker(stop, secs):
    if "--no-motion" in sys.argv:
        return
    """Keep the pointer moving so the game actually produces frames.

    Historical baseline (README 9.1) used 12s of continuous mouse movement;
    an idle battle scene emits almost no content and --latency then mixes in
    very old timestamps, producing a meaningless ~11 fps.
    """
    t0 = time.time()
    while time.time() - t0 < secs and not stop.is_set():
        sh(["shell", "input swipe 1400 600 1650 600 120"], 15)
        sh(["shell", "input swipe 1650 600 1400 600 120"], 15)


def stamps(name):
    raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "' + name + '"']))
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


def fps_motion(seconds=12, motion=True):
    name = layer()
    if not name:
        print("no layer")
        return None
    print("LAYER", name, flush=True)
    sh(["shell", "dumpsys SurfaceFlinger --latency-clear"])
    prev = 0
    all_ts = []
    per_sec = []
    stop = threading.Event()
    th = None
    if motion:
        th = threading.Thread(target=motion_worker, args=(stop, seconds), daemon=True)
        th.start()
    t0 = time.time()
    while time.time() - t0 < seconds:
        ts = stamps(name)
        new = [t for t in ts if t > prev]
        if new:
            all_ts.extend(new)
            prev = max(new)
        per_sec.append(len(new))
        print("  t=%.1f +%d frames" % (time.time() - t0, len(new)), flush=True)
        time.sleep(1)
    stop.set()
    if th:
        th.join(timeout=3)
    if len(all_ts) < 2:
        print("no rows")
        return None
    d = [(all_ts[i + 1] - all_ts[i]) / 1e6 for i in range(len(all_ts) - 1)]
    d = [x for x in d if 0 < x < 1000]
    b = collections.Counter("<20" if x < 20 else "<40" if x < 40 else
                            "<80" if x < 80 else ">=80" for x in d)
    print("MOTION per_sec=%s avg=%.1f" % (per_sec, sum(per_sec) / len(per_sec)), flush=True)
    print("FPS frames=%d fps=%.1f median=%.2f p90=%.2f max=%.1f %s" % (
        len(d) + 1, 1000 / (sum(d) / len(d)) if d else 0,
        statistics.median(d), sorted(d)[int(len(d) * .9)], max(d), dict(b)), flush=True)
    return sum(per_sec) / len(per_sec)


def cpu_rates(secs=8):
    pids = {}
    for n in ("Ekd5.exe", "libtinyproot.so", "com.winlatcn", "wineserver"):
        o = txt(sh(["shell", "pidof " + n])).split()
        if o:
            pids[n] = o[0]

    def jj(p):
        s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
        f = s.rsplit(")", 1)[1].split()
        return int(f[11]) + int(f[12])

    t0 = {n: jj(p) for n, p in pids.items()}
    time.sleep(secs)
    for n, p in pids.items():
        print("  %-14s %.2f core cpus=%s" % (
            n, (jj(p) - t0[n]) / secs / 100,
            txt(sh(["shell", "grep Cpus_allowed_list /proc/%s/status" % p]))
            .strip().split(":")[-1].strip()), flush=True)


def main():
    grab()
    im = Image.open(TMP)
    print("screen=%s" % (im.size,), flush=True)
    s, n = state("measure_00_title.png")
    print("start state=%s shot=%d" % (s, n), flush=True)
    if "--measure-only" in sys.argv:
        if s != "scene":
            print("NOT IN SCENE - inspect measure_00_title.png")
            return 3
        fps_motion(12, motion="--no-motion" not in sys.argv)
        cpu_rates(8)
        print("DONE", flush=True)
        return 0
    if s == "scene":
        print("already in scene")
    else:
        # open the load-save menu (historical phone-landscape coords)
        for i in range(6):
            tap(1785, 595)
            s, n = state("measure_1%d_dialog.png" % i)
            print("  tap%d -> %s (%d)" % (i + 1, s, n), flush=True)
            if s in ("dialog", "scene"):
                break
        if s == "scene":
            pass
        else:
            # dismiss any "confirm?" popup with 否, then select a save slot
            tap(1547, 686)
            time.sleep(1)
            for i in range(4):
                tap(1400, 806)
                s, n = state("measure_2%d_slot.png" % i)
                print("  slot%d -> %s (%d)" % (i + 1, s, n), flush=True)
                if s == "dialog":
                    tap(1344, 686)  # 是
                    time.sleep(5)
                    s, n = state("measure_3%d_confirm.png" % i)
                    print("  confirm%d -> %s (%d)" % (i + 1, s, n), flush=True)
                if s == "scene":
                    break
    s, n = state("measure_final.png")
    print("FINAL state=%s shot=%d" % (s, n), flush=True)
    if s != "scene":
        print("NOT IN SCENE - inspect measure_final.png")
        return 3
    time.sleep(2)
    fps_motion(12, motion="--no-motion" not in sys.argv)
    cpu_rates(8)
    print("DONE", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
