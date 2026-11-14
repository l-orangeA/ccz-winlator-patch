"""ext4 迁移 A/B：从 G: 盘（imagefs ext4）启动游戏并测战斗 FPS+IO。
v2: 标题按钮自适应（红色块检测+多候选），场景判定要求读档框真实打开过。
用法: python fps_check_ext4.py [seconds]
"""
import sys, time
from io import BytesIO
from PIL import Image
sys.path.insert(0, r"D:\ccz_winlator_patch\unpack_tools")
import a0_rop_trace as T
import boot_test as B
import battle_auto as F

EXEC_PATH = "/data/data/com.winlatcn/files/imagefs/home/xuser-1/ccz/Ekd5.exe"


def launch_ext4():
    T.sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    T.sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    r = T.sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "%s"' % EXEC_PATH])
    print(T.txt(r).strip()[:200], flush=True)


def red_button_candidates():
    """截图右半屏找深红墨迹按钮（跨行累计聚类），返回设备坐标候选（自上而下）。"""
    b = T.shot()
    im = Image.open(BytesIO(b)).convert("RGB")
    w, h = im.size
    px = im.load()
    bars = []  # [y_sum, y_n, x_sum, x_n]
    for y in range(int(h * 0.15), int(h * 0.9), 4):
        for x in range(int(w * 0.5), w, 4):
            r, g, b2 = px[x, y]
            if r > 110 and g < 70 and b2 < 70:
                for bar in bars:
                    if abs(bar[0] // bar[1] - y) < 40:
                        bar[0] += y; bar[1] += 1; bar[2] += x; bar[3] += 1
                        break
                else:
                    bars.append([y, 1, x, 1])
    cands = [(bar[2] // bar[3], bar[0] // bar[1]) for bar in bars if bar[1] >= 6]
    if len(cands) >= 2:
        return [cands[1], (1785, 595), cands[0]] + cands[2:]  # 第2条=契阔谈宴(读档)优先
    return [(1785, 595)] + cands  # 历史坐标兜底


def open_load_dialog():
    """依次尝试候选点，弹出任一对话框（截图骤降 <900KB）即成功。"""
    for i, (x, y) in enumerate(red_button_candidates()):
        if not B.ekd_alive():
            return False
        T.sh(["shell", "input tap %d %d" % (x, y)])
        time.sleep(1.6)
        b = len(T.shot())
        print("  cand%d tap(%d,%d) shot=%d" % (i, x, y, b), flush=True)
        if b < 900000:
            return True
        # 点空地关掉可能误开的菜单
        T.sh(["shell", "input tap 200 1100"])
        time.sleep(0.8)
    return False


def load_battle_ext4():
    if not open_load_dialog():
        return False
    OUT = T.OUT
    T.sh(["shell", "input tap 1400 806"])  # No.018 槽位
    time.sleep(2)
    b = len(T.shot())
    open(OUT + "\\ext4_slot.png", "wb").write(T.shot())
    print("  slot tap: shot=%d (saved ext4_slot.png)" % b, flush=True)
    if b < 900000:
        T.sh(["shell", "input tap 1344 686"])  # 确认"是"
        for wait in (3, 4, 5, 6, 7, 8, 9, 10):
            time.sleep(wait if wait in (3,) else 1)
            bb = len(T.shot())
            open(OUT + "\\ext4_load_%d.png" % wait, "wb").write(T.shot())
            if B.ekd_alive() and 900000 < bb < 2600000:
                print("  battle appeared at +%ds shot=%d" % (wait, bb), flush=True)
                return True
            if not B.ekd_alive():
                print("  game died at +%ds" % wait, flush=True)
                return False
        print("  battle never appeared (last shot saved)", flush=True)
        return False
    print("  slot click did not open confirm", flush=True)
    return False


def io_snap(pid):
    s = F.txt(F.sh(["shell", "cat /proc/%s/io" % pid]))
    d = {}
    for line in s.splitlines():
        p = line.split(": ")
        if len(p) == 2 and p[0] in ("rchar", "syscr", "read_bytes"):
            d[p[0]] = int(p[1])
    return d


def main():
    secs = int(sys.argv[1]) if len(sys.argv) > 1 else 30
    for attempt in range(3):
        launch_ext4()
        ok, t = B.wait_boot(220)
        print("boot%d ready=%s %.0fs" % (attempt + 1, ok, t), flush=True)
        if not ok:
            continue
        if not load_battle_ext4():
            print("battle load failed", flush=True)
            continue
        print("battle ready (ext4), measuring fps %ds" % secs, flush=True)
        pid = F.txt(F.sh(["shell", "pidof Ekd5.exe"])).split()[0]
        io0 = io_snap(pid)
        F.fps(secs)
        io1 = io_snap(pid)
        print("IO delta: rchar=%d KB syscr=%d read_bytes=%d KB" % (
            (io1.get("rchar", 0) - io0.get("rchar", 0)) // 1024,
            io1.get("syscr", 0) - io0.get("syscr", 0),
            (io1.get("read_bytes", 0) - io0.get("read_bytes", 0)) // 1024), flush=True)
        F.cpu_rates(10)
        return
    print("ALL FAILED")


if __name__ == "__main__":
    main()

