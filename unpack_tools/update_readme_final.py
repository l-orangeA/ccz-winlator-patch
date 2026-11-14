#!/usr/bin/env python3
"""README 最终更新."""
p = r"D:\ccz_winlator_patch\README.md"
data = open(p, encoding="utf-8").read()
if "0.4.4 双通道日志不可复现" in data:
    print("already updated")
    raise SystemExit(0)
anchor = "## 后续大幅提速候选"
idx = data.find(anchor)
if idx >= 0:
    add = """## ② box64 0.4.4 最终结论（2026-11-08，日志 §21-34）
- **终止。** 六层修复穷尽后仍无法稳定通过解包（双通道日志存活的 1/1 不可复现）
- 0.4.4 的 defer_signal + signal32.c + PROT_DYNAREC 三个机制与 hwbp 模拟系统性不兼容
- 资产：r113/fix110b/fix110c/fix110d 构建物+补丁脚本+死亡现场日志全部存档

"""
    data = data[:idx] + add + data[idx:]
else:
    data += "\n" + add
open(p, "w", encoding="utf-8", newline="\n").write(data)
print("updated")
