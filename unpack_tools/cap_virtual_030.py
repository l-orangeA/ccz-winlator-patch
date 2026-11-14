"""0.3.0 对照: WINEDEBUG=+virtual + WINE_HWBP_LOG, 启动存活后拉日志找 stack-limits 警告.
用法: python cap_virtual_030.py
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


# env: prod + HWBP_LOG + WINEDEBUG=+virtual（0.3.0 原版 box64）
# 先强制回滚 box64 并复验
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64 before:", txt(r).strip()[:60], flush=True)
if "dd297b6d3b0966f6f8acf0d60495f82c" not in txt(r):
    r = sh(["shell", "run-as com.winlatcn sh -c 'cp files/imagefs/usr/local/bin/box64.bak030 files/imagefs/usr/local/bin/box64 && echo rolled'"])
    print(txt(r).strip(), flush=True)
    r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
    print("box64 now:", txt(r).strip()[:60], flush=True)
    if "dd297b6d" not in txt(r):
        raise SystemExit("rollback failed")
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["WINE_HWBP_LOG"] = "1"
kv["WINEDEBUG"] = "+virtual"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_v030.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip(), flush=True)
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:60], "(want dd297b6d = 0.3.0)", flush=True)

sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
# 等解包完成（arms=1536）
t0 = time.time()
ok = False
while time.time() - t0 < 200:
    s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
    if "arms=1536" in s or "arms=1600" in s:
        ok = True
        print("unpack done at +%.0fs: %s" % (time.time() - t0, s), flush=True)
        break
    time.sleep(5)
time.sleep(10)  # 标题期再采一段
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
r = sh(["exec-out", "run-as com.winlatcn sh -c 'grep -c \"outside of stack\" files/imagefs/home/xuser-1/hwbp.log 2>/dev/null || echo 0'"])
print("outside-of-stack warnings on 0.3.0:", txt(r).strip(), flush=True)
r = sh(["exec-out", "run-as com.winlatcn sh -c 'grep -m3 \"outside of stack\" files/imagefs/home/xuser-1/hwbp.log 2>/dev/null'"])
print(txt(r).strip()[:400], flush=True)
# 恢复 env=prod
sh(["push", PROD, "/data/local/tmp/cnew.json"], 60)
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-restored"])
sh(["shell", "rm -f /data/local/tmp/cnew.json"])
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
sh(["shell", "am force-stop com.winlatcn"])
print("cleaned", flush=True)
