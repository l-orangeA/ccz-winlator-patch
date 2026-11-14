#!/usr/bin/env python3
"""把 Winlator 11.2 容器对齐老 ccz 容器（能对齐的全对齐）。

可对齐：envVars / cpuList / cpuListWoW64 / wincomponents / drives /
        box64Preset / screenSize / showFPS / name
不可对齐（v11.2 已移除旧驱动）：
        graphicsDriver: virgl -> 保持 vortek,gladio
        dxwrapper: wined3d -> 保持 dxvk
        audioDriver: pulseaudio -> 保持 alsa
"""
import json
import os
import subprocess
import time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
HERE = os.path.dirname(os.path.abspath(__file__))
OLD_CFG = "files/imagefs/home/xuser-1/.container"
NEW_CFG = "files/rootfs/home/xuser-1/.container"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def cat(pkg, path):
    return sh(["exec-out", "run-as %s cat %s" % (pkg, path)]).stdout.decode("utf-8", "replace").strip()


def main():
    old = json.loads(cat("com.winlatcn", OLD_CFG))
    raw_new = cat("com.winlator", NEW_CFG)
    new = json.loads(raw_new)
    open(os.path.join(HERE, "container11_before.json"), "w", encoding="utf-8").write(raw_new)

    copy_keys = ["screenSize", "envVars", "cpuList", "cpuListWoW64",
                 "wincomponents", "drives", "box64Preset"]
    for k in copy_keys:
        if k in old:
            new[k] = old[k]
    new["name"] = "ccz"
    if "showFPS" in old:
        new["showFPS"] = old["showFPS"]
    new["hudMode"] = 0

    out = json.dumps(new, ensure_ascii=False, separators=(",", ":"))
    print("对齐后:", out[:400], flush=True)
    print("\n保留 v11.2 驱动栈（旧驱动已移除）：")
    print("  graphicsDriver =", new.get("graphicsDriver"))
    print("  dxwrapper      =", new.get("dxwrapper"))
    print("  audioDriver    =", new.get("audioDriver"))

    p = os.path.join(HERE, "container11_synced.json")
    open(p, "w", encoding="utf-8").write(out)
    sh(["push", p, "/data/local/tmp/c11s.json"])
    sh(["shell", "run-as com.winlator cp /data/local/tmp/c11s.json " + NEW_CFG])
    sh(["shell", "rm -f /data/local/tmp/c11s.json"])
    got = cat("com.winlator", NEW_CFG)
    print("\n回读一致:", got == out, flush=True)

    sh(["shell", "am force-stop com.winlator"])
    time.sleep(2)
    sh(["shell", "monkey -p com.winlator -c android.intent.category.LAUNCHER 1"])
    time.sleep(6)
    print("app 已重启，容器配置生效。", flush=True)


if __name__ == "__main__":
    main()
