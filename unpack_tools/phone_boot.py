#!/usr/bin/env python3
"""Boot the 8.0 production container on the phone and stop at the title screen.

Replaces auto_load.py's REFS-based state detection (no1.png / tap8.png are
missing from the workspace). Uses MSE against auto_shot.png (title) only,
plus screenshot byte-size heuristics, and saves the title screenshot for
manual coordinate verification.
"""
import os
import subprocess
import sys
import time
from PIL import Image

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
BASE = os.path.dirname(os.path.abspath(__file__))
TMP = os.path.join(BASE, "phone_boot_state.png")
OUT = os.path.join(BASE, "winlator11", "phone_title_20261109.png")
CFG = "files/imagefs/home/xuser-1/.container"
PREF = "shared_prefs/com.winlatcn_preferences.xml"


def sh(args, t=90, inp=None):
    return subprocess.run([ADB, "-s", SER] + args, capture_output=True,
                          env=ENV, timeout=t, input=inp)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def vec(p):
    im = Image.open(p).convert("L").resize((160, 72))
    return list(im.getdata())


def mse(a, b):
    return sum((x - y) ** 2 for x, y in zip(a, b)) / len(a)


TITLE = vec(os.path.join(BASE, "auto_shot.png"))


def grab():
    r = sh(["exec-out", "screencap -p"], 60)
    b = r.stdout if r else b""
    open(TMP, "wb").write(b)
    return b


def state():
    b = grab()
    if len(b) < 10000:
        return "off", len(b), -1
    d = mse(vec(TMP), TITLE)
    if d < 25:
        return "title", len(b), d
    if len(b) > 1500000:
        return "scene", len(b), d
    return "other", len(b), d


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


def launch():
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; '
                  'rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])


def main():
    set_pref("true")
    launch()
    t0 = time.time()
    last = None
    while time.time() - t0 < 230:
        s, n, d = state()
        tag = "%s/%d/%.0f" % (s, n, d)
        if tag != last:
            print("t=%.1f %s" % (time.time() - t0, tag), flush=True)
            last = tag
        if s == "title":
            open(OUT, "wb").write(open(TMP, "rb").read())
            print("TITLE REACHED t=%.1f shot=%s" % (time.time() - t0, OUT), flush=True)
            return 0
        time.sleep(3)
    open(OUT, "wb").write(open(TMP, "rb").read())
    print("NO TITLE (last shot=%s)" % OUT, flush=True)
    return 2


if __name__ == "__main__":
    sys.exit(main())
