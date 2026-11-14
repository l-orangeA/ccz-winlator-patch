#!/usr/bin/env python3
"""P0 实验结束后一条命令恢复生产态：box64=0.3.0 + ntdll=fix110b + prod env。

用法: <venv>\\python.exe unpack_tools\\p0_restore.py
验收: 终端打印四件套 md5，与 §1 终态表逐项比对。
"""
import os
import subprocess
import time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
ROOT = r"D:\ccz_winlator_patch"
PROD = os.path.join(ROOT, "device_backup", "phone_container_20261106_prod.txt")
NTDLL = os.path.join(ROOT, "build", "ntdll_fix110b.so")
CFG = "files/imagefs/home/xuser-1/.container"
WANT = {
    "box64": "dd297b6d3b0966f6f8acf0d60495f82c",
    "ntdll.so": "eddb74af96587b15589d430b1199a07b",
    "win32u.so": "2be1316c83d603c4defae8669fcc1dfb",
    "wineserver": "9df1d0c325f2b9c2f7e71c82fb5fab70",
}


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    # 1) box64 回 0.3.0
    print(txt(sh(["shell", "run-as com.winlatcn sh -c 'cp files/imagefs/usr/local/bin/box64.bak030 files/imagefs/usr/local/bin/box64 && echo b64-rolled'"])).strip())

    # 2) ntdll 回 fix110b
    sh(["push", NTDLL, "/data/local/tmp/p0n"], 300)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/p0n files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so && echo ntdll-ok"])
    sh(["shell", "rm -f /data/local/tmp/p0n"])

    # 3) env 回生产
    tmp = os.path.join(ROOT, "unpack_tools", "a0_trace", "container_p0_restore.json")
    open(tmp, "wb").write(open(PROD, "rb").read())
    sh(["push", tmp, "/data/local/tmp/p0cfg.json"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/p0cfg.json %s && echo env-ok" % CFG])
    sh(["shell", "rm -f /data/local/tmp/p0cfg.json"])

    # 4) 清残留
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])

    # 5) 四件套复核
    r = txt(sh(["shell", "run-as com.winlatcn md5sum "
                "files/imagefs/usr/local/bin/box64 "
                "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so "
                "files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so "
                "files/imagefs/opt/wine/bin/wineserver"]))
    print("\n=== 四件套 md5 ===")
    ok = True
    for line in r.strip().splitlines():
        print(" ", line.strip())
        parts = line.split()
        if len(parts) == 2:
            key = parts[1].rsplit("/", 1)[-1]
            if key in WANT and parts[0] != WANT[key]:
                print("  ★ MISMATCH:", key, "want", WANT[key])
                ok = False
    print("\n" + ("PASS：生产态已恢复" if ok else "FAIL：请人工核对"))


if __name__ == "__main__":
    main()
