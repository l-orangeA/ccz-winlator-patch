#!/usr/bin/env python3
"""KILL-LEFTOVER: pkill stale proot children at Activity start.

Root cause (2026-11-09 23:1x):
  am force-stop only kills the app process; GuestProgramLauncherComponent.stop()
  only does Process.killProcess(proot pid). The daemonized wineserver /
  winedevice.exe / Ekd5.exe survive, so every "restart" stacks another shell
  instance until they starve each other (looked like an unfixable freeze).

This patch runs, at the very start of XServerDisplayActivity.onCreate:
  sh -c "pkill -f Ekd5.exe; pkill -f wineserver; pkill -f winedevice.exe"
wrapped in try/catch so a missing pkill can never crash the app.
"""
import sys

A = "/root/wn_gl60/smali/com/winlator/XServerDisplayActivity.smali"
s = open(A, encoding="utf-8").read()

anchor = """    invoke-super/range {p0 .. p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V

    .line 131"""
if s.count(anchor) != 1:
    sys.exit("FAIL: anchor count=%d" % s.count(anchor))

block = """    invoke-super/range {p0 .. p1}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V

    # KILL-LEFTOVER: am force-stop cannot kill proot children (wineserver/Ekd5)
    :try_start_kill
    const-string v2, "sh"
    const-string v3, "-c"
    const-string v4, "pkill -f Ekd5.exe; pkill -f wineserver; pkill -f winedevice.exe"
    const/4 v6, 0x3
    new-array v5, v6, [Ljava/lang/String;
    const/4 v6, 0x0
    aput-object v2, v5, v6
    const/4 v6, 0x1
    aput-object v3, v5, v6
    const/4 v6, 0x2
    aput-object v4, v5, v6
    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;
    move-result-object v2
    invoke-virtual {v2, v5}, Ljava/lang/Runtime;->exec([Ljava/lang/String;)Ljava/lang/Process;
    :try_end_kill
    .catch Ljava/lang/Exception; {:try_start_kill .. :try_end_kill} :catch_kill
    goto :goto_kill
    :catch_kill
    move-exception v2
    :goto_kill

    .line 131"""
s = s.replace(anchor, block, 1)
open(A, "w", encoding="utf-8", newline="\n").write(s)
print("OK: KILL-LEFTOVER inserted; marker count =", s.count("KILL-LEFTOVER"))
