"""B1: wineserver 钉大核对照实验。
前提：游戏已在战斗画面（读档 No.018 汜水关）。
用法: python b1_pin_test.py run [ntoggle]
流程: 记录 pids/亲和 → 未钉核跑 N 次范围开关并计 CPU → taskset wineserver 大核 → 再跑 N 次 → 对比。
"""
import subprocess, os, sys, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = os.environ.get("SERIAL", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"


def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def pidof(name):
    o = txt(sh(["shell", "pidof " + name])).split()
    return o[0] if o else None


def aff(p):
    return txt(sh(["shell", "grep Cpus_allowed_list /proc/%s/status" % p])).strip().split(":")[-1].strip()


def jiffies(p):
    s = txt(sh(["shell", "cat /proc/%s/stat" % p]))
    try:
        f = s.rsplit(")", 1)[1].split()
        return int(f[11]) + int(f[12])
    except Exception:
        return None


def tap(x, y):
    sh(["shell", "input tap %d %d" % (x, y)])


def screen(name):
    with open(name, "wb") as f:
        p = subprocess.run([ADB, "-s", SER, "exec-out", "screencap -p"],
                           capture_output=True, env=ENV, timeout=60)
        f.write(p.stdout)


def wm_size():
    out = txt(sh(["shell", "wm size"]))
    for line in out.splitlines():
        if "Physical" in line or ":" in line:
            try:
                w, h = line.split(":")[1].strip().split("x")
                return int(w), int(h)
            except Exception:
                pass
    return 2400, 1080


# 视觉坐标（截图按宽边 1920 渲染观察）：骑兵团 (849,455)，空地 (1300,780)
def dev_coords():
    w, h = wm_size()
    imgw = 1920  # 观察基准
    sx = w / imgw
    return int(849 * sx), 455, int(1300 * sx), 780


def measure(ntoggle, label):
    pids = {}
    for n in ("Ekd5.exe", "com.winlatcn", "wineserver"):
        p = pidof(n)
        if p:
            pids[n] = p
    t0 = {n: jiffies(p) for n, p in pids.items()}
    ux, uy, gx, gy = dev_coords()
    clk = time.time()
    for i in range(ntoggle):
        tap(ux, uy); time.sleep(1.0)
        if i == 0:
            screen(OUT + "\\b1_%s_sel.png" % label)
        tap(gx, gy); time.sleep(1.0)
    wall = time.time() - clk
    print("  [%s] toggles=%d wall=%.1fs" % (label, ntoggle, wall), flush=True)
    for n, p in pids.items():
        j1 = jiffies(p)
        if t0[n] is not None and j1 is not None:
            print("    %-14s cpu=%.3fs (%.0f ms/toggle)  aff=%s" % (
                n, (j1 - t0[n]) / 100.0, 1000.0 * (j1 - t0[n]) / 100.0 / ntoggle, aff(p)), flush=True)


def status():
    for n in ("Ekd5.exe", "com.winlatcn", "wineserver"):
        p = pidof(n)
        if p:
            print("  %-14s pid=%-8s aff=%s" % (n, p, aff(p)))


def run(ntoggle=6):
    print("== before ==")
    status()
    measure(ntoggle, "unpinned")
    ws = pidof("wineserver")
    r = txt(sh(["shell", "taskset -p f0 %s" % ws]))
    print("taskset: %s" % r.strip())
    r = txt(sh(["shell", "grep Cpus_allowed_list /proc/%s/status" % ws]))
    print("wineserver aff now: %s" % r.strip())
    time.sleep(2)
    print("== after ==")
    status()
    measure(ntoggle, "pinned")


if __name__ == "__main__":
    if sys.argv[1] == "run":
        run(int(sys.argv[2]) if len(sys.argv) > 2 else 6)
    elif sys.argv[1] == "status":
        status()
