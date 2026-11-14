"""部署 ntdll fix109diag + r113 box64, 启动死亡场景, 拉 MPROT 诊断日志.
用法: python run_fix109diag.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 1) ntdll fix109diag
SRC = r"D:\ccz_winlator_patch\build\ntdll_fix109diag.so"
r = sh(["push", SRC, "/data/local/tmp/n109"])
print(txt(r).strip()[:80], flush=True)
r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/n109 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ntdll-ok"])
print(txt(r).strip())
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print("ntdll:", txt(r).strip()[:70], "(want e62fb8c0...)", flush=True)

# 2) box64 确认 r113
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:60], "(want d234aa57...)", flush=True)

# 3) 启动
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
# 等死亡
t0 = time.time()
while time.time() - t0 < 120:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if p:
        time.sleep(12)
        p2 = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
        if not p2:
            print("died at +%.0fs" % (time.time() - t0), flush=True)
            break
        # 活着就继续等
    time.sleep(2)
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
# 拉日志
r = sh(["exec-out", "run-as com.winlatcn cat files/imagefs/home/xuser-1/hwbp.log"], 300)
if len(r.stdout) > 100:
    open(OUT + "\\diag109.log", "wb").write(r.stdout)
    print("log saved:", len(r.stdout), "bytes", flush=True)
else:
    print("no log:", len(r.stdout), flush=True)
