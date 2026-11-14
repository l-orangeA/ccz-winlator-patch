#!/usr/bin/env python3
"""README 更新 0.4.4 最终结论."""
p = r"D:\ccz_winlator_patch\README.md"
data = open(p, encoding="utf-8").read()
if "0.4.4 兼容性深挖最终结论" in data:
    print("already")
    raise SystemExit(0)
anchor = "## 后续大幅提速候选（2026-11-07 存档，详见日志 §20-26）"
idx = data.find(anchor)
if idx < 0:
    # 找不到就用追加
    data += "\n## 0.4.4 兼容性深挖最终结论（2026-11-08，日志 §21-31）\n- 六层根因链：①毛刺吞掉(r113修复) ②KUSD冲突(fix110b) ③预存在映射遗忘(fix110b)\n  ④⑤镜像视图未建立/多阶段壳 ⑥壳入口无映射——box64 0.4.4 信号基础设施与 hwbp 模拟根本性不兼容\n- 实验矩阵全测 → 均无法稳定通过解包 → 终止。资产：r113/fix110b 构建物+补丁+死亡现场日志\n- 终态：0.3.0 + fix110b + r109g + r112b + 生产env + 新游戏v2.20；PC已同步换装\n"
    open(p, "w", encoding="utf-8", newline="\n").write(data)
    print("appended")
else:
    add = "## 0.4.4 兼容性深挖最终结论（2026-11-08，日志 §21-31）\n- 六层根因链：①毛刺吞掉(r113修复) ②KUSD冲突(fix110b) ③预存在映射遗忘(fix110b)\n  ④⑤镜像视图未建立/多阶段壳 ⑥壳入口无映射——box64 0.4.4 信号基础设施与 hwbp 模拟根本性不兼容\n- 实验矩阵全测 → 均无法稳定通过解包 → 终止。资产：r113/fix110b 构建物+补丁+死亡现场日志\n- 终态：0.3.0 + fix110b + r109g + r112b + 生产env + 新游戏v2.20；PC已同步换装\n\n"
    data = data[:idx] + add + data[idx:]
    open(p, "w", encoding="utf-8", newline="\n").write(data)
    print("inserted before anchor")
