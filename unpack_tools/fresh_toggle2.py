import sys,time,subprocess,os
sys.path.insert(0,r"D:/ccz_winlator_patch/unpack_tools")
import auto_load as A

def sh(c,t=30): return A.txt(A.sh(["shell",c],t))
def ticks(pid):
    s=sh("cat /proc/%d/stat"%pid).rsplit(")",1)[-1].split()
    return int(s[11])+int(s[12])
def pids():
    a=sh("pidof com.winlatcn").split(); g=sh("pidof Ekd5.exe").split()
    return (int(a[0]) if a else None, int(g[0]) if g else None)

A.launch()
s=None
for i in range(110):
    s=A.state()[0]
    if s in ("title","scene"): break
    time.sleep(2)
print("boot_state",s,flush=True)
if s=="title":
    d=A.open_dialog()
    if d=="confirm": A.tap(1547,686); time.sleep(1); d="dialog"
    if d in ("dialog","confirm"): A.select_slot()
# ensure scene
for i in range(8):
    s=A.state()[0]
    if s=="scene": break
    if s=="confirm": A.tap(1344,686); time.sleep(4)
    elif s=="dialog": A.tap(1400,806); time.sleep(3)
    elif s=="title": A.open_dialog(); time.sleep(1)
    else: time.sleep(2)
print("battle_state",s,flush=True)
if s!="scene":
    print("NO BATTLE"); sys.exit(2)
APP,GAME=pids()
aff=sh("grep Cpus_allowed_list /proc/%d/status"%GAME).strip()
print("pids",APP,GAME,aff,flush=True)
time.sleep(2)
g0=ticks(GAME); a0=ticks(APP); t0=time.time()
for i in range(10):
    sh("input tap 1176 728"); time.sleep(0.35)
    sh("input tap 1800 1000"); time.sleep(0.35)
dt=time.time()-t0
g=(ticks(GAME)-g0)/100; a=(ticks(APP)-a0)/100
print("RESULT %s: %.1fs game=%.2fs (%.3f/toggle) app=%.2fs (%.3f/toggle)"%(aff,dt,g,g/10,a,a/10),flush=True)
