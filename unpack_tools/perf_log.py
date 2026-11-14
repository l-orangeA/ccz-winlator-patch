import os, sys, time, statistics, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import auto_load as A

sh = A.sh
txt = A.txt

A.set_pref()
sh(["shell", "run-as com.winlatcn rm -f files/perf.txt"])
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

time.sleep(12)
print("--- perf.txt (idle battle) ---", flush=True)
print(txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", "files/perf.txt"])), flush=True)

name = A.layer()
if name:
    raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "' + name + '"']))
    ts = []
    for line in raw.splitlines():
        p = line.split()
        if len(p) == 3:
            try:
                v = [int(x) for x in p]
            except ValueError:
                continue
            if v[1] > 0:
                ts.append(v[1])
    iv = [(ts[i + 1] - ts[i]) / 1e6 for i in range(len(ts) - 1)]
    iv = [x for x in iv if 0 < x < 1000]
    if iv:
        print("LATENCY n=%d median=%.2f p90=%.2f max=%.1f buckets=%s" % (
            len(iv) + 1, statistics.median(iv), sorted(iv)[int(len(iv) * .9)], max(iv),
            dict(collections.Counter('<8' if x < 8 else '<20' if x < 20 else '<40' if x < 40 else '<80' if x < 80 else '>=80' for x in iv))), flush=True)

print("--- app threads ---", flush=True)
app = (txt(sh(["shell", "pidof com.winlatcn"])).split() or [""])[0]
if app:
    print(txt(sh(["shell", "for t in $(ls /proc/%s/task); do echo $t $(cat /proc/%s/task/$t/comm); done | grep -iE 'GLThread|Thread-5|mali'" % (app, app)])), flush=True)
print("DONE", flush=True)
