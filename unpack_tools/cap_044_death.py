"""0.4.4+fix110b 死亡现场: 部署 + HWBP_LOG + 启动 + 死亡后拉 sim 事件日志.
用法: python cap_044_death.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
SRC044 = r"D:\ccz_winlator_patch\unpack_tools\box64_044_glibc231"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 0) 解锁检查
r = sh(["shell", "run-as com.winlatcn echo unlock-ok 2>&1"])
print("runas:", txt(r).strip()[:60], flush=True)
if "unlock-ok" not in txt(r):
    raise SystemExit("PHONE LOCKED - unlock first")

# 1) 部署 box64 0.4.4 + 确认 ntdll fix110b
r = sh(["push", SRC044, "/data/local/tmp/b44"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/b44 files/imagefs/usr/local/bin/box64 && echo b44-ok"])
sh(["shell", "rm -f /data/local/tmp/b44"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print(txt(r).strip()[:160], flush=True)
if "a7360b1e" not in txt(r) or "eddb74af" not in txt(r):
    raise SystemExit("DEPLOY MISMATCH")

# 2) env: 纯生产 + WINE_HWBP_LOG
import json
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["WINE_HWBP_LOG"] = "1"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_h44.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip(), flush=True)

# 3) 启动 → 轮询出生 → 等死
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
        print("born +%.0fs pid=%s" % (born, p), flush=True)
        break
    time.sleep(1)
if not born:
    raise SystemExit("never born")
while time.time() - t0 < 200:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p:
        print("DIED at born+%.0fs" % (time.time() - t0), flush=True)
        break
    time.sleep(2)
else:
    print("alive 200s (progressing?)", flush=True)
    s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
    print("stats:", s, flush=True)
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["exec-out", "run-as com.winlatcn cat files/imagefs/home/xuser-1/hwbp.log"], 600)
if len(r.stdout) > 100:
    open(OUT + "\\death044.log", "wb").write(r.stdout)
    print("log saved:", len(r.stdout), "bytes", flush=True)
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
