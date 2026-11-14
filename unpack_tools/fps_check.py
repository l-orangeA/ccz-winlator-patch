"""最终状态战斗 FPS 实测：boot_test 可靠进战斗 + battle_auto.fps 计数。
用法: python fps_check.py [seconds]
"""
import sys, time
sys.path.insert(0, r"D:\ccz_winlator_patch\unpack_tools")
import a0_rop_trace as T
import boot_test as B
import battle_auto as F


def main():
    secs = int(sys.argv[1]) if len(sys.argv) > 1 else 30
    for attempt in range(3):
        T.cmd_launch()
        ok, t = B.wait_boot(220)
        print("boot%d ready=%s %.0fs" % (attempt + 1, ok, t), flush=True)
        if not ok:
            continue
        scene = False
        for i in range(3):
            T.cmd_battle()
            time.sleep(4)
            b = len(T.shot())
            if B.ekd_alive() and 700000 < b < 2600000:
                scene = True
                break
        if not scene:
            print("no scene"); continue
        B.normalize_window()
        print("battle ready, measuring fps %ds" % secs, flush=True)
        game_pid = F.txt(F.sh(["shell", "pidof Ekd5.exe"])).split()[0]

        def io_snap():
            s = F.txt(F.sh(["shell", "cat /proc/%s/io" % game_pid]))
            d = {}
            for line in s.splitlines():
                p = line.split(": ")
                if len(p) == 2 and p[0] in ("rchar", "syscr", "read_bytes"):
                    d[p[0]] = int(p[1])
            return d

        io0 = io_snap()
        F.fps(secs)
        io1 = io_snap()
        print("IO delta: rchar=%d KB syscr=%d read_bytes=%d KB" % (
            (io1.get("rchar", 0) - io0.get("rchar", 0)) // 1024,
            io1.get("syscr", 0) - io0.get("syscr", 0),
            (io1.get("read_bytes", 0) - io0.get("read_bytes", 0)) // 1024), flush=True)
        F.cpu_rates(10)
        return
    print("ALL FAILED")


if __name__ == "__main__":
    main()
