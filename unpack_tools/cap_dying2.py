"""v2: 设备侧出生监视器 + am start。用法: python cap_dying2.py"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 部署监视脚本
sh(["push", r"D:\ccz_winlator_patch\unpack_tools\watch_birth.sh", "/data/local/tmp/watch.sh"])
sh(["shell", "chmod +x /data/local/tmp/watch.sh"])

# 启动监视器（设备侧后台，输出到文件）
sh(["shell", "sh /data/local/tmp/watch.sh > /data/local/tmp/watch_out.txt 2>&1 &"])
print("watcher started", flush=True)
time.sleep(1)

# 启动游戏
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(1)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)

# 等监视器完成（最多 90s）
t0 = time.time()
while time.time() - t0 < 90:
    r = sh(["shell", "cat /data/local/tmp/watch_out.txt 2>/dev/null"])
    out = txt(r).strip()
    if "CAUGHT" in out or "NEVER" in out:
        print("WATCHER:\n" + out, flush=True)
        break
    time.sleep(3)
else:
    print("watcher timeout", flush=True)

# 拉结果
r = sh(["shell", "wc -l /data/local/tmp/dying_maps.txt 2>/dev/null || echo no-maps"])
print(txt(r).strip(), flush=True)
r = sh(["exec-out", "run-as com.winlatcn cat /data/local/tmp/dying_maps.txt"], 60)
if len(r.stdout) > 50:
    open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\dying_maps_v2.txt", "wb").write(r.stdout)
    print("maps saved to PC:", len(r.stdout), "bytes", flush=True)
