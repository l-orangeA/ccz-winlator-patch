#!/usr/bin/env python3
"""抓 hwbp_update 的调用点：作者版 0.4.4 + 探针 ntdll（fix110c + HWBP_CALLER）。

日志走 WINE_HWBP_LOG=1（stderr 重定向到 /home/xuser/hwbp.log，由容器启动脚本处理）。
只用于取证，不用于验收。跑完设备停在实验态，需 p0_restore.py。
"""
import hashlib
import json
import os
import subprocess
import time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
ROOT = r"D:\ccz_winlator_patch"
OUT = os.path.join(ROOT, "unpack_tools", "a0_trace")
PROD = os.path.join(ROOT, "device_backup", "phone_container_20261106_prod.txt")
CFG = "files/imagefs/home/xuser-1/.container"
B64 = os.path.join(ROOT, "unpack_tools", "box64_044_glibc231")
NTDLL = os.path.join(ROOT, "build", "ntdll_hwbp_probe.so")
LOG_DEV = "files/imagefs/home/xuser-1/hwbp.log"
EXEC = "/storage/emulated/0/ccz/Ekd5.exe"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def deploy(local, remote, tag):
    want = hashlib.md5(open(local, "rb").read()).hexdigest()
    tmp = "/data/local/tmp/" + tag
    sh(["push", local, tmp], 300)
    sh(["shell", "run-as com.winlatcn cp %s %s" % (tmp, remote)])
    sh(["shell", "rm -f " + tmp])
    got = txt(sh(["shell", "run-as com.winlatcn md5sum " + remote])).strip()
    print("  %s %s want %s" % (tag, got[:40], want[:12]), flush=True)
    if want not in got:
        raise SystemExit("deploy md5 mismatch")


def main():
    print("=== HWBP_CALLER 取证：作者版 0.4.4 + 探针 ntdll ===", flush=True)
    deploy(B64, "files/imagefs/usr/local/bin/box64", "hb64")
    deploy(NTDLL, "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so", "hnt")

    d = json.load(open(PROD, encoding="utf-8"))
    kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
    kv["WINE_HWBP_LOG"] = "1"
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    tmp = os.path.join(OUT, "container_hwbp_caller.json")
    open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    sh(["push", tmp, "/data/local/tmp/hcfg"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/hcfg %s" % CFG])
    sh(["shell", "rm -f /data/local/tmp/hcfg"])

    sh(["shell", "run-as com.winlatcn sh -c 'rm -f %s files/imagefs/home/xuser-1/hwbp_stats.txt'" % LOG_DEV])
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    t0 = time.time()
    sh(["shell", "am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path " + EXEC])
    print("  launched", flush=True)
    while time.time() - t0 < 90:
        s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
        if "arms=" in s:
            try:
                if int(s.split("arms=")[1].split()[0]) >= 1500:
                    print("  +%3.0fs %s" % (time.time() - t0, s), flush=True)
                    break
            except ValueError:
                pass
        time.sleep(3)
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(1)
    r = sh(["exec-out", "run-as com.winlatcn cat " + LOG_DEV], 180)
    data = r.stdout
    p = os.path.join(OUT, "hwbp_caller.log")
    open(p, "wb").write(data)
    print("  log %d bytes -> %s" % (len(data), p), flush=True)
    n = 0
    for ln in data.decode("utf-8", "replace").splitlines():
        if "HWBP_CALLER" in ln or "WCTX" in ln or "GCTX_DBG" in ln:
            print("   ", ln[:150], flush=True)
            n += 1
            if n >= 18:
                break
    print("  ⚠ 设备在实验态，跑 p0_restore.py", flush=True)


if __name__ == "__main__":
    main()
