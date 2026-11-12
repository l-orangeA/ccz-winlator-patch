"""部署/回滚 win32u 到手机。
用法: python deploy_win32u.py <r109e|r109d|r109c|orig>
"""
import subprocess, os, sys

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "10AD981EF20020P"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
REMOTE = "files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so"

SRCS = {
    "r109g": r"D:\ccz_winlator_patch\build\win32u_r109g.so",
    "r110": r"D:\ccz_winlator_patch\build\win32u_r110.so",
    "r109f": r"D:\ccz_winlator_patch\build\win32u_r109f.so",
    "r109e": r"D:\ccz_winlator_patch\build\win32u_r109e.so",
    "r109d": r"D:\ccz_winlator_patch\build\win32u_r109d.so",
    "r109c": r"D:\ccz_winlator_patch\build\win32u_r109c.so",
    "orig": r"D:\ccz_winlator_patch\device_backup\win32u_orig_20261106\win32u.so",
}
MD5S = {
    "r109g": "2be1316c83d603c4defae8669fcc1dfb",
    "r110": "85d84d3419d7ce0366be8d9eb77df95e",
    "r109f": "d4e71a61769482fe00e69f885776ef8a",
    "r109e": "68c1940f442a5a37e45eca1926d173b4",
    "r109d": "03c84e0f3800b45f47cb1862c692511d",
    "r109c": "3f3f93d2c7bf3e94410dcd461607bd12",
    "orig": "2726461e2368c16565fd86b61bfbb878",
}


def sh(a, t=180):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else ""
    if which not in SRCS:
        print(__doc__)
        return
    src = SRCS[which]
    tmp = "/data/local/tmp/w32u.so"
    r = sh(["push", src, tmp], 300)
    print(r.stdout.decode("utf-8", "replace").strip())
    r = sh(["shell", "run-as com.winlatcn cp %s %s && echo copied" % (tmp, REMOTE)])
    print(r.stdout.decode("utf-8", "replace").strip() or r.stderr.decode("utf-8", "replace").strip())
    r = sh(["shell", "run-as com.winlatcn md5sum " + REMOTE])
    print("device md5:", r.stdout.decode("utf-8", "replace").strip())
    sh(["shell", "rm -f " + tmp])
    print("expected:", MD5S[which])


if __name__ == "__main__":
    main()
