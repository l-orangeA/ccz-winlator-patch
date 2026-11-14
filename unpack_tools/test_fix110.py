"""fix110 + r113 真机验证: 部署两者, 启动, 轮询 stats 看壳是否突破 384 arms 并完成解包.
用法: python test_fix110.py
"""
import subprocess, os, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 1) 部署 ntdll fix110 + box64 r113
r = sh(["push", r"D:\ccz_winlator_patch\build\ntdll_fix110.so", "/data/local/tmp/n110"])
r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/n110 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ntdll-ok"])
print(txt(r).strip(), flush=True)
r = sh(["push", r"D:\ccz_winlator_patch\build\box64_r113_044.so", "/data/local/tmp/bc"])
r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/bc files/imagefs/usr/local/bin/box64 && echo b64-ok"])
print(txt(r).strip(), flush=True)
sh(["shell", "rm -f /data/local/tmp/n110 /data/local/tmp/bc"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so files/imagefs/usr/local/bin/box64"])
print(txt(r).strip(), "(want 7363f071 / d234aa57)", flush=True)

# 2) env: prod + INTERP_SIGNAL + HWBP_LOG
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["BOX64_DYNAREC_INTERP_SIGNAL"] = "1"
kv["WINE_HWBP_LOG"] = "1"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_f110.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"], 60)
print(txt(sh(["shell", "run-as com.winlatcn sh -c 'echo env-ok'"])).strip(), flush=True)

# 3) 启动 + 轮询 stats
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
    if not p and time.time() - t0 > 15:
        print("DIED at +%.0fs (last=%s)" % (time.time() - t0, last), flush=True)
        break
    time.sleep(3)
# 标题确认
time.sleep(10)
b = txt(sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log'"]))
print(txt(sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log'"])).strip(), flush=True)
r = sh(["exec-out", "run-as com.winlatcn sh -c 'grep -m2 FIX110 files/imagefs/home/xuser-1/hwbp.log 2>/dev/null'"])
print("FIX110 line:", txt(r).strip()[:120], flush=True)
