import json, subprocess, os, sys, time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
DEV = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"
STATS = "files/imagefs/home/xuser-1/hwbp_stats.txt"

def sh(a, t=60):
    try:
        return subprocess.run([ADB, "-s", DEV] + a, capture_output=True, env=ENV, timeout=t)
    except subprocess.TimeoutExpired:
        return None

def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""

def read_cfg():
    return json.loads(txt(sh(["exec-out", "run-as com.winlatcn cat " + CFG])))

def write_cfg(d):
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "env_test_cfg.json")
    open(p, "w", encoding="utf-8").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")))
    sh(["push", p, "/data/local/tmp/env_test_cfg.json"], 60)
    return "ok" in txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/env_test_cfg.json " + CFG]))

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
    sh(["shell", 'run-as com.winlatcn sh -c "rm -f ' + STATS + '; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'],
       60)
    for _ in range(4):
        sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                      '--ei container_id 1 --es exec_path "/storage/emulated/0/Download/ccz/Ekd5.exe"'], 60)
        for _ in range(30):
            top = txt(sh(["shell", "dumpsys activity activities | grep topResumedActivity | head -1"]))
            if "XServerDisplay" in top:
                return True
            time.sleep(0.5)
    return False

def pidof(name):
    o = txt(sh(["shell", "pidof " + name])).split()
    return o[0] if o else None

def stat(p):
    s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
    if not s or ")" not in s:
        return None
    f = s.rsplit(")", 1)[1].split()
    try:
        return dict(state=f[0], ut=int(f[11]), st=int(f[12]), core=int(f[36]))
    except Exception:
        return None

def vol(p):
    s = txt(sh(["shell", "grep '^voluntary_ctxt' /proc/%s/status" % p]))
    try:
        return int(s.split(":")[1])
    except Exception:
        return None

label = sys.argv[1] if len(sys.argv) > 1 else "test"
pairs = {}
remove = ()
for a in sys.argv[2:]:
    if a.startswith("-"):
        remove += (a[1:],)
    elif "=" in a:
        k, v = a.split("=", 1)
        pairs[k] = v
if pairs or remove:
    ok, envs = set_env(pairs, remove)
    print(label, "env写入:", ok)
    print("  env:", envs)

if not launch():
    print(label, ": 会话没起来")
    sys.exit(2)
time.sleep(3)
ekd = pidof("Ekd5.exe"); pro = pidof("libtinyproot.so")
print(label, ": ekd=", ekd, "proot=", pro)
if not ekd:
    st = txt(sh(["shell", "run-as com.winlatcn cat " + STATS]))
    print(label, ": Ekd5 不在, stats=", st.strip())
    sys.exit(3)
s0 = stat(ekd); v0 = vol(ekd); p0 = stat(pro) if pro else None
t0 = time.time(); hist = {}
while time.time() - t0 < 24:
    s = stat(ekd)
    if s:
        hist[s['state']] = hist.get(s['state'], 0) + 1
    time.sleep(0.04)
s1 = stat(ekd); v1 = vol(ekd); p1 = stat(pro) if pro else None
dt = time.time() - t0
sts = txt(sh(["shell", "run-as com.winlatcn cat " + STATS])).strip()
print(label, "states=", hist)
print(label, "Ekd5 cpu=%.2f core  proot cpu=%.2f core  vol=%.0f/s  core=%s" % (
    ((s1['ut'] + s1['st']) - (s0['ut'] + s0['st'])) / dt / 100,
    (((p1['ut'] + p1['st']) - (p0['ut'] + p0['st'])) / dt / 100) if (p0 and p1) else -1,
    (v1 - v0) / dt, s1['core']))
print(label, "stats=", sts if sts else "(无)")
img = sh(["exec-out", "screencap -p"], 60)
if img:
    b = len(img.stdout)
    print(label, "screenshot=", b, "bytes")
    open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "envtest_%s.png" % label), "wb").write(img.stdout)
