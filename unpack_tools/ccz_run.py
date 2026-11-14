import sys, time, os, re, subprocess
sys.path.insert(0, r"D:\ccz_winlator_patch\unpack_tools")
import wl_ui as U

def start_container():
    U.sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
    U.sh(["shell", "monkey -p com.winlatcn -c android.intent.category.LAUNCHER 1"], timeout=30)
    time.sleep(3)
    r = None
    for attempt in range(8):
        r = U.dump()
        if U.find_text(r, "\u5bb9\u5668\u7ba1\u7406") is not None:
            break
        U.sh(["shell", "input keyevent 4"]); time.sleep(1.2)
        r = U.dump()
        if U.find_text(r, "\u5bb9\u5668\u7ba1\u7406") is not None:
            break
        U.open_drawer(r); time.sleep(1.0)
    n = U.find_text(r, "\u5bb9\u5668\u7ba1\u7406")
    if n is None:
        print("ERR drawer"); return False
    U.tap_node(n); time.sleep(1.5)
    r = U.dump()
    bt = U.find_res(r, "BTRun")
    if bt is None:
        print("ERR no BTRun"); return False
    U.tap_node(bt)
    return True

def screen(name):
    with open(name, "wb") as f:
        p = subprocess.run([U.ADB, "-s", U.SERIAL, "exec-out", "screencap", "-p"],
                           capture_output=True, env=dict(os.environ, MSYS_NO_PATHCONV="1"), timeout=60)
        f.write(p.stdout)
    return name

def wait_desktop(timeout=120):
    """Wait until the app's file-manager UI disappears (container desktop up)."""
    t0 = time.time()
    while time.time() - t0 < timeout:
        r = U.dump()
        if U.find_text(r, "\u5bb9\u5668\u7ba1\u7406") is None and U.find_res(r, "BTRun") is None:
            return True
        time.sleep(2)
    return False

def tap(x, y):
    U.tap(x, y)

def key(code):
    U.sh(["shell", f"input keyevent {code}"])

def text(s):
    s = s.replace(" ", "%s")
    U.sh(["shell", f"input text '{s}'"])

if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "start"
    if cmd == "start":
        print("OK" if start_container() else "FAIL")
    elif cmd == "screen":
        print(screen(sys.argv[2] if len(sys.argv) > 2 else "scr.png"))
    elif cmd == "wait":
        print("OK" if wait_desktop() else "TIMEOUT")
    elif cmd == "tap":
        tap(int(sys.argv[2]), int(sys.argv[3]))
    elif cmd == "key":
        key(int(sys.argv[2]))
    elif cmd == "text":
        text(sys.argv[2])
