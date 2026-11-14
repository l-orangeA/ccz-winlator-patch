#!/usr/bin/env python3
"""抓 64 位 NtSetContextThread 的 CONTEXT：作者版 0.4.4 + ctx 探针 ntdll。

WCTX64 无条件写 /home/xuser/ctx_probe.log（前 400 条），prod env、零日志通道。
跑完设备停在实验态，需 p0_restore.py。
"""
import hashlib
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
NTDLL = os.path.join(ROOT, "build", "ntdll_ctx_probe.so")
LOG_DEV = "files/imagefs/home/xuser-1/ctx_probe.log"
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
    print("=== WCTX64 取证：作者版 0.4.4 + ctx 探针 ===", flush=True)
    deploy(B64, "files/imagefs/usr/local/bin/box64", "cb64")
    deploy(NTDLL, "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so", "cnt")

    tmp = os.path.join(OUT, "container_ctx_probe.json")
    open(tmp, "wb").write(open(PROD, "rb").read())
    sh(["push", tmp, "/data/local/tmp/ccfg"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/ccfg %s" % CFG])
    sh(["shell", "rm -f /data/local/tmp/ccfg"])

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
    r = sh(["exec-out", "run-as com.winlatcn cat " + LOG_DEV], 120)
    data = r.stdout
    p = os.path.join(OUT, "ctx_probe.log")
    open(p, "wb").write(data)
    print("  log %d bytes -> %s" % (len(data), p), flush=True)
    for ln in data.decode("utf-8", "replace").splitlines()[:20]:
        print("   ", ln[:160], flush=True)
    print("  ⚠ 设备在实验态，跑 p0_restore.py", flush=True)


if __name__ == "__main__":
    main()
