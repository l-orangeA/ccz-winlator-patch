import json, subprocess, os, sys, time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
DEV = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"
STATS = "files/imagefs/home/xuser-1/hwbp_stats.txt"

def sh(a, t=60, inp=None):
    try:
        return subprocess.run([ADB, "-s", DEV] + a, capture_output=True, env=ENV, timeout=t, input=inp)
    except subprocess.TimeoutExpired:
        return None

def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""

def read_cfg():
    return json.loads(txt(sh(["exec-out", "run-as com.winlatcn cat " + CFG])))

def write_cfg(d):
    blob = json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    r = sh(["shell", "run-as com.winlatcn sh -c 'cat > " + CFG + "'"], inp=blob)
    back = txt(sh(["exec-out", "run-as com.winlatcn cat " + CFG]))
    return json.loads(back) == d

def set_env(pairs, remove=()):
    d = read_cfg()
    kv = {}
    for it in d["envVars"].split():
        if "=" in it:
            k, v = it.split("=", 1)
            kv[k] = v
    for k in remove:
        kv.pop(k, None)
    kv.update(pairs)
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    return write_cfg(d), d["envVars"]

def launch():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f ' + STATS + '; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'], 60)
    for _ in range(4):
        sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                      '--ei container_id 1 --es exec_path "/storage/emulated/0/Download/ccz/Ekd5.exe"'], 60)
        for _ in range(30):
            top = txt(sh(["shell", "dumpsys activity activities | grep topResumedActivity | head -1"]))
            if "XServerDisplay" in top:
                return True
            time.sleep(0.5)
    return False

def pidof(n):
    o = txt(sh(["shell", "pidof " + n])).split()
    return o[0] if o else None

def vol(p):
    s = txt(sh(["shell", "grep '^voluntary_ctxt' /proc/%s/status" % p]))
    try:
        return int(s.split(":")[1])
    except Exception:
        return None

def cpu(p):
    s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
    if not s or ")" not in s:
        return None
    f = s.rsplit(")", 1)[1].split()
    return int(f[11]) + int(f[12])

label = sys.argv[1]
pairs, remove = {}, ()
for a in sys.argv[2:]:
    if a.startswith("-"):
        remove += (a[1:],)
    elif "=" in a:
        k, v = a.split("=", 1)
        pairs[k] = v
ok, envs = set_env(pairs, remove)
print("env写入:", ok)
print("env:", envs)
if not launch():
    print("会话没起来"); sys.exit(2)

t0 = time.time()
seen = None
while time.time() - t0 < 100:
    p = pidof("Ekd5.exe")
    if p:
        seen = (p, time.time() - t0)
        break
    time.sleep(1)
if not seen:
    print("Ekd5 100s 内未出现")
    print("stats:", txt(sh(["shell", "run-as com.winlatcn cat " + STATS])).strip() or "(无)")
    img = sh(["exec-out", "screencap -p"], 40)
    if img:
        print("screenshot:", len(img.stdout))
    sys.exit(3)
p, tseen = seen
print("Ekd5 出现于 t=%.0fs pid=%s" % (tseen, p))
for label2, delay in (("20s", 20), ("50s", 30)):
    v0, c0 = vol(p), cpu(p)
    t1 = time.time()
    while time.time() - t1 < 6:
        time.sleep(0.5)
        if not pidof("Ekd5.exe"):
            break
    v1, c1 = vol(p), cpu(p)
    if v1 is None:
        print(delay, "阶段: Ekd5 已退出")
    else:
        print(delay, "阶段: vol=%.0f/s cpu=%.2f core" % ((v1 - v0) / 6.0, (c1 - c0) / 6.0 / 100))
    time.sleep(delay)
print("stats:", txt(sh(["shell", "run-as com.winlatcn cat " + STATS])).strip() or "(无)")
img = sh(["exec-out", "screencap -p"], 40)
if img:
    print("screenshot:", len(img.stdout), "bytes")
    open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "ss_%s.png" % label), "wb").write(img.stdout)
