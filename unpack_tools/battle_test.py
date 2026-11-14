import subprocess, os, sys, time, json, statistics, collections

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
P = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"

def sh(a, t=90, inp=None):
    return subprocess.run([ADB, "-s", P] + a, capture_output=True, env=ENV, timeout=t, input=inp)

def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""

def set_preset(preset):
    d = json.loads(txt(sh(["exec-out", "run-as com.winlatcn cat " + CFG])))
    d["box64Preset"] = preset
    sh(["shell", "run-as com.winlatcn sh -c 'cat > " + CFG + "'"], inp=json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    return d["screenSize"]

def launch_to_battle():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
    t0 = time.time(); title = None
    while time.time() - t0 < 200:
        img = sh(["exec-out", "screencap -p"], 40)
        b = len(img.stdout) if img else 0
        if b > 400000:
            title = time.time() - t0; break
        time.sleep(2)
    print("title at", round(title or -1, 1), flush=True)
    if not title:
        return False
    # 契阔谈宴 -> 读取进度
    sh(["shell", "input tap 2174 595"]); time.sleep(2.5)
    img = sh(["exec-out", "screencap -p"], 40)
    print("dialog shot", len(img.stdout) if img else 0, flush=True)
    # 选 18 号存档
    sh(["shell", "input tap 1400 820"]); time.sleep(5)
    img = sh(["exec-out", "screencap -p"], 40)
    print("battle shot", len(img.stdout) if img else 0, flush=True)
    return True

def layer_name():
    out = txt(sh(["shell", "dumpsys SurfaceFlinger --list"]))
    for line in out.splitlines():
        if "SurfaceView[" in line and "(BLAST)" in line and "XServerDisplayActivity" in line:
            i = line.find("SurfaceView[")
            j = line.find(" parentId")
            if i >= 0 and j > i:
                return line[i:j].strip()
    return None

def fps_stats():
    name = layer_name()
    if not name:
        print("no layer"); return None
    raw = txt(sh(["shell", 'dumpsys SurfaceFlinger --latency "' + name + '"']))
    rows = []
    for line in raw.splitlines():
        p = line.split()
        if len(p) == 3:
            try: v = tuple(int(x) for x in p)
            except: continue
            if v[1] > 0: rows.append(v)
    if len(rows) < 2:
        print("no rows"); return None
    ts = [r[1] for r in rows]
    d = [(ts[i+1]-ts[i])/1e6 for i in range(len(ts)-1)]
    d = [x for x in d if 0 < x < 1000]
    span = sum(d)/1000.0
    fps = (len(d))/span if span else 0
    b = collections.Counter("<20" if x < 20 else "<40" if x < 40 else "<80" if x < 80 else ">=80" for x in d)
    print("frames=%d span=%.2fs fps=%.1f median=%.2fms p90=%.2fms max=%.1fms %s" % (
        len(rows), span, fps, statistics.median(d), sorted(d)[int(len(d)*0.9)], max(d), dict(b)))
    return fps

def cpu_rates(secs=15):
    pids = {}
    for n in ("Ekd5.exe", "libtinyproot.so", "com.winlatcn"):
        o = txt(sh(["shell", "pidof " + n])).split()
        if o: pids[n] = o[0]
    def jj(p):
        s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
        f = s.rsplit(")", 1)[1].split(); return int(f[11]) + int(f[12])
    t0 = {n: jj(p) for n, p in pids.items()}
    time.sleep(secs)
    for n, p in pids.items():
        print("  %-14s %.2f core" % (n, (jj(p)-t0[n])/secs/100))

if __name__ == "__main__":
    preset = sys.argv[1] if len(sys.argv) > 1 else "COMPATIBILITY"
    print("preset:", set_preset(preset))
    if launch_to_battle():
        time.sleep(3)
        fps_stats()
        time.sleep(2)
        fps_stats()
        cpu_rates()
