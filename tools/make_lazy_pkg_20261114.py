# -*- coding: utf-8 -*-
"""生成懒人包的使用说明，并把签名后的 APK 打成最终 zip。"""
import os
import zipfile

ROOT = r"D:\ccz_winlator_patch"
WORK = os.path.join(ROOT, "build", "_lazy_pkg")
APK = os.path.join(ROOT, "build", "ccz_lazy_signed.apk")
OUT = os.path.join(ROOT, "deliverables", "曹操传Winlator懒人包_20261114.zip")
TOP = "曹操传Winlator懒人包_20261114"

README = r'''曹操传加强版 v2.20 —— Winlator 8.0 懒人包（2026-11-14）
============================================================

【这个包里只有两样东西】
  曹操传Winlator懒人包.apk    装它就行
  使用说明.txt                本文件

不需要 adb、不需要连电脑、不需要跑脚本。
Wine 补丁已经打进 APK 内部的系统镜像里，新建容器会自动使用兼容模式。


【三步开始玩】
1. 安装 APK。
   ★ 如果这台设备以前装过 Winlator（com.winlatcn）：
     必须先「卸载」或「清除应用数据」再装，否则系统镜像不会重新解包，
     内置的补丁就不会生效。（游戏本体在 /sdcard/ccz，卸载 App 不会删游戏）
     ★ 实测机型：骁龙 765G / Android 12，卸载失败版后全新安装 -> 建容器 -> 出标题，全程通过。
2. 打开 App，等第一次的系统文件安装进度条走完（约 1 分钟）。
   容器管理 -> 右上「添加」-> 直接点「确认」建一个容器。
   Box64 预设已强制为「兼容」，不用改。
3. 容器管理 -> ccz -> 管理 -> 磁盘(D:) -> ccz -> Ekd5.exe 的运行按钮
   20~60 秒出标题画面即成功。


【游戏本体与存档】
- 本包不含游戏。游戏目录固定为 /sdcard/ccz（容器内 D:\ccz）。
  把整个游戏目录（Ekd5.exe、E5/、Map/、SV/、SoundTrk/ 等）放进去即可。
- 存档在 /sdcard/ccz/SV。SV.E5S 是空索引，SV001.E5S 起才是存档。
- 要全新开局：删掉 SV 目录里的 SV0*.E5S，只留 SV.E5S。
- 升级前先整目录备份 /sdcard/ccz。


【两台设备要区分】
  骁龙 / Adreno（如小米平板 6S Pro / 8 Gen 2）
      默认 turnip，装完直接用。
  天玑 / Mali（如 vivo / 天玑 9200）
      建好容器后，在容器「编辑」里把图形驱动改成 Virgl，其它不动。
      不改会用 Turnip，X 桌面可能起不来。
差异只在图形驱动；补丁本身两台通用。


【这个版本修了什么】
1. 三件套补丁（ntdll / win32u / wineserver）已内置进系统镜像，
   不再需要每次重装后重新推送。
2. 根治「新建容器自动变成 PERFORMANCE」：
   原版会读全局设置里的 box64_preset，一旦是 PERFORMANCE，
   等于 BOX64_DYNAREC_STRONGMEM=0，会让加壳程序在解包早期卡死（黑屏）。
   本版新建容器强制兼容模式，且把 PERFORMANCE 预设本身也改成安全参数。
3. PERFORMANCE 预设本身也改成安全参数，即便手动选中它也不会卡死。
   实测新建容器为：turnip / dxvk / alsa / 兼容，游戏照常出标题；
   dxwrapper 与音频驱动对本游戏无影响，不用改。


【卡住怎么办】
- 保持屏幕常亮。熄屏会造成「假死」，容器看着不动其实是被系统挂起。
- 卡过后先清后台再重开；别在最近任务里反复上滑清理。
- 在设置里执行过「重装系统文件」不用怕，补丁是内置的，解包完就带上了。
- 验收（需 adb）：hwbp_stats arms=1536 faults=44090 hits=384。


【内置文件的校验值】
  ntdll      4e7d876de5f0d0defebd8a34b664e403
  win32u     2be1316c83d603c4defae8669fcc1dfb
  wineserver 9df1d0c325f2b9c2f7e71c82fb5fab70

【适用】仅 Winlator 8.0 + Wine 9.2 + box64 0.3.0；官方 11.2 不适用。
【源码与回滚】https://github.com/l-orangeA/ccz-winlator-patch
'''


def main():
    os.makedirs(WORK, exist_ok=True)
    txt = os.path.join(WORK, "使用说明.txt")
    with open(txt, "w", encoding="gbk", newline="\r\n") as f:
        f.write(README)
    print("说明已生成:", txt, os.path.getsize(txt), "B (GBK/CRLF)")

    if not os.path.isfile(APK):
        print("签名 APK 尚未就绪，跳过打包:", APK)
        return
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_STORED, allowZip64=True) as z:
        z.write(APK, TOP + "/曹操传Winlator懒人包.apk")
        z.write(txt, TOP + "/使用说明.txt")
    print("成品:", OUT)
    print("大小: %.1f MB" % (os.path.getsize(OUT) / 1048576.0))
    with zipfile.ZipFile(OUT) as z:
        for n in z.namelist():
            print("  ", n, z.getinfo(n).file_size)


if __name__ == "__main__":
    main()
