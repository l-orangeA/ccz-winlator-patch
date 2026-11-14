import subprocess, os, sys, time, json

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
DEV = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")

def sh(a, t=60):
    try:
        return subprocess.run([ADB, "-s", DEV] + a, capture_output=True, env=ENV, timeout=t)
    except subprocess.TimeoutExpired:
        return None

def text(r):
    return r.stdout.decode("utf-8", "replace") if r else ""

print("== force-stop ==")
sh(["shell", "am force-stop com.winlatcn"]); time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])

print("== launch ==")
for _ in range(3):
    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 --es exec_path "/storage/emulated/0/Download/ccz/Ekd5.exe"'], 60)
    for _ in range(20):
        top = text(sh(["shell", "dumpsys activity activities | grep topResumedActivity | head -1"]))
        if "XServerDisplay" in top:
            print("activity up")
            break
        time.sleep(0.5)
    else:
        continue
    break

seen = {}
t0 = time.time()
last_print = 0
while time.time() - t0 < 70:
    out = text(sh(["shell", "ps -A -o PID,PPID,UID,S,NAME"]))
    interesting = []
    for line in out.splitlines():
        parts = line.split()
        if len(parts) < 6:
            continue
        pid, ppid, uid, st, name = parts[0], parts[1], parts[2], parts[3], " ".join(parts[4:])
        if any(k in name for k in ("Ekd5", "box64", "wine", "libtinyproot", "proot", "services.exe", "explorer")):
            interesting.append((pid, ppid, uid, st, name))
    now = time.time() - t0
    if now - last_print >= 4:
        last_print = now
        print("--- t=%.0fs ---" % now)
        for pid, ppid, uid, st, name in interesting:
            st_out = text(sh(["shell", "grep -E 'Name|Uid|NoNewPrivs|Seccomp' /proc/%s/status 2>/dev/null" % pid]))
            vals = {}
            for l in st_out.splitlines():
                if ":" in l:
                    k, v = l.split(":", 1)
                    vals[k.strip()] = v.strip()
            key = (pid, name)
            sig = (vals.get("NoNewPrivs"), vals.get("Seccomp"), vals.get("Seccomp_filters"), st)
            print("  pid=%-6s ppid=%-6s uid=%-6s st=%s %-22s NNP=%s Seccomp=%s filters=%s" % (
                pid, ppid, uid, st, name[:22], sig[0], sig[1], sig[2]))
            if key not in seen:
                seen[key] = (now, sig)
                print("      FIRST sighting t=%.0fs" % now)

img = sh(["exec-out", "screencap -p"], 60)
if img:
    open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "phone_now.png"), "wb").write(img.stdout)
    print("screenshot bytes:", len(img.stdout))
