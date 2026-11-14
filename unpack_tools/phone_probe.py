import json, subprocess, os, sys, time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
DEV = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"

def sh(a, t=90):
    return subprocess.run([ADB, "-s", DEV] + a, capture_output=True, env=ENV, timeout=t)

def read_cfg():
    return json.loads(sh(["exec-out", "run-as com.winlatcn cat " + CFG], 40).stdout.decode("utf-8", "replace"))

def write_cfg(d, name="tmp_cfg.json"):
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), name)
    open(p, "w", encoding="utf-8").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")))
    sh(["push", p, "/data/local/tmp/" + name], 60)
    r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/%s %s && echo ok" % (name, CFG)], 40)
    return b"ok" in r.stdout

def set_env(pairs):
    d = read_cfg()
    kv = {}
    for it in d["envVars"].split():
        if "=" in it:
            k, v = it.split("=", 1)
            if k not in pairs:
                kv[k] = v
    kv.update(pairs)
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    return write_cfg(d)

def launch():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    for _ in range(3):
        sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                      '--ei container_id 1 --es exec_path "/storage/emulated/0/Download/ccz/Ekd5.exe"'], 60)
        for _ in range(20):
            top = sh(["shell", "dumpsys activity activities | grep topResumedActivity | head -1"]).stdout.decode("utf-8", "ignore")
            if "XServerDisplay" in top:
                return True
            time.sleep(0.5)
    return False

def watch(secs=25):
    """返回 (是否出现过, 最后是否还活着, 存活秒数)"""
    t0 = time.time()
    seen = False
    alive_until = 0.0
    while time.time() - t0 < secs:
        out = sh(["shell", "ps -A -o NAME"]).stdout.decode("utf-8", "ignore")
        if "Ekd5.exe" in out:
            seen = True
            alive_until = time.time() - t0
        time.sleep(1.0)
    return seen, alive_until

def main():
    label = sys.argv[1]
    pairs = {}
    for a in sys.argv[2:]:
        if "=" in a:
            k, v = a.split("=", 1)
            pairs[k] = v
    if pairs:
        ok = set_env(pairs)
        print("env写入:", ok, pairs)
    if not launch():
        print("%s: 会话没起来" % label); return 2
    seen, alive = watch(25)
    print("%s: 进程出现=%s 存活到 %.0fs %s" % (label, seen, alive, "[OK] 没崩" if alive >= 22 else "[BAD] 早早退出"))
    return 0

if __name__ == "__main__":
    sys.exit(main())
