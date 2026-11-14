"""r109d 系列验收 v2：进程存活 + 截图区间判据（游戏画面 0.5~2.5MB，桌面 ~3.3MB，黑屏 <400KB）。
用法: python boot_test.py [ntoggle]
"""
import subprocess, os, sys, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
PROD = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt"
CFG = "files/imagefs/home/xuser-1/.container"
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import a0_rop_trace as T


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def ekd_alive():
    return txt(sh(["shell", "ps -A | grep -c 'Ekd5'"])).strip() not in ("", "0")


def shot_len():
    return len(T.shot())


def restore_prod():
    tmp = OUT + "\\container_prod.json"
    open(tmp, "wb").write(open(PROD, "rb").read())
    sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
    r = sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo ok"])
    print("env:", txt(r).strip())


def wait_boot(maxs=240):
    """标题就绪 = Ekd5 存活 且 截图呈游戏画面（非桌面/非黑屏）连续 2 次。"""
    t0 = time.time()
    streak = 0
    while time.time() - t0 < maxs:
        ok = ekd_alive()
        b = shot_len()
        game_like = ok and 500000 < b < 2500000
        print("  t=%3.0fs alive=%s shot=%d" % (time.time() - t0, ok, b), flush=True)
        streak = streak + 1 if game_like else 0
        if streak >= 2:
            return True, time.time() - t0
        time.sleep(3)
    return False, time.time() - t0


def window_bounds(im):
    """从灰度图找游戏窗口的亮区包围盒（排除顶部状态栏）。返回 (x0,y0,x1,y1) 物理坐标。"""
    from PIL import ImageOps
    g = im.point(lambda v: 255 if v > 28 else 0)
    w, h = g.size
    top = int(h * 0.05)
    px = g.load()
    xs = []
    ys = []
    for y in range(top, h, max(1, (h - top) // 60)):
        row_hit = -1
        row_last = -1
        for x in range(0, w, max(1, w // 120)):
            if px[x, y]:
                if row_hit < 0:
                    row_hit = x
                row_last = x
        if row_hit >= 0:
            xs.append(row_hit)
            xs.append(row_last)
            ys.append(y)
    if not xs:
        return None
    return (min(xs), min(ys), max(xs), max(ys))


def normalize_window():
    """检测窗口边界，返回窗口矩形；找不到则退回全屏假设。"""
    from io import BytesIO
    from PIL import Image
    b = T.shot()
    im = Image.open(BytesIO(b)).convert("L")
    r = window_bounds(im)
    if r is None:
        print("  window detect failed, assume full screen", flush=True)
        return (0, 0, im.size[0], im.size[1])
    x0, y0, x1, y1 = r
    print("  window bounds: %dx%d at (%d,%d)" % (x1 - x0, y1 - y0, x0, y0), flush=True)
    return r


ENGAGE_RELS = [(0.416, 0.674), (0.50, 0.55), (0.46, 0.78), (0.55, 0.45), (0.62, 0.60)]
DESEL_REL = (0.85, 0.90)


def smart_toggle(win, game, g0, prev_rel=None):
    """在窗口内找一个能选中单位（CPU 跳变）的点，然后点空地取消。返回 (本段耗时, 生效点) 或 None。"""
    rels = ([prev_rel] if prev_rel else []) + [r for r in ENGAGE_RELS if r != prev_rel]
    for rel in rels:
        x = int(win[0] + rel[0] * (win[2] - win[0]))
        y = int(win[1] + rel[1] * (win[3] - win[1]))
        t0 = time.time()
        g_before = jiffies(game)
        sh(["shell", "input tap %d %d" % (x, y)])
        time.sleep(0.8)
        g_after = jiffies(game)
        if g_before is not None and g_after is not None and g_after - g_before >= 4:
            dx, dy = DESEL_REL
            sh(["shell", "input tap %d %d" % (int(win[0] + dx * (win[2] - win[0])),
                                              int(win[1] + dy * (win[3] - win[1])))])
            time.sleep(0.35)
            return time.time() - t0, rel
    return None


def pidof(name):
    o = txt(sh(["shell", "pidof " + name])).split()
    return o[0] if o else None


def jiffies(p):
    for _ in range(3):
        s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
        try:
            f = s.rsplit(")", 1)[1].split()
            return int(f[11]) + int(f[12])
        except Exception:
            time.sleep(0.5)
    return None


def alive(p):
    return txt(sh(["shell", "ls /proc/%s/stat" % p])).strip() != ""


def main():
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 10
    restore_prod()
    for attempt in range(3):
        T.cmd_launch()
        ok, t = wait_boot(240)
        print("boot attempt%d: ready=%s t=%.1fs" % (attempt + 1, ok, t), flush=True)
        if ok:
            break
    else:
        print("BOOT FAILED 3x")
        sys.exit(2)
    # 读档进战斗
    scene = False
    for i in range(3):
        T.cmd_battle()
        time.sleep(4)
        b = shot_len()
        alive = ekd_alive()
        print("battle attempt%d: shot=%d alive=%s" % (i + 1, b, alive), flush=True)
        if alive and 700000 < b < 2600000:
            scene = True
            break
    if not scene:
        print("NO SCENE - abort")
        sys.exit(3)
    time.sleep(2)
    win = normalize_window()
    app, game, ws = pidof("com.winlatcn"), pidof("Ekd5.exe"), pidof("wineserver")
    print("pids app=%s game=%s ws=%s" % (app, game, ws), flush=True)
    if not (app and game):
        sys.exit(4)
    time.sleep(2)
    a0, g0, w0 = jiffies(app), jiffies(game), jiffies(ws)
    t0 = time.time()
    engaged_rel = None
    ok_toggles = 0
    for i in range(n):
        res = smart_toggle(win, game, g0, engaged_rel)
        if res is None:
            print("  toggle %2d: no engage point found (skip)" % (i + 1), flush=True)
            continue
        _, engaged_rel = res
        ok_toggles += 1
        g = jiffies(game)
        print("  toggle %2d: engaged at %s game cum=%.3fs" % (i + 1, engaged_rel, (g - g0) / 100), flush=True)
    wall = time.time() - t0
    a, g, w = jiffies(app), jiffies(game), jiffies(ws)
    if None in (a, g, w) or ok_toggles == 0:
        print("RESULT: invalid (ok_toggles=%d)" % ok_toggles)
        sys.exit(5)
    print("RESULT: ok=%d/%d wall=%.1fs game=%.2fs (%.3f/toggle) app=%.2fs (%.3f/toggle) wineserver=%.2fs (%.3f/toggle)"
          % (ok_toggles, n, wall, (g - g0) / 100, (g - g0) / 100 / ok_toggles,
             (a - a0) / 100, (a - a0) / 100 / ok_toggles,
             (w - w0) / 100, (w - w0) / 100 / ok_toggles), flush=True)
    sh(["shell", "input tap 1176 728"])
    time.sleep(0.6)
    open(OUT + "\\toggle_sel.png", "wb").write(T.shot())
    print("toggle_sel.png saved", flush=True)


if __name__ == "__main__":
    main()
