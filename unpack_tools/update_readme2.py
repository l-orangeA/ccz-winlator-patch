#!/usr/bin/env python3
"""README 追加 fix110c/d 突破."""
p = r"D:\ccz_winlator_patch\README.md"
data = open(p, encoding="utf-8").read()
if "fix110c/d 突破" in data:
    print("already")
    raise SystemExit(0)
old = "- 终态：0.3.0 + fix110b + r109g + r112b + 生产env + 新游戏v2.20；PC已同步换装"
new = old + """
- **fix110c/d 突破**：壳首次在 0.4.4 上解包成功（arms=1600），但 Dr 命中率低导致 anti-debug
  循环卡住——需适配 signal32.c 的 32 位 sigcontext 构造逻辑（详见日志 §32）
- 资产：`build/ntdll_fix110c.so`、`build/ntdll_fix110d.so`、补丁脚本全套"""
if old in data:
    data = data.replace(old, new)
    open(p, "w", encoding="utf-8", newline="\n").write(data)
    print("updated")
else:
    print("anchor not found, appending to end")
    data += "\n- **fix110c/d 突破**：壳首次在 0.4.4 上解包成功（arms=1600），但 Dr 命中率低导致 anti-debug 循环卡住——需适配 signal32.c 的 32 位 sigcontext 构造逻辑。资产已存档。\n"
    open(p, "w", encoding="utf-8", newline="\n").write(data)
    print("appended to end")
