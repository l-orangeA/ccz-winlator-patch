import sys, time, os, subprocess, io
from PIL import Image

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = "TABLET_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")

def sh(a, t=90):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)

def bright_frac(data):
    im = Image.open(io.BytesIO(data)).convert("RGB")
    W, H = im.size
    px = im.load()
    n = tot = 0
    for y in range(0, H, 20):
        for x in range(0, W, 20):
            r, g, b = px[x, y]
            tot += 1
            if r > 90 or g > 90 or b > 90:
                n += 1
    return n / float(tot)

def top_is_ours():
    out = sh(["shell", "dumpsys activity activities | grep topResumedActivity | head -1"]).stdout.decode("utf-8", "ignore")
    return "com.winlatcn" in out

def run(label, exe, secs=90):
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    t0 = time.time()
    t_top = None
    for attempt in range(3):
        sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                      '--ei container_id 1 --es exec_path "%s"' % exe], 60)
        for _ in range(25):
            if top_is_ours():
                t_top = time.time() - t0
                break
            time.sleep(0.3)
        if t_top is not None:
            break
    if t_top is None:
        print("%s: activity 没起来" % label); return
    t_black = t_win = None
    while time.time() - t0 < secs:
        d = sh(["exec-out", "screencap", "-p"], 60).stdout
        if d:
            f = bright_frac(d)
            now = time.time() - t0
            if t_black is None and f < 0.02:
                t_black = now
            elif t_black is not None and t_win is None and f > 0.02:
                t_win = now
                break
        time.sleep(0.2)
    print("%s: activity %.1fs | 全屏黑 %s | 游玩窗口 %s"
          % (label, t_top,
             ("%.1fs" % t_black) if t_black else "未捕获",
             ("%.1fs" % t_win) if t_win else "未出现"))

if __name__ == "__main__":
    run(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 90)
