"""换装新游戏包: 备份 → 推包 → 合并(排除 SV) → 清理.
用法: python swap_game.py
"""
import subprocess, os, time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PCNEW = r"D:\ccz_winlator_patch\build\newgame\曹操传加强版v2.20"


def sh(a, t=900):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


# 1) 设备侧全量备份
print("backup...", flush=True)
r = sh(["shell", "rm -rf /sdcard/ccz_backup_20261107; cp -r /sdcard/ccz /sdcard/ccz_backup_20261107 && echo BACKUP-OK && du -sh /sdcard/ccz_backup_20261107"], 900)
print(txt(r).strip()[:200], flush=True)

# 2) 推包到 /data/local/tmp
print("push package...", flush=True)
r = sh(["push", PCNEW, "/data/local/tmp/newgame"], 900)
print(txt(r).strip()[:150], flush=True)

# 3) 删除暂存区的 SV（保留设备存档）+ 合并
r = sh(["shell", "rm -rf /data/local/tmp/newgame/SV; ls /data/local/tmp/newgame | wc -l"])
print("staged files:", txt(r).strip(), flush=True)
r = sh(["shell", "cp -r /data/local/tmp/newgame/* /sdcard/ccz/ && echo MERGE-OK"], 900)
print(txt(r).strip()[:150], flush=True)

# 4) 校验关键文件
r = sh(["shell", "md5sum /sdcard/ccz/Ekd5.exe /sdcard/ccz/SV/SV018.E5S 2>/dev/null | head -3"])
print("verify:\n" + txt(r).strip()[:300], flush=True)
r = sh(["shell", "ls /sdcard/ccz/SV | wc -l"])
print("SV saves kept:", txt(r).strip(), flush=True)
r = sh(["shell", "ls /sdcard/ccz/Itemicon.dll 2>/dev/null && echo NEW-DLL-OK"], 60)
print(txt(r).strip(), flush=True)
# 清暂存
sh(["shell", "rm -rf /data/local/tmp/newgame"])
print("done", flush=True)
