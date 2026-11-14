#!/usr/bin/env python3
"""追加 §33+§34 最终结论."""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8").read()
if "33. ② 0.4.4 双通道日志不可复现" in data:
    print("already appended")
    raise SystemExit(0)
add = """

## 33. ② 0.4.4 双通道日志不可复现 → 0.4.4 终态确认（2026-11-08）

重跑 r113+fix110b+INTERP_SIGNAL+双通道日志（WINEDEBUG=+virtual + DYNAREC_LOG=2）→ +21s 死亡。
之前 cap_demote 的存活（200s 出游戏画面）确认为 1/1 偶然，不可复现。

**0.4.4 的最终定论：无法通过黑盒补丁修复。** 六层修复（毛刺/KUSD/映射注册/PROT_NONE 跳过/
低地址排除/SIGTRAP 不可延迟）已穷尽所有可触及的层面。0.4.4 的信号处理基础设施与 hwbp
模拟存在**系统级的时序和上下文差异**，修复需要 box64 上游对 32 位 wow64 信号路径的重构。

**终态固化：0.3.0 + fix110b ntdll + r109g win32u + r112b wineserver + 生产 env + 新游戏 v2.20**
已验证（72.1s 出标题，arms=1600 完美档案）。新游戏 v2.20 在此配置上完美运行。

## 34. 新游戏 v2.20 换装确认（2026-11-08）★

- 网盘新包 v2.20 → PC 解压 → md5 全量对比（40 文件变更：Ekd5.exe/AutoDLL.ini/Imsg.e5/Logo.avi/
  Itemicon.dll(新增)/36 个 RS 战役脚本；存档 SV*.E5S 零风险）
- 设备：新游戏已换入 /sdcard/ccz，存档 456 文件全保留，读档/战斗验证通过
- PC：robocopy 换入 D:\\ccz\\曹操传加强版2.2，SV 459 存档保留
- 回滚锚点：/sdcard/ccz_backup_20261107/
- 清理：删除了本战役死产物（~460MB），鼠标改回拖拽式

### 当前手机最优态（已验证）
box64=原版 0.3.0（dd297b6d）+ ntdll=fix110b（eddb74af）+ win32u=r109g（2be1316c）+
wineserver=r112b（9df1d0c3）+ 生产 env + 新游戏 v2.20 → **72.1s 出标题，arms=1600 完美**"""
open(p, "w", encoding="utf-8", newline="\n").write(data + add)
print("appended")
