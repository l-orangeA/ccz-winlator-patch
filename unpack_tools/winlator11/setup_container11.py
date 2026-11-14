#!/usr/bin/env python3
"""把壳适配写入 Winlator 11.2 的 Container-1：
- envVars 追加 WINEDEBUG=-all WINE_SS=0xe00 WINE_HWBP_SOFT=1
- drives 追加 F:/storage/emulated/0/ccz
改完 force-stop 重启，让配置生效。"""
import json
import os
import subprocess
import time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
HERE = os.path.dirname(os.path.abspath(__file__))
CFG = "files/rootfs/home/xuser-1/.container"
LOCAL = os.path.join(HERE, "container11.json")
LOCAL_NEW = os.path.join(HERE, "container11_new.json")


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def main():
    r = sh(["exec-out", "run-as com.winlator cat " + CFG])
    raw = r.stdout.decode("utf-8", "replace").strip()
    if not raw:
        raise SystemExit("读不到 .container")
    open(LOCAL, "w", encoding="utf-8").write(raw)
    d = json.loads(raw)
    print("原配置:", json.dumps(d, ensure_ascii=False)[:200], flush=True)

    env = d.get("envVars", "")
    add = [x for x in ("WINEDEBUG=-all", "WINE_SS=0xe00", "WINE_HWBP_SOFT=1") if x.split("=")[0] not in env]
    if add:
        d["envVars"] = (env + " " + " ".join(add)).strip()
    if "storage/emulated/0/ccz" not in d.get("drives", ""):
        d["drives"] = d.get("drives", "") + "F:/storage/emulated/0/ccz"
    open(LOCAL_NEW, "w", encoding="utf-8").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")))
    print("新配置:", json.dumps(d, ensure_ascii=False)[:260], flush=True)

    sh(["push", LOCAL_NEW, "/data/local/tmp/c11.json"])
    sh(["shell", "run-as com.winlator cp /data/local/tmp/c11.json " + CFG])
    sh(["shell", "rm -f /data/local/tmp/c11.json"])
    got = sh(["exec-out", "run-as com.winlator cat " + CFG]).stdout.decode("utf-8", "replace")
    print("回读 envVars 含 HWBP:", "WINE_HWBP_SOFT=1" in got, flush=True)
    print("回读 drives 含 ccz:", "storage/emulated/0/ccz" in got, flush=True)

    sh(["shell", "am force-stop com.winlator"])
    time.sleep(2)
    sh(["shell", "monkey -p com.winlator -c android.intent.category.LAUNCHER 1"])
    time.sleep(5)
    print("已重启 app，配置应生效。", flush=True)


if __name__ == "__main__":
    main()
