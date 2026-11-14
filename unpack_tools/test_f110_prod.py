"""决定性测试: fix110+r113 + 纯生产 env（零日志零特殊开关）→ 壳能否完成解包出标题.
用法: python test_f110_prod.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 校验部署
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so files/imagefs/usr/local/bin/box64"])
print(txt(r).strip()[:150], "(want 7363f071/d234aa57)", flush=True)

# env = 纯生产
tmp = r"D:\ccz_winlator_patch\unpack_tools\a0_trace\container_pure.json"
open(tmp, "wb").write(open(PROD, "rb").read())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-pure"])).strip(), flush=True)
sh(["shell", "rm -f /data/local/tmp/cnew.json"])

# 清理 + 启动
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
# 轮询 arms 到解包完成
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
print("unpack complete (pure env):", done, flush=True)
# 等标题
time.sleep(15)
p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
print("game alive:", p or "DEAD", flush=True)
