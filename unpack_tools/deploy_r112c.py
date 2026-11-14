"""部署 r112c（0.4.4 no_tf 修复版）。用法: python deploy_r112c.py"""
import subprocess, os

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
REMOTE = "files/imagefs/usr/local/bin/box64"
SRC = r"D:\ccz_winlator_patch\build\box64_r112c_044.so"
MD5 = "a416ecd069f1f349a0d497c312434893"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


r = sh(["push", SRC, "/data/local/tmp/bc"])
print(txt(r).strip()[:90])
r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/bc %s && echo copied" % REMOTE])
print(txt(r).strip())
r = sh(["shell", "run-as com.winlatcn md5sum " + REMOTE])
print("device:", txt(r).strip())
print("expect:", MD5)
sh(["shell", "rm -f /data/local/tmp/bc"])
