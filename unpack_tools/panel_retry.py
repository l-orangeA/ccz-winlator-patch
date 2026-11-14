"""r109f 面板截图（带 4 次启动重试）。用法: python panel_retry.py <输出名>"""
import sys, time
sys.path.insert(0, r"D:\ccz_winlator_patch\unpack_tools")
import a0_rop_trace as T
import boot_test as B


def sh(a, t=60):
    return T.sh(a, t)


def txt(r):
    return T.txt(r)


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else "panel"
    for attempt in range(4):
        T.cmd_launch()
        ok, t = T.wait_title(180)
        print("attempt%d title=%s %.0fs" % (attempt + 1, ok, t), flush=True)
        if not ok:
            continue
        T.cmd_battle()
        time.sleep(4)
        b = len(T.shot())
        print("battle shot=%d" % b, flush=True)
        win = B.normalize_window()
        game = (txt(sh(["shell", "pidof Ekd5.exe"])).split() or [None])[0]
        if not game:
            print("no game"); continue
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
                print("engaged at", (x, y), flush=True)
                break
        time.sleep(1.2)
        open(T.OUT + "\\" + name + ".png", "wb").write(T.shot())
        print("SAVED", name, "point:", hit, flush=True)
        return
    print("ALL ATTEMPTS FAILED")


if __name__ == "__main__":
    main()
