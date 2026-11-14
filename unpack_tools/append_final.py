#!/usr/bin/env python3
"""追加 §32 终结到日志."""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8").read()
if "32. ② 最终状态" in data:
    print("already")
    raise SystemExit(0)
add = """

## 32. ② 最终状态（2026-11-08 深夜）

### fix110c/d 实验结果
- fix110c（跳过 PROT_NONE 保留区）：壳**首次在 box64 0.4.4 上完整解包**（arms=1600, t=24.2s）
- 但 Dr 命中率低（hits=25 vs 400）→ 壳在 anti-debug 循环卡住
- 原因：fix110c 排除视图后，box64 0.4.4 自身的 SMC 页保护（PROT_DYNAREC 切换）
  与 hwbp 的页保护（PROT_WRITEWATCH 切换）**在同一物理页上冲突**——两套 mprotect 互相覆盖
- fix110d（跳过低地址）确认了同样的现象

### 静态源码对比的关键发现
- box64 0.4.4 新增了 `signal32.c`（独立 32 位信号转发模块）和 `defer_signal` 延迟机制
- `hwbp_update` 在 NT API 层面（NtSetContextThread/NtGetContextThread）被调用——与 box64 版本无关
- 但 box64 0.4.4 的 SMC 保护系统（custommem.c:2670 的 mprotect 调用）会覆盖 hwbp 的页保护
- **两套页保护系统（box64 SMC + wine hwbp）在 0.4.4 上不可共存**——这是结构性冲突

### 手机最终态（已验证 72.1s 出标题）
- box64 = 原版 0.3.0（dd297b6d）——0.4.4 需要解决 SMC+hwbp 共存问题后才能用
- ntdll = **fix110b**（eddb74af）——保留预映射注册修复（对 0.3.0 无回归）
- win32u = r109g + wineserver = r112b + 生产 env + 新游戏 v2.20
- 全部 fix110b/c/d 构建物和补丁脚本已存档，未来 box64 重构后可继续"""
open(p, "w", encoding="utf-8", newline="\n").write(data + add)
print("appended")
