import os, zipfile, time, sys

GAME = r"D:\ccz\曹操传加强版2.2"
PATCH = r"D:\ccz_winlator_patch"
OUT = r"C:\Users\MECHREV\Desktop\曹操传Winlator整合包_20261102.zip"

STORE_EXT = {".apk", ".avi", ".wav", ".png", ".jpg", ".zip", ".gz", ".7z"}

README = """曹操传加强版 v2.20 —— Winlator 平板整合包（2026-11-02）
=========================================================

★ 这版的模拟器安装包**已经内置了 Wine 补丁**，装完不需要再往容器里拷任何文件。
   （补丁=nvdll 里的软件硬件断点模拟；同时已内置默认值，容器也不需要额外环境变量。）

解压后目录：
  1-游戏\\        游戏本体（含最新存档）
  2-模拟器\\      Winlator 8.0：winlator8.0_ccz_bundled.apk（已内置补丁+桌面快捷方式）
                  winlator8.0_original.apk（原版，仅用于回滚）
  3-备份\\        ntdll 补丁单独文件（只在"已有容器但没装内置版"时需要）
  4-文档\\        技术记录

部署（三步）
------------
1) 安装模拟器：
   adb install -r 2-模拟器\\winlator8.0_ccz_bundled.apk

2) 拷游戏：把 1-游戏 里的内容整个放到 /sdcard/Download/ccz22/
   （容器里的 D: 盘就是这个目录，游戏路径是 D:\\Download\\ccz22\\Ekd5.exe）

3) 打开 Winlator，长按桌面上的 Winlator 图标 → 菜单里点"曹操传"（或把"曹操传"拖到桌面做成常驻图标）。
   首次启动会解包约 50 秒，之后进标题画面。

说明
----
- 存档在 1-游戏\\SV\\ 里（SV*.E5S），本包内的是 PC 上最新的进度。
- 容器里如果已经玩过，建议先在 Winlator 里新建/使用自己的容器；
  本包不附带容器数据（容器里有你的存档和设置，属于"你的机器"的部分）。
- 回滚模拟器：adb install -r 2-模拟器\\winlator8.0_original.apk（同签名，容器数据不丢）。
- 本整合包不含任何破解/绕过正版的工具；补丁只是让原本的反调试机制在 ARM 上正常工作。
"""

def add_tree(z, root, prefix, skip_dirs=()):
    n = 0
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in skip_dirs]
        for fn in filenames:
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, root)
            arc = os.path.join(prefix, rel).replace("\\", "/")
            ext = os.path.splitext(fn)[1].lower()
            comp = zipfile.ZIP_STORED if ext in STORE_EXT else zipfile.ZIP_DEFLATED
            try:
                z.write(full, arc, compress_type=comp, compresslevel=6 if comp else None)
                n += 1
            except Exception as e:
                print("skip", full, e)
    return n

def main():
    t0 = time.time()
    if os.path.exists(OUT):
        os.remove(OUT)
    with zipfile.ZipFile(OUT, "w", allowZip64=True) as z:
        z.writestr("曹操传Winlator整合包/说明.txt", README)
        print("game:", add_tree(z, GAME, "曹操传Winlator整合包/1-游戏", skip_dirs={"-C", "-xf", "tar"}))
        # PC 上 Ekd5.exe 缺失（只剩 Ekd5_orig_backup.exe，同大小同时间戳），补进包里
        src = os.path.join(GAME, "Ekd5_orig_backup.exe")
        dst = "曹操传Winlator整合包/1-游戏/Ekd5.exe"
        if os.path.exists(src) and not any(n.endswith("/1-游戏/Ekd5.exe") for n in z.namelist()):
            z.write(src, dst)
            print("added Ekd5.exe from Ekd5_orig_backup.exe")
        for f in ("winlator8.0_ccz_bundled.apk", "winlator8.0_original.apk", "README.md", "testkey.p12"):
            p = os.path.join(PATCH, "apk", f)
            if os.path.exists(p):
                z.write(p, "曹操传Winlator整合包/2-模拟器/" + f)
        for f, dst in (("ntdll_fix104.so", "3-备份/ntdll_fix104.so"),
                       ("ntdll_fix103.so", "3-备份/ntdll_fix103.so"),
                       ("ntdll_fix99.so", "3-备份/ntdll_fix99.so")):
            p = os.path.join(PATCH, "build", f)
            if os.path.exists(p):
                z.write(p, "曹操传Winlator整合包/" + dst)
        z.write(os.path.join(PATCH, "README.md"), "曹操传Winlator整合包/4-文档/补丁README.md")
        z.write(os.path.join(PATCH, "report", "Wine底层修复-只读栈异常投递.md"),
                "曹操传Winlator整合包/4-文档/Wine底层修复-只读栈异常投递.md")
        z.write(os.path.join(PATCH, "report", "方案1-box64原生Dr-可行性.md"),
                "曹操传Winlator整合包/4-文档/方案1-可行性.md")
    sz = os.path.getsize(OUT)
    print("OK %s  %.1f MB  %.0fs" % (OUT, sz / 1e6, time.time() - t0))

if __name__ == "__main__":
    sys.exit(main())
