import sys, time, os, subprocess
sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)))
import wl_ui as U

def sh(a, t=40):
    return subprocess.run([U.ADB, "-s", U.SERIAL] + a, capture_output=True,
                          env=dict(os.environ, MSYS_NO_PATHCONV="1"), timeout=t)

def size():
    p = sh(["exec-out", "screencap", "-p"], 60)
    return len(p.stdout), p.stdout

def force_stop():
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])

def open_container():
    sh(["shell", "monkey -p com.winlatcn -c android.intent.category.LAUNCHER 1"], 40)
    time.sleep(3)
    r = None
    for _ in range(8):
        r = U.dump()
        if U.find_text(r, "容器管理") is not None: break
        U.open_drawer(r); time.sleep(1.0); r = U.dump()
        if U.find_text(r, "容器管理") is not None: break
        sh(["shell", "input keyevent 4"]); time.sleep(1.2)
    n = U.find_text(r, "容器管理")
    if n is None: return False
    U.tap_node(n); time.sleep(1.5)
    r = U.dump()
    m = U.find_res(r, "BTMenu")
    if m is None: return False
    U.tap_node(m); time.sleep(1)
    r = U.dump(); n = U.find_text(r, "管理")
    if n is None: return False
    U.tap_node(n); time.sleep(2)
    return True

def run_exe(name):
    for title in ["磁盘 (D:)", "Download", "ccz22", name]:
        ok = False
        for _ in range(5):
            r = U.dump()
            if U.tile_run(r, title): ok = True; break
            time.sleep(1)
        if not ok:
            print("ERR tile", title); return False
        time.sleep(1.4)
    return True

if __name__ == "__main__":
    name = sys.argv[1] if len(sys.argv) > 1 else "Ekd5.exe"
    out = sys.argv[2] if len(sys.argv) > 2 else "shot_measure.png"
    force_stop()
    if not open_container(): print("ERR container"); sys.exit(2)
    t0 = time.time()
    if not run_exe(name): print("ERR run"); sys.exit(2)
    t_launch = time.time() - t0
    last = 0
    while time.time() - t0 < 420:
        n, data = size()
        if n > 800000:
            print("TITLE_AT %.1f s (launch+%.1f)" % (time.time() - t0, t_launch))
            open(out, "wb").write(data)
            sys.exit(0)
        time.sleep(4)
    print("TIMEOUT last=%d" % last)
    sys.exit(3)
