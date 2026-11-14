import os, sys, time, statistics
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import auto_load as A

sh = A.sh
txt = A.txt

A.set_pref()
ok = False
for attempt in (1, 2, 3):
    A.launch()
    if A.wait_state("title", 220, "boot%d" % attempt):
        ok = True
        break
    time.sleep(3)
if not ok:
    print("NO TITLE"); sys.exit(2)
print("TITLE", flush=True)
s = A.open_dialog()
if s == "confirm":
    A.tap(1547, 686); time.sleep(1); s = "dialog"
if s != "dialog":
    print("DIALOG FAILED (%s)" % s); sys.exit(3)
if not A.select_slot():
    print("BATTLE LOAD FAILED"); sys.exit(4)
print("BATTLE OK", flush=True)
time.sleep(6)

def layer():
    out = txt(sh(["shell", "dumpsys SurfaceFlinger --list"]))
    for line in out.splitlines():
        if "SurfaceView[" in line and "(BLAST)" in line and "XServerDisplayActivity" in line:
            i = line.find("SurfaceView["); j = line.find(" parentId")
            seg = line[i:j] if i >= 0 and j > i else line
            return seg.strip()
    return None

NAME = layer()
print("LAYER", NAME, flush=True)

def stamps():
    raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "%s"' % NAME]))
    r = []
    for l in raw.replace("\r", "").splitlines():
        q = l.split()
        if len(q) == 3:
            try:
                v = [int(x) for x in q]
            except ValueError:
                continue
            if 1e12 < v[1] < 1e18:
                r.append(v[1])
    return r

def ticks(path):
    s = txt(sh(["shell", "cat " + path])).strip()
    if not s:
        return 0
    f = s.rsplit(")", 1)[-1].split()
    return int(f[11]) + int(f[12])

app = (txt(sh(["shell", "pidof com.winlatcn"])).split() or [""])[0]
game = (txt(sh(["shell", "pidof Ekd5.exe"])).split() or [""])[0]
gl = txt(sh(["shell", "for t in $(ls /proc/%s/task); do c=$(cat /proc/%s/task/$t/comm); case $c in GLThread*) echo $t;; esac; done | head -1" % (app, app)])).strip()
print("app=%s game=%s gl=%s" % (app, game, gl), flush=True)

res = []
for i in range(6):
    a = stamps(); t0 = time.time(); ca = ticks("/proc/%s/stat" % app); cg = ticks("/proc/%s/stat" % game) if game else 0
    cgl = ticks("/proc/%s/task/%s/stat" % (app, gl)) if gl else 0
    time.sleep(1.0)
    b = stamps(); t1 = time.time(); ca2 = ticks("/proc/%s/stat" % app); cg2 = ticks("/proc/%s/stat" % game) if game else 0
    cgl2 = ticks("/proc/%s/task/%s/stat" % (app, gl)) if gl else 0
    new = len([t for t in b if t > (max(a) if a else 0)])
    dt = t1 - t0
    res.append(new / dt)
    print("sample%d frames=%.0f/s app=%.2f game=%.2f gl=%.2f" % (
        i, new / dt, (ca2 - ca) / 100 / dt, (cg2 - cg) / 100 / dt, (cgl2 - cgl) / 100 / dt), flush=True)
print("AVG_FPS %.1f" % statistics.mean(res), flush=True)
print("DONE", flush=True)
