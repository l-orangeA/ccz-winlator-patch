"""抓 r113 挂死现场：开 WINE_HWBP_LOG 启动 → 等 75s（挂死稳定）→ 停 → 拉 hwbp.log 尾部。
用法: python cap_hang.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"


def sh(a, t=120):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 1) env 加 WINE_HWBP_LOG（读改写）
r = sh(["exec-out", "run-as com.winlatcn cat files/imagefs/home/xuser-1/.container"], 40)
import json
d = json.loads(r.stdout.decode("utf-8", "replace"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["WINE_HWBP_LOG"] = "1"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_hang.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json files/imagefs/home/xuser-1/.container && echo env-ok"])).strip())
print("INTERP_SIGNAL in env:", "BOX64_DYNAREC_INTERP_SIGNAL=1" in d["envVars"])

# 2) 确认 r113 在位
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:60], "(want d234aa57...)")

# 3) 启动并等挂死稳定
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
r = sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print(txt(r).strip()[:120], flush=True)
time.sleep(75)
r = sh(["shell", "pidof Ekd5.exe"])
print("Ekd5 pid:", txt(r).strip() or "DEAD", flush=True)

# 4) 停止并拉日志尾部
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log'"])
print(txt(r).strip())
r = sh(["shell", "run-as com.winlatcn sh -c 'tail -c 4194304 files/imagefs/home/xuser-1/hwbp.log | gzip -c'"], 300)
if len(r.stdout) > 50:
    p = OUT + "\\hang_tail.log.gz"
    open(p, "wb").write(r.stdout)
    print("saved", p, len(r.stdout), "bytes")
else:
    print("no log content:", txt(r)[:200])
