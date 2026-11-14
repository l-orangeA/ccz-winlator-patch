"""A0: 实测游戏 ROP/尺寸直方图 —— 通过 WINEDEBUG=+bitblt 落盘到 hwbp.log 再拉回分析。
用法: python a0_rop_trace.py <backup|env|restore|launch|waittitle|battle|trace|stop|pull|clean>
环境变量: SERIAL (默认 PHONE_SERIAL_REDACTED), ADB
"""
import subprocess, os, sys, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = os.environ.get("SERIAL", "PHONE_SERIAL_REDACTED")
PKG = "com.winlatcn"
CFG = "files/imagefs/home/xuser-1/.container"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
BAK = r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_a0.json"


def sh(a, t=60, inp=None):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t, input=inp)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def shot():
    r = sh(["exec-out", "screencap -p"], 60)
    return r.stdout if r else b""


def cmd_backup():
    r = sh(["exec-out", "run-as " + PKG + " cat " + CFG], 40)
    open(BAK, "wb").write(r.stdout)
    d = json.loads(r.stdout.decode("utf-8", "replace"))
    print("backup ok ->", BAK)
    print("envVars:", d["envVars"])


def _write_container(d):
    data = json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    tmp = OUT + "\\container_a0.json"
    open(tmp, "wb").write(data)
    sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
    r = sh(["shell", "run-as " + PKG + " cp /data/local/tmp/cnew.json " + CFG + " && echo ok"], 40)
    print(txt(r).strip())


def cmd_env():
    os.makedirs(OUT, exist_ok=True)
    d = json.loads(open(BAK, encoding="utf-8").read())
    kv = {}
    for it in d["envVars"].split():
        if "=" in it:
            k, v = it.split("=", 1)
            kv[k] = v
    kv["WINEDEBUG"] = "+bitblt"
    kv["WINE_HWBP_LOG"] = "1"
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    _write_container(d)
    print("envVars now:", d["envVars"])


def cmd_restore():
    sh(["push", BAK, "/data/local/tmp/cbak.json"], 60)
    r = sh(["shell", "run-as " + PKG + " cp /data/local/tmp/cbak.json " + CFG + " && echo restored"], 40)
    print(txt(r).strip())


def cmd_launch():
    sh(["shell", "am force-stop " + PKG])
    time.sleep(2)
    sh(["shell", 'run-as ' + PKG + ' sh -c "rm -f files/imagefs/home/xuser-1/hwbp.log files/imagefs/home/xuser-1/hwbp_stats.txt; rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    r = sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
    print(txt(r).strip()[:200])


def wait_title(maxs=200):
    t0 = time.time(); stable = 0; last = 0
    while time.time() - t0 < maxs:
        b = len(shot())
        if b > 800000 and abs(b - last) < 60000:
            stable += 1
        else:
            stable = 0
        last = b
        if stable >= 2:
            return True, time.time() - t0
        time.sleep(3)
    return False, time.time() - t0


def cmd_waittitle(maxs=200):
    ok, t = wait_title(maxs)
    print("title", ok, round(t, 1))


def set_pref():
    sh(["shell", "am force-stop " + PKG])
    time.sleep(1)
    out = txt(sh(["exec-out", "run-as " + PKG + " cat shared_prefs/com.winlatcn_preferences.xml"]))
    if "move_cursor_to_touchpoint" not in out and "</map>" in out:
        out = out.replace("</map>", '    <boolean name="move_cursor_to_touchpoint" value="true" />\n</map>')
        sh(["shell", "run-as " + PKG + " sh -c 'cat > shared_prefs/com.winlatcn_preferences.xml'"], inp=out.encode())
        print("pref set")


def cmd_battle():
    """契阔谈宴 -> 读档 No.018 -> 确认。坐标沿用 battle_auto（本机已校准）。"""
    for attempt in range(3):
        sh(["shell", "input tap 1785 595"])
        time.sleep(1.0)
        sh(["shell", "input tap 1785 595"])
        time.sleep(3)
        b = len(shot())
        if b < 900000:
            print("load dialog open (shot=%d)" % b, flush=True)
            break
    else:
        print("load dialog NOT open", flush=True)
        return
    sh(["shell", "input tap 1400 806"])
    time.sleep(2)
    b = len(shot())
    print("after slot click shot=%d" % b, flush=True)
    if b < 900000:
        sh(["shell", "input tap 1344 686"])
        time.sleep(5)
        b = len(shot())
        print("after confirm shot=%d" % b, flush=True)
    print("battle screen shot=%d" % b, flush=True)


def cmd_trace(secs=30):
    """战斗稳态采样：前 8 秒静置，之后每 2.5 秒点一次地图中心(1200,540)诱发选单位/画范围。"""
    time.sleep(8)
    t0 = time.time(); i = 0
    while time.time() - t0 < secs:
        print("tap %d at +%.1fs" % (i, time.time() - t0), flush=True)
        sh(["shell", "input tap 1200 540"])
        i += 1
        time.sleep(2.5)
    print("trace window done", flush=True)


def cmd_stop():
    sh(["shell", "am force-stop " + PKG])
    print("stopped")


def cmd_pull():
    os.makedirs(OUT, exist_ok=True)
    r = sh(["shell", "run-as " + PKG + " ls -l files/imagefs/home/xuser-1/hwbp.log"])
    print(txt(r).strip())
    r = sh(["exec-out", "run-as " + PKG + " sh -c 'gzip -c files/imagefs/home/xuser-1/hwbp.log'"], 900)
    if r.returncode == 0 and len(r.stdout) > 1000:
        p = OUT + "\\hwbp_a0.log.gz"
        open(p, "wb").write(r.stdout)
        print("gz bytes:", len(r.stdout), "->", p)
    else:
        print("gzip path failed rc=%s out=%s, trying plain" % (r.returncode, txt(r)[:120]))
        r = sh(["exec-out", "run-as " + PKG + " cat files/imagefs/home/xuser-1/hwbp.log"], 1800)
        p = OUT + "\\hwbp_a0.log"
        open(p, "wb").write(r.stdout)
        print("raw bytes:", len(r.stdout), "->", p)


def cmd_clean():
    sh(["shell", "run-as " + PKG + " rm -f files/imagefs/home/xuser-1/hwbp.log"])
    sh(["shell", "run-as " + PKG + " rm -f files/imagefs/home/xuser-1/hwbp_stats.txt"])
    print("device logs removed")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    if cmd == "backup":
        cmd_backup()
    elif cmd == "env":
        cmd_env()
    elif cmd == "restore":
        cmd_restore()
    elif cmd == "launch":
        cmd_launch()
    elif cmd == "waittitle":
        cmd_waittitle(int(sys.argv[2]) if len(sys.argv) > 2 else 200)
    elif cmd == "pref":
        set_pref()
    elif cmd == "battle":
        cmd_battle()
    elif cmd == "trace":
        cmd_trace(int(sys.argv[2]) if len(sys.argv) > 2 else 30)
    elif cmd == "stop":
        cmd_stop()
    elif cmd == "pull":
        cmd_pull()
    elif cmd == "clean":
        cmd_clean()
    else:
        print(__doc__)
