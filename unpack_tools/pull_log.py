"""拉 hwbp.log 原始字节。用法: python pull_log.py"""
import subprocess, os

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
r = subprocess.run([ADB, "-s", SER, "exec-out", "run-as com.winlatcn cat files/imagefs/home/xuser-1/hwbp.log"],
                   capture_output=True, env=ENV, timeout=120)
p = r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log"
open(p, "wb").write(r.stdout)
print("saved", len(r.stdout), "bytes")
