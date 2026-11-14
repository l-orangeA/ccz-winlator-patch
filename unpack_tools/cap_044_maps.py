"""0.4.4 死亡 boot 的 maps 抓取: 设备侧监视器 + 启动。
用法: python cap_044_maps.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 部署 0.4.4 + fix110b
r = sh(["push", r"D:\ccz_winlator_patch\unpack_tools\box64_044_glibc231", "/data/local/tmp/b44"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/b44 files/imagefs/usr/local/bin/box64 && echo b44-ok"])
sh(["shell", "rm -f /data/local/tmp/b44"])
r = sh(["push", r"D:\ccz_winlator_patch\build\ntdll_fix110b.so", "/data/local/tmp/nb"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/nb files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ntdll-ok"])
sh(["shell", "rm -f /data/local/tmp/nb"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print(txt(r).strip()[:160], "(want a7360b1e/eddb74af)", flush=True)

# env: 纯生产（无日志）
sh(["push", PROD, "/data/local/tmp/cnew.json"], 60)
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json files/imagefs/home/xuser-1/.container && echo env-ok"])
sh(["shell", "rm -f /data/local/tmp/cnew.json"])

# 启动设备侧监视器（后台）
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
sh(["shell", "sh /data/local/tmp/watch.sh > /data/local/tmp/watch_out.txt 2>&1 &"])
print("watcher started", flush=True)
time.sleep(1)

# 启动游戏
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)

# 等监视器报告
while time.time() - t0 < 120:
    r = sh(["shell", "cat /data/local/tmp/watch_out.txt 2>/dev/null"])
    out = txt(r).strip()
    if "CAUGHT" in out or "NEVER" in out:
        print("WATCHER:\n" + out, flush=True)
        break
    time.sleep(3)
else:
    print("watcher still running", flush=True)

# 拉死亡时刻的 maps
r = sh(["shell", "wc -l /data/local/tmp/dying_maps.txt 2>/dev/null || echo no-maps"])
print(txt(r).strip(), flush=True)
r = sh(["exec-out", "run-as com.winlatcn cat /data/local/tmp/dying_maps.txt"], 60)
if len(r.stdout) > 50:
    open(OUT + "\\dying_maps_044.txt", "wb").write(r.stdout)
    print("maps saved to PC:", len(r.stdout), "bytes", flush=True)
    # 分析：0x400000-0x425000 的映射状态
    lines = r.stdout.decode("utf-8", "replace").splitlines()
    for ln in lines:
        try:
            a, b = ln.split()[0].split("-")
            a, b = int(a, 16), int(b, 16)
            if a < 0x425000 and b > 0x400000:
                print("  IMAGE REGION:", ln[:150], flush=True)
        except Exception:
            pass
sh(["shell", "am force-stop com.winlatcn"])
