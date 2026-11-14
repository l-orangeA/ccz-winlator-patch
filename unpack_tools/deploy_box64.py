"""部署/回滚 box64 到手机（imagefs/usr/local/bin/box64）。
用法: python deploy_box64.py <fast|rollback>
fast = 部署补丁版 build/box64_r110b（先自动备份当前 box64 为 box64.work030）
rollback = 恢复 box64.bak030（原版）
"""
import subprocess, os, sys

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
REMOTE = "files/imagefs/usr/local/bin/box64"


def sh(a, t=180):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else ""
    if which == "fast":
        src = r"D:\ccz_winlator_patch\build\box64_r111.so"
        tmp = "/data/local/tmp/b64new.so"
        print(txt(sh(["shell", "run-as com.winlatcn sh -c 'cp %s %s.work030 2>/dev/null; echo backed'" % (REMOTE, REMOTE)])).strip())
        r = sh(["push", src, tmp], 300)
        print(r.stdout.decode("utf-8", "replace").strip())
        r = sh(["shell", "run-as com.winlatcn cp %s %s && echo copied" % (tmp, REMOTE)])
        print(txt(r).strip())
        r = sh(["shell", "run-as com.winlatcn md5sum " + REMOTE])
        print("device md5:", txt(r).strip())
        sh(["shell", "rm -f " + tmp])
        print("expected: 4f9c18aade89f6c36dd160a9288583f1")
    elif which == "rollback":
        r = sh(["shell", "run-as com.winlatcn sh -c 'cp %s.bak030 %s && echo rolled-back'" % (REMOTE, REMOTE)])
        print(txt(r).strip())
        r = sh(["shell", "run-as com.winlatcn md5sum " + REMOTE])
        print("device md5:", txt(r).strip())
    else:
        print(__doc__)


if __name__ == "__main__":
    main()
