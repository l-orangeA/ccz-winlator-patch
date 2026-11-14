"""0.3.0 对照: 抓 born+3s 的 /proc/PID/maps 低区+大区. 用法: python cap_maps_030.py"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 确认 0.3.0 在位（不匹配就回滚）
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:70], flush=True)
if "dd297b6d3b0966f6f8acf0d60495f82c" not in txt(r):
    print("NOT 0.3.0 -> rolling back", flush=True)
    r = sh(["shell", "run-as com.winlatcn sh -c 'cp files/imagefs/usr/local/bin/box64.bak030 files/imagefs/usr/local/bin/box64 && echo rolled'"])
    print(txt(r).strip())
    r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
    print("now:", txt(r).strip()[:70], flush=True)
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
p = None
while time.time() - t0 < 120:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if p:
        print("born +%.1fs pid=%s" % (time.time() - t0, p), flush=True)
        break
    time.sleep(0.3)
if not p:
    print("never born"); raise SystemExit(1)
time.sleep(3)  # 到达与 0.4.4 死亡相当的生命周期点
r = sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/maps'" % p], 60)
maps = txt(r)
open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\maps_030.txt", "w", encoding="utf-8").write(maps)
low = [ln for ln in maps.splitlines() if ln and int(ln.split("-")[0], 16) < 0x40000000]
print("--- low maps (<1GB) ---", flush=True)
for ln in low[:30]:
    print(ln[:140], flush=True)
print("--- giant ---", flush=True)
for ln in maps.splitlines():
    if "30110" in ln or ("---p" in ln and int(ln.split("-")[0], 16) < 0x1000000 < int(ln.split("-")[1].split()[0], 16)):
        print(ln[:140], flush=True)
        break
