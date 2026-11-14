"""r109c 加载期取证：WINEDEBUG=+loaddll,+seh + stderr→hwbp.log，看 win32u.so 是否加载/初始化、有没有异常。
用法: python diag_load.py
"""
import subprocess, os, sys, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import a0_rop_trace as T


def set_env():
    d = json.load(open(PROD, encoding="utf-8"))
    kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
    kv["WINEDEBUG"] = "+loaddll,+seh"
    kv["WINE_HWBP_LOG"] = "1"
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    tmp = OUT + "\\container_diag2.json"
    open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
    r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo ok"])
    print(T.txt(r).strip())


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def main():
    set_env()
    T.cmd_launch()
    time.sleep(150)  # 解包+标题窗口期（无论死活都取证）
    r = sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log'"])
    print(T.txt(r).strip())
    r = sh(["shell", "run-as com.winlatcn sh -c 'tail -c 6291456 files/imagefs/home/xuser-1/hwbp.log | gzip -c'"], 300)
    open(OUT + "\\diag_load_tail.log.gz", "wb").write(r.stdout)
    print("tail saved", len(r.stdout))
    r = sh(["shell", "run-as com.winlatcn cat files/imagefs/home/xuser-1/hwbp_stats.txt"])
    print(T.txt(r).strip())
    # 恢复生产 env
    tmp = OUT + "\\container_prod.json"
    open(tmp, "wb").write(open(PROD, "rb").read())
    sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo restored"])
    sh(["shell", "am force-stop com.winlatcn"])


if __name__ == "__main__":
    main()
