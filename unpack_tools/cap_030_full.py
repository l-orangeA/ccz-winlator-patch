"""0.3.0 解包期完整 +virtual 日志: 启动 → 解包完成 → 拉全量日志.
用法: python cap_030_full.py
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


r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:60], "(want dd297b6d)", flush=True)
if "dd297b6d" not in txt(r):
    print("NOT 0.3.0 -> rolling back", flush=True)
    sh(["shell", "run-as com.winlatcn sh -c 'cp files/imagefs/usr/local/bin/box64.bak030 files/imagefs/usr/local/bin/box64 && echo rolled'"])
    r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
    print("now:", txt(r).strip()[:60], flush=True)
    if "dd297b6d" not in txt(r):
        raise SystemExit("rollback failed")

# env: prod + HWBP_LOG + WINEDEBUG=+virtual
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["WINE_HWBP_LOG"] = "1"
kv["WINEDEBUG"] = "+virtual"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_030f.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip(), flush=True)

sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
while time.time() - t0 < 200:
    s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
    if "arms=1536" in s or "arms=1600" in s:
        print("unpack done at +%.0fs: %s" % (time.time() - t0, s), flush=True)
        break
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p and time.time() - t0 > 30:
        print("DIED at +%.0fs" % (time.time() - t0), flush=True)
        break
    time.sleep(4)
time.sleep(5)
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["exec-out", "run-as com.winlatcn sh -c 'gzip -c files/imagefs/home/xuser-1/hwbp.log'"], 600)
if len(r.stdout) > 100:
    open(OUT + "\\v030_full.log.gz", "wb").write(r.stdout)
    print("full gz saved:", len(r.stdout), "bytes", flush=True)
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
