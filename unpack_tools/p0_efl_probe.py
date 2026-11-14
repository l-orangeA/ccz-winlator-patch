#!/usr/bin/env python3
"""EFL_PROBE 单变量实验：作者版 0.4.4（含只读探针）+ fix110c，prod 零日志 env。

拉回 /home/xuser/efl_probe.log 到 a0_trace/efl_probe.log。
实验结束设备停在实验态，需跑 p0_restore.py。
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
B64 = os.path.join(ROOT, "build", "box64_efl_probe.so")
NTDLL = os.path.join(ROOT, "build", "ntdll_fix110c.so")
LOG_DEV = "files/imagefs/home/xuser-1/efl_probe.log"
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
    print("  %s md5 %s want %s" % (tag, got[:40], want[:12]), flush=True)
    if want not in got:
        raise SystemExit("ERROR: 部署 md5 不符")


def main():
    print("=== EFL_PROBE：作者版 0.4.4 + fix110c，prod env ===", flush=True)
    deploy(B64, "files/imagefs/usr/local/bin/box64", "p64")
    deploy(NTDLL, "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so", "pn")

    tmp = os.path.join(OUT, "container_efl_probe.json")
    open(tmp, "wb").write(open(PROD, "rb").read())
    sh(["push", tmp, "/data/local/tmp/pcfg"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/pcfg %s" % CFG])
    sh(["shell", "rm -f /data/local/tmp/pcfg"])

    sh(["shell", "run-as com.winlatcn sh -c 'rm -f %s files/imagefs/home/xuser-1/hwbp_stats.txt'" % LOG_DEV])
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])

    t0 = time.time()
    sh(["shell", "am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path " + EXEC])
    last = ""
    while time.time() - t0 < 120:
        s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
        if s and s != "no" and s != last:
            print("  +%3.0fs %s" % (time.time() - t0, s), flush=True)
            last = s
        if "arms=" in s and "hits=" in s:
            try:
                arms = int(s.split("arms=")[1].split()[0])
                hits = int(s.split("hits=")[1].split()[0])
                if arms >= 2000 and hits <= 50:
                    print("  ★ 失控循环已复现，提前结束", flush=True)
                    break
            except ValueError:
                pass
        p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
        if not p and time.time() - t0 > 45:
            print("  ★ 进程退出", flush=True)
            break
        time.sleep(3)

    r = sh(["exec-out", "run-as com.winlatcn cat " + LOG_DEV], 120)
    data = r.stdout
    path = os.path.join(OUT, "efl_probe.log")
    open(path, "wb").write(data)
    print("  probe log: %d bytes -> %s" % (len(data), path), flush=True)
    print("  前 12 行:", flush=True)
    for ln in data.decode("utf-8", "replace").splitlines()[:12]:
        print("   ", ln, flush=True)
    sh(["shell", "am force-stop com.winlatcn"])
    print("  ⚠ 设备仍在实验态，跑 p0_restore.py 恢复", flush=True)


if __name__ == "__main__":
    main()
