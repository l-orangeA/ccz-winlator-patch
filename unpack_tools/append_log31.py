#!/usr/bin/env python3
"""追加 §31 最终结论."""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8").read()
if "31. ② 最终结论" in data:
    print("already")
    raise SystemExit(0)
add = """

## 31. ② 最终结论（2026-11-08）★

0.4.4 的完整实验矩阵（所有组合都测试过）：
- 作者版/自编译版 × fix108/fix110b × 纯env/各种日志 × FUSE/ext4 × 大小块/INTERP_SIGNAL/RDTSC
  → **所有组合都无法稳定通过解包**（唯一次存活未复现）。
- 0.4.4 的不兼容是**多层的**：已修 3 层（毛刺/KUSD/预存在映射），剩余层数未知。
- 每层修复让壳走得更远，但 0.4.4 的 dynarec/信号基础设施与 hwbp 模拟的交互
  存在根本性的差异，需要 box64 上游级别的修复（非黑盒补丁可解决）。

**终态定版**：0.3.0 + fix110b ntdll + r109g win32u + r112b wineserver + 生产 env + 新游戏 v2.20。
验证：72.3s 出标题、arms=1536 完美、游戏存活。
fix110b 在 0.3.0 上无回归（保留——修复了 wine-under-box64 的真实 bug）。

### 0.4.4 未来重启的条件
- box64 上游对 32 位 wow64 信号路径的重大重构版本发布
- 或者拿到 Winlator 作者的 0.4.4 编译补丁集（glitch 修复等非上游改动）
- 届时从 §30 检查点 + 全部补丁脚本继续，预计仍需多轮迭代"""
open(p, "w", encoding="utf-8", newline="\n").write(data + add)
print("appended")
