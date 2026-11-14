"""自动战斗 FPS+IO 测量（不点击，只采样）。用法: python measure_autobattle.py [seconds]"""
import sys
sys.path.insert(0, r"D:\ccz_winlator_patch\unpack_tools")
import battle_auto as F


def io_snap(pid):
    s = F.txt(F.sh(["shell", "cat /proc/%s/io" % pid]))
    d = {}
    for line in s.splitlines():
        p = line.split(": ")
        if len(p) == 2 and p[0] in ("rchar", "syscr", "read_bytes"):
            d[p[0]] = int(p[1])
    return d


def main():
    secs = int(sys.argv[1]) if len(sys.argv) > 1 else 60
    pid = F.txt(F.sh(["shell", "pidof Ekd5.exe"])).split()[0]
    print("Ekd5 pid=%s, measuring %ds auto-battle" % (pid, secs), flush=True)
    io0 = io_snap(pid)
    F.fps(secs)
    io1 = io_snap(pid)
    print("IO delta: rchar=%d KB syscr=%d read_bytes=%d KB" % (
        (io1.get("rchar", 0) - io0.get("rchar", 0)) // 1024,
        io1.get("syscr", 0) - io0.get("syscr", 0),
        (io1.get("read_bytes", 0) - io0.get("read_bytes", 0)) // 1024), flush=True)
    F.cpu_rates(10)


if __name__ == "__main__":
    main()
