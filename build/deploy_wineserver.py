"""部署/回滚 wineserver。
用法: python deploy_wineserver.py <r112b|r112|orig>
"""
import subprocess, os, sys

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "10AD981EF20020P"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
REMOTE = "files/imagefs/opt/wine/bin/wineserver"

SRCS = {
    "r112b": r"D:\ccz_winlator_patch\build\wineserver_r112b.so",
    "r112": r"D:\ccz_winlator_patch\build\wineserver_r112",
    "orig": "ORIG",
}
MD5S = {
    "r112b": "9df1d0c325f2b9c2f7e71c82fb5fab70",
    "r112": "2c85566762d7434af4ca2ca2d4afb493",
    "orig": "9ebc79db4294ef7bedc4c975345b4e8c",
}


def sh(a, t=180):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else ""
    if which not in SRCS:
        print(__doc__)
        return
    tmp = "/data/local/tmp/ws112"
    if which != "orig":
        r = sh(["push", SRCS[which], tmp], 300)
        print(txt(r).strip())
        r = sh(["shell", "run-as com.winlatcn cp %s %s && echo copied" % (tmp, REMOTE)])
        print(txt(r).strip())
        sh(["shell", "rm -f " + tmp])
    else:
        r = sh(["shell", "run-as com.winlatcn sh -c 'cp %s.orig %s && echo restored'" % (REMOTE, REMOTE)])
        print(txt(r).strip())
    r = sh(["shell", "run-as com.winlatcn md5sum " + REMOTE])
    print("device md5:", txt(r).strip())
    print("expected :", MD5S[which])


if __name__ == "__main__":
    main()
