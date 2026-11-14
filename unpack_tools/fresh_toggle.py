import sys,time,subprocess,os
sys.path.insert(0,r"D:/ccz_winlator_patch/unpack_tools")
import auto_load as A
def ticks(pid):
    r=A.sh(["shell","cat /proc/%d/stat"%pid],20)
    s=r.stdout.decode("utf-8","ignore").rsplit(")",1)[-1].split()
    return int(s[11])+int(s[12])
def pids():
    return int(A.txt(A.sh(["shell","pidof com.winlatcn"])).split()[0]), int(A.txt(A.sh(["shell","pidof Ekd5.exe"])).split()[0])
A.launch()
s=None
for i in range(110):
    s=A.state()[0]
    if s in ("title","scene"): break
    time.sleep(2)
print("state",s,flush=True)
if s=="title":
    d=A.open_dialog()
    if d=="confirm": A.tap(1547,686); time.sleep(1); d="dialog"
    if d in ("dialog","confirm"): print("load",A.select_slot(),flush=True)
time.sleep(4)
APP,GAME=pids(); print("pids",APP,GAME,flush=True)
g0=ticks(GAME); a0=ticks(APP); t0=time.time()
for i in range(10):
    A.sh(["shell","input tap 1176 728"]); time.sleep(0.35)
    A.sh(["shell","input tap 1800 1000"]); time.sleep(0.35)
dt=time.time()-t0
g=(ticks(GAME)-g0)/100; a=(ticks(APP)-a0)/100
print("TOGGLES10: %.1fs game=%.2fs (%.3f/toggle) app=%.2fs (%.3f/toggle)"%(dt,g,g/10,a,a/10),flush=True)
