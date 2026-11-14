import os, sys, time, statistics, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import auto_load as A

P = A.P
sh = A.sh
txt = A.txt

def cat(path):
    return txt(sh(["shell", "cat " + path]))

def pids_of(name):
    return txt(sh(["shell", "pidof " + name])).split()

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
s = A.open_dialog()
if s == "confirm":
    A.tap(1547, 686); time.sleep(1); s = "dialog"
if s != "dialog":
    print("DIALOG FAILED (%s)" % s); sys.exit(3)
if not A.select_slot():
    print("BATTLE LOAD FAILED"); sys.exit(4)
print("BATTLE OK", flush=True)

app = pids_of("com.winlatcn")[0]
game = pids_of("Ekd5.exe")[0]
print("app=%s game=%s" % (app, game), flush=True)

# settle, then sample entirely on-device (no adb chatter during the window)
time.sleep(12)
gltid = txt(sh(["shell", "for t in $(ls /proc/%s/task); do c=$(cat /proc/%s/task/$t/comm); [ \"$c\" = GLThread* ] && echo $t; done | head -1" % (app, app)])).strip()
xstid = txt(sh(["shell", "for t in $(ls /proc/%s/task); do c=$(cat /proc/%s/task/$t/comm); [ \"$c\" = Thread-5 ] && echo $t; done | head -1" % (app, app)])).strip()
print("gltid=%s xstid=%s" % (gltid, xstid), flush=True)

dev = r'''
P=%s
G=%s
GT=%s
XT=%s
OUT=/data/local/tmp/perf_clean.txt
: > $OUT
for i in $(seq 1 160); do
  A=$(cat /proc/$P/stat 2>/dev/null | awk '{print $14+$15}')
  G=$(cat /proc/$G/stat 2>/dev/null | awk '{print $14+$15}')
  GL=$(cat /proc/$P/task/$GT/stat 2>/dev/null | awk '{print $14+$15}')
  XS=$(cat /proc/$P/task/$XT/stat 2>/dev/null | awk '{print $14+$15}')
  W=$(cat /proc/$G/wchan 2>/dev/null)
  S=$(cat /proc/$G/stat 2>/dev/null | awk '{print $3}')
  F=$(cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq 2>/dev/null)
  echo "$i $A $G $GL $XS $S $F $W" >> $OUT
  sleep 0.1
done
''' % (app, game, gltid, xstid or "1")
print(txt(sh(["shell", dev], 120)), flush=True)
raw = txt(sh(["shell", "cat /data/local/tmp/perf_clean.txt"]))
print("SAMPLES=%d" % len(raw.splitlines()), flush=True)

rows = []
for line in raw.splitlines():
    p = line.split()
    if len(p) >= 8 and p[0].isdigit():
        rows.append(p)
d = []
for i in range(1, len(rows)):
    a0, a1 = rows[i - 1], rows[i]
    try:
        d.append((int(a1[0]) - int(a0[0]), int(a1[1]) - int(a0[1]), int(a1[2]) - int(a0[2]),
                  int(a1[3]) - int(a0[3]), int(a1[4]) - int(a0[4]), a1[5], a1[6], a1[7]))
    except ValueError:
        pass
if d:
    print("ticks/sample (100ms): app avg %.1f max %d | game avg %.1f max %d | GL avg %.1f | XS avg %.1f" % (
        statistics.mean(x[1] for x in d), max(x[1] for x in d),
        statistics.mean(x[2] for x in d), max(x[2] for x in d),
        statistics.mean(x[3] for x in d), statistics.mean(x[4] for x in d)), flush=True)
    # samples where the game used no CPU at all
    idle = [x for x in d if x[2] == 0]
    print("game-idle samples %d/%d, wchan sets: %s" % (len(idle), len(d), collections.Counter(x[7] for x in idle).most_common(6)), flush=True)
    busy = [x for x in d if x[2] > 0]
    print("game-busy samples %d, avg game %.1f gl %.1f xs %.1f" % (len(busy), statistics.mean(x[2] for x in busy),
          statistics.mean(x[3] for x in busy), statistics.mean(x[4] for x in busy)), flush=True)
    print("game tick histogram:", dict(collections.Counter(x[2] for x in d)), flush=True)
    print("gl tick histogram:", dict(collections.Counter(x[3] for x in d)), flush=True)

# ONE latency dump after the quiet window
name = A.layer()
if name:
    raw2 = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "' + name + '"']))
    rr = []
    for line in raw2.splitlines():
        p = line.split()
        if len(p) == 3:
            try:
                v = tuple(int(x) for x in p)
            except ValueError:
                continue
            if v[1] > 0:
                rr.append(v[1])
    iv = [(rr[i + 1] - rr[i]) / 1e6 for i in range(len(rr) - 1)]
    iv = [x for x in iv if x > 0]
    if iv:
        span = sum(iv) / 1000
        print("LATENCY n=%d span=%.2fs fps=%.1f median=%.2f p90=%.2f max=%.1f buckets=%s" % (
            len(iv) + 1, span, len(iv) / span, statistics.median(iv), sorted(iv)[int(len(iv) * .9)],
            max(iv), dict(collections.Counter("<20" if x < 20 else "<40" if x < 40 else "<80" if x < 80 else ">=80" for x in iv))), flush=True)

print("DISPLAY:", txt(sh(["shell", "dumpsys SurfaceFlinger | grep -E 'activeMode|Display.*fps|refresh' | head -8"])), flush=True)
print("GFXINFO:", txt(sh(["shell", "dumpsys gfxinfo com.winlatcn | grep -E 'Total frames|Janky|percentile' | head -8"])), flush=True)
print("LOGCAT:", txt(sh(["shell", "logcat -d -t 800 | grep -E 'GC freed|Choreographer|Davey|Skipped|art ' | tail -15"])), flush=True)
print("DONE", flush=True)
