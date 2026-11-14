"""部署 r112d（0.4.4 SIGTRAP 不可延迟 + glitch 修复版）。用法: python deploy_r112d.py"""
import subprocess, os

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
REMOTE = "files/imagefs/usr/local/bin/box64"
SRC = r"D:\ccz_winlator_patch\build\box64_r112d_044.so"
MD5 = "18b1f670ab3bd54f933a32ac0b7d6075"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


r = sh(["push", SRC, "/data/local/tmp/bc"])
print(txt(r).strip()[:90])
r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/bc %s && echo copied" % REMOTE])
print(txt(r).strip())
r = sh(["shell", "run-as com.winlatcn md5sum " + REMOTE])
print("device:", txt(r).strip()[:60])
print("expect:", MD5)
sh(["shell", "rm -f /data/local/tmp/bc"])
