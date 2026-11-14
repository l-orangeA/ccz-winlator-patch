#!/usr/bin/env python3
"""P0 实验台健康闸门：不部署任何东西，直接验证当前生产态能否正常解包。

用途：连续实验出现「早死」时，区分设备卡死簇 vs 实验二进制本身的问题。
判定：arms=1600/1536 且 hits>=380 且进程存活 → PASS（设备正常）。
"""
import os
import re
import subprocess
import time
from datetime import datetime

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
EXEC = "/storage/emulated/0/ccz/Ekd5.exe"


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp_stats.txt'"])
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    t0 = time.time()
    sh(["shell", "am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path " + EXEC])
    last, final, status = "", "", "TIMEOUT"
    while time.time() - t0 < 200:
        s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
        if s and s != "no":
            final = s
            if s != last:
                print("  +%3.0fs  %s" % (time.time() - t0, s), flush=True)
                last = s
        p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
        if not p and time.time() - t0 > 25:
            status = "DIED"
            break
        ma, mh = re.search(r"arms=(\d+)", s), re.search(r"hits=(\d+)", s)
        if ma and mh and int(ma.group(1)) in (1536, 1600) and int(mh.group(1)) >= 380:
            status = "HEALTHY"
            break
        time.sleep(3)
    alive = txt(sh(["shell", "pidof Ekd5.exe"])).strip() or "DEAD"
    print("  ★ status=%s alive=%s elapsed=%.0fs" % (status, alive, time.time() - t0), flush=True)
    open(os.path.join(OUT, "p0_health_result.txt"), "w", encoding="utf-8", newline="\n").write(
        "\n".join([datetime.now().isoformat(timespec="seconds"), "status=" + status,
                   "final=" + (final or "(no stats)"), "alive=" + alive]) + "\n")
    sh(["shell", "am force-stop com.winlatcn"])
    print("PASS：设备正常，早死来自实验二进制" if status == "HEALTHY"
          else "设备异常或启动失败：按卡死簇流程处理（adb reboot）")


if __name__ == "__main__":
    main()
