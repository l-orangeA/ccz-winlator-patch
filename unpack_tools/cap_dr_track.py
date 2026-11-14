"""fix110c Dr 追踪采集: 启动 0.4.4, 等 arms 积累, 拉完整日志分析 Dr 匹配.
用法: python cap_dr_track.py
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


# 1) 确认 0.4.4 + fix110c 在位
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print(txt(r).strip()[:160], flush=True)
if "a7360b1e" not in txt(r):
    print("deploying 0.4.4...", flush=True)
    sh(["push", r"D:\ccz_winlator_patch\unpack_tools\box64_044_glibc231", "/data/local/tmp/b44"])
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/b44 files/imagefs/usr/local/bin/box64"])
    sh(["shell", "rm -f /data/local/tmp/b44"])
if "44a0dcb0" not in txt(r):
    print("deploying fix110c...", flush=True)
    sh(["push", r"D:\ccz_winlator_patch\build\ntdll_fix110c.so", "/data/local/tmp/nc"])
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/nc files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
    sh(["shell", "rm -f /data/local/tmp/nc"])

# 2) env: 纯生产 + WINE_HWBP_LOG
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["WINE_HWBP_LOG"] = "1"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_drt.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip(), flush=True)

# 3) 启动 → 等 arms 积累到高位 → 停 → 拉日志
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
# 等 45 秒（让 arms 积累，壳进入 anti-debug 循环）
time.sleep(45)
s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
print("stats at +45s:", s, flush=True)
# 停止 + 拉日志
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["exec-out", "run-as com.winlatcn cat files/imagefs/home/xuser-1/hwbp.log"], 300)
if len(r.stdout) > 100:
    p = OUT + "\\fix110c_dr.log"
    open(p, "wb").write(r.stdout)
    print("log saved:", len(r.stdout), "bytes ->", p, flush=True)
else:
    print("no log:", len(r.stdout), "bytes", flush=True)
