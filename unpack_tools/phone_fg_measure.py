import subprocess, os, sys, time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
DEV = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
STATS = "files/imagefs/home/xuser-1/hwbp_stats.txt"

def sh(a, t=60):
    try:
        return subprocess.run([ADB, "-s", DEV] + a, capture_output=True, env=ENV, timeout=t)
    except subprocess.TimeoutExpired:
        return None

def text(r):
    return r.stdout.decode("utf-8", "replace") if r else ""

def launch():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f ' + STATS + '; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'],
       60)
    for _ in range(4):
        sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                      '--ei container_id 1 --es exec_path "/storage/emulated/0/Download/ccz/Ekd5.exe"'], 60)
        for _ in range(30):
            top = text(sh(["shell", "dumpsys activity activities | grep topResumedActivity | head -1"]))
            if "XServerDisplay" in top:
                return True
            time.sleep(0.5)
    return False

def jiffies(pid):
    s = text(sh(["shell", "cat /proc/%s/stat 2>/dev/null" % pid]))
    if not s or ")" not in s:
        return None
    f = s.rsplit(")", 1)[1].split()
    try:
        return int(f[11]) + int(f[12])  # utime+stime (fields 14,15)
    except Exception:
        return None

def state(pid):
    s = text(sh(["shell", "grep -E '^State|^voluntary_ctxt' /proc/%s/status 2>/dev/null" % pid]))
    d = {}
    for l in s.splitlines():
        if ":" in l:
            k, v = l.split(":", 1)
            d[k.strip()] = v.strip()
    return d

print("launch ...")
if not launch():
    print("会话没起来（XServerDisplay 未到前台）")
    sys.exit(2)
print("XServerDisplay 前台，开始采样")

t0 = time.time()
shot_idx = 0
prev = {}
while time.time() - t0 < 190:
    now = time.time() - t0
    pids = {}
    out = text(sh(["shell", "ps -A -o PID,PPID,S,NAME"]))
    for line in out.splitlines():
        p = line.split()
        if len(p) >= 4 and p[3] in ("libtinyproot.so", "Ekd5.exe"):
            pids[p[3]] = (p[0], p[1], p[2])
    st = text(sh(["shell", "run-as com.winlatcn cat " + STATS + " 2>/dev/null"]))
    # stats line: pid=… arms=… faults=… steps=… hits=…
    vals = {}
    for tok in st.split():
        if "=" in tok:
            k, v = tok.split("=", 1)
            try:
                vals[k] = int(v)
            except Exception:
                pass
    pro = pids.get("libtinyproot.so")
    ekd = pids.get("Ekd5.exe")
    pj = jiffies(pro[0]) if pro else None
    ej = jiffies(ekd[0]) if ekd else None
    ps = state(pro[0]) if pro else {}
    es = state(ekd[0]) if ekd else {}
    cg = text(sh(["shell", "cat /proc/%s/cgroup 2>/dev/null | grep cpuset" % ekd[0]])) if ekd else ""
    cg = cg.strip().split(":")[-1] if cg else "-"
    line = "t=%3.0fs arms=%-5s faults=%-6s steps=%-6s hits=%-4s | proot %s %s | Ekd5 %s %s | %s" % (
        now, vals.get("arms", "-"), vals.get("faults", "-"), vals.get("steps", "-"), vals.get("hits", "-"),
        (pro[0] if pro else "-"), (str(pj) + "j " + ps.get("State", "")) if pro else "gone",
        (ekd[0] if ekd else "-"), (str(ej) + "j " + es.get("State", "")) if ekd else "gone", cg)
    print(line, flush=True)
    if now > 20 and int(now) % 20 < 5:
        shot_idx += 1
        img = sh(["exec-out", "screencap -p"], 60)
        if img:
            b = len(img.stdout)
            print("    screenshot %d bytes" % b, flush=True)
            if b > 300000:
                open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "phone_t%d.png" % shot_idx), "wb").write(img.stdout)
    time.sleep(4)
