import sys, time, os, subprocess, re

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = "88f39f0d"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")

def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)

def pids():
    out = sh(["shell", "ps -A -o PID,NAME"]).stdout.decode("utf-8", "ignore")
    r = {}
    for line in out.splitlines():
        p = line.split()
        if len(p) >= 2:
            if p[1] == "Ekd5.exe":
                r["game"] = int(p[0])
            elif p[1] == "com.winlatcn":
                r["app"] = int(p[0])
    return r

def cpu_ticks(pid):
    out = sh(["shell", "run-as com.winlatcn cat /proc/%d/stat" % pid]).stdout.decode("utf-8", "ignore")
    if not out.strip():
        return None
    # 字段 14/15 = utime/stime（comm 里可能有空格，取最后一个 ')' 之后切）
    tail = out[out.rfind(")") + 2:].split()
    return int(tail[11]) + int(tail[12])

def main(label, secs=12):
    p = pids()
    if "game" not in p or "app" not in p:
        print("ERR no processes", p); return 2
    a0 = cpu_ticks(p["app"]); g0 = cpu_ticks(p["game"])
    if a0 is None or g0 is None:
        print("ERR cannot read stat"); return 2
    t0 = time.time()
    sh(["shell", "input swipe 700 1200 2300 1200 %d" % (secs * 1000)], secs + 30)
    t1 = time.time()
    a1 = cpu_ticks(p["app"]); g1 = cpu_ticks(p["game"])
    dt = t1 - t0
    da = (a1 - a0) / 100.0; dg = (g1 - g0) / 100.0
    print("%s: %.1fs wall | app %.2fs (%.0f%%) | game %.2fs (%.0f%%) | total %.2fs (%.0f%%)"
          % (label, dt, da, da / dt * 100, dg, dg / dt * 100, da + dg, (da + dg) / dt * 100))
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "run",
                  int(sys.argv[2]) if len(sys.argv) > 2 else 12))
