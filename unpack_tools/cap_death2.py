"""v2: 等进程出生 → 再等死亡 → 抓 logcat。用法: python cap_death2.py"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"


def sh(a, t=120):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:70], flush=True)
sh(["shell", "logcat -c"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp.log; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
r = sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print(txt(r).strip()[:120], flush=True)
# 阶段1: 等出生
born_t = None
while time.time() - t0 < 120:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if p:
        born_t = time.time() - t0
        print("Ekd5 BORN at +%.1fs pid=%s" % (born_t, p), flush=True)
        break
    time.sleep(1)
if not born_t:
    print("never born", flush=True)
    sys_exit = 1
    raise SystemExit(1)
# 阶段2: 等死亡（最多 180s）
t1 = time.time()
death_t = None
while time.time() - t1 < 180:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p:
        death_t = time.time() - t1
        print("Ekd5 DIED at born+%.1fs" % death_t, flush=True)
        break
    time.sleep(2)
else:
    print("still alive after 180s (hung?)", flush=True)
time.sleep(3)
r = sh(["shell", "logcat -d"])
out = txt(r)
keep = [ln for ln in out.splitlines() if
        any(k in ln for k in ("Fatal signal", "backtrace", "tombstone", "SIGSEGV", "SIGILL", "SIGABRT", "SIGBUS",
                              "Ekd", "box64", "DEBUG"))]
open(OUT + "\\death_logcat.txt", "w", encoding="utf-8", errors="replace").write("\n".join(keep))
print("saved %d filtered lines" % len(keep), flush=True)
for ln in keep[-40:]:
    print(ln[:180], flush=True)
