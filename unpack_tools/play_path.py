import sys, time, os, subprocess
sys.path.insert(0, r"D:\ccz_winlator_patch\unpack_tools")
import wl_ui as U

def shot(name):
    p = subprocess.run([U.ADB, "-s", U.SERIAL, "exec-out", "screencap", "-p"],
                       capture_output=True, env=dict(os.environ, MSYS_NO_PATHCONV="1"), timeout=60)
    open(name, "wb").write(p.stdout)
    return len(p.stdout)

def main(paths, exe, out):
    U.sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
    U.sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    U.sh(["shell", "monkey -p com.winlatcn -c android.intent.category.LAUNCHER 1"], timeout=40); time.sleep(4)
    r = U.dump()
    if U.find_text(r, "容器管理") is None:
        U.open_drawer(r); time.sleep(1.5); r = U.dump()
    n = U.find_text(r, "容器管理")
    if n is None:
        print("ERR drawer"); return 2
    U.tap_node(n); time.sleep(1.5)
    r = U.dump(); m = U.find_res(r, "BTMenu")
    if m is None:
        print("ERR BTMenu"); return 2
    U.tap_node(m); time.sleep(1)
    r = U.dump(); n = U.find_text(r, "管理")
    if n is None:
        print("ERR 管理"); return 2
    U.tap_node(n); time.sleep(2)
    t0 = time.time()
    for t in list(paths) + [exe]:
        ok = False
        for _ in range(5):
            r = U.dump()
            if U.tile_run(r, t):
                ok = True; break
            time.sleep(1)
        if not ok:
            print("ERR tile", t); return 2
        time.sleep(1.4)
    while time.time() - t0 < 420:
        n2 = shot(out)
        if n2 > 800000:
            print("TITLE_AT %.1f s" % (time.time() - t0)); return 0
        time.sleep(4)
    print("TIMEOUT"); return 3

if __name__ == "__main__":
    exe = sys.argv[1]
    paths = sys.argv[2].split(",") if len(sys.argv) > 2 and sys.argv[2] else []
    sys.exit(main(paths, exe, sys.argv[3] if len(sys.argv) > 3 else "play_test.png"))
