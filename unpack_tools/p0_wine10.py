#!/usr/bin/env python3
"""E23：作者版 box64 + Wine 10.10 三件套（ntdll/win32u/wineserver）。

只验 hits 与存活，不做验收。跑完 p0_restore.py 恢复。
"""
import hashlib
import os
import shutil
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
EXEC = "/storage/emulated/0/ccz/Ekd5.exe"
W10 = "//wsl$/Ubuntu2004x64/root/wine1010"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def deploy(local, remote, tag):
    want = hashlib.md5(open(local, "rb").read()).hexdigest()
    tmp = "/data/local/tmp/" + tag
    sh(["push", local, tmp], 600)
    sh(["shell", "run-as com.winlatcn cp %s %s" % (tmp, remote)])
    sh(["shell", "rm -f " + tmp])
    got = txt(sh(["shell", "run-as com.winlatcn md5sum " + remote])).strip()
    print("  %-8s %s want %s" % (tag, got[:40], want[:12]), flush=True)
    if want not in got:
        raise SystemExit("deploy md5 mismatch: " + tag)


def main():
    print("=== E23: box64 0.4.4 + Wine 10.10 ===", flush=True)
    deploy(B64, "files/imagefs/usr/local/bin/box64", "w64")
    deploy(os.path.join(W10, "dlls/ntdll/ntdll.so"),
           "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so", "wn")
    deploy(os.path.join(W10, "dlls/win32u/win32u.so"),
           "files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so", "wu")
    deploy(os.path.join(W10, "server/wineserver"),
           "files/imagefs/opt/wine/bin/wineserver", "ws")

    tmp = os.path.join(OUT, "container_wine10.json")
    open(tmp, "wb").write(open(PROD, "rb").read())
    sh(["push", tmp, "/data/local/tmp/wcfg"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/wcfg %s" % CFG])
    sh(["shell", "rm -f /data/local/tmp/wcfg"])

    sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp_stats.txt'"])
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
        if "arms=" in s:
            try:
                if int(s.split("arms=")[1].split()[0]) >= 1800:
                    print("  ★ 到达判定点", flush=True)
                    break
            except ValueError:
                pass
        p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
        if not p and time.time() - t0 > 50:
            print("  ★ 进程退出（Wine 10 兼容性问题？）", flush=True)
            break
        time.sleep(3)
    else:
        print("  ★ 120s 超时", flush=True)
    sh(["shell", "am force-stop com.winlatcn"])
    print("  ⚠ 设备在实验态，跑 p0_restore.py", flush=True)


if __name__ == "__main__":
    main()
