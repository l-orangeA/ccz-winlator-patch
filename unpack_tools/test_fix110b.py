"""fix110b 验证: 部署 ntdll, 启动, 轮询 stats 到解包完成, 拉视图日志.
用法: python test_fix110b.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


r = sh(["push", r"D:\ccz_winlator_patch\build\ntdll_fix110b.so", "/data/local/tmp/n110b"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/n110b files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ntdll-ok"])
sh(["shell", "rm -f /data/local/tmp/n110b"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print(txt(r).strip()[:70], "(want eddb74af...)", flush=True)
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print(txt(r).strip()[:60], "(want d234aa57 r113)", flush=True)

sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
last = ""
done = False
while time.time() - t0 < 240:
    s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
    if s != "no" and s != last:
        print("+%3.0fs %s" % (time.time() - t0, s), flush=True)
        last = s
        if "arms=1600" in s or "arms=1536" in s:
            done = True
            break
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p and time.time() - t0 > 20:
        print("DIED at +%.0fs (last=%s)" % (time.time() - t0, last), flush=True)
        break
    time.sleep(3)
print("unpack complete:", done, flush=True)
time.sleep(8)
r = sh(["exec-out", "run-as com.winlatcn sh -c 'grep -c \"FIX110 view\" files/imagefs/home/xuser-1/hwbp.log 2>/dev/null; grep -m2 \"FIX110 view\" files/imagefs/home/xuser-1/hwbp.log 2>/dev/null; grep -m2 \"7ffe\" files/imagefs/home/xuser-1/hwbp.log 2>/dev/null'"])
print("views log:\n" + txt(r).strip()[:600], flush=True)
