"""最终恢复生产态并验证. 用法: python final_restore.py"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


r = sh(["shell", "run-as com.winlatcn sh -c 'cp files/imagefs/usr/local/bin/box64.bak030 files/imagefs/usr/local/bin/box64 && echo rolled'"])
print(txt(r).strip())
tmp = r"D:\ccz_winlator_patch\unpack_tools\a0_trace\container_final.json"
open(tmp, "wb").write(open(r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt", "rb").read())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json files/imagefs/home/xuser-1/.container && echo env-ok"])
sh(["shell", "rm -f /data/local/tmp/cnew.json"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64 files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so files/imagefs/opt/wine/bin/wineserver"])
print(txt(r).strip()[:250], flush=True)
sh(["shell", "am force-stop com.winlatcn"])
print("production state restored", flush=True)
