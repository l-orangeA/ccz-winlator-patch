"""范围开关验收测试（fresh_toggle2 协议，参考图缺失版）。
用法: python toggle_test.py [ntoggle]   前提: 设备已连接，游戏未启动
流程: launch → waittitle → 读档战斗 → N 次 [tap(1176,728)+0.35s / tap(1800,1000)+0.35s] → CPU 统计
基线: 游戏 0.306 s/次、App 0.345 s/次（2026-11-06 fr60_glpin + 原版 win32u）
"""
import subprocess, os, sys, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import a0_rop_trace as T


def pidof(name):
    r = T.sh(["shell", "pidof " + name])
    o = T.txt(r).split()
    return o[0] if o else None


def jiffies(p):
    s = T.txt(T.sh(["shell", "cat /proc/%s/stat" % p]))
    try:
        f = s.rsplit(")", 1)[1].split()
        return int(f[11]) + int(f[12])
    except Exception:
        return None


def main():
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 10
    for attempt in range(3):
        T.cmd_launch()
        ok, t = T.wait_title(200)
        print("title attempt%d: %s %.1fs" % (attempt + 1, ok, t), flush=True)
        if ok:
            break
    else:
        print("BOOT FAILED 3x")
        sys.exit(2)
    scene = False
    for i in range(3):
        T.cmd_battle()
        time.sleep(3)
        if len(T.shot()) > 1500000:
            scene = True
            break
        print("scene check %d failed" % (i + 1), flush=True)
    if not scene:
        print("NO SCENE - abort (no blind toggles)")
        sys.exit(3)
    app, game, ws = pidof("com.winlatcn"), pidof("Ekd5.exe"), pidof("wineserver")
    if not (app and game and ws):
        print("PIDS MISSING - abort")
        sys.exit(4)
    print("pids app=%s game=%s wineserver=%s" % (app, game, ws), flush=True)
    time.sleep(2)
    a0, g0, w0 = jiffies(app), jiffies(game), jiffies(ws)
    t0 = time.time()
    for i in range(n):
        T.sh(["shell", "input tap 1176 728"])
        time.sleep(0.35)
        T.sh(["shell", "input tap 1800 1000"])
        time.sleep(0.35)
        print("toggle %d done" % (i + 1), flush=True)
    wall = time.time() - t0
    a, g, w = jiffies(app), jiffies(game), jiffies(ws)
    print("RESULT: wall=%.1fs game=%.2fs (%.3f/toggle) app=%.2fs (%.3f/toggle) wineserver=%.2fs (%.3f/toggle)"
          % (wall, (g - g0) / 100, (g - g0) / 100 / n, (a - a0) / 100, (a - a0) / 100 / n,
             (w - w0) / 100, (w - w0) / 100 / n), flush=True)
    # 存一张选中瞬间截图供人工核对（第 1 次开关内的画面可能已关闭）
    T.sh(["shell", "input tap 1176 728"])
    time.sleep(0.6)
    open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\toggle_sel.png", "wb").write(T.shot())


if __name__ == "__main__":
    main()
