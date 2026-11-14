#!/usr/bin/env python3
"""追加 §32 到日志."""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8").read()
if "32. ② fix110d 实验与第六层边界" in data:
    print("already")
    raise SystemExit(0)
add = """

## 32. ② fix110d 实验与第六层边界确认（2026-11-08 深夜）

fix110b 的视图注册把 box64 0.4.4 的 768MB PROT_NONE 保留区（0x110000-0x30110000）也注册了，
堵死了壳的镜像映射（0x400000）。fix110c 跳过 PROT_NONE 修复了这个问题——壳首次在 0.4.4
上完整解包（arms=1600），但 Dr 命中率低（25 vs 400）→ 壳在 anti-debug 循环冻结。

fix110d（跳过低地址 <1GB）让壳解包完成（arms=1600）但进入 faults 234K / hits=25 死循环。
确认 box64 0.4.4 的 `hwbp_match_write` 在 0.4.4 信号上下文中无法正确匹配——这是更深层的
信号处理差异（0.3.0 和 0.4.4 的 signal32.c 的 sigcontext 构造逻辑完全不同）。

### 当前结论
- **壳可以在 box64 0.4.4 上开始解包了**（fix110c/d 的贡献）——这是从"完全不可用"到"解包可跑"的突破
- 但 anti-debug 循环的 Dr 命中率需要 box64 0.4.4 信号层的深层适配——超出当前黑盒补丁的能力范围
- 需要对比 signal32.c（0.4.4）和 signals.c（0.3.0）的 32 位 sigcontext 构造逻辑，逐项适配

### 修复进展总结
| 版本 | 修了什么 | 壳状态 |
|---|---|---|
| 无补丁 | — | 0.4.4 壳完全不可用 |
| r113 | box64 毛刺吞掉恢复 | 仍不可用 |
| fix110/fix110b | +wine 预映射注册+KUSD 跳过 | 仍不可用 |
| fix110c | +跳过 PROT_NONE 保留区 | **解包可跑！**（arms=1600） |
| fix110d | +跳过低地址 | 解包可跑但 anti-debug 循环 |
| **fix110b（0.3.0）** | 0.3.0+预映射注册 | **完美（保留）** |

### 手机最终态（已验证 72.1s）
- box64 = 原版 0.3.0（dd297b6d）——0.4.4 需要更多层适配，暂回滚
- ntdll = **fix110b**（eddb74af）——保留预映射注册修复
- win32u = r109g + wineserver = r112b + 生产 env + 新游戏 v2.20
- 全部 fix110c/d 构建物和补丁脚本已存档"""
open(p, "w", encoding="utf-8", newline="\n").write(data + add)
print("appended")
