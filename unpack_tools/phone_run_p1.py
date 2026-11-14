#!/usr/bin/env python3
"""End-to-end P1 run on the phone, no human in the loop.

restart app (so prefs take effect) -> boot container -> wait title ->
enter battle -> click 结束所有部队的命令 (mouse hovers it at 1110,100) ->
confirm 是 (1344,686) -> 60s rolling-window FPS/interval probe.

Env:
  P1_CPULIST   target cpuListWoW64 (default 4,5,6,7 = baseline)
  WL_SER       device serial
Keeps move_cursor_to_touchpoint=true so adb taps land; caller restores it.
"""
import json
import os
import statistics
import subprocess
import sys
import time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"
PREF = "shared_prefs/com.winlatcn_preferences.xml"
BASE = os.path.dirname(os.path.abspath(__file__))
TMP = os.path.join(BASE, "p1_run_state.png")
TARGET = os.environ.get("P1_CPULIST", "4,5,6,7")


def sh(a, t=90, inp=None):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True,
                          env=ENV, timeout=t, input=inp)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def state(save=None):
    b = sh(["exec-out", "screencap -p"], 60).stdout
    open(TMP, "wb").write(b)
    if save:
        open(os.path.join(BASE, "winlator11", save), "wb").write(b)
    n = len(b)
    if n < 10000:
        return "off", n
    if n > 1100000:
        return "scene", n
    if 900000 < n < 1100000:
        return "title", n
    if 500000 < n < 800000:
        return "dialog", n
    if n < 200000:
        return "loading", n
    return "other", n


def tap(x, y, gap=1.2):
    sh(["shell", "input tap %d %d" % (x, y)])
    time.sleep(gap)


def set_pref(value):
    xml = txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", PREF]))
    if not xml.strip():
        return False
    key = 'move_cursor_to_touchpoint" value="%s"' % value
    other = 'move_cursor_to_touchpoint" value="%s"' % ("false" if value == "true" else "true")
    if key in xml:
        return True
    if other in xml:
        xml = xml.replace(other, key)
    else:
        xml = xml.replace("</map>",
                          '    <boolean name="move_cursor_to_touchpoint" value="%s" />\n</map>' % value)
    sh(["shell", "run-as com.winlatcn sh -c 'cat > " + PREF + "'"], inp=xml.encode())
    back = txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", PREF]))
    return key in back


def set_cfg(field, value):
    d = json.loads(txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", CFG])))
    old = d.get(field)
    if old == value:
        return old
    d[field] = value
    sh(["shell", "run-as com.winlatcn sh -c 'cat > " + CFG + "'"],
       inp=json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    back = json.loads(txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", CFG])))
    assert back.get(field) == value, "cfg write failed"
    return old


def launch():
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; '
                  'rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])


def wait_title(maxs=230):
    t0 = time.time()
    hits = 0
    while time.time() - t0 < maxs:
        s, n = state()
        if s == "title":
            hits += 1
            if hits >= 2:
                print("TITLE t=%.1f" % (time.time() - t0), flush=True)
                return True
        else:
            hits = 0
        time.sleep(3)
    print("NO TITLE")
    return False


def enter_battle():
    s = "other"
    for i in range(6):
        tap(1785, 595)
        s, n = state()
        print("  tap%d -> %s (%d)" % (i + 1, s, n), flush=True)
        if s in ("dialog", "scene"):
            break
    if s == "scene":
        return True
    tap(1547, 686)
    time.sleep(1)
    for i in range(4):
        tap(1400, 806)
        s, n = state()
        print("  slot%d -> %s (%d)" % (i + 1, s, n), flush=True)
        if s == "dialog":
            tap(1344, 686)
            time.sleep(5)
            s, n = state()
            print("  confirm%d -> %s (%d)" % (i + 1, s, n), flush=True)
        if s == "scene":
            return True
    return False


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
            if 1e12 < v[1] < 1e18:  # LONG_MAX sentinel filter
                out.append(v[1])
    return out


def probe(seconds=60):
    layer = find_layer()
    print("LAYER", layer, flush=True)
    prev, per, allts = 0, [], []
    for i in range(seconds):
        ts = stamps(layer)
        new = [t for t in ts if t > prev]
        if ts:
            prev = max(prev, max(ts))
        allts.extend(new)
        per.append(len(new))
        print("  t=%2d +%3d" % (i + 1, len(new)), flush=True)
        time.sleep(1)
    d = [(allts[i + 1] - allts[i]) / 1e6 for i in range(len(allts) - 1)]
    d = [x for x in d if 0 < x < 1000]
    q = sorted(d) if d else []
    res = {
        "per_sec": per,
        "avg_fps": round(sum(per) / len(per), 1),
        "active_avg": round(sum(per) / max(1, sum(1 for x in per if x > 0)), 1),
        "active_sec": sum(1 for x in per if x > 0),
        "intervals": {
            "n": len(d),
            "p50": round(q[len(q) // 2], 2) if d else None,
            "p90": round(q[int(len(q) * .9)], 2) if d else None,
            "p99": round(q[min(len(q) - 1, int(len(q) * .99))], 2) if d else None,
            "max": round(max(d), 2) if d else None,
            "ge80": sum(1 for x in d if x >= 80),
            "ge100": sum(1 for x in d if x >= 100),
        },
    }
    print("RESULT", json.dumps(res, ensure_ascii=False), flush=True)
    return res


def main():
    old = set_cfg("cpuListWoW64", TARGET)
    print("CFG cpuListWoW64 %s -> %s" % (old, TARGET), flush=True)
    assert set_pref("true"), "pref write failed"
    print("PREF touchpoint=true", flush=True)
    launch()
    if not wait_title():
        return 2
    if not enter_battle():
        print("ENTER BATTLE FAILED")
        return 3
    time.sleep(2)
    print("=== click auto-battle (1110,100) ===", flush=True)
    tap(1110, 100)
    time.sleep(1.5)
    s, n = state("p1_auto_dialog.png")
    print("after click: %s (%d)" % (s, n), flush=True)
    if s == "dialog":
        tap(1344, 686)
        print("confirmed 是", flush=True)
        time.sleep(2)
    res = probe(60)
    out = os.path.join(BASE, "winlator11", "p1_%s_60s.json" % TARGET.replace(",", "-"))
    open(out, "w", encoding="utf-8").write(json.dumps(
        {"cpuListWoW64": TARGET, "result": res}, ensure_ascii=False, indent=2))
    print("SAVED", out, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
