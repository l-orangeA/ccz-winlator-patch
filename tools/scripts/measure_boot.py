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
    for y in range(0, H, 16):
        for x in range(0, W, 16):
            r, g, b = px[x, y]
            tot += 1
            if r > 90 or g > 90 or b > 90:
                n += 1
    return n / float(tot)

def run(label, exe_path, limit=300, thresh=0.02):
    sh(["shell", "am force-stop com.winlatcn"]); time.sleep(1)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    t0 = time.time()
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 --es exec_path "%s"' % exe_path], 60)
    t_black = None
    while time.time() - t0 < limit:
        d = sh(["exec-out", "screencap", "-p"], 60).stdout
        if d:
            f = bright_frac(d)
            if t_black is None:
                if f < 0.02:            # 全黑 = 容器 X 桌面起来了
                    t_black = time.time() - t0
            elif f >= thresh:           # 黑底上出现窗口
                print("%s: 全屏黑 %.1f s -> 窗口 %.1f s (bright=%.3f)"
                      % (label, t_black, time.time() - t0, f))
                return
        time.sleep(0.3)
    print("%s: TIMEOUT" % label)

if __name__ == "__main__":
    run(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 300,
        float(sys.argv[4]) if len(sys.argv) > 4 else 0.02)
