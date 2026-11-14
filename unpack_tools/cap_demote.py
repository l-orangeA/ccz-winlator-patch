"""全程捕获: fix110b + WINEDEBUG=+virtual + BOX64_DYNAREC_LOG=2, 全量日志, 找代码页 EXEC 丢失时刻.
用法: python cap_demote.py
"""
import subprocess, os, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"


def sh(a, t=600):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["BOX64_DYNAREC_INTERP_SIGNAL"] = "1"
kv["WINE_HWBP_LOG"] = "1"
kv["WINEDEBUG"] = "+virtual"
kv["BOX64_DYNAREC_LOG"] = "2"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_dm.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip(), flush=True)

sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
born = None
while time.time() - t0 < 120:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if p:
        born = time.time() - t0
        print("born +%.0fs" % born, flush=True)
        break
    time.sleep(1)
if born:
    while time.time() - t0 < 200:
        p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
        if not p:
            print("DIED at born+%.0fs" % (time.time() - t0), flush=True)
            break
        time.sleep(2)
    else:
        print("alive 200s", flush=True)
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["exec-out", "run-as com.winlatcn sh -c 'gzip -c files/imagefs/home/xuser-1/hwbp.log'"], 600)
if len(r.stdout) > 100:
    open(OUT + "\\demote_full.log.gz", "wb").write(r.stdout)
    print("full gz saved:", len(r.stdout), "bytes", flush=True)
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
