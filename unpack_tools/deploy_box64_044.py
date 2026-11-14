"""部署 box64 0.4.4（11.2 作者编译版）做甄别实验。
用法: python deploy_box64_044.py <deploy|rollback>
deploy = 备份当前 box64 为 box64.b044test 并换 0.4.4
rollback = 恢复 box64.bak030（原版 0.3.0）
"""
import subprocess, os, sys

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
REMOTE = "files/imagefs/usr/local/bin/box64"
SRC = r"D:\ccz_winlator_patch\unpack_tools\box64_044_glibc231"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else ""
    if which == "deploy":
        print(txt(sh(["shell", "run-as com.winlatcn sh -c 'cp %s %s.b044test 2>/dev/null; echo backed'" % (REMOTE, REMOTE)])).strip())
        r = sh(["push", SRC, "/data/local/tmp/b644"])
        print(r.stdout.decode("utf-8", "replace").strip())
        r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/b644 %s && echo copied" % REMOTE])
        print(txt(r).strip())
        sh(["shell", "rm -f /data/local/tmp/b644"])
    elif which == "rollback":
        r = sh(["shell", "run-as com.winlatcn sh -c 'cp %s.bak030 %s && echo rolled'" % (REMOTE, REMOTE)])
        print(txt(r).strip())
    else:
        print(__doc__)
        return
    r = sh(["shell", "run-as com.winlatcn md5sum " + REMOTE])
    print("device md5:", txt(r).strip())


if __name__ == "__main__":
    main()
