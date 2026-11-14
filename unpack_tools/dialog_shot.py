"""部署指定构建并拍读档对话框（文字最密集的稳定界面）。
用法: python dialog_shot.py <r109g|r109f|orig> <输出名>
"""
import subprocess, os, sys, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import a0_rop_trace as T


def sh(a, t=60):
    return T.sh(a, t)


def txt(r):
    return T.txt(r)


def main():
    build, name = sys.argv[1], sys.argv[2]
    r = subprocess.run([ADB, "-s", SER, "shell",
                        f"run-as com.winlatcn sh -c 'cp /data/local/tmp/{build}.so files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so 2>/dev/null'"], env=ENV, capture_output=True, timeout=30)
    md = txt(sh(["shell", "run-as com.winlatcn md5sum files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so"]))
    print("deployed:", md.strip(), flush=True)
    for attempt in range(4):
        T.cmd_launch()
        ok, t = T.wait_title(180)
        print("attempt%d title=%s %.0fs" % (attempt + 1, ok, t), flush=True)
        if not ok:
            continue
        # 打开读档对话框，不点确认
        for a in range(3):
            sh(["shell", "input tap 1785 595"])
            time.sleep(1.0)
            sh(["shell", "input tap 1785 595"])
            time.sleep(3)
            b = len(T.shot())
            if b < 900000:
                print("dialog open shot=%d" % b, flush=True)
                time.sleep(1)
                open(OUT + "\\" + name + ".png", "wb").write(T.shot())
                print("SAVED", name, flush=True)
                return
        print("dialog never opened", flush=True)
    print("ALL FAILED")


if __name__ == "__main__":
    main()
