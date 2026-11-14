#!/usr/bin/env python3
"""Clean-boot probe with per-sample process accounting (2026-11-11).

Zero binary changes: only force-stop / pkill -x / clear tmp / am start.
Collects, during VProtect unpack:
  - hwbp_stats.txt timeline (arms / faults / steps / hits / t)
  - /proc/<Ekd5>/stat  utime, stime, state, num_threads, processor
  - /proc/<Ekd5>/status voluntary_ctxt_switches, Cpus_allowed_list
Purpose: independently verify the fitted 450 us/fault cost by splitting
user-time vs kernel-time, without recompiling ntdll.

Usage: python phone_boot_probe_20261111.py [label]
"""
import json
import os
import re
import subprocess
import sys
import time

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
EXEC = os.environ.get("WL_EXEC", "/storage/emulated/0/ccz/Ekd5.exe")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "winlator11")
LABEL = sys.argv[1] if len(sys.argv) > 1 else "asfound"
TITLE_BYTES = 900000
TIMEOUT = 200

def parse_hwbp(line):
    """Tolerate both production and PROBE hwbp_stats.txt layouts."""
    out = {}
    for k in ("arms", "faults", "rfault", "wfault", "steps", "hits"):
        m = re.search(r"\b" + k + r"=(\d+)", line)
        if m:
            out[k] = int(m.group(1))
    m = re.search(r"(?:^|\s)t=([\d.]+)", line)
    if m:
        out["ht"] = float(m.group(1))
    return out


def sh(args, timeout=25):
    try:
        return subprocess.run([ADB, "-s", SER] + args, capture_output=True,
                              env=ENV, timeout=timeout)
    except subprocess.TimeoutExpired:
        return None


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def pidof(name):
    return txt(sh(["shell", "pidof " + name])).strip().split()


def hwbp():
    return txt(sh(["shell", "run-as com.winlatcn cat "
                   "files/imagefs/home/xuser-1/hwbp_stats.txt"])).strip()


def shot():
    r = sh(["exec-out", "screencap -p"], 30)
    return len(r.stdout) if r else 0


def proc_sample():
    inner = ("p=$(pidof Ekd5.exe); echo P=$p; if [ -n \"$p\" ]; then "
             "cat /proc/$p/stat; cat /proc/$p/io; "
             "grep -E \"^(State|Threads|voluntary_ctxt_switches|nonvoluntary_ctxt_switches|Cpus_allowed_list):\" "
             "/proc/$p/status; fi; "
             "for q in 0 4 7; do printf \"F%s=\" $q; "
             "cat /sys/devices/system/cpu/cpufreq/policy$q/scaling_cur_freq 2>/dev/null || echo NA; done")
    return txt(sh(["shell", "run-as com.winlatcn sh -c '" + inner + "'"]))


def parse_sample(raw):
    out = {"pid": None}
    lines = [l.strip() for l in raw.splitlines() if l.strip()]
    if not lines:
        return out
    m = re.match(r"P=(\d+)", lines[0])
    if not m or not m.group(1):
        return out
    out["pid"] = int(m.group(1))
    for l in lines[1:]:
        if l.startswith("State:"):
            out["state"] = l.split(":", 1)[1].strip()
        elif l.startswith("Threads:"):
            out["threads"] = int(l.split(":")[1])
        elif l.startswith("voluntary_ctxt_switches:"):
            out["vctx"] = int(l.split(":")[1])
        elif l.startswith("nonvoluntary_ctxt_switches:"):
            out["nvctx"] = int(l.split(":")[1])
        elif l.startswith("Cpus_allowed_list:"):
            out["cpus"] = l.split(":", 1)[1].strip()
        elif re.match(r"^F[047]=", l):
            try:
                out["freq_" + l[1]] = int(l.split("=", 1)[1])
            except (ValueError, IndexError):
                pass
        elif re.match(r"^(rchar|wchar|syscr|syscw|read_bytes|write_bytes|cancelled_write_bytes):", l):
            k, v = l.split(":", 1)
            try:
                out[k] = int(v.strip())
            except ValueError:
                pass
        elif re.match(r"^\d+ \(.*\) ", l):
            f = l.split()
            try:
                out["utime"] = int(f[13])
                out["stime"] = int(f[14])
                out["minflt"] = int(f[9])
                out["majflt"] = int(f[11])
                out["state"] = f[2]
                out["cpu"] = int(f[38]) if len(f) > 38 else -1
            except (ValueError, IndexError):
                pass
    return out


def clean():
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    # Kill the tracer FIRST (-9), then its tracees, and do two rounds:
    # killing tracees first leaves orphans that re-arm and cause the
    # "stall cluster" (rock §36.6/§36.7).
    for _ in range(2):
        sh(["shell", "run-as com.winlatcn sh -c "
            "'pkill -9 -x libtinyproot.so; sleep 1; "
            "pkill -9 -x Ekd5.exe; pkill -9 -x wineserver; "
            "pkill -9 -x winedevice.exe; pkill -9 -x services.exe; "
            "pkill -9 -x explorer.exe'"], 30)
        time.sleep(1)
    left = [n for n in ("wineserver", "Ekd5.exe", "winedevice.exe",
                        "services.exe", "explorer.exe", "libtinyproot.so") if pidof(n)]
    # orphans can survive the pkill if their parent is still exiting
    if left:
        time.sleep(2)
        sh(["shell", "run-as com.winlatcn sh -c "
            "'pkill -9 -x libtinyproot.so; pkill -9 -x winedevice.exe; "
            "pkill -9 -x wineserver; pkill -9 -x Ekd5.exe'"], 30)
        time.sleep(1)
        left = [n for n in ("wineserver", "Ekd5.exe", "winedevice.exe",
                            "services.exe", "explorer.exe", "libtinyproot.so") if pidof(n)]
    sh(["shell", "run-as com.winlatcn rm -f "
        "files/imagefs/home/xuser-1/hwbp_stats.txt"], 20)
    sh(["shell", "run-as com.winlatcn sh -c "
        "\"rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* "
        "files/imagefs/tmp/shm/*\""], 30)
    return left


def main():
    os.makedirs(OUT, exist_ok=True)
    log_path = os.path.join(OUT, "boot_probe_20261111_%s.log" % LABEL)
    json_path = os.path.join(OUT, "boot_probe_20261111_%s.json" % LABEL)
    lf = open(log_path, "w", encoding="utf-8")
    clk = txt(sh(["shell", "getconf CLK_TCK"])).strip()
    try:
        hz = int(clk)
    except ValueError:
        hz = 100

    def emit(level, msg):
        line = "%s %7.1fs  %s" % (level, time.time() - t0, msg)
        print(line, flush=True)
        lf.write(line + "\n")
        lf.flush()

    print("【要点摘要】干净启动 + 解包期 CPU/内核时间拆分（零二进制改动）", flush=True)
    left = clean()
    print("★ CLEAN 残留进程: %s" % (left or "none"), flush=True)
    print("· CLK_TCK=%d  设备=%s  exec=%s" % (hz, SER, EXEC), flush=True)

    t0 = time.time()
    lf.write("【要点摘要】干净启动实测；关键项以 ★ 标记\n")
    lf.write("设备=%s  CLK_TCK=%d  exec=%s\n" % (SER, hz, EXEC))
    lf.write("CLEAN 残留=%s\n" % (left or "none"))

    sh(["shell", 'am start -n com.winlatcn/com.winlator.XServerDisplayActivity '
                  '--ei container_id 1 --es exec_path "%s"' % EXEC])
    marks, hw_rows, samples = {}, [], []
    last_hw, last_shot, title_hits = "", -99, 0
    last_arms_t = 0.0

    while time.time() - t0 < TIMEOUT:
        now = time.time() - t0
        if "ws" not in marks and pidof("wineserver"):
            marks["ws"] = round(now, 1)
            emit("★", "wineserver up")
        if "ekd" not in marks and pidof("Ekd5.exe"):
            marks["ekd"] = round(now, 1)
            emit("★", "Ekd5.exe up")

        s = parse_sample(proc_sample())
        if s.get("pid"):
            s["t"] = round(now, 2)
            samples.append(s)

        h = hwbp()
        if h and h != last_hw:
            last_hw = h
            d = parse_hwbp(h)
            if d.get("arms"):
                row = {"t": round(now, 2), "arms": d["arms"], "faults": d.get("faults", 0),
                       "steps": d.get("steps", 0), "hits": d.get("hits", 0), "ht": d.get("ht", 0.0),
                       "rfault": d.get("rfault"), "wfault": d.get("wfault")}
                hw_rows.append(row)
                last_arms_t = now
                emit("·", "hwbp %s" % h)
                if d["arms"] >= 1600 and "unpack" not in marks:
                    marks["unpack"] = round(now, 1)
                    emit("★", "UNPACK DONE (arms=1600)")

        if now - last_shot >= 2:
            n = shot()
            last_shot = now
            if n > TITLE_BYTES:
                title_hits += 1
                emit("·", "title-ish %d (hit %d)" % (n, title_hits))
                if title_hits >= 2:
                    marks["title"] = round(now, 1)
                    emit("★", "TITLE reached")
                    break
            else:
                title_hits = 0

        if now > 60 and now - last_arms_t > 30 and "title" not in marks:
            emit("★", "STALLED (hwbp 30s 无进展) -> 提前结束")
            break
        time.sleep(0.35)

    elapsed = round(time.time() - t0, 1)

    # --- unpack-window CPU split ---
    split = {}
    seg = [s for s in samples
           if marks.get("ekd", 0) <= s["t"] <= marks.get("unpack", marks.get("title", elapsed))]
    if len(seg) >= 2 and seg[0].get("utime") is not None and seg[-1].get("utime") is not None:
        du = seg[-1]["utime"] - seg[0]["utime"]
        ds = seg[-1]["stime"] - seg[0]["stime"]
        dv = (seg[-1].get("vctx", 0) - seg[0].get("vctx", 0))
        span = seg[-1]["t"] - seg[0]["t"]
        split = {"span_s": round(span, 2), "samples": len(seg),
                 "utime_s": round(du / hz, 2), "stime_s": round(ds / hz, 2),
                 "cpu_total_s": round((du + ds) / hz, 2),
                 "stime_pct": round(100.0 * ds / max(1, du + ds), 1),
                 "vctx_delta": dv, "vctx_per_s": round(dv / max(0.01, span), 0),
                 "threads_first": seg[0].get("threads"), "threads_last": seg[-1].get("threads"),
                 "cpus": seg[0].get("cpus"), "cpu_core_first": seg[0].get("cpu"),
                 "cpu_core_last": seg[-1].get("cpu")}

    res = {"label": LABEL, "elapsed": elapsed, "marks": marks,
           "hwbp_last": last_hw, "hw_rows": hw_rows,
           "cpu_split": split, "clk_tck": hz, "samples": samples,
           "exec": EXEC, "clean_leftover": left,
           "sample_count": len(samples)}
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(res, f, ensure_ascii=False, indent=2)

    # --- summary at top of log (rewrite with header) ---
    lf.close()
    body = open(log_path, "r", encoding="utf-8").read()
    with open(log_path, "w", encoding="utf-8") as f:
        f.write("【要点摘要】\n")
        f.write("★ 总耗时 %.1fs  阶段: %s\n" % (elapsed, json.dumps(marks, ensure_ascii=False)))
        f.write("★ hwbp 终值: %s\n" % (last_hw or "(无)"))
        f.write("★ 采样点 %d  解包期 CPU 拆分: %s\n" %
                (len(samples), json.dumps(split, ensure_ascii=False)))
        f.write("★ 结论数据见 %s\n" % os.path.basename(json_path))
        f.write("---- 明细 ----\n" + body)

    print("\n【要点摘要】", flush=True)
    print("★ 总耗时 %.1fs  marks=%s" % (elapsed, json.dumps(marks, ensure_ascii=False)), flush=True)
    print("★ hwbp 终值: %s" % (last_hw or "(无)"), flush=True)
    print("★ 解包期 CPU 拆分: %s" % json.dumps(split, ensure_ascii=False), flush=True)
    print("★ 日志: %s" % log_path, flush=True)
    print("★ JSON: %s" % json_path, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
