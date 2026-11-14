"""抓死亡 boot 的 maps: 高频轮询(100ms) → 出生即抓 maps+KUSD状态 → 死亡确认.
用法: python cap_dying_maps.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 确认 0.4.4 + fix110b 在位
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print(txt(r).strip()[:160], flush=True)
if "a7360b1e" not in txt(r):
    print("deploying 0.4.4...", flush=True)
    sh(["push", r"D:\ccz_winlator_patch\unpack_tools\box64_044_glibc231", "/data/local/tmp/b44"])
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/b44 files/imagefs/usr/local/bin/box64 && echo ok"])
    sh(["shell", "rm -f /data/local/tmp/b44"])
if "eddb74af" not in txt(r):
    sh(["push", r"D:\ccz_winlator_patch\build\ntdll_fix110b.so", "/data/local/tmp/nb"])
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/nb files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ok"])
    sh(["shell", "rm -f /data/local/tmp/nb"])

sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched, fast polling...", flush=True)
# 高频轮询：一出生就抓 maps（死亡窗口 2.1s，每 150ms 轮询）
born_pid = None
while time.time() - t0 < 60:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if p:
        born_pid = p
        born_t = time.time() - t0
        print("BORN +%.2fs pid=%s — grabbing maps NOW" % (born_t, p), flush=True)
        break
    time.sleep(0.15)
if not born_pid:
    raise SystemExit("never born")
# 抓 maps（低区 0x400000-0x600000 + 7ffe）
r = sh(["shell", "run-as com.winlatcn sh -c 'grep -E \"^00[0-9a-f]|^0[12]\" /proc/%s/maps | head -50'" % born_pid], 30)
maps1 = txt(r)
print("--- maps at birth ---", flush=True)
print(maps1[:2000], flush=True)
open(OUT + "\\dying_maps_birth.txt", "w", encoding="utf-8").write(maps1)
# 死亡确认
while time.time() - t0 < 60:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p:
        print("DIED +%.2fs after birth" % (time.time() - born_t), flush=True)
        break
    time.sleep(0.2)
else:
    print("still alive 60s", flush=True)
