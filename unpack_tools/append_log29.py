#!/usr/bin/env python3
"""追加 §29 到日志."""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8").read()
if "29. ② 第六层" in data:
    print("already appended")
    raise SystemExit(0)
anchor = "set_env_phone.py prod。"
idx = data.rfind(anchor)
assert idx > 0, "anchor not found"
add = """

## 29. ② 第六层：第一行为分歧点定位（2026-11-08）★ 重大突破

sim 事件序列对齐（0.3.0 健康全程 27551 事件 vs 0.4.4 死亡 39 事件）：

```
两版完全相同的前 20 个事件（loader 分配、NTCONT rip=7ffff9d938 eax=ecx=405880）
0.3.0：NTCONT 后 → virtual_init 继续 → KUSD(0x7ffe0000) 分配 st=0 → 正常
0.4.4：同一 NTCONT 后 → SEGV rip=405880 addr=405880 err=14  ← 页面"不存在"！
```

- **err=14（0b1110）= 指令取指于不存在的页**——壳的入口代码 0x405880 在 0.4.4 上
  NtContinue 时**根本没映射**（比所有已修层都早：任何监视活动之前）。
- 0.3.0 同一时刻：0x401000-0x409fff c-r-x（镜像视图存在）。
- 0.4.4 的镜像视图（SEC_IMAGE）从未建立——wine 加载器的 PE 镜像映射在 0.4.4 上
  失败/被跳过 → 入口点无映射 → AV → 壳逻辑无法继续 → 死亡/挂起。
- 这解释了 boot 间差异：镜像映射的成败是 boot 敏感的（与 50% 黑屏率吻合）。

### 下一层（静态分析目标）
- wine 9.2 主 exe 镜像视图创建路径：virtual_map_section / loader 的 SEC_IMAGE 处理——
  找 0.4.4 上失败/跳过的条件（box64 0.4.4 的 mmap 定位层与它的交互）。
- 修复方向：wine virtual_init 的 fix110b 注册已知问题，但镜像视图创建本身
  在 0.4.4 失败的原因需从 wine 加载器源码定位。"""
data = data[:idx + len(anchor)] + add + data[idx + len(anchor):]
open(p, "w", encoding="utf-8", newline="\n").write(data)
print("appended after anchor at", idx)
