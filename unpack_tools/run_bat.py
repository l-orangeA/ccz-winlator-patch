import sys, time
sys.path.insert(0, r"D:\ccz_winlator_patch\unpack_tools")
import wl_ui as U

def run_bat(name):
    U.sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
    U.sh(["shell", "monkey -p com.winlatcn -c android.intent.category.LAUNCHER 1"], timeout=30)
    time.sleep(3)
    r = None
    for attempt in range(8):
        r = U.dump()
        if U.find_text(r, "容器管理") is not None:
            break
        U.open_drawer(r); time.sleep(1.0)
        r = U.dump()
        if U.find_text(r, "容器管理") is not None:
            break
        U.sh(["shell", "input keyevent 4"]); time.sleep(1.2)
    n = U.find_text(r, "容器管理")
    if n is None:
        print("ERR drawer"); return False
    U.tap_node(n); time.sleep(1.5); r = U.dump()
    menu = U.find_res(r, "BTMenu")
    if menu is None:
        print("ERR no BTMenu"); return False
    U.tap_node(menu); time.sleep(1)
    r = U.dump(); n = U.find_text(r, "管理")
    if n is None:
        print("ERR no 管理"); return False
    U.tap_node(n); time.sleep(2)
    for title in ["磁盘 (D:)", "Download", "ccz22", name]:
        r = U.dump()
        if not U.tile_run(r, title):
            print("ERR tile", title); return False
        time.sleep(1.8)
    return True

if __name__ == "__main__":
    print("OK" if run_bat(sys.argv[1]) else "FAIL")
