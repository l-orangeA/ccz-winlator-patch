"""抓 r113 短命进程的完整 maps(含 tinyproot 定位). 用法: python cap_fullmaps.py"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
p = None
while time.time() - t0 < 60:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if p:
        print("born +%.1fs pid=%s" % (time.time() - t0, p), flush=True)
        break
    time.sleep(0.1)
if not p:
    raise SystemExit("never born")
r = sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/maps'" % p], 60)
maps = txt(r)
open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\maps_r113_full.txt", "w", encoding="utf-8", errors="replace").write(maps)
print("maps saved, lines=%d" % len(maps.splitlines()), flush=True)
# 找包含 8ae000 的映射
for ln in maps.splitlines():
    try:
        a, b = ln.split()[0].split("-")
        if int(a, 16) <= 0x8ae000 < int(b, 16):
            print("8AE000 IN:", ln[:180], flush=True)
    except Exception:
        pass
# tinyproot 相关行
for ln in maps.splitlines():
    if "tinyproot" in ln:
        print("TP:", ln[:180], flush=True)
# 死亡确认
time.sleep(4)
p2 = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
print("alive after 4s:", p2 or "DEAD", flush=True)
