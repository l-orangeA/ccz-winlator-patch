"""时序竞态验证: 0.4.4(作者版) + fix110b + BIGBLOCK=0(降速) + 纯 env 其余不变.
用法: python test_bigblock0.py
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


# 部署 0.4.4 + fix110b
r = sh(["push", r"D:\ccz_winlator_patch\unpack_tools\box64_044_glibc231", "/data/local/tmp/b44"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/b44 files/imagefs/usr/local/bin/box64 && echo b44-ok"])
sh(["shell", "rm -f /data/local/tmp/b44"])
r = sh(["push", r"D:\ccz_winlator_patch\build\ntdll_fix110b.so", "/data/local/tmp/n110b"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/n110b files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ntdll-ok"])
sh(["shell", "rm -f /data/local/tmp/n110b"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"])
print(txt(r).strip()[:160], "(want a7360b1e/eddb74af)", flush=True)

# env: 纯生产 + BIGBLOCK=0（覆盖默认的 3）
d = json.load(open(PROD, encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["BOX64_DYNAREC_BIGBLOCK"] = "0"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_bb0.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
print(txt(sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])).strip(), flush=True)
print("BIGBLOCK=0, 其余纯生产", flush=True)

# 启动 + 轮询
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
t0 = time.time()
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)
last = ""
result = "timeout"
while time.time() - t0 < 420:
    s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
    if s != "no" and s != last:
        print("+%3.0fs %s" % (time.time() - t0, s), flush=True)
        last = s
        if "arms=1600" in s or "arms=1536" in s:
            result = "UNPACK-COMPLETE"
            break
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not p and time.time() - t0 > 25:
        result = "DIED (last=%s)" % last
        print(result + " at +%.0fs" % (time.time() - t0), flush=True)
        break
    time.sleep(3)
print("RESULT:", result, flush=True)
if "UNPACK" in result:
    time.sleep(15)
    print("game alive:", txt(sh(["shell", "pidof Ekd5.exe"])).strip() or "DEAD", flush=True)
