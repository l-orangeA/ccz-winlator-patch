"""备份手机侧 win32u 两个半边 + 当前部署 ntdll → device_backup\\win32u_orig_20261106\\"""
import subprocess, os

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
DST = r"D:\ccz_winlator_patch\device_backup\win32u_orig_20261106"

pairs = [
    ("files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so", "win32u.so"),
    ("files/imagefs/opt/wine/lib/wine/x86_64-windows/win32u.dll", "win32u.dll"),
    ("files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so", "ntdll_deployed_20261106.so"),
]

os.makedirs(DST, exist_ok=True)
for remote, local in pairs:
    r = subprocess.run([ADB, "-s", SER, "exec-out", "run-as com.winlatcn cat " + remote],
                       capture_output=True, env=ENV, timeout=120)
    p = os.path.join(DST, local)
    open(p, "wb").write(r.stdout)
    print(local, len(r.stdout), "bytes")
