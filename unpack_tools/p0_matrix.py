#!/usr/bin/env python3
"""P0 组合矩阵：单变量部署 + 一次 boot + 硬指标验收。

用法: <venv>\\python.exe unpack_tools\\p0_matrix.py <B|H|E|F|G|D>

每个格子：
  1) push box64 + ntdll，run-as 覆盖，读回 md5 校验（不符即停）
  2) env 强制回生产快照（零日志通道）
  3) 清残留 → force-stop → am start → 轮询 hwbp_stats / pidof
  4) 判定以 hits 为硬指标（健康档案 arms=1600 hits=400）
  5) 结果写 a0_trace/p0_<cell>_result.txt，并截图存证

注意：本脚本不自动恢复生产态。整轮结束后必须跑 p0_restore.py。
"""
import hashlib
import os
import re
import subprocess
import sys
import time
from datetime import datetime

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
ROOT = r"D:\ccz_winlator_patch"
OUT = os.path.join(ROOT, "unpack_tools", "a0_trace")
PROD = os.path.join(ROOT, "device_backup", "phone_container_20261106_prod.txt")
CFG = "files/imagefs/home/xuser-1/.container"
REMOTE_B64 = "files/imagefs/usr/local/bin/box64"
REMOTE_NTDLL = "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so"
EXEC = "/storage/emulated/0/ccz/Ekd5.exe"

CELLS = {
    # cell: (box64 本地路径, ntdll 本地路径, 说明)  md5 由脚本自动算并逐项校验
    "B": ("unpack_tools/box64_044_glibc231", "build/ntdll_fix110c.so",
          "历史校准：作者版无补丁 + fix110c（已测：失控循环 hits=25）"),
    "H": ("build/box64_base_no_patch.so", "build/ntdll_fix110c.so",
          "ntf 配置无补丁基线 + fix110c（已测：早死）"),
    "E": ("build/box64_r112d_rebuilt.so", "build/ntdll_fix110c.so",
          "ntf 配置 r112d + fix110c（已测：早死）"),
    "E2": ("build/box64_r112d_044.so", "build/ntdll_fix110c.so",
           "01:29 旧 r112d + fix110c（已测：早死）"),
    "E3": ("build/box64_r112d_authorbase.so", "build/ntdll_fix110c.so",
           "★ 作者版配置 + 六处补丁 + fix110c（严格单变量对照 B）"),
    "E4": ("build/box64_r112d_only.so", "build/ntdll_fix110c.so",
           "★★ 最小单变量：作者版 + 仅 r112d(SIGTRAP 不可延迟) + fix110c"),
    "E7": ("build/box64_tf_fix.so", "build/ntdll_fix110c.so",
           "★★★ TF_FIX：同步 Wine 清掉的 TF（严格单变量对照 B）"),
    "E8": ("build/box64_tf_fix.so", "build/ntdll_fix110d.so",
           "TF_FIX + fix110d"),
    "E9": ("build/box64_tf_fix2.so", "build/ntdll_fix110c.so",
           "★★★ TF_FIX2：外层保留 Wine 清掉的 TF（单变量对照 B / E7）"),
    "E10": ("build/box64_notf_author.so", "build/ntdll_fix110c.so",
            "★★★ NOTF_E10：作者版配置删两处 no_tf 写点（单变量对照 B）"),
    "E11": ("build/box64_A1.so", "build/ntdll_fix110c.so",
            "路线 A / A1：删内层 old_eflags 恢复（单变量对照 B）"),
    "E12": ("build/box64_A2.so", "build/ntdll_fix110c.so",
            "路线 A / A1+A2：再删外层 EFLAGS 保存恢复（对照 E11）"),
    "E13": ("build/box64_A3.so", "build/ntdll_fix110c.so",
            "路线 A / A1+A2+A3：统一同步 TF 到 emu 与宿主 xFlags（对照 E12）"),
    "E14": ("build/box64_A3PROBE.so", "build/ntdll_fix110c.so",
            "A3 + 只读探针：记录实际进入的函数/宿主 x26/defer 命中"),
    "E15": ("build/box64_A3B.so", "build/ntdll_fix110c.so",
            "A3B：绕过 siglongjmp，让内核用打过补丁的 ucontext 返回 guest"),
    "E16": ("build/box64_A3C.so", "build/ntdll_fix110c.so",
            "A3C：完整上下文同步 + native_epilog，替代 siglongjmp"),
    "E17": ("build/box64_A3D.so", "build/ntdll_fix110c.so",
            "A3D：完整寄存器同步 + 原 PC 正常返回（不 siglongjmp）"),
    "E18": ("build/box64_ENGINE.so", "build/ntdll_fix110c.so",
            "★★★ X26_SYNC 引擎版：siglongjmp 后强制 x26 = emu->eflags"),
    "E19": ("unpack_tools/box64_044_glibc231", "build/ntdll_w1.so",
            "★★★ W1：restore_context 对 32 位 guest 跳过 Dr 覆盖"),
    "E20": ("unpack_tools/box64_044_glibc231", "build/ntdll_gctxw.so",
            "GCTXW：get_thread_wow64_context 写缓存前后取证"),
    "E21": ("unpack_tools/box64_044_glibc231", "build/ntdll_setprobe.so",
            "SETPROBE：只抓含 DEBUG 位的 32/64 位 SetContext"),
    "E22": ("unpack_tools/box64_044_glibc231", "build/ntdll_drf.so",
            "★★★ DRF：32 位 Get 返回前过滤越界 Dr1/Dr7"),
    "F3": ("build/box64_r112d_authorbase.so", "build/ntdll_fix110d.so",
           "作者版配置 + r112d 补丁 + fix110d"),
    "F": ("build/box64_r112d_rebuilt.so", "build/ntdll_fix110d.so",
          "ntf 配置 r112d + fix110d"),
    "G": ("build/box64_r113_044.so", "build/ntdll_fix110c.so",
          "隔离项：r113(无 r112d) + fix110c"),
    "D": ("build/box64_r112d_044.so", "build/ntdll_fix110b.so",
          "历史校准：r112d + fix110b"),
}


def sh(a, t=300):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


def deploy(local, remote, tag):
    p = os.path.join(ROOT, local)
    if not os.path.isfile(p):
        raise SystemExit("缺少产物: " + p)
    want = hashlib.md5(open(p, "rb").read()).hexdigest()
    tmp = "/data/local/tmp/p0_" + tag
    sh(["push", p, tmp], 300)
    sh(["shell", "run-as com.winlatcn cp %s %s && echo copied" % (tmp, remote)])
    sh(["shell", "rm -f " + tmp])
    got = txt(sh(["shell", "run-as com.winlatcn md5sum " + remote])).strip()
    return want, got


def main():
    cell = (sys.argv[1] if len(sys.argv) > 1 else "").upper()
    if cell not in CELLS:
        raise SystemExit(__doc__)
    b64, ntdll, note = CELLS[cell]
    print("=== P0 格子 %s：%s ===" % (cell, note), flush=True)

    # 1) 部署并校验（md5 自动算，逐项比对）
    w1, g1 = deploy(b64, REMOTE_B64, "b64")
    w2, g2 = deploy(ntdll, REMOTE_NTDLL, "ntdll")
    print("  box64 : %s  want=%s" % (g1[:70], w1[:12]), flush=True)
    print("  ntdll : %s  want=%s" % (g2[:70], w2[:12]), flush=True)
    if w1 not in g1 or w2 not in g2:
        raise SystemExit("ERROR: 部署 md5 不符，停止实验（设备停在实验态）")

    # 2) env 强制回生产
    tmp = os.path.join(OUT, "container_p0_%s.json" % cell)
    open(tmp, "wb").write(open(PROD, "rb").read())
    sh(["push", tmp, "/data/local/tmp/p0cfg.json"], 60)
    sh(["shell", "run-as com.winlatcn cp /data/local/tmp/p0cfg.json %s && echo env-prod" % CFG])
    sh(["shell", "rm -f /data/local/tmp/p0cfg.json"])

    # 3) 清残留 + 启动
    sh(["shell", "run-as com.winlatcn sh -c 'rm -f files/imagefs/home/xuser-1/hwbp_stats.txt files/imagefs/home/xuser-1/hwbp.log'"])
    sh(["shell", "am force-stop com.winlatcn"])
    time.sleep(2)
    sh(["shell", 'run-as com.winlatcn sh -c "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"'])
    t0 = time.time()
    sh(["shell", "am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path " + EXEC])
    print("  launched", flush=True)

    # 4) 轮询
    last, final, status, died_at = "", "", "TIMEOUT", ""
    no_proc, last_alive = 0, None
    while time.time() - t0 < 300:
        s = txt(sh(["shell", "run-as com.winlatcn sh -c 'cat files/imagefs/home/xuser-1/hwbp_stats.txt 2>/dev/null || echo no'"])).strip()
        if s and s != "no":
            final = s
            if s != last:
                print("  +%3.0fs  %s" % (time.time() - t0, s), flush=True)
                last = s
        p = txt(sh(["shell", "pidof Ekd5.exe"])).strip()
        alive_now = bool(p)
        if alive_now != last_alive:
            print("  +%3.0fs  process=%s" % (time.time() - t0, "UP" if alive_now else "DOWN"), flush=True)
            last_alive = alive_now
        no_proc = 0 if alive_now else no_proc + 1
        if no_proc >= 3 and time.time() - t0 > 45:
            status, died_at = "DIED", "+%.0fs" % (time.time() - t0)
            print("  ★ DIED at %s (last=%s)" % (died_at, last), flush=True)
            break
        ma, mh = re.search(r"arms=(\d+)", s), re.search(r"hits=(\d+)", s)
        if ma and mh and int(ma.group(1)) >= 2000 and int(mh.group(1)) <= 50:
            status = "RUNAWAY-REARM"
            print("  ★ 失控循环提前判定：arms=%s hits=%s @+%.0fs" %
                  (ma.group(1), mh.group(1), time.time() - t0), flush=True)
            break
        if ("arms=1600" in s or "arms=1536" in s) and time.time() - t0 > 40:
            status = "UNPACK-COMPLETE"
            break
        time.sleep(3)

    # 观察 12s 看是否存活
    alive = txt(sh(["shell", "pidof Ekd5.exe"])).strip() or "DEAD"
    hits = re.search(r"hits=(\d+)", final)
    hits_n = int(hits.group(1)) if hits else -1
    verdict = status
    if status == "UNPACK-COMPLETE":
        verdict = "PASS" if (hits_n >= 380 and alive != "DEAD") else "LOW-HITS"
    print("  ★ verdict=%s  alive=%s  hits=%s" % (verdict, alive, hits_n), flush=True)

    # 截图存证
    try:
        png = sh(["exec-out", "screencap", "-p"], 60).stdout
        if len(png) > 10000:
            open(os.path.join(OUT, "p0_%s_screen.png" % cell), "wb").write(png)
    except Exception as e:
        print("  screenshot skipped:", e)

    # 5) 结果落盘
    rec = [
        "P0 cell %s  %s" % (cell, datetime.now().isoformat(timespec="seconds")),
        "note      : " + note,
        "box64     : %s  md5=%s" % (b64, g1),
        "ntdll     : %s  md5=%s" % (ntdll, g2),
        "env       : prod (WINEDEBUG=-all, WINE_SS=0xe00, WINE_HWBP_SOFT=1)",
        "elapsed   : %.1fs" % (time.time() - t0),
        "status    : %s %s" % (status, died_at),
        "final     : " + (final or "(no stats)"),
        "hits      : %d" % hits_n,
        "alive     : " + alive,
        "verdict   : " + verdict,
    ]
    open(os.path.join(OUT, "p0_%s_result.txt" % cell), "w", encoding="utf-8", newline="\n").write("\n".join(rec) + "\n")
    print("  result -> a0_trace/p0_%s_result.txt" % cell, flush=True)

    sh(["shell", "am force-stop com.winlatcn"])
    print("  ⚠ 设备仍停在实验态；整轮结束必须跑 p0_restore.py", flush=True)


if __name__ == "__main__":
    main()
