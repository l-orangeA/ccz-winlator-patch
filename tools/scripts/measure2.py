import sys, time, os, subprocess

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = "88f39f0d"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")

def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)

def shot():
    p = sh(["exec-out", "screencap", "-p"], 60)
    return len(p.stdout), p.stdout

def main(out, limit=300):
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(1)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    t0 = time.time()
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 '
                  '--es exec_path "/storage/emulated/0/Download/ccz22/Ekd5.exe"'], 60)
    from PIL import Image
    import io
    while time.time() - t0 < limit:
        n, data = shot()
        if n > 600000:
            im = Image.open(io.BytesIO(data)).convert("RGB")
            W, H = im.size
            px = im.load()
            blacks = tot = 0
            for y in range(0, H, 16):
                for x in range(0, W, 16):
                    r, g, b = px[x, y]
                    tot += 1
                    if r < 12 and g < 12 and b < 12:
                        blacks += 1
            frac = blacks / float(tot)
            if frac > 0.30:
                open(out, "wb").write(data)
                print("TITLE_AT %.1f s (black=%.2f)" % (time.time() - t0, frac))
                return 0
        time.sleep(3)
    print("TIMEOUT")
    return 3

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "m.png"))
