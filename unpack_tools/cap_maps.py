"""抓 8ae000 映射: 高频轮询 Ekd5 出生 → 立即 dump /proc/PID/maps 中 8ae000 附近行.
用法: python cap_maps.py
"""
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
print("launched, polling...", flush=True)
got = 0
while time.time() - t0 < 60:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if p:
        born = time.time() - t0
        # 立刻抓 maps（run-as cat）
        r = sh(["shell", "run-as com.winlatcn sh -c 'grep -E \"^00[0-9a-f]|^0[0-9a-f]{2}\" /proc/%s/maps | head -40'" % p], 30)
        print("born at +%.1fs pid=%s" % (born, p), flush=True)
        print("--- maps (low region) ---", flush=True)
        print(txt(r)[:2500], flush=True)
        # 抓 stack 相关行
        r2 = sh(["shell", "run-as com.winlatcn sh -c 'grep -iE \"stack|8ae|008ae\" /proc/%s/maps | head -20'" % p], 30)
        print("--- 8ae/stack lines ---", flush=True)
        print(txt(r2)[:1200], flush=True)
        got = 1
        break
    time.sleep(0.15)
if not got:
    print("never born in 60s", flush=True)
# 等死亡
time.sleep(6)
p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
print("alive after 6s:", p or "DEAD", flush=True)
