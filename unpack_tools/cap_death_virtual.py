"""r113 死亡 + WINEDEBUG=+virtual: wine 视角的内存操作序列.
用法: python cap_death_virtual.py
"""
import subprocess, os, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# env: prod + INTERP_SIGNAL + HWBP_LOG + WINEDEBUG=+virtual
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["BOX64_DYNAREC_INTERP_SIGNAL"] = "1"
kv["WINE_HWBP_LOG"] = "1"
kv["WINEDEBUG"] = "+virtual"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_virt.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip(), flush=True)
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:60], flush=True)

# 启动 → 等死
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
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
while time.time() - t0 < 240:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p:
        print("DIED at born+%.0fs" % (time.time() - t0), flush=True)
        break
    time.sleep(2)
else:
    print("alive 180s+", flush=True)
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log'"])
print(txt(r).strip(), flush=True)
# 拉尾部（+virtual 日志会很大）
r = sh(["exec-out", "run-as com.winlatcn sh -c 'tail -c 8388608 files/imagefs/home/xuser-1/hwbp.log'"], 600)
if len(r.stdout) > 100:
    open(OUT + "\\death_virtual.log", "wb").write(r.stdout)
    print("tail saved:", len(r.stdout), "bytes", flush=True)
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
