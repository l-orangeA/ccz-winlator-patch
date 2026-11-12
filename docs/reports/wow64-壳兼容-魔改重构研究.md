# Winlator 11.2 / Wine 10 new wow64：壳兼容「魔改重构」可行性研究

> 2026-11-09 17:1x｜**纯研究 + 源码取证，设备未改动**（本轮只读 WSL 源码与实机日志）。
> 承接：`README.md` §②、`2026-11-06_渲染与卡顿攻坚日志.md` §35.19、本日 v3–v8 实验。
> 本文不重复 `box64-0.4.4-重构路线可行性研究.md` 的 Dr/EFLAGS 部分 —— 那些是 0.4.4 升版路线，
> 本文针对的是**新 wow64 异常恢复路径**这个独立根因。

---

## 0. 结论先行

| 项 | 结论 |
|---|---|
| 一句话 | **别再在 Wine 异常分发里打补丁了**：v3–v8 六个变体全灭是因为「吞掉 int3 后要重建 32 位机器帧」，而那条恢复通道是给 syscall 设计的，不是给断点设计的 |
| ★ 主推 | **R-A：下沉到 box64 CPU 模拟层**（`EmuInt3` / `x86int3.c`），把 `0xCC` 在产生 Wine 异常**之前**变成「EIP+1 继续」，从根上绕开整套 wow64 异常/恢复链 |
| ★ 备选 | **R-B：借 Wine 原生 `RESET_STATE` + `iretq` 恢复路径**（`set_thread_wow64_context` 已实现），不手搓机器帧 |
| 工作量 | R-A 约 1 天（含 2–3 轮单变量）；R-B 半天（先做只读探针） |
| 止损 | R-A 出现「任何一次 `hits` 未上升 / 壳在 int3 之外的地址早死」即停；R-B 探针若证明 `RESET_STATE` 位在进入汇编前已被清，直接弃 |
| 与生产态关系 | 8.0 生产态（box64 0.3.0 + Wine 9.2）**完全不受影响**，改的是 11.2 独占的 0.4.4 / Wine 10.10 |

---

## 1. 问题本质（本日新增的字节级证据）

### 1.1 死亡链（实机日志 `unpack_tools/winlator11/wine_dbg_8765.log`）

```
init_peb starting L"C:\windows\wfm.exe" in experimental wow64 mode
seh:dispatch_exception code=80000003 (EXCEPTION_BREAKPOINT) addr=00587033
seh:call_seh_handlers handler at 00586F8C returned 0
seh:handle_syscall_fault code=c0000005 addr=0x3f0004c574
err:virtual:virtual_setup_exception nested exception on signal stack
```

壳在 `0x00587033` 执行 1 字节 `int3`（VProtect 反调试探针），SEH handler 拒收，
Wine 10 的 `NtRaiseException` 尾经 wow64 syscall trampoline 落到无效地址 → 宿主级访问违例 →
信号栈嵌套异常 → 进程死。**壳从未活到设 Dr 阶段**（无 `hwbp_stats.txt`）。

### 1.2 v3–v8 单变量矩阵（全部未通关）

| 版本 | 改动 | 实证结果 |
|---|---|---|
| v3 | `i386-windows/ntdll.dll` 前置拦截 + `ERR` | `SKIP-INT3` 从未出现 → **i386 PE ntdll 不在 32 位 SEH 路径上** |
| v4 | 改打 `wow64cpu.dll` + `x86_64-windows/ntdll.dll` | `Eip 587033→587034` 生效，但崩在 `call_seh_handlers` 内部，拦截点没返回 |
| v5 | 拦截提前到 SEH 分发之前 | **int3 被吞掉**，壳继续 → `unsupported syscall 586f8c` → `c0000005 @ C000001C` |
| v6 | 不做 64 位 fixup，保留 32 位上下文 | 直接 `nested exception`（NtContinue 到裸 32 位地址） |
| v7 | fixup 后跳 `syscall_32to64_return` 标签 | `nested exception`（`%r13` 上下文不可用） |
| v8 | 按汇编公式把 `Eip+1` 写入 32 位栈顶 | 栈顶修正生效，仍 `unsupported syscall` → `nested exception` |

原始日志：`wine_dbg_11197 / 11659 / 12151 / 12704 / 13383.log`。

### 1.3 全灭的公式级原因（实读 `wine1010/dlls/wow64cpu/cpu.c`）

```
syscall_32to64:                 ; 32 位代码执行 syscall 的入口
  xchgq %r14,%rsp
  movl (%r14),%edx
  movl %edx,0xb8(%r13)        ; context->Eip <- 32 位栈顶
  ...
  movq %rax,%rcx              ; syscall 号
  call Wow64SystemServiceEx
  movl %eax,0xb0(%r13)        ; context->Eax <- NTSTATUS
syscall_32to64_return:
  btrl $0,-4(%r13)            ; 测并清 cpu->Flags 的 RESET_STATE 位
  jc .Lsyscall_32to64_return  ; 置位 -> 用 iretq 从 wow_frame 恢复 32 位
  movl 0xb8(%r13),%edx
  ljmp *(%r14)                ; 否则用栈顶返回地址切回 32 位
```

`BTCpuResetToConsistentState`（`cpu.c:391`）在异常前把上下文**伪装成「刚进入 64 位 syscall」**：
`context->Rip = syscall_32to64`、`SegCs = cs64_sel`、`Rsp = R14`。
这套 fixup 是给**异常展开**用的。拿它直接 `NtContinue` 恢复断点，就会重进 syscall prolog，
`%eax`（`0x00586f8c`，恰好是壳 handler 地址）被当成 syscall 号 → 返回 `0xC000001C`
→ 栈顶/Eip 被污染 → 崩。

**结论**：此路要成，必须重建 32 位 machine frame（`%r13` 的 `I386_CONTEXT`、`%r14` 的 32 位栈、
`iretq` 五元组），属于改 CPU 模拟层，不是补异常分发。

---

## 2. 已排除 / 已证伪（别再重复）

- ❌ 改 `i386-windows/ntdll.dll`（v3 证伪，不在路径上）
- ❌ 改 `x86_64-windows/ntdll.dll` 的 `dispatch_exception` 尾部（v3/v4 证伪）
- ❌ 单纯 `Eip += 1` + `NtContinue`（v5–v8 全灭）
- ❌ 手改 `context->Rip` 指向 prolog / return 标签 / 裸 32 位地址（v5/v6/v7）
- ❌ 只修 32 位栈顶返回值（v8）
- ❌ 挂 `winedbg --auto`：日志实证 `[BOX64] winedbg detected, not launching it!`，box64 主动拦截
- ❌ `Wine 10.10` 三件套裸替换（`替代路线研究.md` §11，缺 libwine.so / wow64 PE 模块 / 前缀）
- ❌ box64 0.4.4 升版修 Dr（`重构路线可行性研究.md` §12，已终止）

---

## 3. ★ 源码取证：两条真正没走过的路

### 3.1 Wine 自带的 32 位恢复机制（R-B 的依据）

`wine1010/dlls/ntdll/unix/signal_x86_64.c:1236 set_thread_wow64_context()`：

```c
if (flags & CONTEXT_I386_CONTROL) {
    WOW64_CPURESERVED *cpu = NtCurrentTeb()->TlsSlots[WOW64_TLS_CPURESERVED];
    wow_frame->Esp = context->Esp;
    wow_frame->Ebp = context->Ebp;
    wow_frame->Eip = context->Eip;          // ← 我们要的 Eip+1
    wow_frame->EFlags = context->EFlags;
    wow_frame->SegCs = cs32_sel;             // ← 0x23
    cpu->Flags |= WOW64_CPURESERVED_FLAG_RESET_STATE;   // :1295
}
```

`cpu.c:210 .Lsyscall_32to64_return` 随后按 `iretq` 五元组
（Eip / SegCs / EFlags / Esp / SegSs）从 `wow_frame` 恢复 32 位执行。
**Wine 自己就有完整、正确的机器帧重建代码**，v3–v8 全部绕开它去手搓，是走错方向。

另两处置位：`unix/signal_x86_64.c:2302-2307`（`wait_suspend` 路径，调试器挂起恢复）、
`:1295`（上下文设置）。9.2 的 `wine0905/dlls/wow64cpu/cpu.c:202` 也有同样的 `btrl` ——
**机制不是 10.10 新增**，所以「9.2 能过、10.10 不能过」的差别不在这里，
更可能在于 9.2 的 32 位异常走 `signal_i386.c` 而 10.10 统一走 `signal_x86_64.c`（待验证，见 §10）。

### 3.2 box64 CPU 模拟层的 `0xCC`（R-A 的依据）

WSL `/root/box64-0.4.4`（源码树在盘，非压缩包）：

```c
// src/emu/x64run.c:1677
case 0xCC:                      /* INT 3 */
    R_RIP = addr;
    EmuInt3( emu, &addr );      // ← 异常在这里诞生，早于任何 Wine 代码
    if (emu->quit) goto fini;
    addr = R_RIP;
    break;
```

同文件 `:1670` 有 `is32bits = (R_CS==0x23)` —— **box64 直接识别 32 位 CS 模式**，
正是 new wow64 跑 32 位壳的方式。另有 `src/emu/x86int3.c`、`x86syscall_32.c`，
`CMakeLists.txt:47` 的 `BOX32` 开关（默认 OFF，标注 "experimental"）。

**含义**：`int3` 完全可以在 CPU 模拟层就地终结（RIP+1 继续），
Wine 侧连 `EXCEPTION_BREAKPOINT` 都收不到 → §1.3 的整条死亡链根本不会启动。
这是架构级绕开，不是补丁级修补。

---

## 4. 候选路线评级

| 路线 | 做法 | 收益 | 工作量 | 风险 | 可验证判据 |
|---|---|---|---|---|---|
| **R-A ★★★** | box64 层 `EmuInt3` 把 `0xCC` 变成「RIP+1 继续」 | 绕开整条异常/恢复链 | ~1 天 | 中：壳的反调试语义未知 | 壳日志不再出现 `80000003`；出现 `hwbp_stats.txt`；`hits≈384` |
| **R-B ★★** | `set_thread_wow64_context` 置 `RESET_STATE` + v5 fixup + `NtContinue` | 复用 Wine 原生 `iretq` 恢复 | 半天 | 中：`RESET_STATE` 位可能被 `btrl` 提前清掉 | `ERR` 探针确认走了 `.Lsyscall_32to64_return`；壳 EIP 落到 `00587034` 后继续 |
| R-C ★ | 内嵌调试器 / 改 `start_debugger` 命令别名绕开 box64 拦截 | 按 Windows 语义吞断点 | 半天 | 高：winedbg 无 stdin 会立即退出 | winedbg 进程存活且发 `cont` |
| R-D ★ | 11.2 里整套降级 Wine 9.2（保留 Java 壳） | 直接用已验证架构 | 2–3 天 | 高：启动器/winhandler 不兼容 | 容器能起 + 壳出标题 |
| R-E ★ | 自研 32 位 `iretq` 恢复 stub | 最彻底 | 3–5 天 | 高：需读 `BTCpuSimulate` 汇编 | 同 R-B，但不依赖 Wine 内部位 |
| R-F ✗ | patch 壳 / 双引擎 / 内存改 `0xCC→0x90` | — | — | 壳有完整性自校验（§35.18 实证） | 不推荐 |

---

## 5. 主推 R-A：详细设计

### 5.1 先做只读探针（**不下结论前必须做**）

在 `/root/box64-0.4.4/src/emu/x64run.c:1677` 与 `x86int3.c` 打 `printf/log`：
- 壳运行期 `0xCC` 命中次数、每次的 `R_RIP`、`R_CS`、是否 `is32bits`
- 命中前后 `emu->quit`、`EmuInt3` 返回值

目的：确认**只有 `0x00587033` 这一个探测点**，还是多阶段壳有多处 int3。
若多处，R-A 需要按地址白名单，而不是全局 NOP 化。

### 5.2 三种语义变体（单变量逐个验，别一次全上）

| 变体 | 行为 | 适用判断 |
|---|---|---|
| A1 | `0xCC` 直接 `RIP += 1` 继续（等价 NOP） | 壳只检查「有没有继续往下跑」 |
| A2 | 保留 `EXCEPTION_BREAKPOINT` 记录但自行恢复，不进 Wine 信号路径 | 壳检查异常记录 / 上次异常码 |
| A3 | 模拟调用一次壳的 SEH handler 后再继续 | 壳检查「handler 是否被调用」（反调试常见） |

**风险核心**：VProtect 这类壳用 int3 判「有无调试器」。A1 让 handler 完全不被调用，
可能被判为「被调试」而走自杀分支。所以 A1 失败不等于 R-A 失败，要接着试 A2/A3。

### 5.3 改造点

| 文件 | 位置 | 改动 |
|---|---|---|
| `/root/box64-0.4.4/src/emu/x64run.c` | `:1677 case 0xCC` | 在 `EmuInt3` 前按白名单分支跳过 |
| `/root/box64-0.4.4/src/emu/x86int3.c` | 全文件 | 若走 box32 路径，同样加分支 |
| 构建 | `CMakeLists.txt` | 沿用 11.2 作者版配置；**先复核 `BOX32` 实际开关**（`patch_box64_044_*.py` 有历史配置） |

⚠️ 历史教训（README §20）：自编译 box64 曾比作者版慢 44%（`ARM_DYNAREC` / linker 配置差异）。
R-A 的二进制必须**先与设备现用 box64 做同窗口 A/B 性能对照**，确认无回归再谈提速。

### 5.4 编译 / 部署 / 验收阶梯

```
构建：WSL /root/box64-0.4.4（沿用 patch_box64_044_*.py 的 cmake 参数）
部署：deploy_box64_044.py（先 push 到 /data/local/tmp，再进 rootfs，保留现用 .orig）
```

| 级 | 判据 | 不满足即停 |
|---|---|---|
| E24 | 容器正常起（wineserver + wfm 存活，无 `System.exit(0)`） | 回滚 box64 |
| E25 | 壳日志里 `80000003` 消失，且 `nested exception` 消失 | 回滚 |
| E26 | 出现 `hwbp_stats.txt`，`arms / hits` 上升（目标 `hits≈384`） | 检查 hwbp5 是否仍生效 |
| E27 | 出标题、读档、战斗正常 | — |
| E28 | 与生产态同窗口性能 A/B（出标题秒数、p90） | 慢 >5% 即弃 |

### 5.5 止损条件

- A1/A2/A3 任一变体出现「壳在 int3 之外的新地址早死」→ 停，回读日志再判，不连续换包
- 同窗口 A/B 性能下降 >5% → 停（本项目的初衷是提速/流畅）
- 出现确定性卡死簇（README §17.4）→ `adb reboot` 复位后再测，不在卡死态下结论

---

## 6. 备选 R-B：详细设计（半天可验）

**第一步必须是只读探针**，不直接改恢复逻辑：

1. 在 `cpu.c:201 btrl` 前后加 `ERR`，打印 `cpu->Flags` 与 `wow_frame->Eip`；
2. 在 `BTCpuResetToConsistentState` 的 `BTCpuSetContext` 之后打印 `cpu->Flags`；
3. 用 v5 的 `ntdll.dll`（`95add750…`）+ 当前 `wow64cpu`（`ce69f435…`）复现，
   看恢复瞬间 `RESET_STATE` 位**是否还置着**、汇编走了 `ljmp` 还是 `iretq`。

若位已被清 → R-B 直接弃。若位置着但仍走 `ljmp` → 说明 `btrl` 的 `-4(%r13)` 与实际
`WOW64_CPURESERVED` 地址不一致（布局错位），需核对 `cpu.c:288` 的
`movq 0x1488(%r12),%rcx` 与 TEB 偏移。

若探针显示位有效，实施：在 `dispatch_exception` 的 v5 拦截分支里，
`NtContinue` 之前调用 `RtlWow64SetThreadContext(GetCurrentThread(), &i386_ctx)`（内部会置位并填
`wow_frame`），再 `NtContinue`。改动 2 个文件、约 30 行，重编 2 个 DLL。

---

## 7. 其它路线速评

- **R-C**：`start_debugger` 命令串改别名（如 `winebg --auto`）可绕 `[BOX64] winedbg detected`，
  但 winedbg 需要 stdin 才能发 `cont`；Winlator 容器的 stdin 多半是 `/dev/null`，成功率低。
- **R-D**：`替代路线研究.md` §11 已证「三件套替换即退出」，整套替换 + wineboot 重建是 1–2 天工程，
  且 11.2 启动器与 9.2 不兼容（README 版本升级评估）。
- **R-E**：读 `cpu.c:272 BTCpuSimulate` 汇编自写 `iretq` stub，最彻底但最重，且 R-B 已覆盖其 90% 收益。
- **9.2 vs 10.10 差异 backport**：两树都在 WSL（`/root/wine0905`、`/root/wine1010`），
  可 diff `signal_i386.c` vs `signal_x86_64.c` 的未处理异常分支。**列为调研项，不是实施项**（见 §10）。

---

## 8. 资产与环境

| 类别 | 路径 |
|---|---|
| Wine 源码树 | WSL `Ubuntu2004x64`：`/root/wine1010`（10.10，含 v8 补丁）、`/root/wine0905`（9.2，fix110b 系） |
| box64 源码树 | WSL `/root/box64-0.4.4`、`/root/box64-0.4.4ntf`、`/root/box64-0.3.0src` |
| 本轮取证副本 | `unpack_tools/winlator11/{exception,pe_signal,signal_x86_64,wow64cpu_cpu}_wine*.c` |
| 实机日志 | `unpack_tools/winlator11/wine_dbg_{11197,11659,12151,12704,13383}.log` |
| 补丁产物 | `build/skipint3/*_v3..v8.dll`、`unpack_tools/patch_skipint3_v3..v8.py` |
| 设备 | `<TABLET_SERIAL>`（24018RPACC / 3048×2032），adb `D:\MuMuPlayer\nx_main\adb.exe` |
| 容器 | ccz = 健康 home 骨架 + 原配置；`wfm.exe = Ekd5.exe`；Container-2 干净可对照 |
| 归档 | 设备 `files/xuser1_bak_1619/`（旧骨架，只移未删）、各 `.orig` 备份 |

**回滚**：`deploy_*` 脚本均支持 `orig`；box64/ntdll/wow64cpu 三件套可逐个回退，
8.0 生产态（另一包名 `com.winlatcn`）完全独立。

---

## 9. 建议执行顺序

1. **R-B 只读探针**（半天，零风险，能立刻判定 Wine 原生恢复通道是否可用）
2. 若 R-B 通 → 直接按 §6 实施，成本最低
3. 若 R-B 不通 → **R-A 只读探针**（§5.1）→ A1 → A2 → A3，每级单变量
4. 全部失败 → 立项 R-E 或接受 8.0 生产态，不再在异常分发层试补丁

---

## 10. 明确待验证（**不要当结论用**）

- box64 0.4.4 在设备上的实际 `BOX32` 编译开关、以及 `0xCC` 走 `x64run.c` 还是 `x86int3.c`
  （需要实读设备 `libbox64.so` 的字符串 / 反汇编确认，不能只看源码树）
- 壳是否**必须**看到 SEH handler 被调用（决定 A1 还是 A3）
- `RESET_STATE` 位在异常展开期间是否已被 `btrl` 提前清掉
- 9.2 能过、10.10 不能过的确切代码差异（两树 diff 未做，本文只确认 RESET_STATE 机制两版都有）
- 壳是否只有 `0x00587033` 一处 int3 探测

> 以上五条任一未验证前，不得宣布「R-A / R-B 一定可行」。

---

## 11. A1 实施结果（2026-11-09 17:2x）：int3 消除成功，但**根因不在 int3**

### 11.1 零编译方案（比 §5 预估更省）

实读 box64 0.4.4 源码，发现**现成开关**，无需重编 box64：

| 依据 | 内容 |
|---|---|
| `src/include/env.h:97` | `BOOLEAN(BOX64_IGNOREINT3, ignoreint3, 0, 0, 1)` |
| `src/emu/x64int3.c:469` | 64 位路径：`if(!BOX64ENV(ignoreint3) && my_context->signals[X64_SIGTRAP]) EmitSignal(...)` |
| `src/emu/x86int3.c:496` | **32 位路径同样检查** `ignoreint3` |
| `src/dynarec/arm64/dynarec_arm64_00.c:3013` | **dynarec 编译期**也检查 `if(!BOX64ENV(ignoreint3))` |
| 设备二进制 | `device_box64_112.bin`（md5 `faf90e2d…`）含 `BOX64_IGNOREINT3` 字符串 2 处 → 开关存在 |

`BOX64_IGNOREINT3=1` 时，`0xCC` 直接 `RIP += 1` 继续，不投递 SIGTRAP →
Wine 侧连 `EXCEPTION_BREAKPOINT` 都收不到。**A1 = 只加一个环境变量，零编译、零部署。**

> 附带纠正 §10 的待验证项：设备 box64 与 WSL 构建产物同源不同配置
> （设备 30,725,672 B / `faf90e2d…`，WSL build 31,915,488 B / `a7360b1e…`，
> `ARM_DYNAREC=ON`、`BOX32=OFF`）；`EmuInt3` 走 `os_linux.c → x64Int3/x86Int3`，
> **不是** `emit_signal_wine.c`（该源文件未被 CMakeLists 引用，两版二进制均无 `RtlRaiseException`）。

### 11.2 单变量 A/B（严格「只差一个变量」）

- 基线：v8 实验（17:00），rootfs = hwbp5 unix + v3 i386 + v5 x64 ntdll + v8 wow64cpu，`wfm.exe = Ekd5.exe`
- A1（17:22）：**只**在 `.container` 的 envVars 末尾追加 ` BOX64_IGNOREINT3=1`，其余全部不动
- 壳日志 `wine_dbg_14655.log` 与 v8 的 `wine_dbg_13383.log` **前 153 行逐字相同**：
  ```
  154c154
  < ... SKIP-INT3 cpu v7: Eip 587033 -> 587034 ...
  > ... err:virtual:virtual_setup_exception nested exception on signal stack addr 0x3f0004b307
  ```
- A1 新日志中 `grep -l 80000003 wine_dbg_146*.log` **零命中**，`SKIP-INT3`（Wine 补丁）也未出现
  → **int3 异常确实被消除，且 Wine 补丁链没被触发**

### 11.3 ★ 结论：A 路线的目标达成，但问题不在这

| 判据 | 结果 |
|---|---|
| E25 `80000003` 消失 | ✅ |
| `nested exception` 消失 | ❌ 仍在 **`0x3f0004b307`**，与 v3–v8 所有崩溃同一地址 |
| E26 `hwbp_stats.txt` | ❌ 无 |
| 壳进程存活 / 出标题 | ❌ 点 ▶ 后 ~6 s `System.exit(0)` |

**`0x3f0004b307` 的归属已查实**：`box64-0.4.4/src/custommem.c:2929`
```c
startingpoint = (void*)(have48bits ? 0x7fff00000000LL : 0x3f00000000LL);
```
即 box64 在 39-bit VA 设备上的 **custommem / dynarec 映射区起点 + 0x4b307**，
不是 Wine 的地址。壳执行到 box64 的 JIT/trampoline 区时发生宿主级异常，
且落在 Wine signal stack 上 → 嵌套异常 → 进程死。

⇒ **int3 只是壳探测链的一环，不是根因**。§1.1 的「先死于 int3 分发」判断需要修正为：
壳在 `0x00587033` 附近有 syscall/trampoline 探测，无论 int3 是否抛异常，
最终都会进 box64 `0x3f0004xxxx` 区并崩。这与 §35.19「壳 hook/探测 syscall 层与
wine 10 syscall 隔离机制冲突」的旧判断一致，现在有了独立证据。

### 11.4 止损判定（按 §5.5 执行）

命中「壳在 int3 之外的新地址早死」→ **A2 / A3 不再做**（它们都只是改变 int3 的语义，
对 `0x3f0004b307` 无效）。R-A 作为「消除 int3」的手段已验证有效并归档，
但**不能单独解决壳兼容**。

设备已恢复基线：`.container` 写回 `ccz_container_1617_backup.json`，
`BOX64_IGNOREINT3` 已移除并回读校验。rootfs 补丁组合保持 v8 态，
便于下一层做同口径对照。

### 11.5 下一层方向（新的根因候选，未验证）

1. **box64 `custommem.c` / `signal32.c` / `defer_signal` 与 Wine 10 new wow64 的交互**：
   `README.md` §② 曾判死 0.4.4 信号基础设施，但当时是 **8.0 + Wine 9.2 + 单独替换 0.4.4**；
   现在是 **11.2 原生 0.4.4 + Wine 10**，属于未交叉验证过的新组合，值得重查。
2. **只读探针**：在 `handle_syscall_fault`（`wine1010/dlls/ntdll/unix/signal_x86_64.c:1955`）
   打印首次异常的 `code/addr/ip`，确认 `0x3f0004b307` 是 SIGSEGV 还是 SIGTRAP、
   以及 `PROT_DYNAREC` 页保护是否与该区间冲突。
3. **单变量开关复核**：`BOX64_DYNAREC_*`、`BOX64_SIGNAL_*`、`defer_signal` 相关编译开关，
   逐个 A/B（沿用 v3–v8 的单变量包节奏）。

> 以上三条均为**假设**，未实测前不得当结论。


---

## 12. §11.5 三条按顺序执行结果（2026-11-09 17:3x–17:5x）

### 12.1 ① 源码重查：box64 0.4.4 信号 / 页保护现状

| 项 | 实证 |
|---|---|
| `PROT_DYNAREC` 页保护 | 存在：`custommem.h:105`、`custommem.c:2316 protectDB` / `:2367 unprotectDB`、`dynarec_native.c:491/614` |
| `defer_signal` | 仅 `signals.h:76` 声明，**env.h 无对应开关**；本版没有运行时开关可切 |
| 可用开关 | `BOX64_DYNAREC_INTERP_SIGNAL`(`env.h:82`)、`SAFEFLAGS/BIGBLOCK/FORWARD`(:52/63/71)、`NOHOTPAGE`(:56)、`BOX64_DYNAREC`(:169 默认 1) |
| `0x3f00000000` 修正 | `custommem.c:2929` 属 **`find47bitBlockElf`**（ELF 47-bit 加载区 hint），**不是 JIT 区**；§11.3 的「JIT/trampoline 映射区」表述修正为「box64 ELF 加载区附近」 |

### 12.2 ② 只读探针（产物 `build/ntdll_wine10_hwbp5_probe.so`，md5 `dfa55204…`）

在 `unix/signal_x86_64.c:1457 setup_raise_exception` 与 `unix/virtual.c:5087 virtual_setup_exception`
的 nested 分支各插一行 `ERR`（`unpack_tools/probe_first_exception.py`），**不改任何行为**；
产物同时含 `WINE_HWBP_SOFT`（hwbp5 未丢）、`PROBE-FIRST`、`PROBE-NESTED`。

实机日志 `wine_dbg_15405.log`：

```
PROBE-FIRST code=80000003 addr=0x587033 rip=587034 trap=3 rsp=0x31fd28   <- SIGTRAP / int3
  (SKIP-INT3 v5 拦截 -> Wow64SystemServiceEx unsupported syscall 586f8c)
PROBE-FIRST code=c0000005 addr=0x587057 info0=0x1 info1=0x79f54a trap=14 <- SIGSEGV，写访问
PROBE-FIRST code=c0000005 addr=0x3f0004b31b info0=0x1 info1=0x79f330 trap=14
PROBE-NESTED code=c0000005 addr=0x3f0004b31b                              <- 信号栈上二次故障
```

**结论**：致命异常是 `SIGSEGV`（`trap=14`），不是 `SIGTRAP`；`0x587057` 是**写访问违例**
（`info0=1`）。`0x3f0004b3xx` 是异常派发过程中在 signal stack 上的二次故障地址，
不是 Wine 的固定 trampoline 常量（全树 grep 无 `0x3f0004` 常量）。

### 12.3 ③ 逐个 A/B（每次只改一个 env，之间恢复基线）

| 变体 | 改动 | PROBE 异常序列 | 判定 |
|---|---|---|---|
| ③-1 | `WINE_HWBP_SOFT=1 -> 0` | 与基线逐字相同 | 排除 hwbp 页保护 |
| ③-2 | `+ BOX64_DYNAREC_INTERP_SIGNAL=1` | 与基线逐字相同 | 排除信号解释器路径 |
| ③-3 | `+ BOX64_DYNAREC=0`（**关闭整个 dynarec**） | 与基线逐字相同 | 排除 dynarec 及其全部子开关 |

**上界推理**（不是猜测）：`DYNAREC=0` 是 dynarec 类开关的**全集关闭**，
在此状态下异常序列仍逐字不变 => `SAFEFLAGS / BIGBLOCK / FORWARD / NOHOTPAGE` 等任何
dynarec 子开关都不可能改变结果，**无需再逐个跑**。按 §5.5 止损停止 ③。

### 12.4 综合结论（三条走完）

- ①②③ 合起来把「box64 侧可配置项」**整类判死**：
  int3 可关（A1）、dynarec 可关、hwbp 可关、信号路径可换解释器 ——
  `0x587057` 写违例 -> `0x3f0004b3xx` nested 这条链**一个字节都没变**。
- 与 §1.3 / §3.1 相互印证：根因在 **Wine 10 new wow64 的 32 位异常恢复 / 机器帧重建**，
  box64 层无解，env 层更无解。
- 剩余可走：R-B（`RESET_STATE` + iretq 只读探针）、R-E（自研 32 位恢复 stub）、
  R-D（整套降级 Wine 9.2）；或按 README 结论接受 8.0 生产态。

### 12.5 设备已恢复基线（逐项 md5 校验）

| 项 | 值 |
|---|---|
| `.container` | 写回 `ccz_container_1617_backup.json`，仅 `WINE_HWBP_SOFT=1` 等原值；`BOX64_IGNOREINT3` / `INTERP_SIGNAL` / `DYNAREC=0` 全部移除 |
| `x86_64-unix/ntdll.so` | `fcd4c7e3…`（hwbp5，从 `.hwbp5bak` 回滚） |
| `i386-windows/ntdll.dll` | `720bf239…`（v3） |
| `x86_64-windows/ntdll.dll` | `95add750…`（v5） |
| `x86_64-windows/wow64cpu.dll` | `ce69f435…`（v8） |

新增资产：`unpack_tools/probe_first_exception.py`、
`build/ntdll_wine10_hwbp5_probe.so`、`unpack_tools/winlator11/ccz_{ignoreint3,hwbp_off,v32,v33}.json`、
`wine_dbg_14655/15405/15802/16211/16554.log`、`k70_17*.png/.log`。
日志与记忆文件仍未改动。


---

## 13. R-B / R-E 最小版结果（2026-11-09 17:5x–18:0x）：轻量方案全部穷尽

### 13.1 R-B：`RESET_STATE` 位本来就置着

产物 `build/skipint3/wow64cpu_rb.dll`（md5 `3d2dadf7…`），在 v8 基础上**只加**
`cpu->Flags |= WOW64_CPURESERVED_FLAG_RESET_STATE` 与两行只读 `ERR`。
常量依据：`winternl.h:1264 WOW64_TLS_CPURESERVED=1`、`:4241 RESET_STATE=1`，
汇编 `btrl $0,-4(%r13)` 测的正是 `cpu->Flags`。

实机 `wine_dbg_17281.log`：

```
PROBE-RB flags_before=1 eip=587034
PROBE-RB flags_after=1
Wow64SystemServiceEx unsupported syscall 586f8c
nested exception on signal stack addr 0x3f0004b307
```

**`flags_before=1` ⇒ Wine 的 `BTCpuSetContext` 早已置位，补置是 no-op。**
§6 的前提（「位被提前清掉」）被证伪 → **R-B 判死**。

### 13.2 R-E 最小版：`R13` 本来就正确

源码依据 `wine1010/dlls/ntdll/tests/wow64.c:3113`：
`context == teb64->TlsSlots[WOW64_TLS_CPURESERVED] + 4`；
`wow64cpu/cpu.c` 的 `syscall_32to64_return` 靠 `%r13` 指该 I386_CONTEXT。
日志里 `r13=0x10020fd24` 看似栈地址，怀疑恢复时 R13 指错。

产物 `build/skipint3/ntdll_x64_windows_re.dll`（md5 `93cb2ea6…`），
在 v5 拦截块的 `NtContinue` 前只做 `context->R13 = cpu_area + 4` + 只读 `ERR`；
wow64cpu 同时回滚到 v8（单变量）。

实机 `wine_dbg_17677.log`：

```
PROBE-RE cpu_area=000000010020FD20 old_r13=20fd24 new_r13=20fd24 r14=20f4b0
Wow64SystemServiceEx unsupported syscall 586f8c
nested exception on signal stack addr 0x3f0004b307
```

`cpu_area=0x10020FD20`，`cpu_area+4=0x10020FD24`；`old_r13` 打印的低 32 位
`0x20fd24` 与之吻合 ⇒ **R13 本来就等于 `cpu_area+4`，修正是 no-op** → **R-E 最小版判死**。

### 13.3 全局结论（本轮全部路线）

| 层 | 试过 | 结论 |
|---|---|---|
| Wine 异常分发补丁 | v3–v8（i386/x64 ntdll、wow64cpu 共 6 变体） | 全灭 |
| box64 CPU 层 | A1 `BOX64_IGNOREINT3=1` | int3 消失，仍崩 `0x3f0004b3xx` |
| box64 开关 | `WINE_HWBP_SOFT=0`、`INTERP_SIGNAL=1`、`DYNAREC=0`（含上界推理） | 整类零影响 |
| Wine 原生恢复位 | R-B `RESET_STATE` | 位本来就是 1 |
| 恢复上下文指针 | R-E 最小版 `R13` | 本来就是 `cpu_area+4` |

两个只读探针共同说明：**恢复所需的上下文数据（R13、RESET_STATE、Eip+1、32 位栈顶）
本来就是对的**，崩溃发生在更深处（`syscall_32to64` prolog 执行 /
`Wow64SystemServiceEx` unsupported syscall 路径对 wow_frame 的污染），
已不属于「改几行 + 重编一个 DLL」能覆盖的范围。

**剩余路线（均为天级工程）**：
1. R-D：11.2 里整套降级 Wine 9.2 + wineboot 重建（2–3 天，启动器兼容性未知）。
2. R-E 完整版：读 `BTCpuSimulate` 汇编，自研 32 位 `iretq` 恢复 stub（3–5 天）。
3. 按 README 结论**接受 8.0 生产态**（0 成本，已验证可玩）。

建议：若无明确性能/新架构收益诉求，停在第 3 项；若要立项，优先 R-D（改动面更可控）。

### 13.4 设备基线（已逐项 md5 校验）

| 文件 | md5 |
|---|---|
| `x86_64-unix/ntdll.so` | `fcd4c7e3…`（hwbp5） |
| `i386-windows/ntdll.dll` | `720bf239…`（v3） |
| `x86_64-windows/ntdll.dll` | `95add750…`（v5，从本地产物恢复） |
| `x86_64-windows/wow64cpu.dll` | `ce69f435…`（v8，从 `.v8bak` 恢复） |

`.container` 保持 `ccz_container_1617_backup.json`（`WINEDEBUG=+loaddll,+seh`、
`WINE_HWBP_SOFT=1`，无 IGNOREINT3 / INTERP_SIGNAL / DYNAREC=0）。

新增资产：`unpack_tools/patch_rb_resetstate.py`、`unpack_tools/patch_re_r13.py`、
`build/skipint3/wow64cpu_rb.dll`、`build/skipint3/ntdll_x64_windows_re.dll`、
`wine_dbg_17281.log`、`wine_dbg_17677.log`、`k70_1755_RB / k70_1800_RE.{png,log}`。
日志与记忆文件仍未改动。
