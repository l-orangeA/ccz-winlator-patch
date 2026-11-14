import os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import auto_load as A

sh = A.sh
txt = A.txt

def sz(name):
    return txt(sh(["shell", "run-as com.winlatcn sh -c 'ls -la files/%s 2>/dev/null || echo NONE'" % name])).strip()

A.set_pref()
sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/beat.txt files/beatu.txt files/perf.txt'"])
ok = False
for attempt in (1, 2, 3):
    A.launch()
    if A.wait_state("title", 220, "boot%d" % attempt):
        ok = True
        break
    time.sleep(3)
if not ok:
    print("NO TITLE"); sys.exit(2)
print("TITLE", flush=True)
s = A.open_dialog()
if s == "confirm":
    A.tap(1547, 686); time.sleep(1); s = "dialog"
if s != "dialog":
    print("DIALOG FAILED (%s)" % s); sys.exit(3)
if not A.select_slot():
    print("BATTLE LOAD FAILED"); sys.exit(4)
print("BATTLE OK", flush=True)

# click once inside the game area (focus / wake) then stay quiet
A.tap(1400, 800, 1.0)
time.sleep(4)
print("T0 beat:", sz("beat.txt"), "beatu:", sz("beatu.txt"), flush=True)
time.sleep(6)
print("T1 beat:", sz("beat.txt"), "beatu:", sz("beatu.txt"), flush=True)
time.sleep(6)
print("T2 beat:", sz("beat.txt"), "beatu:", sz("beatu.txt"), flush=True)
print("app/game:", txt(sh(["shell", "pidof com.winlatcn; pidof Ekd5.exe"])).strip(), flush=True)
print("DONE", flush=True)
