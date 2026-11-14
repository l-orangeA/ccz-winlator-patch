"""重跑 r113 死亡场景并抓 logcat 的 native crash 证据。
用法: python cap_death.py
"""
import subprocess, os, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"


def sh(a, t=120):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 确认 r113 在位 + env（WINE_HWBP_LOG 留着，INTERP_SIGNAL 留着）
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:70], flush=True)

sh(["shell", "logcat -c"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp.log; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
r = sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print(txt(r).strip()[:120], flush=True)
# 轮询进程死亡时刻
death_t = None
while time.time() - t0 < 120:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p:
        death_t = time.time() - t0
        print("Ekd5 DIED at +%.1fs" % death_t, flush=True)
        break
    time.sleep(2)
else:
    print("still alive at 120s", flush=True)
time.sleep(3)
# 抓 logcat 崩溃证据
r = sh(["shell", "logcat -d -t 500 *:E AndroidRuntime:E DEBUG:I 2>/dev/null | grep -iE 'fatal|signal|Ekd|box64|winlat|tombstone|abort' | tail -60"])
out = txt(r)
if not out.strip():
    r = sh(["shell", "logcat -d | grep -iE 'Fatal signal|backtrace:|tombstone' | tail -40"])
    out = txt(r)
open(OUT + "\\death_logcat.txt", "w", encoding="utf-8", errors="replace").write(out)
print("logcat evidence saved (%d chars)" % len(out), flush=True)
print(out[:3000], flush=True)
