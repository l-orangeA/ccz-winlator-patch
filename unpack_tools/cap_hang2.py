"""抓 r113 挂死现场 v2：先部署 r113 + 正确 env，再启动等待挂死，拉日志。
用法: python cap_hang2.py
"""
import subprocess, os, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
R113 = r"D:\ccz_winlator_patch\build\box64_r113_044.so"
CFG = "files/imagefs/home/xuser-1/.container"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 1) 部署 r113
r = sh(["push", R113, "/data/local/tmp/bc"])
print(txt(r).strip()[:80])
r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/bc files/imagefs/usr/local/bin/box64 && echo b64-ok"])
print(txt(r).strip())
sh(["shell", "rm -f /data/local/tmp/bc"])

# 2) env: prod + INTERP_SIGNAL + HWBP_LOG
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["BOX64_DYNAREC_INTERP_SIGNAL"] = "1"
kv["WINE_HWBP_LOG"] = "1"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_hang2.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip())

# 3) 校验
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:70], flush=True)

# 4) 启动 → 等 75s 挂死
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
r = sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print(txt(r).strip()[:120], flush=True)
time.sleep(75)
r = sh(["shell", "pidof Ekd5.exe"])
pid = txt(r).strip()
print("Ekd5 pid:", pid or "DEAD", flush=True)
if pid:
    r = sh(["shell", "cat /proc/%s/stat" % pid])
    print("state:", txt(r).strip().rsplit(")", 1)[-1].split()[0:3], flush=True)

# 5) 停 + 拉日志尾
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log 2>/dev/null'"])
print(txt(r).strip())
r = sh(["shell", "run-as com.winlatcn sh -c 'tail -c 4194304 files/imagefs/home/xuser-1/hwbp.log 2>/dev/null | gzip -c'"], 300)
if len(r.stdout) > 50:
    p = OUT + "\\hang_tail2.log.gz"
    open(p, "wb").write(r.stdout)
    print("saved", p, len(r.stdout), "bytes")
else:
    print("no log:", txt(r)[:150])
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
