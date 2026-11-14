import os, sys, time, statistics, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import auto_load as A

P = A.P
sh = A.sh
txt = A.txt

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
print("TITLE", flush=True)
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
ws = (pids_of("wineserver") or ["0"])[0]
pa = (pids_of("libpulseaudio.so") or ["0"])[0]
gltid = txt(sh(["shell", "for t in $(ls /proc/%s/task); do c=$(cat /proc/%s/task/$t/comm); [ \"$c\" = GLThread* ] && echo $t; done | head -1" % (app, app)])).strip()
xstid = txt(sh(["shell", "for t in $(ls /proc/%s/task); do c=$(cat /proc/%s/task/$t/comm); [ \"$c\" = Thread-5 ] && echo $t; done | head -1" % (app, app)])).strip()
print("app=%s game=%s gl=%s xs=%s" % (app, game, gltid, xstid), flush=True)
print("IO_READABLE:", txt(sh(["shell", "cat /proc/%s/io 2>&1 | head -2" % game])).strip(), flush=True)

time.sleep(10)
dev = r'''
P=%s
G=%s
GL=%s
XS=%s
WS=%s
PA=%s
OUT=/data/local/tmp/perf_clean2.txt
: > $OUT
for i in $(seq 1 150); do
  AS=$(cat /proc/$P/stat 2>/dev/null | awk '{print $14+$15}')
  GS=$(cat /proc/$G/stat 2>/dev/null | awk '{print $14+$15" "$10" "$12" "$3}')
  GLS=$(cat /proc/$P/task/$GL/stat 2>/dev/null | awk '{print $14+$15}')
  XSS=$(cat /proc/$P/task/$XS/stat 2>/dev/null | awk '{print $14+$15}')
  W=$(cat /proc/$G/wchan 2>/dev/null)
  F=$(cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq 2>/dev/null)
  RB=$(grep read_bytes /proc/$G/io 2>/dev/null | awk '{print $2}')
  RC=$(grep rchar /proc/$G/io 2>/dev/null | awk '{print $2}')
  WSS=$(cat /proc/$WS/stat 2>/dev/null | awk '{print $14+$15}')
  PAS=$(cat /proc/$PA/stat 2>/dev/null | awk '{print $14+$15}')
  echo "$i|$AS|$GS|$GLS|$XSS|$F|${RB:-0}|${RC:-0}|$W|${WSS:-0}|${PAS:-0}" >> $OUT
  sleep 0.1
done
''' % (app, game, gltid or "1", xstid or "1", ws, pa)
sh(["shell", dev], 120)
raw = txt(sh(["shell", "cat /data/local/tmp/perf_clean2.txt"]))
rows = [l.split('|') for l in raw.splitlines() if '|' in l]

def num(x):
    try:
        return int(x)
    except ValueError:
        return 0

d = []
for i in range(1, len(rows)):
    a, b = rows[i - 1], rows[i]
    if len(a) < 9 or len(b) < 9:
        continue
    g0 = [num(x) for x in a[2].split()]
    g1 = [num(x) for x in b[2].split()]
    if len(g0) < 4 or len(g1) < 4:
        continue
    d.append(dict(app=num(b[1]) - num(a[1]), gam=g1[0] - g0[0], minf=g1[1] - g0[1], majf=g1[2] - g0[2],
                  state=b[2].split()[3],
                  gl=num(b[3]) - num(a[3]), xs=num(b[4]) - num(a[4]),
                  f=num(b[5]), rb=num(b[6]) - num(a[6]), rc=num(b[7]) - num(a[7]), w=b[8].strip(),
                  ws=num(b[9]) - num(a[9]), pa=num(b[10]) - num(a[10])))

if d:
    print("SAMPLES", len(d), flush=True)
    print("avg ticks/100ms app=%.1f game=%.1f gl=%.1f xs=%.1f | max game=%d" % (
        statistics.mean(x["app"] for x in d), statistics.mean(x["gam"] for x in d),
        statistics.mean(x["gl"] for x in d), statistics.mean(x["xs"] for x in d), max(x["gam"] for x in d)), flush=True)
    idle = [x for x in d if x["gam"] == 0]
    print("game-idle %d, wchan=%s" % (len(idle), collections.Counter(x["w"] for x in idle).most_common(8)), flush=True)
    print("game tick hist", dict(collections.Counter(x["gam"] for x in d)), flush=True)
    print("GL tick hist", dict(collections.Counter(x["gl"] for x in d)), flush=True)
    print("XS tick hist", dict(collections.Counter(x["xs"] for x in d)), flush=True)
    print("majf total=%d, samples with majf>0: %d | minf total=%d | read_bytes/sample max=%d rchar/sample max=%d" % (
        sum(x["majf"] for x in d), sum(1 for x in d if x["majf"] > 0), sum(x["minf"] for x in d),
        max(x["rb"] for x in d), max(x["rc"] for x in d)), flush=True)
    print("state hist", dict(collections.Counter(x["state"] for x in d)), flush=True)
    print("avg ticks/100ms wineserver=%.1f pulseaudio=%.1f" % (
        statistics.mean(x["ws"] for x in d), statistics.mean(x["pa"] for x in d)), flush=True)

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

print("DISPLAY:", txt(sh(["shell", "dumpsys SurfaceFlinger | grep -E 'activeMode|Display.*[0-9]+Hz|refresh' | head -6"])).strip(), flush=True)
print("DONE", flush=True)
