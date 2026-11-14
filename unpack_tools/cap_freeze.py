"""抓冻结标本 v2: 启动 → 轮询 stats → 冻结(连续两次相同)时保持进程,现场解剖+拉日志.
用法: python cap_freeze.py
"""
import subprocess, os, time, json

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
OUT = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
R113 = r"D:\ccz_winlator_patch\build\box64_r113_044.so"
CFG = "files/imagefs/home/xuser-1/.container"


def sh(a, t=120):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def stats():
    return txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()


# 1) 部署 r113 + env
r = sh(["push", R113, "/data/local/tmp/bc"])
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/bc files/imagefs/usr/local/bin/box64 && echo b64"])
sh(["shell", "rm -f /data/local/tmp/bc"])
d = json.load(open(r"D:\ccz_winlator_patch\device_backup\phone_container_20261106_prod.txt", encoding="utf-8"))
kv = dict(i.split("=", 1) for i in d["envVars"].split() if "=" in i)
kv["BOX64_DYNAREC_INTERP_SIGNAL"] = "1"
kv["WINE_HWBP_LOG"] = "1"
d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
tmp = OUT + "\\container_fr.json"
open(tmp, "wb").write(json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode())
sh(["push", tmp, "/data/local/tmp/cnew.json"], 60)
sh(["shell", "run-as com.winlatcn cp /data/local/tmp/cnew.json " + CFG + " && echo env-ok"])
r = sh(["shell", "run-as com.winlatcn md5sum files/imagefs/usr/local/bin/box64"])
print("box64:", txt(r).strip()[:60], flush=True)

# 2) 启动
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp.log files/imagefs/home/xuser-1/hwbp_stats.txt'"])
sh(["shell", "am force-stop com.winlatcn"])
time.sleep(2)
sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
print("launched", flush=True)

# 3) 轮询: 先等出生, 再找冻结
t0 = time.time()
last = None
frozen = False
pid = None
born = False
while time.time() - t0 < 300:
    p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
    if not born:
        if p:
            born = True
            pid = p
            print("BORN at +%.0fs pid=%s" % (time.time() - t0, p), flush=True)
        time.sleep(1)
        continue
    if not p:
        print("DIED after birth (last=%s)" % last, flush=True)
        break
    s = stats()
    if s != "no" and s == last and s:
        print("FROZEN at born+%.0fs: %s" % (time.time() - t0, s), flush=True)
        frozen = True
        break
    last = s
    time.sleep(3)
if not born:
    print("never born", flush=True)
elif not frozen:
    print("no freeze caught (last=%s)" % last, flush=True)

# 4) 冻结现场: 线程 + wchan
if frozen and pid:
    r = sh(["shell", "run-as com.winlatcn sh -c 'ls /proc/%s/task'" % pid])
    tasks = txt(r).split()
    print("=== frozen specimen pid=%s threads=%d ===" % (pid, len(tasks)), flush=True)
    lines = []
    for t in tasks:
        name = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/task/%s/comm'" % (pid, t)])).strip()
        stat = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/task/%s/stat'" % (pid, t)]))
        state = stat.rsplit(")", 1)[-1].split()[0] if ")" in stat else "?"
        ut = stat.rsplit(")", 1)[-1].split()[13] if ")" in stat else "?"
        wc = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat /proc/%s/task/%s/wchan'" % (pid, t)])).strip()
        lines.append("  tid=%-7s %-18s st=%s ut=%-6s wchan=%s" % (t, name, state, ut, wc))
    print("\n".join(lines), flush=True)
    open(OUT + "\\freeze_threads.txt", "w", encoding="utf-8", errors="replace").write("\n".join(lines))

# 5) 拉日志（进程保持活着拉）
r = sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/imagefs/home/xuser-1/hwbp.log'"])
print(txt(r).strip(), flush=True)
r = sh(["exec-out", "run-as com.winlatcn cat files/imagefs/home/xuser-1/hwbp.log"], 300)
if len(r.stdout) > 100:
    open(OUT + "\\freeze_raw.log", "wb").write(r.stdout)
    print("log saved:", len(r.stdout), "bytes", flush=True)

# 6) 收尾停止
sh(["shell", "am force-stop com.winlatcn"])
print("done", flush=True)
