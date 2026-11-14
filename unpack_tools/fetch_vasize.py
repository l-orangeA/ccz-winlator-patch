"""拉取容器内文本文件. 用法: python fetch_vasize.py <guest文件名>"""
import subprocess, os, sys

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
r = subprocess.run([ADB, "-s", SER, "exec-out",
                    "run-as com.winlatcn cat files/imagefs/home/xuser-1/.wine/drive_c/" + sys.argv[1]],
                   capture_output=True, env=ENV, timeout=60)
sys.stdout.buffer.write(r.stdout)
