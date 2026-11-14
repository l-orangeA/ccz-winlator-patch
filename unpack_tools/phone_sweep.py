import json, os, subprocess, sys, time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
DEV = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
CFG = "files/imagefs/home/xuser-1/.container"
STATS = "files/imagefs/home/xuser-1/hwbp_stats.txt"

def sh(a, t=90):
    return subprocess.run([ADB, "-s", DEV] + a, capture_output=True, env=ENV, timeout=t)

def read_cfg():
    return json.loads(sh(["exec-out", "run-as com.winlatcn cat " + CFG], 40).stdout.decode("utf-8", "replace"))

def write_cfg(d):
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sweep_cfg.json")
    open(p, "w", encoding="utf-8").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")))
    sh(["push", p, "/data/local/tmp/sweep_cfg.json"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/sweep_cfg.json " + CFG], 40)

def set_env(pairs):
    d = read_cfg()
    kv = {}
    for it in d["envVars"].split():
        if "=" in it:
            k, v = it.split("=", 1)
            kv[k] = v
    kv.update(pairs)
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    write_cfg(d)

def launch():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn rm -f ' + STATS])
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

def probe(ss, secs=70):
    set_env({"WINE_SS": ss})
    if not launch():
        return (ss, "会话未启动", "", "")
    alive = 0
    for _ in range(secs // 2):
        time.sleep(2)
        out = sh(["shell", "ps -A -o TIME,NAME"]).stdout.decode("utf-8", "ignore")
        if "Ekd5.exe" in out:
            alive += 2
    cpu = sh(["shell", "ps -A -o TIME,NAME|grep -i ekd5"]).stdout.decode("utf-8", "ignore").strip()
    stats = sh(["shell", "run-as com.winlatcn cat " + STATS]).stdout.decode("utf-8", "ignore").strip()
    return (ss, "存活 %ds" % alive, cpu, stats or "-")

if __name__ == "__main__":
    vals = sys.argv[1:] or ["0xe14", "0xe10", "0xe0c", "0xe08", "0xe04", "0x800"]
    for v in vals:
        r = probe(v)
        print("%-8s | %-8s | %-18s | %s" % r)
        sys.stdout.flush()
