#!/usr/bin/env python3
"""Compare 2026-11-09 vs 2026-11-11 boot probes: fit fault cost independently."""
import json
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
W = os.path.join(BASE, "winlator11")


def fit(rows, tag):
    n = len(rows)
    sa = sb = st = saa = sbb = sab = sat = sbt = 0.0
    for r in rows:
        a, f, t = r["arms"], r["faults"], r["ht"]
        sa += a; sb += f; st += t
        saa += a * a; sbb += f * f; sab += a * f
        sat += a * t; sbt += f * t
    det = saa * sbb - sab * sab
    x = (sat * sbb - sbt * sab) / det
    y = (saa * sbt - sab * sat) / det
    err = [r["ht"] - (x * r["arms"] + y * r["faults"]) for r in rows]
    rmse = (sum(e * e for e in err) / n) ** 0.5
    print("%s  n=%d  t = %.8f*arms + %.8f*faults" % (tag, n, x, y))
    print("   per-fault = %.1f us | 64arms VM = %.3f s | max|resid|=%.2f s | rmse=%.3f s"
          % (y * 1e6, x * 64, max(abs(e) for e in err), rmse))
    return x, y


def diff_beta(rows, x, tag):
    print("  差分法（VM 基线 = %.3f s / 64 arms）:" % (x * 64))
    vals = []
    for i in range(1, len(rows)):
        p, q = rows[i - 1], rows[i]
        da, df, dt = q["arms"] - p["arms"], q["faults"] - p["faults"], q["ht"] - p["ht"]
        if da <= 0 or df <= 0:
            continue
        rem = dt - x * da
        if rem <= 0:
            continue
        b = rem / df * 1e6
        vals.append(b)
        if df < 100 or df > 2600:
            print("    arms %4d->%4d  df=%5d  dt=%.1f  rem=%.2f  beta=%6.0f us"
                  % (p["arms"], q["arms"], df, dt, rem, b))
    if vals:
        print("    全部 %d 段 beta: min=%.0f  中位=%.0f  max=%.0f us"
              % (len(vals), min(vals), sorted(vals)[len(vals) // 2], max(vals)))


def load_1109():
    rows = []
    for l in open(os.path.join(W, "boot_clean_20261109.log"), encoding="utf-8", errors="replace"):
        m = re.search(r"arms=(\d+) faults=(\d+) steps=(\d+) hits=(\d+) t=([\d.]+)", l)
        if m:
            a, f, s, h, t = m.groups()
            rows.append({"arms": int(a), "faults": int(f), "ht": float(t)})
    return rows


def cpu_window(res):
    js = res["hw_rows"]
    if not js:
        return
    t0, t1 = js[0]["t"], js[-1]["t"]
    seg = [s for s in res["samples"] if t0 <= s.get("t", -1) <= t1 and "utime" in s]
    if len(seg) < 2:
        return
    hz = res["clk_tck"]
    du = seg[-1]["utime"] - seg[0]["utime"]
    ds = seg[-1]["stime"] - seg[0]["stime"]
    dv = seg[-1].get("vctx", 0) - seg[0].get("vctx", 0)
    span = seg[-1]["t"] - seg[0]["t"]
    print("  解包窗口 %.1f~%.1f s (%.1f s, %d 采样): utime=%.2f s  stime=%.2f s  "
          "CPU合计=%.2f s (%.0f%% 墙钟)  stime占CPU=%.1f%%  vctx/s=%.0f"
          % (t0, t1, span, len(seg), du / hz, ds / hz, (du + ds) / hz,
             100 * (du + ds) / hz / max(0.01, span),
             100 * ds / max(1, du + ds), dv / max(0.01, span)))
    print("  hwbp 内部计时 %.1f s；线程 %s→%s；亲和 %s；运行核 %s→%s"
          % (js[-1]["ht"] - js[0]["ht"], seg[0].get("threads"), seg[-1].get("threads"),
             seg[0].get("cpus"), seg[0].get("cpu"), seg[-1].get("cpu")))


def main():
    r09 = load_1109()
    print("=== 2026-11-09 干净基线（42.6 s / arms=1600 / t=26.8） ===")
    x09, y09 = fit(r09, "11-09")
    diff_beta(r09, x09, "11-09")

    path = os.path.join(W, "boot_probe_20261111_%s.json" %
                        (sys.argv[1] if len(sys.argv) > 1 else "asfound"))
    res = json.load(open(path, encoding="utf-8"))
    r11 = [{"arms": r["arms"], "faults": r["faults"], "ht": r["ht"]} for r in res["hw_rows"]]
    print("\n=== 2026-11-11 实测（%.1f s / %s） ===" % (res["elapsed"], res["hwbp_last"]))
    x11, y11 = fit(r11, "11-11")
    diff_beta(r11, x11, "11-11")
    cpu_window(res)

    print("\n=== 对比 ===")
    print("  β: 11-09 %.1f us  ->  11-11 %.1f us   (比值 %.2f)"
          % (y09 * 1e6, y11 * 1e6, y11 / max(1e-9, y09)))
    print("  64arms VM: 11-09 %.3f s  ->  11-11 %.3f s" % (x09 * 64, x11 * 64))
    print("  总启动: 11-09 42.6 s  ->  11-11 %.1f s" % res["elapsed"])


if __name__ == "__main__":
    main()
