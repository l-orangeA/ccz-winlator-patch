import subprocess, os, sys, time, json, statistics, collections
from PIL import Image

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
P = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"
BASE = os.path.dirname(os.path.abspath(__file__))
TMP = os.path.join(BASE, "auto_state.png")

def sh(a, t=90, inp=None):
    return subprocess.run([ADB, "-s", P] + a, capture_output=True, env=ENV, timeout=t, input=inp)

def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""

def vec(p):
    im = Image.open(p).convert("L").resize((160, 72))
    return list(im.getdata())

def mse(a, b):
    return sum((x - y) ** 2 for x, y in zip(a, b)) / len(a)

REFS = {k: vec(v) for k, v in {
    "title": os.path.join(BASE, "auto_shot.png"),
    "dialog": os.path.join(BASE, "no1.png"),
    "confirm": os.path.join(BASE, "tap8.png"),
}.items()}

def grab():
    r = sh(["exec-out", "screencap -p"], 60)
    b = r.stdout if r else b""
    open(TMP, "wb").write(b)
    return len(b)

def state():
    b = grab()
    if b < 10000:
        return "off", b, {}
    v = vec(TMP)
    d = {k: mse(v, REFS[k]) for k in REFS}
    if d["title"] < 20:
        return "title", b, d
    if d["confirm"] < d["dialog"] and d["confirm"] < 120:
        return "confirm", b, d
    if d["dialog"] < 150:
        return "dialog", b, d
    if b > 1500000:
        return "scene", b, d
    return "other", b, d

def tap(x, y, gap=1.2):
    sh(["shell", "input tap %d %d" % (x, y)])
    time.sleep(gap)

def set_pref():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
    out = txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", "shared_prefs/com.winlatcn_preferences.xml"]))
    if 'move_cursor_to_touchpoint" value="true"' not in out:
        if 'move_cursor_to_touchpoint" value="false"' in out:
            out = out.replace('    <boolean name="move_cursor_to_touchpoint" value="false" />',
                              '    <boolean name="move_cursor_to_touchpoint" value="true" />')
        else:
            out = out.replace("</map>", '    <boolean name="move_cursor_to_touchpoint" value="true" />\n</map>')
        sh(["shell", "run-as com.winlatcn sh -c 'cat > shared_prefs/com.winlatcn_preferences.xml'"], inp=out.encode())

def launch():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])

def wait_state(want, maxs=220, label=""):
    t0 = time.time()
    seen = None
    while time.time() - t0 < maxs:
        s, b, d = state()
        if s != seen:
            print("  [%s] t=%.1f state=%s shot=%d" % (label, time.time() - t0, s, b), flush=True)
            seen = s
        if s == want:
            return True
        time.sleep(2)
    return False

def open_dialog():
    for i in range(6):
        tap(1785, 595)
        s, b, d = state()
        print("  tap%d -> %s (shot=%d)" % (i + 1, s, b), flush=True)
        if s in ("dialog", "confirm"):
            return s
        if s == "scene":
            return "scene"
    return None

def select_slot():
    for i in range(3):
        tap(1400, 806)
        s, b, d = state()
        print("  slot tap%d -> %s (shot=%d) %s" % (i + 1, s, b, {k: round(v) for k, v in d.items()}), flush=True)
        if s == "confirm":
            tap(1344, 686)  # 是
            time.sleep(5)
            s2, b2, _ = state()
            print("  confirm -> %s (shot=%d)" % (s2, b2), flush=True)
            if s2 == "scene":
                return True
        if s == "scene":
            return True
    return False

def layer():
    out = txt(sh(["shell", "dumpsys SurfaceFlinger --list"]))
    for line in out.splitlines():
        if "SurfaceView[" in line and "(BLAST)" in line and "XServerDisplayActivity" in line:
            i = line.find("SurfaceView["); j = line.find(" parentId")
            if i >= 0 and j > i:
                return line[i:j].strip()
    return None

def fps(seconds=10):
    name = layer()
    if not name:
        print("no layer"); return None
    rows = []
    end = time.time() + seconds
    while time.time() < end:
        raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "' + name + '"']))
        rr = []
        for line in raw.splitlines():
            p = line.split()
            if len(p) == 3:
                try: v = tuple(int(x) for x in p)
                except: continue
                if v[1] > 0: rr.append(v[1])
        rows = rr
        time.sleep(2)
    if len(rows) < 2:
        print("no rows"); return None
    d = [(rows[i + 1] - rows[i]) / 1e6 for i in range(len(rows) - 1)]
    d = [x for x in d if 0 < x < 1000]
    span = sum(d) / 1000
    b = collections.Counter("<20" if x < 20 else "<40" if x < 40 else "<80" if x < 80 else ">=80" for x in d)
    print("FPS frames=%d span=%.2fs fps=%.1f median=%.2f p90=%.2f max=%.1f %s" % (
        len(d) + 1, span, len(d) / span if span else 0, statistics.median(d),
        sorted(d)[int(len(d) * .9)], max(d), dict(b)), flush=True)
    return len(d) / span if span else None

def cpu_rates(secs=8):
    pids = {}
    for n in ("Ekd5.exe", "libtinyproot.so", "com.winlatcn", "wineserver"):
        o = txt(sh(["shell", "pidof " + n])).split()
        if o: pids[n] = o[0]
    def jj(p):
        s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
        f = s.rsplit(")", 1)[1].split(); return int(f[11]) + int(f[12])
    t0 = {n: jj(p) for n, p in pids.items()}
    time.sleep(secs)
    for n, p in pids.items():
        print("  %-14s %.2f core cpus=%s" % (n, (jj(p) - t0[n]) / secs / 100,
              txt(sh(["shell", "grep Cpus_allowed_list /proc/%s/status" % p])).strip().split(":")[-1].strip()))

if __name__ == "__main__":
    label = sys.argv[1] if len(sys.argv) > 1 else "run"
    if len(sys.argv) > 3:
        d = json.loads(txt(sh(["exec-out", "run-as com.winlatcn cat " + CFG])))
        d[sys.argv[2]] = sys.argv[3]
        sh(["shell", "run-as com.winlatcn sh -c 'cat > " + CFG + "'"], inp=json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
        print("cfg %s=%s" % (sys.argv[2], sys.argv[3]))
    set_pref()
    ok=False
    for attempt in (1,2,3):
        launch()
        if wait_state("title", 200, "boot%d" % attempt):
            ok=True; break
        print("boot attempt %d failed" % attempt, flush=True)
        time.sleep(3)
    if not ok:
        print("NO TITLE"); sys.exit(2)
    s = open_dialog()
    if s == "confirm":
        tap(1547, 686)  # 否
        time.sleep(1)
        s = "dialog"
    elif s == "scene":
        print("already in scene?")
    if s != "dialog":
        print("DIALOG FAILED (%s)" % s); sys.exit(3)
    if not select_slot():
        print("BATTLE LOAD FAILED"); sys.exit(4)
    print("BATTLE OK", flush=True)
    time.sleep(2)
    fps()
    cpu_rates()
