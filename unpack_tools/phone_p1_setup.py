#!/usr/bin/env python3
"""P1 single-variable setup on the 8.0 production phone.

Baseline: cpuListWoW64 = 4,5,6,7 (verified effective: Ekd5 Cpus_allowed_list=4-7).
Variant : cpuListWoW64 = 7  (pin the game to the prime core, remove migration).

Steps: read live .container -> change ONLY that field -> write back ->
restart container -> wait for title -> click through to battle -> restore
mouse prefs. No FPS measurement here (user clicks 自动战斗, then we probe).
"""
import json
import os
import subprocess
import sys
import time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"
PREF = "shared_prefs/com.winlatcn_preferences.xml"
TARGET = os.environ.get("P1_CPULIST", "7")
BASE = os.path.dirname(os.path.abspath(__file__))
TMP = os.path.join(BASE, "p1_state.png")


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
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(1)
    xml = txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", PREF]))
    if not xml.strip():
        return
    key = 'move_cursor_to_touchpoint" value="%s"' % value
    other = 'move_cursor_to_touchpoint" value="%s"' % ("false" if value == "true" else "true")
    if key in xml:
        return
    if other in xml:
        xml = xml.replace(other, key)
    else:
        xml = xml.replace("</map>",
                          '    <boolean name="move_cursor_to_touchpoint" value="%s" />\n</map>' % value)
    sh(["shell", "run-as", "com.winlatcn", "sh -c 'cat > " + PREF + "'"],
       inp=xml.encode())


def read_cfg():
    return json.loads(txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", CFG])))


def write_cfg(d):
    payload = json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode()
    sh(["shell", "run-as", "com.winlatcn", "sh -c 'cat > " + CFG + "'"], inp=payload)


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
                print("TITLE t=%.1f shot=%d" % (time.time() - t0, n), flush=True)
                return True
        else:
            hits = 0
        time.sleep(3)
    print("NO TITLE")
    return False


def enter_battle():
    for i in range(6):
        tap(1785, 595)
        s, n = state("p1_dialog_%d.png" % i)
        print("  tap%d -> %s (%d)" % (i + 1, s, n), flush=True)
        if s in ("dialog", "scene"):
            break
    if s == "scene":
        return True
    tap(1547, 686)
    time.sleep(1)
    for i in range(4):
        tap(1400, 806)
        s, n = state("p1_slot_%d.png" % i)
        print("  slot%d -> %s (%d)" % (i + 1, s, n), flush=True)
        if s == "dialog":
            tap(1344, 686)
            time.sleep(5)
            s, n = state("p1_confirm_%d.png" % i)
            print("  confirm%d -> %s (%d)" % (i + 1, s, n), flush=True)
        if s == "scene":
            return True
    return False


def main():
    d = read_cfg()
    old = d.get("cpuListWoW64")
    print("BASELINE cpuListWoW64=%s -> VARIANT %s" % (old, TARGET), flush=True)
    d["cpuListWoW64"] = TARGET
    write_cfg(d)
    back = read_cfg()
    assert back["cpuListWoW64"] == TARGET, "config write failed"
    print("CONFIG OK (only cpuListWoW64 changed)", flush=True)

    set_pref("true")
    launch()
    if not wait_title():
        set_pref("false")
        return 2
    ok = enter_battle()
    s, n = state("p1_final.png")
    set_pref("false")
    print("FINAL state=%s shot=%d enter_battle=%s" % (s, n, ok), flush=True)
    print("READY - click 自动战斗, then run the 60s probe", flush=True)
    return 0 if ok else 3


if __name__ == "__main__":
    sys.exit(main())
