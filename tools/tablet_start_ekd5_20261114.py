import os, sys, time

os.environ["ADB"] = r"C:\Users\MECHREV\AppData\Local\Temp\adbx\adb.exe"
sys.path.insert(0, r"D:\ccz_winlator_patch\tools")
import wl_ui as U


def main():
    U.sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(1)
    U.sh(["shell", "monkey -p com.winlatcn -c android.intent.category.LAUNCHER 1"], timeout=30)
    time.sleep(3)
    r = None
    for _ in range(8):
        r = U.dump()
        if U.find_text(r, "容器管理") is not None:
            break
        U.open_drawer(r)
        time.sleep(1.0)
        r = U.dump()
        if U.find_text(r, "容器管理") is not None:
            break
        U.sh(["shell", "input keyevent 4"])
        time.sleep(1.2)
    n = U.find_text(r, "容器管理")
    if n is None:
        print("ERR drawer")
        return False
    U.tap_node(n)
    time.sleep(1.5)
    r = U.dump()
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
    time.sleep(2)
    for title in ["磁盘 (D:)", "ccz", "Ekd5.exe"]:
        r = U.dump()
        found = U.find_text(r, title)
        print("step", title, "found" if found is not None else "MISSING")
        if not U.tile_run(r, title):
            titles = [x.get("text") for x in r.iter()
                      if x.get("resource-id", "").endswith("TVTitle")][:25]
            print("ERR tile", title, "titles=", titles)
            return False
        time.sleep(1.8)
    print("OK launched")
    return True


if __name__ == "__main__":
    print("OK" if main() else "FAIL")
