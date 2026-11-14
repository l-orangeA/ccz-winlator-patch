#!/usr/bin/env python3
"""P1 variant run with container-start retry (the first boot often wedges in proot).

Changes ONLY cpuListWoW64, restarts the app, retries the boot if wineserver
does not appear within 90s (force-stop + clear tmp + start again, max 3 tries),
then enters battle, clicks the auto-battle button, probes 60s, and reports the
game process affinity so we can confirm the variant actually landed.
"""
import json
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import phone_run_p1 as R  # noqa: E402

ADB = R.ADB
SER = R.SER
TARGET = os.environ.get("P1_CPULIST", "6,7")
OUT = os.path.join(R.BASE, "winlator11", "p1_%s_60s.json" % TARGET.replace(",", "-"))


def sh(a, t=90):
    return R.sh(a, t)


def wineserver_pid():
    return R.txt(sh(["shell", "pidof wineserver"])).strip()


def ekd5_pid():
    return R.txt(sh(["shell", "pidof Ekd5.exe"])).strip()


def affinity(pid):
    if not pid:
        return None
    return R.txt(sh(["shell", "grep Cpus_allowed_list /proc/%s/status" % pid])).strip()


def boot(max_tries=3, per_try=90):
    for attempt in range(1, max_tries + 1):
        print("BOOT attempt %d" % attempt, flush=True)
        sh(["shell", "am force-stop com.winlatcn"])
        time.sleep(2)
        sh(["shell", 'run-as com.winlatcn sh -c "rm -f files/imagefs/home/xuser-1/hwbp_stats.txt; '
                      'rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
        sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                      '--ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"'])
        t0 = time.time()
        while time.time() - t0 < per_try:
            if wineserver_pid():
                print("wineserver up at t=%.0fs" % (time.time() - t0), flush=True)
                return True
            time.sleep(5)
        print("attempt %d wedged (no wineserver in %ds)" % (attempt, per_try), flush=True)
    return False


def main():
    old = R.set_cfg("cpuListWoW64", TARGET)
    print("CFG cpuListWoW64 %s -> %s" % (old, TARGET), flush=True)
    assert R.set_pref("true"), "pref write failed"
    if not boot():
        print("BOOT FAILED")
        return 2
    if not R.wait_title(220):
        print("NO TITLE")
        return 3
    if not R.enter_battle():
        print("ENTER BATTLE FAILED")
        return 4
    time.sleep(2)
    R.tap(1110, 100)
    time.sleep(1.5)
    s, n = R.state("p1_%s_click.png" % TARGET.replace(",", "-"))
    print("AFTER_CLICK", s, n, flush=True)
    if s == "dialog":
        R.tap(1344, 686)
        print("CONFIRMED_YES", flush=True)
        time.sleep(2)
    pid = ekd5_pid()
    aff = affinity(pid)
    print("GAME pid=%s affinity=%s" % (pid, aff), flush=True)
    res = R.probe(60)
    payload = {"cpuListWoW64": TARGET, "game_pid": pid, "affinity": aff, "result": res}
    open(OUT, "w", encoding="utf-8").write(json.dumps(payload, ensure_ascii=False, indent=2))
    print("SAVED", OUT, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
