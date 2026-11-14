"""给 .container 的 drives 追加 G: 盘（指向 imagefs 内游戏目录），或还原。
用法: python set_drives_g.py <add|remove>
"""
import subprocess, os, sys, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
CFG = "files/imagefs/home/xuser-1/.container"
GPATH = "/data/data/com.winlatcn/files/imagefs/home/xuser-1/ccz"


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "add"
    r = sh(["exec-out", "run-as com.winlatcn cat " + CFG], 40)
    d = json.loads(r.stdout.decode("utf-8", "replace"))
    drives = d.get("drives", "")
    if mode == "add":
        if "G:" in drives:
            print("G: already present:", drives)
            return
        d["drives"] = drives + "G:" + GPATH
    else:
        if "G:" not in drives:
            print("no G: to remove")
            return
        d["drives"] = drives.split("G:")[0]
    tmp = OUT + "\\container_g.json"
    open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
    r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo ok"])
    print(txt(r).strip())
    print("drives now:", d["drives"])
    sh(["shell", "rm -f /data/local/tmp/cnew.json"])


if __name__ == "__main__":
    main()
