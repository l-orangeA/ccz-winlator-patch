import os, sys, time, statistics, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import auto_load as A

P = A.P
sh = A.sh
txt = A.txt

def cat(path):
    return txt(sh(["shell", "cat " + path]))

def pids_of(name):
    o = txt(sh(["shell", "pidof " + name])).split()
    return o

def read_cpu(pid):
    s = cat("/proc/%s/stat" % pid)
    if not s:
        return None, None
    f = s.rsplit(")", 1)[-1].split()
    ticks = int(f[11]) + int(f[12])
    threads = {}
    for t in txt(sh(["shell", "ls /proc/%s/task" % pid])).split():
        ts = cat("/proc/%s/task/%s/stat" % (pid, t))
        if not ts:
            continue
        tf = ts.rsplit(")", 1)[-1].split()
        comm = ts.split("(", 1)[1].rsplit(")", 1)[0]
        threads[comm] = int(tf[11]) + int(tf[12])
    return ticks, threads

def freq():
    out = {}
    for c in (0, 3, 4, 6, 7):
        v = cat("/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq" % c).strip()
        out[c] = int(v) // 1000 if v.isdigit() else None
    return out

def temps():
    out = {}
    for z in txt(sh(["shell", "ls /sys/class/thermal"])).split():
        if not z.startswith("thermal_zone"):
            continue
        v = cat("/sys/class/thermal/%s/temp" % z).strip()
        if v and v.lstrip("-").isdigit():
            out[z] = int(v) / 1000.0
    return out

def hwbp():
    p = "files/imagefs/home/xuser-1/hwbp_stats.txt"
    return txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", p])).strip()

A.set_pref()
ok = False
for attempt in (1, 2, 3):
    A.launch()
    if A.wait_state("title", 200, "boot%d" % attempt):
        ok = True
        break
    time.sleep(3)
if not ok:
    print("NO TITLE"); sys.exit(2)
s = A.open_dialog()
if s == "confirm":
    A.tap(1547, 686); time.sleep(1); s = "dialog"
if s != "dialog":
    print("DIALOG FAILED (%s)" % s); sys.exit(3)
if not A.select_slot():
    print("BATTLE LOAD FAILED"); sys.exit(4)
print("BATTLE OK", flush=True)
time.sleep(2)

app = pids_of("com.winlatcn")
game = pids_of("Ekd5.exe")
print("app=%s game=%s" % (app, game), flush=True)

# hwbp stats before/after
h0 = hwbp()
print("HWBP_BEFORE:\n" + h0, flush=True)

# snapshot CPU
app_pid = app[0] if app else None
game_pid = game[0] if game else None
t0a, th0a = read_cpu(app_pid) if app_pid else (None, {})
t0g, th0g = read_cpu(game_pid) if game_pid else (None, {})
f0 = freq(); temp0 = temps()
wall0 = time.time()

fps_samples = []
for i in range(5):
    A.fps(4)

time.sleep(6)
wall = time.time() - wall0
t1a, th1a = read_cpu(app_pid) if app_pid else (None, {})
t1g, th1g = read_cpu(game_pid) if game_pid else (None, {})
f1 = freq(); temp1 = temps()

if t0a is not None:
    print("APP total %.2f core over %.1fs" % ((t1a - t0a) / 100.0 / wall, wall), flush=True)
    rates = sorted(((th1a[k] - th0a.get(k, 0)) / 100.0 / wall, k) for k in th1a if k in th0a)
    print("APP threads:", ", ".join("%s=%.2f" % (k, r) for r, k in rates[-8:][::-1]), flush=True)
if t0g is not None:
    print("GAME total %.2f core" % ((t1g - t0g) / 100.0 / wall), flush=True)
    rates = sorted(((th1g[k] - th0g.get(k, 0)) / 100.0 / wall, k) for k in th1g if k in th0g)
    print("GAME threads:", ", ".join("%s=%.2f" % (k, r) for r, k in rates[-10:][::-1]), flush=True)

print("FREQ before", f0, "after", f1, flush=True)
print("TEMP before", {k: round(v, 1) for k, v in sorted(temp0.items())}, flush=True)
print("TEMP after ", {k: round(v, 1) for k, v in sorted(temp1.items())}, flush=True)
h1 = hwbp()
print("HWBP_AFTER:\n" + h1, flush=True)

# GC / frame drops in logcat
log = txt(sh(["shell", "logcat -d -t 400 | grep -E 'GC freed|Choreographer|Davey|Skipped|ANR' | tail -30"]))
print("LOGCAT:\n" + log, flush=True)

# restore cursor pref
A.sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
out = A.txt(A.sh(["exec-out", "run-as", "com.winlatcn", "cat", "shared_prefs/com.winlatcn_preferences.xml"]))
if "move_cursor_to_touchpoint" in out:
    out = out.replace('    <boolean name="move_cursor_to_touchpoint" value="true" />\n', '    <boolean name="move_cursor_to_touchpoint" value="false" />\n')
    A.sh(["shell", "run-as com.winlatcn sh -c 'cat > shared_prefs/com.winlatcn_preferences.xml'"], inp=out.encode())
print("DONE", flush=True)
