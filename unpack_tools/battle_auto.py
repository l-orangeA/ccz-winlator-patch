import subprocess, os, sys, time, json, statistics, collections

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
P = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"
SHOT = r"D:\ccz_winlator_patch\unpack_tools\auto_shot.png"

def sh(a, t=90, inp=None):
    return subprocess.run([ADB, "-s", P] + a, capture_output=True, env=ENV, timeout=t, input=inp)

def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""

def shot():
    r = sh(["exec-out", "screencap -p"], 60)
    b = r.stdout if r else b""
    open(SHOT, "wb").write(b)
    return len(b), b

def set_cfg(**kw):
    d = json.loads(txt(sh(["exec-out", "run-as com.winlatcn cat " + CFG])))
    d.update(kw)
    sh(["shell", "run-as com.winlatcn sh -c 'cat > " + CFG + "'"], inp=json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    back = json.loads(txt(sh(["exec-out", "run-as com.winlatcn cat " + CFG])))
    return {k: back.get(k) for k in kw}

def set_pref():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
    out = txt(sh(["exec-out", "run-as", "com.winlatcn", "cat", "shared_prefs/com.winlatcn_preferences.xml"]))
    if "move_cursor_to_touchpoint" not in out:
        out = out.replace("</map>", '    <boolean name="move_cursor_to_touchpoint" value="true" />\n</map>')
        sh(["shell", "run-as com.winlatcn sh -c 'cat > shared_prefs/com.winlatcn_preferences.xml'"], inp=out.encode())

def launch():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])

def wait_title(maxs=200):
    t0 = time.time(); stable = 0; last = 0
    while time.time() - t0 < maxs:
        b, _ = shot()
        if b > 800000 and abs(b - last) < 60000:
            stable += 1
        else:
            stable = 0
        last = b
        if stable >= 2:
            return True, time.time() - t0
        time.sleep(3)
    return False, time.time() - t0

def tap(x, y, n=1, gap=0.9):
    for _ in range(n):
        sh(["shell", "input tap %d %d" % (x, y)])
        time.sleep(gap)

def load_battle():
    # 契阔谈宴 (title button) -> 读取进度
    for attempt in range(3):
        tap(1785, 595, 2, 1.0); time.sleep(3)
        b, _ = shot()
        if b < 900000:
            print("load dialog open (shot=%d)" % b, flush=True)
            break
    else:
        print("load dialog NOT open", flush=True)
        return False
    # 选 No.018（截图坐标 1400,840）
    tap(1400, 806, 2, 0.8); time.sleep(2)
    b, _ = shot()
    print("after slot click shot=%d" % b, flush=True)
    if b < 900000:
        # 可能出现确认框（是/否）；点“是”
        tap(1344, 686); time.sleep(5)
        b, _ = shot()
        print("after confirm shot=%d" % b, flush=True)
    return b > 1500000

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
    n0 = None; rows = []
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
    d = [(rows[i+1]-rows[i])/1e6 for i in range(len(rows)-1)]
    d = [x for x in d if 0 < x < 1000]
    span = sum(d)/1000
    b = collections.Counter("<20" if x < 20 else "<40" if x < 40 else "<80" if x < 80 else ">=80" for x in d)
    print("FPS frames=%d span=%.2fs fps=%.1f median=%.2f p90=%.2f max=%.1f %s" % (
        len(d)+1, span, len(d)/span if span else 0, statistics.median(d),
        sorted(d)[int(len(d)*.9)], max(d), dict(b)), flush=True)
    return len(d)/span if span else None

def cpu_rates(secs=10):
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
        print("  %-14s %.2f core  cpus=%s" % (n, (jj(p)-t0[n])/secs/100,
              txt(sh(["shell", "grep Cpus_allowed_list /proc/%s/status" % p])).strip().split(":")[-1].strip()))

if __name__ == "__main__":
    label = sys.argv[1] if len(sys.argv) > 1 else "test"
    if len(sys.argv) > 2:
        print("cfg:", set_cfg(cpuList=sys.argv[2]))
    set_pref()
    launch()
    ok, t = wait_title()
    print("title", ok, round(t, 1), flush=True)
    if not ok:
        sys.exit(2)
    if not load_battle():
        print("battle load FAILED", flush=True)
        sys.exit(3)
    print("battle loaded", flush=True)
    time.sleep(2)
    fps()
    cpu_rates()
