#!/usr/bin/env python3
"""追加 §33 最终技术总结."""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8").read()
if "33. ② 最终技术总结" in data:
    print("already")
    raise SystemExit(0)
add = """

## 33. ② 最终技术总结（2026-11-08）★ 深挖结束

### 已确认的完整因果链
0.4.4 的 **defer_signal 延迟机制** + **signal32.c 独立 32 位信号转发** + **protectDB/unprotectDB
的 PROT_DYNAREC 页保护切换** = 三个 box64 0.4.4 特有机制与 hwbp 模拟交互时产生复合故障：
1. defer_signal 延迟 SIGTRAP → hwbp 的精确 RIP 丢失 → 单步错位
2. signal32.c 用 I386_EIP 构造 32 位 sigframe → EIP/EFL/ERR/TRAPNO 格式差异 → sim 状态错乱
3. protectDB/unprotectDB 的 PROT_DYNAREC 切换 → 与 hwbp 的 mprotect 竞争同一物理页

### 修复进展
- fix110c（跳过 PROT_NONE 保留区注册）让壳首次在 0.4.4 上开始解包——但 Dr 命中率低
- 深层修复需要适配 0.4.4 的三个新机制（defer_signal、signal32.c、PROT_DYNAREC 切换）——
  每个都需要 box64 源码级的修改（非黑盒补丁），工作量超出当前可用时间

### 最终状态
- 手机：0.3.0 + fix110b ntdll + r109g win32u + r112b wineserver + 生产 env + 新游戏 v2.20
- 验证：72.1s 出标题、arms=1600 完美、新游戏兼容性全通过
- **fix110b 的 wine 预映射注册修复保留在手机上**——它修复了 wine-under-box64 的真实 bug
- ② 的全部资产（补丁脚本、构建物、日志、分析工具）已存档"""
open(p, "w", encoding="utf-8", newline="\n").write(data + add)
print("appended")
