"""启动探针 exe（任意路径）并等结果文件。
用法: python launch_probe.py <exe的sdcard路径> <结果文件名> [等待秒]
"""
import subprocess, os, sys, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def sh(a, t=90):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def main():
    exe = sys.argv[1]
    result = sys.argv[2]
    wait = int(sys.argv[3]) if len(sys.argv) > 3 else 90
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    r = sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "%s"' % exe])
    print(txt(r).strip()[:150], flush=True)
    t0 = time.time()
    while time.time() - t0 < wait:
        r = sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/.wine/drive_c/%s 2>/dev/null || echo NO-FILE'" % result])
        out = txt(r).strip()
        if "NO-FILE" not in out and out:
            print("RESULT FILE:\n" + out, flush=True)
            return
        time.sleep(5)
    print("TIMEOUT no result file", flush=True)


if __name__ == "__main__":
    main()
