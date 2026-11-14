"""手机容器 envVars 增删（不依赖平板脚本）。
用法: python set_env_phone.py K=V [K=V ...]   （K= 为删除）
"""
import subprocess, os, sys, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
CFG = "files/imagefs/home/xuser-1/.container"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    pairs = sys.argv[1:]
    if pairs == ["prod"]:
        d = json.load(open(PROD, encoding="utf-8"))
    else:
        r = sh(["exec-out", "run-as com.winlatcn cat " + CFG], 40)
        d = json.loads(r.stdout.decode("utf-8", "replace"))
        kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
        for spec in pairs:
            k, v = spec.split("=", 1)
            if v == "":
                kv.pop(k, None)
            else:
                kv[k] = v
        d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    tmp = OUT + "\\container_tmp.json"
    open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
    r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo ok"])
    print(txt(r).strip())
    r = sh(["exec-out", "run-as com.winlatcn cat " + CFG], 40)
    d2 = json.loads(r.stdout.decode("utf-8", "replace"))
    print("envVars now:", d2["envVars"])


if __name__ == "__main__":
    main()
