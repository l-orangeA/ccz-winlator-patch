"""解剖冻结的 Ekd5 进程: 每线程的 name/state/wchan/kernel stack.
用法: python inspect_frozen.py <pid>
"""
import subprocess, os, sys

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PID = sys.argv[1] if len(sys.argv) > 1 else "10615"


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 线程列表
r = sh(["shell", "run-as com.winlatcn sh -c 'ls /proc/%s/task'" % PID])
tasks = txt(r).split()
print("threads:", len(tasks), flush=True)
for t in tasks:
    name = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/task/%s/comm 2>/dev/null'" % (PID, t)])).strip()
    stat = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/task/%s/stat 2>/dev/null'" % (PID, t)]))
    state = stat.rsplit(")", 1)[-1].split()[0] if ")" in stat else "?"
    utime = stat.rsplit(")", 1)[-1].split()[13] if ")" in stat else "?"
    wchan = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/task/%s/wchan 2>/dev/null'" % (PID, t)])).strip()
    print("  tid=%-7s %-18s state=%s utime=%-6s wchan=%s" % (t, name, state, utime, wchan), flush=True)
# 主线程的内核栈（可能无权限）
r = sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/stack 2>/dev/null || echo no-perm'" % PID])
print("=== main thread kernel stack ===")
print(txt(r)[:800], flush=True)
