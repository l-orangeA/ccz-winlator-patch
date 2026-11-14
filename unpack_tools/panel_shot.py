"""在已到标题画面的游戏里：读档→选单位→截面板图。
用法: python panel_shot.py <输出名>
"""
import subprocess, os, sys, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import a0_rop_trace as T
import boot_test as B


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else "panel"
    T.cmd_battle()
    time.sleep(4)
    b = len(T.shot())
    print("battle shot=%d" % b, flush=True)
    win = B.normalize_window()
    game = (txt(sh(["shell", "pidof Ekd5.exe"])).split() or [None])[0]
    if not game:
        print("no game"); sys.exit(2)
    # 找一个能选中单位的点
    hit = None
    for rel in B.ENGAGE_RELS:
        x = int(win[0] + rel[0] * (win[2] - win[0]))
        y = int(win[1] + rel[1] * (win[3] - win[1]))
        g0 = B.jiffies(game)
        sh(["shell", "input tap %d %d" % (x, y)])
        time.sleep(0.9)
        g1 = B.jiffies(game)
        if g0 is not None and g1 is not None and g1 - g0 >= 4:
            hit = (x, y)
            print("engaged at %s" % ((x, y),), flush=True)
            break
    time.sleep(1.2)
    open(OUT + "\\" + name + ".png", "wb").write(T.shot())
    print("saved", name + ".png", "panel point:", hit, flush=True)


if __name__ == "__main__":
    main()
