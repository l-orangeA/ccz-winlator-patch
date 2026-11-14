"""r109 崩溃取证：开 stderr 落盘（WINE_HWBP_LOG=1），启动→标题→截图→点一次契阔谈宴→逐秒盯 pid。
用法: python diag_r109.py
"""
import subprocess, os, sys, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import a0_rop_trace as T

CFG = "files/imagefs/home/xuser-1/.container"


def set_envlog():
    d = json_load_backup()
    kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
    kv["WINE_HWBP_LOG"] = "1"
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    tmp = OUT + "\\container_diag.json"
    open(tmp, "wb").write(__import__("json").dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
    T.sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
    r = T.sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo ok"])
    print(T.txt(r).strip())


def json_load_backup():
    import json
    # 用生产配置（WINEDEBUG=-all）做基底，只加 WINE_HWBP_LOG=1；绝不能让 +dib 回来
    return json.load(open(r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt", encoding="utf-8"))


def pidof(name):
    r = T.sh(["shell", "pidof " + name])
    return T.txt(r).strip()


def save_shot(name):
    open(OUT + "\\" + name, "wb").write(T.shot())


def main():
    set_envlog()
    for attempt in range(3):
        T.cmd_launch()
        ok, t = T.wait_title(200)
        print("title %s %.1fs" % (ok, t), flush=True)
        if ok:
            break
    else:
        sys.exit(2)
    save_shot("diag_title.png")
    a = T.txt(T.sh(["shell", "ps -A | grep -c winlat"])).strip()
    g = T.txt(T.sh(["shell", "ps -A | grep -c Ekd5"])).strip()
    print("ps before click: winlat=%s Ekd5=%s" % (a, g), flush=True)
    T.sh(["shell", "input tap 1785 595"])
    for i in range(15):
        time.sleep(1)
        a = T.txt(T.sh(["shell", "ps -A | grep -c winlat"])).strip()
        g = T.txt(T.sh(["shell", "ps -A | grep -c Ekd5"])).strip()
        print("t+%ds winlat=%s Ekd5=%s" % (i + 1, a, g), flush=True)
        if a == "0":
            break
    save_shot("diag_after.png")
    # 拉日志（只要 tail 部分，hwbp.log 可能很大；用 tail -c 2M）
    r = T.sh(["shell", "run-as com.winlatcn sh -c 'tail -c 2097152 files/imagefs/home/xuser-1/hwbp.log | gzip -c'"], 300)
    if len(r.stdout) > 100:
        open(OUT + "\\diag_tail.log.gz", "wb").write(r.stdout)
        print("log tail saved", len(r.stdout))
    r = T.sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log'"])
    print(T.txt(r).strip())


if __name__ == "__main__":
    main()
