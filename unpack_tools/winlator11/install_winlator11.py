#!/usr/bin/env python3
"""安装 Winlator 11.2 并做基础探测（包信息 / 是否 debuggable / 数据目录可达性）。"""
import os
import subprocess
import time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
APK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Winlator_11.2.apk")


def sh(a, t=600):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    print("=== 1/4 安装 APK ===", flush=True)
    r = sh(["install", "-r", APK])
    print((txt(r) + r.stderr.decode("utf-8", "replace")).strip()[-400:], flush=True)

    print("=== 2/4 包信息 ===", flush=True)
    print(txt(sh(["shell", "dumpsys package com.winlator"])).count("versionName"), flush=True)
    for line in txt(sh(["shell", "dumpsys package com.winlator"])).splitlines():
        if "versionName" in line or "versionCode" in line or "debuggable" in line.lower():
            print("  " + line.strip(), flush=True)

    print("=== 3/4 数据目录可达性 ===", flush=True)
    print("  path:", txt(sh(["shell", "pm path com.winlator"])).strip()[:200], flush=True)
    r = sh(["shell", "run-as com.winlator ls files/imagefs 2>&1 | head -5"])
    print("  run-as:", txt(r).strip()[:200] or "(空)", flush=True)
    r = sh(["shell", "ls /storage/emulated/0/Android/data/com.winlator 2>&1 | head -5"])
    print("  外部 data:", txt(r).strip()[:200] or "(空)", flush=True)

    print("=== 4/4 启动 ===", flush=True)
    sh(["shell", "monkey -p com.winlator -c android.intent.category.LAUNCHER 1"])
    time.sleep(6)
    print("  pidof:", txt(sh(["shell", "pidof com.winlator"])).strip() or "(未启动)", flush=True)
    print("  完成。首次初始化可能需 1–3 分钟，之后截图确认。", flush=True)


if __name__ == "__main__":
    main()
