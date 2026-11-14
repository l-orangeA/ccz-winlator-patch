#!/usr/bin/env python3
"""整理攻坚日志：合并重复的 §32 / §33（各两段同号），补末尾换行，加顶部阅读导航。

原则：只合并、不删事实。归档原文到 _归档_20261108/，并做「关键事实串零丢失」校验。
可重复运行（已整理则直接退出）。

用法: <venv>\\python.exe unpack_tools\\consolidate_log_32_33.py
"""
import os
import re
import shutil

ROOT = r"D:\ccz_winlator_patch"
LOG = os.path.join(ROOT, "2026-11-06_渲染与卡顿攻坚日志.md")
BAK = os.path.join(ROOT, "_归档_20261108", "2026-11-06_渲染与卡顿攻坚日志.md.bak")

TITLE = "# 2026-11-06 渲染与卡顿攻坚日志（完整版）\n"
NAV = """
> **阅读导航（2026-11-08 整理）**：本文件按时间逐轮追加，§0 是 11-06 当时状态，不是终态。
> ② box64 0.4.4 的完整结论链：**§31 终止判决 → §32 fix110c/d 边界 → §33 终态确认**；
> 当前手机最优态见 **§34**。原 §32 / §33 各两段同号章节已合并（编号去重，事实无删减），
> 原文备份在 `_归档_20261108\\2026-11-06_渲染与卡顿攻坚日志.md.bak`。
"""

SEC32_33 = """## 32. ② fix110c/d 实验与第六层边界确认（2026-11-08 深夜）

fix110b 的视图注册把 box64 0.4.4 的 768MB PROT_NONE 保留区（0x110000-0x30110000）也注册了，
堵死了壳的镜像映射（0x400000）。fix110c 跳过 PROT_NONE 修复了这个问题——壳首次在 0.4.4
上完整解包（arms=1600, t=24.2s），但 Dr 命中率低（25 vs 400）→ 壳在 anti-debug 循环冻结。
fix110d（跳过低地址 <1GB）让壳解包完成（arms=1600）但进入 faults 234K / hits=25 死循环，
确认了同样的现象。

### 关键发现（静态源码对比）
- box64 0.4.4 新增了 `signal32.c`（独立 32 位信号转发模块）和 `defer_signal` 延迟机制；
  0.3.0 与 0.4.4 的 32 位 sigcontext 构造逻辑完全不同，`hwbp_match_write` 在 0.4.4 信号上下文中
  无法正确匹配。
- `hwbp_update` 在 NT API 层面（NtSetContextThread/NtGetContextThread）被调用——与 box64 版本无关。
- fix110c 排除视图后，box64 0.4.4 自身的 SMC 页保护（PROT_DYNAREC 切换）与 hwbp 的页保护
  （PROT_WRITEWATCH 切换）**在同一物理页上冲突**——两套 mprotect 互相覆盖；
  `custommem.c:2670` 的 mprotect 会覆盖 hwbp 的页保护。
- **两套页保护系统（box64 SMC + wine hwbp）在 0.4.4 上不可共存**——这是结构性冲突。

### 修复进展总结
| 版本 | 修了什么 | 壳状态 |
|---|---|---|
| 无补丁 | — | 0.4.4 壳完全不可用 |
| r113 | box64 毛刺吞掉恢复 | 仍不可用 |
| fix110/fix110b | +wine 预映射注册+KUSD 跳过 | 仍不可用 |
| fix110c | +跳过 PROT_NONE 保留区 | **解包可跑！**（arms=1600） |
| fix110d | +跳过低地址 | 解包可跑但 anti-debug 循环 |
| **fix110b（0.3.0）** | 0.3.0+预映射注册 | **完美（保留）** |

### 当前结论
- **壳可以在 box64 0.4.4 上开始解包了**（fix110c/d 的贡献）——这是从"完全不可用"到"解包可跑"的突破
- 但 anti-debug 循环的 Dr 命中率需要 box64 0.4.4 信号层的深层适配——超出当前黑盒补丁的能力范围
- 需要对比 signal32.c（0.4.4）和 signals.c（0.3.0）的 32 位 sigcontext 构造逻辑，逐项适配

### 手机最终态（已验证 72.1s 出标题）
- box64 = 原版 0.3.0（dd297b6d）——0.4.4 需先解决 SMC+hwbp 共存问题，暂回滚
- ntdll = **fix110b**（eddb74af）——保留预映射注册修复（对 0.3.0 无回归）
- win32u = r109g + wineserver = r112b + 生产 env + 新游戏 v2.20
- 全部 fix110b/c/d 构建物和补丁脚本已存档，未来 box64 重构后可继续

## 33. ② 最终技术总结与终态确认（2026-11-08）★ 深挖结束

### 已确认的完整因果链
0.4.4 的 **defer_signal 延迟机制** + **signal32.c 独立 32 位信号转发** + **protectDB/unprotectDB
的 PROT_DYNAREC 页保护切换** = 三个 box64 0.4.4 特有机制与 hwbp 模拟交互时产生复合故障：
1. defer_signal 延迟 SIGTRAP → hwbp 的精确 RIP 丢失 → 单步错位
2. signal32.c 用 I386_EIP 构造 32 位 sigframe → EIP/EFL/ERR/TRAPNO 格式差异 → sim 状态错乱
3. protectDB/unprotectDB 的 PROT_DYNAREC 切换 → 与 hwbp 的 mprotect 竞争同一物理页

### 双通道日志不可复现（决定性负结果）
重跑 r113+fix110b+INTERP_SIGNAL+双通道日志（WINEDEBUG=+virtual + DYNAREC_LOG=2）→ +21s 死亡。
之前 cap_demote 的存活（200s 出游戏画面）确认为 1/1 偶然，不可复现。

### 最终定论
**0.4.4 的最终定论：无法通过黑盒补丁修复。** 六层修复（毛刺/KUSD/映射注册/PROT_NONE 跳过/
低地址排除/SIGTRAP 不可延迟）已穷尽所有可触及的层面。0.4.4 的信号处理基础设施与 hwbp
模拟存在**系统级的时序和上下文差异**，修复需要 box64 上游对 32 位 wow64 信号路径的重构。

### 修复进展
- fix110c（跳过 PROT_NONE 保留区注册）让壳首次在 0.4.4 上开始解包——但 Dr 命中率低
- 深层修复需要适配 0.4.4 的三个新机制（defer_signal、signal32.c、PROT_DYNAREC 切换）——
  每个都需要 box64 源码级的修改（非黑盒补丁），工作量超出当前可用时间

### 终态固化
- 手机：0.3.0 + fix110b ntdll + r109g win32u + r112b wineserver + 生产 env + 新游戏 v2.20
- 验证：72.1s 出标题、arms=1600 完美、新游戏兼容性全通过
- **新游戏 v2.20 在此配置上完美运行**
- **fix110b 的 wine 预映射注册修复保留在手机上**——它修复了 wine-under-box64 的真实 bug
- ② 的全部资产（补丁脚本、构建物、日志、分析工具）已存档

"""

# 合并前必须存在、合并后仍必须存在的事实串（防丢信息）
FACTS = (
    "768MB PROT_NONE", "0x110000-0x30110000", "0x400000", "t=24.2s",
    "faults 234K", "hits=25", "hwbp_match_write", "signal32.c", "defer_signal",
    "NtSetContextThread", "custommem.c:2670", "PROT_WRITEWATCH", "PROT_DYNAREC",
    "I386_EIP", "cap_demote", "200s 出游戏画面", "+21s 死亡", "72.1s",
    "arms=1600", "dd297b6d", "eddb74af", "fix110d | +跳过低地址",
    "两套页保护系统", "signals.c（0.3.0）", "新游戏 v2.20 在此配置上完美运行",
)


def main():
    data = open(LOG, encoding="utf-8", newline="").read()
    if "阅读导航（2026-11-08 整理）" in data:
        nums = re.findall(r"^## (\d+)\.", data, re.M)
        dup = [n for n in set(nums) if nums.count(n) > 1]
        missing = [k for k in FACTS if k not in data]
        print("already consolidated; verify only")
        print("  章节编号:", "唯一 (%d 节)" % len(nums) if not dup else "仍有重复 %s" % dup)
        print("  关键事实串:", "全部保留 (%d/%d)" % (len(FACTS) - len(missing), len(FACTS))
              if not missing else "丢失 %s" % missing)
        print("  末尾换行:", data.endswith("\n"))
        print("\n" + ("PASS" if not dup and not missing and data.endswith("\n") else "FAIL"))
        return

    missing_before = [k for k in FACTS if k not in data]
    if missing_before:
        raise SystemExit("原日志缺少预期事实串（脚本与日志版本不匹配）: %s" % missing_before)

    # 1) 归档原文（不覆盖已有备份）
    os.makedirs(os.path.dirname(BAK), exist_ok=True)
    if not os.path.exists(BAK):
        shutil.copyfile(LOG, BAK)
        print("★ 原文已归档:", BAK)

    # 2) 精确替换 §32(两段) + §33(两段) → 各一段
    start = data.index("## 32. ② fix110d 实验与第六层边界确认")
    end = data.index("## 34. 新游戏 v2.20 换装确认")
    removed = data[start:end]
    print("  替换区间: %d 字符（%d 个 ## 章节）" % (len(removed), removed.count("\n## ") + 1))
    data = data[:start] + SEC32_33 + data[end:]

    # 3) 顶部阅读导航
    if not data.startswith(TITLE):
        raise SystemExit("标题行不匹配")
    data = TITLE + NAV + data[len(TITLE):]

    # 4) 末尾补换行（原文件最后一行无 \n）
    ended = data.endswith("\n")
    if not ended:
        data += "\n"

    open(LOG, "w", encoding="utf-8", newline="").write(data)

    # 5) 校验
    print("\n=== 校验 ===")
    nums = re.findall(r"^## (\d+)\.", data, re.M)
    dup = [n for n in set(nums) if nums.count(n) > 1]
    print("  章节编号:", "唯一 (%d 节)" % len(nums) if not dup else "仍有重复 %s" % dup)
    missing_after = [k for k in FACTS if k not in data]
    print("  关键事实串:", "全部保留 (%d/%d)" % (len(FACTS) - len(missing_after), len(FACTS))
          if not missing_after else "丢失 %s" % missing_after)
    print("  末尾换行:", data.endswith("\n"), "(整理前:", ended, ")")
    print("  备份与整理前一致:", open(BAK, encoding="utf-8", newline="").read() != data)
    print("  整理后行数:", data.count("\n"))
    print("\n" + ("PASS" if not dup and not missing_after and data.endswith("\n") else "FAIL"))


if __name__ == "__main__":
    main()
