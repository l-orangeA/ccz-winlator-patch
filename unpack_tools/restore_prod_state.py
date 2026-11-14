"""恢复手机生产态: ntdll=fix108 + box64=0.3.0 + env=prod. 用法: python restore_prod_state.py"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# ntdll = fix108
r = sh(["push", r"D:\ccz_winlator_patch\build\ntdll_fix108.so", "/data/local/tmp/n108"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/n108 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ntdll-ok"])
sh(["shell", "rm -f /data/local/tmp/n108"])
# box64 = 0.3.0
r = sh(["shell", "run-as com.winlatcn sh -c 'cp files/imagefs/usr/local/bin/box64.bak030 files/imagefs/usr/local/bin/box64 && echo b64-ok'"])
# env = prod
tmp = r"D:\ccz_winlator_patch\unpack_tools\a0_trace\container_restore.json"
open(tmp, "wb").write(open(PROD, "rb").read())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json files/imagefs/home/xuser-1/.container && echo env-ok"])
sh(["shell", "rm -f /data/local/tmp/cnew.json"])
# 校验
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so files/imagefs/usr/local/bin/box64"])
print(txt(r).strip())
print("want ntdll=c3e15c39... box64=dd297b6d...")
# 清残留并启动验证
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
t0 = time.time()
while time.time() - t0 < 240:
    time.sleep(5)
    s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
    if "arms=1600" in s or "arms=1536" in s:
        print("unpack complete at +%.0fs: %s" % (time.time() - t0, s), flush=True)
        break
    if time.time() - t0 > 200 and "arms=1" not in s:
        print("stats:", s, flush=True)
time.sleep(8)
p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
print("game alive:", p or "DEAD", flush=True)
