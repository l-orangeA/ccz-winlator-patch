"""不杀进程的平板启动对照：不做 am force-stop / pkill / 清 tmp。"""
import os, re, sys, time

os.environ["ADB"] = r"C:\Users\MECHREV\AppData\Local\Temp\adbx\adb.exe"
sys.path.insert(0, r"D:\ccz_winlator_patch\tools")
import wl_ui as U


def parse(b):
    m = re.findall(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)[0]
    return tuple(map(int, m))


def click_tile(title, max_scroll=8):
    # 文件管理器会记住上次的滚动位置，先拉回顶部
    for _ in range(2):
        U.sh(["shell", "input swipe 1500 500 1500 1800 300"])
        time.sleep(0.6)
    for i in range(max_scroll):
        r = U.dump()
        t = U.find_text(r, title)
        if t is not None:
            x1, y1, x2, y2 = parse(t.get("bounds"))
            cx = (x1 + x2) // 2
            best = None
            for n in r.iter():
                if not n.get("resource-id", "").endswith("BTRun") or not n.get("bounds"):
                    continue
                bx1, by1, bx2, by2 = parse(n.get("bounds"))
                bcx = (bx1 + bx2) // 2
                delta = by1 - y2
                if 0 <= delta <= 220 and abs(bcx - cx) < 300:
                    if best is None or delta < best[0]:
                        best = (delta, n)
            if best is not None:
                x, y = U.tap_node(best[1])
                return True, f"tap ({x},{y}) delta={best[0]} scroll={i}"
        # 目标不可见（或运行按钮被导航栏挡住）→ 上滑一屏再找
        U.sh(["shell", "input swipe 1500 1700 1500 900 400"])
        time.sleep(0.9)
    return False, "not found after scroll"


def main():
    # app 已死，直接起 MainActivity；不 force-stop
    U.sh(["shell", "am start -n com.winlatcn/com.winlator.MainActivity"], timeout=30)
    time.sleep(3)
    r = U.dump()
    print("step0 容器管理:", U.find_text(r, "容器管理") is not None)
    menu = U.find_res(r, "BTMenu")
    if menu is None:
        print("ERR no BTMenu")
        return False
    U.tap_node(menu)
    time.sleep(1)
    r = U.dump()
    n = U.find_text(r, "管理")
    if n is None:
        print("ERR no 管理")
        return False
    U.tap_node(n)
    time.sleep(3)

    for title in ["磁盘 (D:)", "ccz", "Ekd5.exe"]:
        ok, info = click_tile(title)
        if not ok and title == "ccz" and "not visible" in info:
            U.sh(["shell", "input swipe 1500 1700 1500 900 400"])
            time.sleep(1.2)
            ok, info = click_tile(title)
        print("step", title, "OK" if ok else "FAIL", info)
        if not ok:
            return False
        time.sleep(2.5)
    print("OK launched (no force-stop)")
    return True


if __name__ == "__main__":
    print("OK" if main() else "FAIL")
