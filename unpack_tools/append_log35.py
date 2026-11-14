#!/usr/bin/env python3
"""追加攻坚日志 §35：0.4.4 升级重构开工（用户明确要求更新日志）。"""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8", newline="").read()
if "## 35." in data:
    print("already appended")
    raise SystemExit(0)

add = """

## 35. ③ 0.4.4 升级重构开工（2026-11-08 14:3x）

### 35.1 前期结论（详见 report/box64-0.4.4-重构路线可行性研究.md）
- 黑盒补丁路线已按止损条件终止（升级方案 §11）：B/E4/E7/E9/E10 单变量链全灭。
- 路线 A（信号层重构）A1–A3D 共 8 个单变量实验全部失败（可行性研究 §10–§13）。
- 根因第一次被直接看到：只读探针（E14）证明 Wine 清 TF 生效、A3 的宿主 xFlags 写回生效，
  但 `siglongjmp` 恢复 callee-saved **x26**（= guest EFLAGS），把它覆盖回 TF=1。
- 三种替换 siglongjmp 的方式（A3B 只改 xFlags / A3C 跳 native_epilog / A3D 全量同步+原 PC）
  分别早死、黑屏、黑屏 → 确认 `signals.c` 层已到头。

### 35.2 决定：改引擎（用户拍板，目标是提速与流畅）
- 唯一合理修复点：`dynarec.c` 的 `EmuRun` setjmp 恢复分支——
  `siglongjmp` 回来后、进 dynarec/解释器前，强制 `x26 = emu->eflags`。
- 实施（报告 §13.5）：
  1. 新增 `src/dynarec/arm64/arm64_set_flags.S`：`mov x26, x0; ret`
  2. `EmuRun` 的 `if ((skip = SigSetJmp(...)))` 分支内调用 `arm64_set_flags(emu->eflags.x64)`，
     并声明 clobber `"x26"`，避免编译器依赖旧值
  3. 先验证不回归（tf3_test/hwbp_test），再上 32 位壳看 `hwbp_stats`
- 风险：改主循环可能影响所有 64/32 位程序；现有 tests/ 仅 4 个探针，回归集不足。
- 收益未验证：x26 修好后 `hits` 是否回到 384 仍未知；即便通过，0.4.4 的 10–30% 提速也需实测。

### 35.3 当前环境
- 设备生产态：0.3.0 + fix110b + r109g + r112b + prod env + v2.20（可玩，72.1s / 44.3fps）。
- WSL 官方树 `/root/box64-0.4.4`：0 处补丁，产物 `a7360b1e`。
- 全部历史产物/脚本/日志按「只移不删」保留；每次编译自带源码备份 + 复原 + md5 校验。
"""
open(p, "w", encoding="utf-8", newline="").write(data + add)
print("appended §35")
