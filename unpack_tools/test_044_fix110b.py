"""决定性测试: 作者版 box64 0.4.4 + ntdll fix110b + 纯 env → 壳能否解包出标题.
用法: python test_044_fix110b.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"
SRC044 = r"D:\ccz_winlator_patch\unpack_tools\box64_044_glibc231"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 1) 部署作者版 0.4.4（ntdll fix110b 已在位——先确认）
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print("ntdll:", txt(r).strip()[:60], "(want eddb74af fix110b)", flush=True)
r = sh(["push", SRC044, "/data/local/tmp/b44"])
print(txt(r).strip()[:90], flush=True)
r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/b44 files/imagefs/usr/local/bin/box64 && echo b44-ok"])
print(txt(r).strip())
sh(["shell", "rm -f /data/local/tmp/b44"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:60], flush=True)

# 2) env = 纯生产
tmp = r"D:\ccz_winlator_patch\unpack_tools\a0_trace\container_pure2.json"
open(tmp, "wb").write(open(PROD, "rb").read())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-pure"])).strip(), flush=True)
sh(["shell", "rm -f /data/local/tmp/cnew.json"])

# 3) 启动 + 轮询
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
last = ""
result = "timeout"
while time.time() - t0 < 300:
    s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
    if s != "no" and s != last:
        print("+%3.0fs %s" % (time.time() - t0, s), flush=True)
        last = s
        if "arms=1600" in s or "arms=1536" in s:
            result = "UNPACK-COMPLETE"
            break
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p and time.time() - t0 > 20:
        result = "DIED (last=%s)" % last
        print(result + " at +%.0fs" % (time.time() - t0), flush=True)
        break
    time.sleep(3)
print("RESULT:", result, flush=True)
if "UNPACK" in result:
    time.sleep(15)
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    print("game alive:", p or "DEAD", flush=True)
    # 出标题了吗（截图字节量）
    r = sh(["exec-out", "screencap -p"], 60)
    print("screenshot bytes:", len(r.stdout), flush=True)
