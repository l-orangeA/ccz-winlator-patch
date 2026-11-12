# Winlator / Wine 底层修复：只读栈上的异常投递

日期：2026-11-01
目标：让 VProtect 加壳的 `Ekd5.exe` 在 Winlator（Wine 9.2 WOW64 + box64）下通过反调试，进而启动引擎。

---

## 一、根因（实机 trace 证据）

用 `BOX64_TRACE=1` + `BOX64_TRACE_FILE` 抓到 Wine 自己的 stderr 后，壳的启动序列是：

1. `0x587033`：`mov fs:[0],esp` 后执行 **`int3`**（反调试第 1 步）。
   壳自己的 SEH 处理器 `0x586F8C` 接住并返回（日志：`call_stack_handlers handler at 00586F8C returned 0`）。
2. `0x587057`：执行 **`push di`**，此时 32 位栈页（`0x8a000`）的保护是 **R-X（prot=5，不可写）**。
   这是壳的反调试第 2 步：把栈页改成只读，再压栈，期待 #PF 被自己的 SEH 接住。
3. Wine 的 wow64 异常投递路径把异常帧写到 `context->Sp` 指向的 32 位栈上：

```c
/* dlls/ntdll/unix/signal_arm64.c */
stack = (struct exc_stack_layout *)(context->Sp & ~15) - 1;
memmove( &stack->context, context, sizeof(*context) );
memmove( &stack->rec, rec, sizeof(*rec) );
```

   这一步再次访问只读栈页（`0x8afbb`），在信号栈上触发嵌套异常：

```
err:virtual:virtual_setup_exception nested exception on signal stack addr 0x3f0007adc4 stack 0x7ffcf170
```

4. `virtual_setup_exception` 对“信号栈上的嵌套异常”直接 `abort_thread(1)` → 进程 `_exit(1)`。
   控制台批处理看到的就是 **EXIT 0**（Windows 侧退出码）。

**结论**：Windows 内核在投递异常时会绕过用户栈页保护；Wine 是纯用户态分发，绕不过去。
上游 Wine master 仍是同样逻辑（`nested exception on signal stack → abort_thread`），换新 Wine 不会修。

---

## 二、补丁（已生成）

文件：`wine-9.2-custom-readonly-stack.patch`
基于：`https://github.com/brunodev85/wine-9.2-custom`（Winlator 8.0 使用的 Wine 9.2 源码，`VERSION = Wine version 9.2`）

```diff
--- a/dlls/ntdll/unix/signal_arm64.c
+++ b/dlls/ntdll/unix/signal_arm64.c
@@ -33,6 +33,7 @@
+#include <sys/mman.h>
@@ -787,6 +788,16 @@
     if (status) return status;
     stack = (struct exc_stack_layout *)(context->Sp & ~15) - 1;
+    /* Windows delivers the exception frame even when the application made its
+     * stack read-only (a classic anti-debug technique).  The guest vprot view is
+     * left untouched; only the host mapping is made writable so that the frame
+     * and the handler's own pushes don't fault in the middle of the dispatch. */
+    {
+        ULONG_PTR page_size = getpagesize();
+        ULONG_PTR page = (ULONG_PTR)stack & ~(page_size - 1);
+        ULONG_PTR end  = ((ULONG_PTR)stack + sizeof(*stack) + page_size - 1) & ~(page_size - 1);
+        mprotect( (void *)page, end - page, PROT_READ | PROT_WRITE );
+    }
     memmove( &stack->context, context, sizeof(*context) );
```

要点：
- 只改 **host 侧映射**（`mprotect`），**不改** Wine 的 vprot 记录 → 壳用 `VirtualQuery` 看到的仍是 R-X，符合它的预期。
- 只覆盖异常帧所在的页（`stack` 到 `stack + sizeof(exc_stack_layout)`）。
- 壳的处理器随后会自己 `VirtualProtect` 恢复保护，最终状态与 Windows 一致。

---

## 三、构建环境要求（关键约束）

镜像里的 glibc 是 **Ubuntu GLIBC 2.31-0ubuntu9**（Ubuntu 20.04 基线）：

```
GNU C Library (Ubuntu GLIBC 2.31-0ubuntu9) stable release version 2.31
```

因此不能随便用新发行版构建（会引入 GLIBC_2.32+ 符号）。推荐：

| 方案 | 说明 |
|---|---|
| **A. 平板原生**（首选） | 平板上装 Termux → `proot-distro` 装 **Ubuntu 20.04 arm64** → 原生编译（8 核，速度最快，glibc 完全匹配）。 |
| B. arm64 Ubuntu 20.04 真机/VM | 有 arm64 Linux 机器时同样可行。 |
| C. x86_64 交叉编译 | 需要 aarch64 交叉工具链 + Ubuntu 20.04 arm64 sysroot（dev 头文件），配置复杂，不推荐。 |

依赖（Ubuntu 20.04 arm64）：

```sh
sudo apt update
sudo apt install -y build-essential gcc make flex bison pkg-config \
  libc6-dev libx11-dev libfreetype6-dev libfontconfig1-dev \
  libgnutls28-dev libpulse-dev libasound2-dev libdbus-1-dev libunwind-dev
```

---

## 四、构建步骤

```sh
# 1. 取源码（48MB）
git clone --depth 1 https://github.com/brunodev85/wine-9.2-custom.git
cd wine-9.2-custom
git apply /path/to/wine-9.2-custom-readonly-stack.patch

# 2. 配置（wow64：同时生成 i386 / x86_64 的 PE 侧 + aarch64 unix 侧）
./configure --enable-archs=i386,x86_64

# 3. 只编 ntdll（我们只替换 unix 侧的 ntdll.so；先试最小目标）
make -j8 dlls/ntdll/ntdll.so
# 若目标名不同：make -j8 dlls/ntdll/all

# 4. 产物
find . -name 'ntdll.so' -path '*unix*' -o -name 'ntdll.so'
```

把生成的 `ntdll.so` 替换到平板上（先备份）：

```sh
# 备份
adb -s <TABLET_SERIAL> exec-out run-as com.winlatcn cat \
  files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so > ntdll.so.orig

# 替换（走 stdin，避免 /sdcard 权限问题）
adb -s <TABLET_SERIAL> shell "run-as com.winlatcn sh -c 'cat > files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so'" < ntdll.so.new

# 回滚
adb -s <TABLET_SERIAL> shell "run-as com.winlatcn sh -c 'cat > files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so'" < ntdll.so.orig
```

---

## 五、验收判据

1. 启动容器（把 `wfm.exe` 换成 `cmd.exe` 以便有控制台），运行 `D:\Download\ccz22\Ekd5.exe`。
2. 开启 `BOX64_TRACE=1 BOX64_TRACE_FILE=/home/xuser/trace.txt`，跑完后检查：
   - **不应再出现** `err:virtual:virtual_setup_exception nested exception on signal stack`；
   - 应能看到壳的 SEH 处理器对 #PF 的 `call_stack_handlers ... returned`，且进程继续；
   - 下一步预期：壳解包完成、跳到 OEP `0x817E0`，引擎开始初始化。
3. 若引擎随后在 `0x46C0C9` 崩（对话框过程空 `this`），那是下一层问题（引擎的对话框初始化），与本次补丁无关，再用同样的 trace 方法定位。

---

## 六、当前阻塞

- 本机 **无管理员权限**（`IsInRole(Administrator)=False`），WSL 发行版未安装；
- GitHub 在本地网络下出现 `Connection reset` / 超时，Termux APK（35MB）、Ubuntu rootfs、源码克隆暂时无法下载；
- 网络恢复后按上面步骤继续即可；补丁和判据已经就绪。

---

## 七、文件

| 文件 | 说明 |
|---|---|
| `wine-9.2-custom-readonly-stack.patch` | 可直接 `git apply` 的补丁 |
| `signal_arm64.c.new` | 打好补丁的完整源文件（对照用） |
| `wc_signal_arm64.c` | 从 `brunodev85/wine-9.2-custom` 拉取的原始文件（Wine 9.2） |
| `ntdll.so` | 平板镜像里当前的 aarch64 `ntdll.so`（654,832 字节，备份/分析用） |


---

# 第二轮：真正改 Wine 底层（2026-11-01 下午）

## 1. 关键事实：镜像里的 Wine Unix 侧是 x86_64，不是 aarch64

`files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so` 的 ELF `e_machine = 62 (EM_X86_64)`，
`/opt/wine/bin/wine`、`winex11.so` 同样是 x86_64。也就是说 Winlator 的 Wine 是
**x86_64 Linux 构建，由 box64 模拟执行**；Android 侧的 aarch64 glibc 只是用来跑 box64/proot。
⇒ 补丁必须改 `dlls/ntdll/unix/signal_x86_64.c`，并且按 **x86_64** 构建。

## 2. 构建环境（已打通，无需管理员）

- 本机用 `wsl --import` 导入了一个 **Ubuntu 20.04 x86_64** 根文件系统（glibc 2.31，与镜像一致），
  完全绕开管理员/WSL 发行版安装。
- 源码必须用 **2024-09-05 的提交 `a48ee6df4a77`**（Winlator 8.0 发布前最后一个提交）。
  用当前 main 编出的 ntdll.so 虽然导出符号一致（277/277），但启动容器会立刻退出（内部 ABI 不一致）。
- 构建命令：`./configure --enable-archs=i386,x86_64 --without-x --without-freetype ...`，
  `make -j8 dlls/ntdll/ntdll.so`。
- 这套构建的 ntdll.so 已实测能让容器正常启动并运行游戏。

## 3. 真正的出错路径（实机 trace）

从 **wfm→cmd 控制台**里启动 `Ekd5.exe`（文件管理器 BTRun 方式不会复现反调试路径）：

1. 壳执行 `int3`（0x587033），SEH 处理器 0x586F8C 接住（4 次 EXCEPTION_BREAKPOINT）。
2. 壳在 `0x587057` 执行 `push`，写入 32 位栈 `0x8afdbe`，该页保护 R-X。
3. Wine 的 `setup_raise_exception()`（signal_x86_64.c）在 `virtual_setup_exception()` 返回后
   把异常帧写到 `0x8af...` → 再次 SIGSEGV → `virtual_setup_exception` 判定
   “nested exception on signal stack” → `_exit(1)`。

## 4. 补丁位置与实测结果

正确的落点是 **`setup_raise_exception()` 里 `virtual_setup_exception()` 之后、写 `stack->rec` 之前**，
不是 `call_user_exception_dispatcher()`（那里根本不会被 wow64 路径调用）。

实测加 `mprotect(page, size, RW)` 后：

```
MPROT rc=-1 errno=13 page=8af000 size=1000
```

`errno=13 (EACCES)`：`0x8af000` 是 **只读共享映射**，加写权限被 Linux 拒绝。
这就是补丁失败的真正原因，不是补丁没执行。

## 5. 下一步（最接近成功的两条）

1. **把 32 位栈改成匿名可写映射**：用 `mmap(MAP_FIXED|MAP_ANONYMOUS|MAP_PRIVATE|RW)` 替换该页并回拷内容。
   已试过“无条件兜底”，但启动期的其他异常也会走这条路径，导致容器起不来 ⇒
   必须**只对 guest 32 位栈页**做替换（用 `/proc/self/maps` 或 Wine 的 vprot 判断该页属性），
   或直接在 Wine 分配 wow64 栈的地方就改成匿名 RW（更干净的根修）。
2. **直接修 wow64 栈的分配方式**：找到 Wine/box64 创建 0x8af000 这段 32 位栈的代码，
   把只读共享映射改成 MAP_ANONYMOUS|MAP_PRIVATE。这样壳的 VirtualProtect(R-X) 仍然生效，
   但 Wine 自己投递异常时可以 mprotect 回 RW（不再 EACCES）。

## 6. 产物

| 文件 | 说明 |
|---|---|
| `ntdll_0905.so` | 2024-09-05 源码原版构建（能启动，基线） |
| `ntdll_fix4.so` | 在 setup_raise_exception 加 mprotect + mmap 兜底的实验版（兜底范围过宽） |
| `wsl_build0905.sh` / `wsl_fix*.py` | WSL 构建与补丁脚本 |
| `signal_x86_64.c.new` | main 分支的补丁参考（实际请用 0905 源码 + wsl_fix*.py） |

## 7. 当前设备状态（已恢复）

- `ntdll.so` 已还原为原版（md5 `dbd94d1f0643cdaa556b22631b36bf0b`，654,832 字节）
- `wfm.exe` 已还原（259,072 字节），容器 envVars 已恢复原值（BOX64_LOG=1、无 TRACE）
- trace/测试文件已清理；WSL Ubuntu2004x64 保留（后续构建用）


---

# 第三轮：fix4 实测（定向兜底）

补丁：`setup_raise_exception()` 里先 `mprotect(page, RW)`；失败则
`mmap(MAP_FIXED|ANON|PRIVATE|RW)` 替换该页并回拷内容。

实测（控制台启动，BOX64_LOG=2）：
- `MPROT_FALLBACK` ×2（兜底确实执行）
- `nested exception` ×1（仍然存在）
- `trace:seh:dispatch_exception code=c00000fd` ×5 → **STATUS_STACK_OVERFLOW**
- 第二次 SIGSEGV 时 `rsp=0xd1cf02ea`（野值）、访问 `0xd1cf02e2`（prot=0 未映射），
  兜底的 `memcpy(0xD1CEF000, ...)` 在 libc 里再次 SIGSEGV → 嵌套 → `_exit(1)`

## 结论：问题已从“只读页”上升到“wow64 32 位栈的登记/增长语义”

Wine 的 `virtual_setup_exception()` 认为故障 RSP（0x8afdc0）在 thread stack 内，
并把这次写只读页当成 **guard page / 栈增长** 处理；grow 失败后把异常码改成
`STATUS_STACK_OVERFLOW (0xC00000FD)` 投递给壳。壳按自己的栈溢出逻辑处理后，
32 位 SP 变成 `0xd1cf02ea`，下一次异常落在未映射区，无法恢复。

## 下一步（明确的两个改法）

1. **让 Wine 别把这次故障当栈溢出**：在 `virtual_setup_exception()` 的 guard 分支里，
   若目标页的 vprot 不含 `VPROT_WRITE`（即应用主动改成只读），应继续投递
   `EXCEPTION_ACCESS_VIOLATION` 而不是 `STATUS_STACK_OVERFLOW`（或直接跳过 grow）。
   这样壳的 #PF 处理器能按原意接住这次 AV。
2. **补 wow64 32 位栈的登记**：让异常路径使用 32 位 TEB 的 StackLimit/StackBase
   （而不是 64 位 TEB），使 0x8af000 段被当作普通栈处理（guard/grow 逻辑正确）。
   这需要看清 wine 在 wow64 下如何维护 32 位 TEB（WOW64_CPURESERVED/TlsSlots）。

设备已还原（原版 ntdll `dbd94d1f…`、原版 wfm、env 恢复），构建产物与脚本保留在 D:\ccz_winlator_patch\unpack_tools。


---

# 第四轮：guard 分支与“guaranteed space”真相

`virtual_setup_exception()` 的实际逻辑（0905 源码）：

```c
if ((get_page_vprot(stack) & VPROT_GUARD) && grow_thread_stack(page, &stack_info))
{
    rec->ExceptionCode = STATUS_STACK_OVERFLOW;   // 0xC00000FD
}
```

`grow_thread_stack()` 在 `page < start + page_size + guaranteed` 时返回
`STATUS_STACK_OVERFLOW`（“inside guaranteed space -> overflow exception”）。

也就是说：壳的 `ESP=0x8afdc0` 落进了该 wow64 线程栈的 **guaranteed 区**，
Wine 按栈溢出处理；这不是简单的“只读页”问题，而是 **32 位栈的边界/guaranteed 大小
与 Windows 不一致**（壳的反调试压栈在 Windows 上不会越界，在 Wine 的 wow64 栈布局下越界了）。

## 由此得到的修法（比前两条更接近根因）

- **调整 wow64 32 位 TEB 的栈字段**：`StackBase / StackLimit / DeallocationStack`
  以及 `grow_thread_stack()` 里 `is_wow` 分支更新的 `WOW_TEB`，让 guaranteed 区不要覆盖
  壳压栈的那一页（或让该页成为可 grow 的 guard 页）。这样 `grow_thread_stack()` 返回 0，
  走正常的栈增长而不是 STATUS_STACK_OVERFLOW。
- 需要先在 Wine 启动 wow64 线程处打印/对比 `WOW_TEB` 的 StackLimit/Base/Guaranteed，
  再决定把哪个字段调成 Windows 语义。

## 结论

- 小补丁（只读页/异常码）不足以让壳通过：它最终触发的是 **栈溢出语义**，
  会在壳的栈溢出处理器里留下野 SP（`0xd1cf02ea`），随后访问未映射页。
- 真正的修点在 wow64 栈的 TEB 边界/guaranteed 大小，而不是异常投递本身。
- 设备保持原版状态；构建产物与脚本在 D:\ccz_winlator_patch\unpack_tools。

---

# 第五轮（2026-11-01 13:30-14:30）：#SS 误报定案 + 真机对照表

## 5.1 推翻第四轮的“guaranteed 区”结论

在 0905 源码的两条 guard 路径都插了探针（`virtual_handle_fault` 的
`HF_GUARD`、`virtual_setup_exception` 的 `SE_GUARD`），实跑结果是
**两条都没有触发**，但异常码已经是 `c00000fd`。

改成在 `segv_handler` 的 `TRAP_x86_PAGEFLT` 分支打印 `trapno` 后拿到真相：

```
SEGV_ENTRY rip=587057 addr=8afdbe err=2 rsp=8afdc0 trap=c
```

`trap=c`（12 = #SS 栈段错误）。box64 的 32 位模拟把“访问 32 位栈所在的
只读页”报告成 #SS，而 Wine 的 `segv_handler()` 对 `TRAP_x86_STKFLT` 的
处理是**直接** `rec->ExceptionCode = EXCEPTION_STACK_OVERFLOW`，根本
不进 `virtual_handle_fault()`，所以之前所有针对 guard 分支的补丁都不会命中。

## 5.2 已落地的修复（fix11）

`signal_x86_64.c` 的 `TRAP_x86_STKFLT` 分支改为和 #PF 同一条路：

```c
case TRAP_x86_STKFLT:  /* Stack fault */
    rec.NumberParameters = 2;
    rec.ExceptionInformation[0] = (ERROR_sig(ucontext) >> 1) & 0x09;
    rec.ExceptionInformation[1] = (ULONG_PTR)siginfo->si_addr;
    rec.ExceptionCode = virtual_handle_fault( siginfo->si_addr,
                          rec.ExceptionInformation[0], (void *)RSP_sig(ucontext) );
    if (!rec.ExceptionCode) { leave_handler( ucontext ); return; }
    break;
```

效果（fix11 实跑）：

```
SEGV_ENTRY rip=587057 addr=8afdbe err=2 trap=c
VHF_EXIT   addr=8afdbe ret=ffffffffc0000005      <- 只读页不再被当栈溢出
STKFLT_FAULT addr=8afdbe ret=c0000005
SRE_ENTRY code=c0000005 addr=587057              <- 壳按 AV 收到（Windows 语义）
handler at 00586F8C returned 0
```

之后壳自己的处理让执行继续到 `587059`（pop 序列），但下一步就撞墙。

## 5.3 新的卡点：壳把 ESP 转到 0xd1cf02ea（未映射）

继续执行到 `0x587072 xchg esp,eax / 0x587073 pushal`，此时

```
PF_ENTRY rip=587073 addr=d1cf02e2 err=4 rsp=d1cf02ea trap=e
SRE_AFTER code=c0000005 stack=d1cefbc0 stackptr=d1cf02e0
err:virtual:virtual_setup_exception nested exception on signal stack addr 0x50173 stack 0x7ffcf000
```

`0xd1cf02ea` 是壳从“表页”里 pop 出来的值：它等于页内 `0x8afde2` 处的
**非对齐 dword**（`EA 02 CF D1`）。

## 5.4 真机（Windows 10 x64）对照 —— 关键发现

在 PC 上直接跑 `Ekd5_orig_backup.exe`（游戏窗口正常出现），以**只读
ReadProcessMemory 外部扫描**（不附加调试器，不扰动反调试）抓到了同一张表：

- 进程里存在**两份**同构表（同一 allocation `0x02F90000` 内）：
  - `0x02FA4400`：**规整版**（`02D20542 02D30542 ...`），`+0xde2` 处读出来是
    `0x054202EA` —— 落在 `0x0542xxxx`（游戏已提交的可写区）→ **合法栈指针**
  - `0x03004000`：**不规则版**（`... 0202C922 02DF0542 0202C91A ...`），
    `+0xde2` 处 = `0xD1CF02EA` —— 与 Wine 里完全一致，且 >2GB 不可映射
- 游戏 exe 不是 LARGE_ADDRESS_AWARE（Characteristics=0x030f），
  所以在 Windows 上 `0xd1cf0000` 也**不可能**是合法地址
- Wine 里我们 dump 到的表页（`0x8af000`）内容 = **不规则版**

### 结论（本轮）

**不是异常投递的问题，而是“壳在 Wine 下选了错的表 / 表内容与 Windows 应选的那份不一致”。**
Windows 上壳用的是规整版（pivot 到合法地址 `0x054202EA`）；
Wine 下同样的读取落到了不规则版（pivot 到不可映射的 `0xD1CF02EA`），
随后 pushal 的异常在野 SP 上无法投递 → `nested exception` → 线程被杀。

下一步用 **PC 原生 Wine（同 wow64 设计、无 box64）** 做对照：
- 若原生 Wine 也走错表 → Wine 自身 wow64 语义问题，可在本地 gdb 里精确定位；
- 若原生 Wine 正常 → 是 box64 的 32 位模拟（#SS/内存布局）差异，用原生行为做基准逐项对齐。

---

# 第六轮（2026-11-01 14:20-15:40）：PC 原生 Wine 对照 + TF 单步线索

## 6.1 在 WSL 里把同一份 Wine 9.2（wow64、无 box64）跑起来

- 完整构建成功（i386+x86_64 PE + x86_64 unix，`Wine build complete.`）
- 环境踩坑（与游戏无关，仅本机 WSL）：
  - `get_host_addr_space_limit()` 在本机只探到 ~0x7FFFFF0000，而 mmap 会落到
    `0x7FFFFE000000`，触发 `alloc_pages_vprot` 断言 → 强制
    `host_addr_space_limit = 0x7ffffffff000` 后正常
  - `pages_vprot_size` 随之为 0x8000；`wine cmd /c echo` 正常输出
- 复现命令：
  ```
  WINEPREFIX=/root/cczprefix WINEDEBUG=-all \
    /root/wine0905/wine Ekd5.exe   # cwd=/root/cczgame
  ```

## 6.2 原生 Wine 的执行轨迹 vs 平板（box64）

原生 Wine（无 box64）跑加壳 Ekd5：

```
SRE_ENTRY code=80000003 addr=587033 rsp=22fd28     <- 同一个 int3
SRE_AFTER   code=80000003 ...
SRE_ENTRY code=80000004 addr=587039 rsp=22fd28     <- 紧接着的单步异常！
SRE_AFTER   code=80000004 ...
... 之后成对出现数百次（58b747/58b767, 588a14/588a4a, ...）
```
统计：`80000003` 与 `80000004` 成对出现（如 139 次、131 次），
全日志 802 条探针，**完全没有** `addr=587057`、没有任何 `c00000fd`、
没有 `nested exception` —— 即原生 Wine **完全没走到平板上的那条野 SP 崩溃路径**。

平板上（box64）同一位置：
```
SRE_ENTRY code=80000003 addr=587033   -> 之后没有 80000004
... 直接线性执行到 587057 push -> 只读页故障 -> 壳处理 -> 587073 野 SP -> 崩
```
保存的设备 trace 切片里 `80000004` 计数 = 0。

## 6.3 结论（当前最强假设）

VProtect 的解包 VM 是**"int3 + TF 单步"驱动**的心跳：int3 进入壳的处理器，
处理器设置 TF，让下一条指令执行完后再触发 `#DB`（单步），如此逐条推进。
原生 Wine 完全遵循；**平板上 box64 的 32 位模拟没有把 TF 单步投递上来**，
于是壳的 VM 心跳断掉 → 代码按字节线性执行 → 撞上只读页 push → 野 SP。

## 6.4 待验证（平板一接上就做）

1. 用 `mingw` 编译的最小 32 位探针 `tf_test.exe`（已生成 `D:\ccz_winlator_patch\unpack_tools\tf_test.exe`）
   - 内部用 `pushfl/orl $0x100,(%esp)/popfl` 设置 TF，VEH 里统计
     `EXCEPTION_SINGLE_STEP` 次数，写到 `C:\tf_result.txt`
   - 对照：Windows 本机 `traps=1`；WSL 原生 Wine `traps=1`（都已实测）
   - 平板上跑：`traps=0` ⇒ 假设成立（box64 32 位不投递 TF 单步）
2. 若成立，按顺序试：
   - `BOX64_DYNAREC=0`（强制解释器）后再跑 tf_test / 游戏
   - box64 0.3.0 二进制（APK 内 `assets/box64/box64-0.3.0.tzst`，未 strip，
     含 `native_singlestep` 符号）可考虑直接打补丁或换 build

---

# 第七轮（2026-11-01 15:40-16:00）：TF 单步与断点 Eip 的对照实验

## 7.1 受控探针（mingw 32 位，PC/原生 Wine/平板 三处对照）

| 探针 | 内容 | Windows | WSL 原生 Wine | 平板(box64) |
|---|---|---|---|---|
| `tf_test.exe` | 用 `pushfd/orl $0x100/popfd` 置 TF，VEH 数单步 | traps=1 | traps=1 | **traps=1** |
| `tf_test2.exe` | int3 → VEH 里改 Context（Eip/TF）→ 继续 | bp=1 traps=1, `bp_eip=addr` | 同左 | bp=1 traps=1, **`bp_eip=addr-1`** |

结论：
- **box64 的单步投递本身是好的**（VEH 改上下文置 TF 后能收到 #DB）；
- **box64 给 32 位异常上下文的 Eip 比 Windows 少 1**（#BP：Windows/Wine 原生 `Eip==ExceptionAddress`，box64 是 `addr-1`）。

## 7.2 在 ntdll.dll(i386) 里纠正断点 Eip（已部署）

`dlls/ntdll/signal_i386.c` `dispatch_exception()` 开头：
```c
if (rec->ExceptionCode == EXCEPTION_BREAKPOINT &&
    context->Eip == (DWORD)(ULONG_PTR)rec->ExceptionAddress - 1)
    context->Eip = (DWORD)(ULONG_PTR)rec->ExceptionAddress;
```
部署后 `tf_test2` 在平板输出 `bp_eip=004015df bp_addr=004015df`，与 Windows 一致。

## 7.3 但壳仍然不做单步

部署修正后重跑加壳 Ekd5：仍然是
```
SRE_ENTRY code=80000003 addr=587033 rsp=31fd28 ef=246 cs=23 np=1 info0=0 ...
DRPROBE dr0=0 dr1=0 dr2=0 dr3=0 dr6=0 dr7=0 eax=586f8c ecx=7ac...
SRE_ENTRY code=c0000005 addr=587057 ...    <- 直接到 push，没有 80000004
```
与原生 Wine 的对应行（同 addr/ef/cs/np/DR 全 0）逐项相同，但原生下一条就是
`code=80000004 addr=587039`（单步），平板上没有。

所以：**壳在平板环境下没有把 TF 置上（或走了另一条分支）**，
而不是“置了 TF 但 box64 不投递”。

## 7.4 已排除的差异项

- 异常码/地址/参数（np=1 info0=0）、EFlags(246)、CS(23)、ESP
- CONTEXT 里的 Dr0-Dr3/Dr6/Dr7（均为 0，与原生一致）
- IO 优先级/单步投递能力（tf_test/tf_test2 证明可用）

## 7.5 正在试的方向

- `BOX64_DYNAREC=0`（解释器）：平板上加壳 Ekd5 在加载完 ws2_32 后就没有下文（疑似更早死掉），
  暂未复现出 int3 序列。
- 下一步候选：对比 32 位 TEB/PEB/heap 字段（ctx_test2 在平板上 GetProcessHeap 处挂住）、
  壳处理器的分支条件（需要更细的 32 位侧探针）、或改用不同 box64/Wine 组合。

设备当前状态：ntdll.so=fix25(带探针)、ntdll.dll(i386)=带 Eip 修正、wfm.exe=cmd、.container 已还原。

---

# 第八轮（2026-11-01 16:00-16:20）：BTRun 通道建立 + box64 32 位内存缺陷

## 8.1 可靠探针通道（重要）

控制台（wfm→cmd）通道不稳定（输入丢失、stdout 管道被 UI 阻塞），改用
**文件管理器 BTRun 跑 .bat**（`D:\ccz_winlator_patch\unpack_tools\run_bat.py` 自动导航），
结果写文件到 `/sdcard/Download/ccz22/*.log`，稳定可复现。

用该通道重测，之前"平板挂住"的 ctx_test 等其实是控制台管道假象，实际全部正常：

| 探针 | 平板结果 | 结论 |
|---|---|---|
| ctx_test | DR0-7=0, BeingDebugged=0, NtGlobalFlag=0, TEB=7ffc2000, PEB=7ffd1000 | 与原生 Wine 一致 |
| ctx_test2 | StackBase=320000 StackLimit=122000 HeapFlags=00000002 | 与原生 Wine 一致 |
| tf_test2 | bp=1 traps=1 bp_eip==bp_addr | BP Eip 修正生效、单步可用 |
| tsc_test | d1=630016（原生 445884 / Windows 182800） | 计时比例正常，排除"太慢被当调试器" |
| cpuid_test | maxleaf=0000000F, leaf1 eax=00040661 ecx=7ED83203 hyp=0，无 hypervisor 叶 | 与 Windows/原生差异明显（原生 maxleaf=1C/20, hyp=1） |

## 8.2 两种启动方式在平板上的不同结局

- wfm→cmd 控制台：走到 0x587057 只读页 push → 0x587073 野 SP → nested exception 崩。
- 文件管理器 BTRun（0game.bat）：进程不崩，**死循环**，box64 日志刷屏：
  ```
  Strange SIGSEGV with Access error on 0x77e71706c8 for 0x202d1c7 mapped, prot=0x7
  ```
  访问地址 `0x0202d1c7` 正是 VProtect 表里的 `0x0202xxxx` 段、且 prot=0x7（RWX）却仍报 SIGSEGV。
- 再加上 `BOX64_DYNAREC=0`（解释器）后，同一个 BTRun 启动 → **EXIT 0**（壳自退）。

三种运行方式三种结局，说明当前 box64 0.3.0 的 32 位模拟在这套壳上有多处缺陷
（异常上下文、内存访问/SMC 处理）。

## 8.3 当前判断

Wine 侧能对的都对上了（异常记录、上下文、DR、TEB/PEB/堆、单步投递能力）；
剩下的分歧集中在 **box64 的 32 位执行/内存语义**（dynarec 的 Strange SIGSEGV、
以及壳处理器在 box64 下不触发单步）。这已超出"改 Wine"的范围。

## 8.4 可选路线（需要拍板）

- A：继续在 Wine 侧逼近（成本中等、成功率不确定）
- B：换运行栈：Winlator 5.x/6.x（box86 + 真 32 位 Wine32，非 wow64）——需要下载 APK
- C：从源码构建/修补 box64（arm64 交叉编译，工作量大但可精准修 32 位缺陷）

---

# 第九轮（2026-11-01 16:40-17:05）：换运行栈验证（Winlator 6.1 / box86 + 真 32 位 Wine）

## 9.1 实验装置

- 从 GitHub releases 经镜像下载 **Winlator 6.1**（265,585,916 字节），
  用「文件管理器点 APK → 允许 → 继续安装」装到平板（MIUI 拒绝 pm install）。
- 6.1 的 imagefs 里确有 `i386-unix/i386-windows` 与 `box86-0.3.0/0.3.5`，
  运行期日志显示使用 **armhf 用户态 libs + box86**（真 32 位 Wine，非 wow64）。
- 输入通道在 6.1 里不响应 adb tap/键盘，改用「替换 wfm.exe 为自写 launcher.exe」
  在容器启动时自动拉起 `D:\ccz22\Ekd5.exe`（launcher 源码/产物在 D:\ccz_winlator_patch\unpack_tools）。

## 9.2 结果：6.1 也在同一个点失败

```
SIGSEGV @... (x86pc=0x587057, esp=0xddfda0) ... for accessing 0xddfd9e (code=2/prot=5)
SIGSEGV @0x62961fa8 (i386-unix/ntdll.so) ...   -> _exit(1)
```

- 同样是 0x587057 的只读页 push；
- 之后在 ntdll.so 里二次 SIGSEGV（异常投递失败）→ 进程退出；
- 全程 **没有任何 SIGTRAP**（单步）。

## 9.3 关键结论（本轮）

- **换到 box86 + 真 32 位 Wine 也不能跑过壳**；
- 在 PC 上的两种 Wine（x86_64 wow64、纯 i386）都能正常走完壳的
  「int3 + 单步」流程（各 1233 次），说明 Wine 本体没问题；
- 问题收敛到 **x86-on-ARM 模拟器**：
  - box86 的 x86run 源码里没有 TF/SIGTRAP 单步逻辑（只有 arm_singlestep 符号）；
  - box64 有 TF 逻辑、且我们的探针证明「VEH 改上下文置 TF 后能收到单步」，
    但加壳游戏在 box64 下依然不产生单步 ⇒ 壳在该环境里没走那条分支（原因仍未定位）；
- 结论：继续在 Wine 侧改**没有可下手的地方**了，下一步只能换模拟器内核
  （如 FEX-Emu/wow64 via Hangover，或给 box86/box64 打补丁），或放弃该加壳版本。

## 9.4 设备现状

- 已停掉两个容器；Winlator 6.1 的 wfm.exe/.container 已还原；
- Winlator 8 容器仍保留调试配置（ntdll.so 带探针、ntdll.dll 带断点 Eip 修正、wfm.exe=cmd、trace 环境）。

---

# 第十轮（2026-11-01 17:05-18:30）：Termux + Hangover(FEX) 与硬件断点线索

## 10.1 FEX/Hangover 环境已搭好（可用）

- Termux 0.118.3(GitHub debug) 已装；`pkg install hangover-wine`(11.16) +
  `hangover-libwow64fex` 安装成功（wine 在
  `/data/data/com.termux/files/usr/opt/hangover-wine/bin/wine`）。
- 运行方式：`HODLL=libwow64fex.dll wine app.exe`（32 位走 FEX）。
- 无显示驱动（X 未装），GUI 起不来，但控制台/探针可跑；WINEDEBUG=+seh 可用。

## 10.2 关键探针结果（三平台对照）

| 探针 | Windows | 原生 Wine(x86) | box64(平板) | FEX(平板) |
|---|---|---|---|---|
| tf_test(TF via POPF) | traps=1 | 1 | 1 | — |
| tf_test2(TF via 异常上下文) | 1 | 1 | 1 | **1** |
| hwbp_test(Dr0+Dr7 执行断点) | **hits=1** | **1** | **0** | **0** |
| 加壳 Ekd5 的 int3 后单步 | 有 | 有(1233) | **无** | **无** |

- FEX 也跑了加壳 Ekd5：同样出现 `EXCEPTION_BREAKPOINT addr=00587033`，
  之后**没有单步**，最后 `virtual_setup_exception stack overflow`。
- 结论：**FEX/box64/box86 都没实现 x86 硬件断点**（Dr0-3+Dr7），
  但 TF 单步在所有平台都可用。

## 10.3 反证实验（重要，避免误判）

在原生 Wine 上把「Dr 写进缓存但不交给 CPU」打补丁后：
- `hwbp_test` 变成 hits=0（补丁生效）；
- 但加壳 Ekd5 **仍然出现 782 次单步** → 壳的“逐条推进”**不是**靠硬件断点，
  而是 TF（或其它上下文途径）。

⇒ 硬件断点不是壳的推进机制；壳在 ARM 上不单步，仍是一个未解的差异。
（TF 通过异常上下文在 ARM 上确认可用，所以不是“TF 不支持”。）

## 10.4 当前状态

- 平板：Winlator 8 仍为调试态（ntdll.so 带探针/修正、wfm.exe=cmd、trace 环境）；
  Winlator 6.1 已还原；Termux + Hangover(FEX) 已装好可复用。
- PC：WSL 内有 wine0905（wow64）、wine32src（i386）、完整对照工具链。

## 10.5 下一步候选

1. 在 wow64 的「32 位上下文回填」路径（NtContinue/wow64 CPU area）上做 native vs FEX 差分，
   确认壳的处理器到底有没有把 TF 写进 pContext（写没写 / 写了被谁清了）。
2. 若确认「壳写了 TF、Wine 在 ARM 上丢了」→ 在 Wine 的 wow64 上下文应用处修复（可落地）。

---

# 第十一轮（2026-11-01 18:35-19:00）：硬件数据断点定案 + 软件模拟

## 11.1 壳的机制（relay/seh 日志实锤）

原生 Wine 9.2 +seh 日志（int3 之后）：

```
call_stack_handlers handler at 00586F8C returned 0
NtGetContextThread 0xfffffffe: dr0=700 dr1=22fe14 dr2=600 dr3=700 dr6=ffff0ff2 dr7=300104
dispatch_exception code=80000004 addr=00587039
```

- 壳在 int3 后 **NtSetContextThread 设 Dr1=栈地址、Dr7=0x300104（1 字节读/写断点）**；
- 随后读回 Dr（平板与原生读回值一致：`dr1=31fe14 dr7=300104`），**唯一差异 Dr6**（原生 0xFFFF0FF2 带命中位，平板 0）；
- 它在等**数据断点触发的 #DB**。平板 Wine 的 server 是 ARM 版，**x86 调试寄存器未实现** → 永不触发 → 壳走 0x587057 替身。

## 11.2 已在 Wine 里实现软件硬件断点

`virtual.c` 新增：
- 设置 Dr 时把目标页挂监视：写断点→VPROT_WRITEWATCH；读/写断点→整页 PROT_NONE 并记录页号；
- 访问命中 → 放开页面、按需单步、投递 EXCEPTION_SINGLE_STEP 并写 Dr6 命中位；
- 非命中访问 → 单步后吞掉并重新上锁（避免干扰正常栈访问）。

`signal_x86_64.c` 新增：rw 断点 fault 分支、trap 抑制/Dr6 写入、TF 从 host 上下文转交 guest 的尝试。

## 11.3 实测轨迹（每版都有变化）

| 版本 | 行为 |
|---|---|
| fix29/30 写监视 | 挂上只读监视，但壳没访问该地址就进了替身 |
| fix41 读/写监视 + host TF | **替身消失**，壳收到 `0x80000004`；但 TF 落在 64 位桥接代码（addr=0x3F0009F412）→ 未处理 → 进程以 0x80000004 退出 |
| fix43 TF 写进 wow CPU area | host 误触发消失，但 guest 未单步，回到替身 |
| fix44 直接投递 SINGLE_STEP | 未命中（页面保护被普通栈访问消化掉） |
| fix45 探针 | `HWBP_ARMDBG rw3 page=31f000 vprot=20 prot=0`；`HWBP_RWDBG addr=31f8d0 idx=0`（普通栈访问、非命中） |
| fix46 host→guest 转交 TF | 又回到替身（guest 没吃到 TF） |

## 11.4 剩余唯一问题

**如何在 box64 里让 32 位 guest 单独执行一条指令**（Wine 的 wow64 在 ARM 上恢复 32 位代码时从哪取 EFlags）。
TF 放在信号上下文会误伤 box64 的 64 位桥接代码；放在 wow CPU area 又不生效。
下一步：在 Wine 恢复 32 位执行的那条路径（wow64 CPU area 的 Eflags 装载处 / box64 的 sigreturn）落 TF，
或改用「非命中访问也整页重新上锁 + 在下次 Syscall 边界重新挂」的折中策略。

---

# 第十二轮（2026-11-01 19:00-19:25）：单步投递对齐原生 + 页保护被栈访问消费的解法

## 12.1 关键突破：栈基址平移

壳监视的字节总在 `StackBase-0x1EC`，而活跃 ESP 在同一页 → 整页 PROT_NONE 会被普通栈访问“消费”。
在 `machine/thread.c: init_thread_stack()` 把 wow64 的 `wow_teb->Tib.StackBase` **减 0xE14** 后：
- 被监视字节落到页首（0x31f000, offset 0），活跃栈落到上一页（ESP=0x31ef18）；
- 该页只剩壳自己会访问 → 监视命中不再被打断。

## 12.2 单步已与原生逐字段对齐（fix47/fix49）

```
SRE_ENTRY code=80000003 addr=587033        (int3)
SRE_ENTRY code=80000004 addr=587039        (数据断点 #DB，与原生同地址)
```
- 投递走 32 位路径：`save_context()+setup_raise_exception()`（不能用 setup_exception，它会覆盖 ExceptionAddress）；
- 按 ModRM 计算指令长度，把地址报成“读之后的下一条指令”；
- 同时写 `amd64_thread_data()->dr6 = 0xFFFF0FF0 | (1<<bp)`。

## 12.3 壳的反应（平板上）

```
HWBP update dr1=31f004 dr7=300104  →  ARM page=31f000 (PROT_NONE)
[我们的单步 addr=587039]
HWBP update dr1=31f004 (重新武装)
GCTX flags=1003f (查询上下文)
HWBP update dr1=31f004
HWBP update dr=0 (清空) → 走 587057 替身
```
壳收到了正确的 #DB、还主动重新武装并查上下文，但**只走了一步就放弃**。

## 12.4 试过但失败的：投递前先仿真那条读（fix50）

在 `mov r32,[mem]` 命中时先把值读进目标寄存器、EIP 顺延，再投 #DB（模拟“读已发生”）。
结果：游戏**启动即退**（没有任何探针），说明仿真分支在启动早期被误触发/写坏寄存器。已回滚到 fix49。

## 12.5 下一步

1. 给 fix50 的仿真分支加触发条件（仅当 rip 落在壳已解密代码区且 Dr 有效时）；
2. 或对比“单步之后壳期望的下一拍”：它重新武装同一地址却在等一个不来的访问，怀疑它检查了某处内存/上下文值（需在 32 位侧记录 context 全量寄存器做 diff）。

---

# 第十三轮（2026-11-01 20:00-21:30）：perf/MTE/TF 全排除，软件断点推进到 ~1400 个 VM 步

## 13.1 三条替代路线逐一否掉

| 路线 | 实测 | 结论 |
|---|---|---|
| perf 硬件断点 | Termux/Winlator 应用域 `PERF_TYPE_BREAKPOINT` 可开、读写都命中；**容器里（proot ptrace）连软件事件都 EACCES** | 不能用 |
| MTE（16B 粒度） | HWCAP2 MTE=0、`prctl`/`mprotect(PROT_MTE)` EINVAL | 不支持 |
| TF 单步 | TF 写 wow 上下文 → box64 单步宿主桥接代码（0x3f00../0x7ace..）；写 wow CPU area 不生效 | 不能用 |

另外：临时 int3 单步在 box64 dynarec 下无效（已翻译块不重取）。

## 13.2 软件断点的完整方案（fix69~fix76）

1. **`#SS` 也要走监视页分支**：box64 把栈内地址的故障报成 `#SS`(trap=c)，之前直接进 `virtual_handle_fault`，被 Wine 的 `VPROT_WRITEWATCH` 语义自动解保护 —— 这是“命中页在栈里就永远不触发”的真因。改成
   `case TRAP_x86_STKFLT: if (hwbp_rw_page_for(siginfo->si_addr)) goto hwbp_pagefault;`
2. **投递必须前进 RIP**（fix49 已证）：`ExceptionAddress = 下一条指令`，ucontext RIP 同步前进。
3. **未命中的栈访问用指令模拟**：页已解保护 → 模拟 `push/pop reg、pushad/popad、pushfd/popfd、push imm、mov/lea、FF/6、FF/2、e8/eb/e9、c3/c2、jcc` 并推进 RIP → 立刻重新上锁。未知指令不再重新上锁（避免死循环）。
   - 修正：`pushad` 入栈顺序（Intel：EAX 先入栈 = 最高地址；内存升序应为 EDI,ESI,EBP,oldESP,EBX,EDX,ECX,EAX）。
4. **guest/host 判定用 CS==0x23**，不能用 `<0x10000000`：i386 ntdll.dll 映射在 0x7acxxxxx，和 64 位 wow64.dll 的地址段重叠。
5. 环境：`WINE_SS=0xe00`（TEB StackBase 平移）+ `WINE_HWBP_SOFT=1`；`WINE_ESP_DROP` 也做了但当前未用。

## 13.3 实测进度

`fix73` 最高 **2841 条 SRE_ENTRY（≈1400 个 int3+单步对）**，与原生序列逐条一致到 `58bae0/58bb29`（含被监视槽 `31f000/31f014/31efbc` 三种偏移、命中页在栈内/栈外两种情况）。

## 13.4 剩余卡点

投递 #DB 之后，**Wine 自己的 wow64 分发器**（宿主 64 位 ntdll.so 的 `set_thread_wow64_context` +0x2d7，以及 i386 ntdll.dll dispatcher 0x7acc8df8 附近）在**同一张受保护栈页**上建帧、读写 CONTEXT。逐条模拟几十条后上下文被改坏：应用时 `eip=0/2/0x1c7`（同时 `Dr1=0x30`，说明 EDI 已被清 0），guest 跳到 0x1c7 → #GP 死循环。
对照实验（`fix75` 分发期全程放开=104 条、`fix76` 命中后放开=963 条）都不如 fix73，说明放开时机必须精确到「guest 下一次恢复执行之前」。

## 13.5 下一步

1. 找到 **64→32 的真正切换点**（wow64.dll / syscall dispatcher），在那里重新上锁；分发整个过程放开。
2. 或按 EMU 日志里 `rip=0x7acc...` 的条目补齐分发器指令语义。
3. 或改 box64 实现 Dr0-3（源码+交叉工具链，工程量最大但最干净）。

产物：`ntdll_fix73.so`（md5 311be415…）为当前最佳，已部署。

## 13.6 后续两轮（fix77~fix79）：抑制装订 + 模拟器精度

- **fix77（抑制装订，2544 条 SRE）**：新增 `hwbp_suppress`——投递合成 #DB 前置位，`hwbp_update` 看到 Dr7==0（壳的处理器已清 Dr）时自动解除。目的是让「分发器阶段」不再被页保护干扰。效果与 fix73 持平，卡点未变。
- **fix78/79（模拟器精度，反而退到 361 条）**：给 `mov r/m` 加 16 位（0x66）处理、补 `0xc7/0x80/0x81/0x83`、修 `jcc` 符号/溢出比较、`pushad` 的 oldESP 槽、`FF /6` 的位宽。
  **实测暴露**：在 `7acce3d4(push ebx;sub esp,4) → 7acce3e5(call) → 7accdcb0(push ebp) → 7accdcdd(ret 4)` 这段，模拟后每次栈操作比预期多减 4 字节，最终 `ret 4` 弹出 `0x31e380`（栈地址）→ `#GP`。
  ⇒ **模拟与 box64 的寄存器/栈回写存在冲突**（怀疑 `hwbp_setreg` 对 RSP 的 32 位截断、或"故障时 ucontext 里的 ESP 是故障前/后的口径不统一"），继续做需要先把这一点用单条指令的受控实验钉死。
- 设备已回退到 **fix73**（311be415…，最高 2841 条 SRE）。

## 13.7 fix80/81：用「故障地址反推栈指针」重写模拟器

思路：push 的存储目标地址 = 执行后的 ESP；pop 的读取地址 = 执行前的 ESP。这样不必猜 box64 是否已经改过 ESP。

实测发现的关键语义（box64 0.3.0）：
- 单条 `push`/`call`/`pushfd`：故障时 ucontext 的 ESP 仍是**执行前**的值，故障地址 = ESP-N。⇒ 模拟后 ESP 应取故障地址。
- **`pushad` 是拆成 8 次 push 实现的**：故障落在**第一次 push（EAX → ESP-4）**，所以故障地址是 `ESP-4` 而不是块基址 `ESP-32`。块基址 = 故障地址 − 28。
- 对照受控实验（`stkfault2.exe`）：`SEGV addr=31a3fc rsp=31a400`（push），与上面一致。

fix80（统一按故障地址当 ESP）得 1509 条 SRE、未知指令 1459 次（说明分发器用到的指令面比想象的大）；fix81（pushad 再 −28）更早失败（50 条）。
⇒ 结论：**继续在这条路上推进，需要把 32 位 ntdll 分发器实际用到的指令集覆盖全**（日志里 0x7acc.... 的 `bytes=` 就是清单），而不是零敲碎打。

设备已回到 fix73（311be415…，最高 2841 条 SRE）。

## 13.8 fix82~84：把 32 位分发的指令面补齐（SRE 2841 → 5034）

从 fix80 日志统计出：**1459 次“未知指令”全部是 `0x21`（`and r/m32, reg`）**，说明分发器大量使用 ALU 内存操作。依次补：
- fix82：补 ALU 的 `r/m, reg` 组（0x00~0x39）→ 未知归零，但出现死循环：**模拟的 `cmp/test` 没有更新 EFLAGS**，条件跳转永远走同一支（74k 次 EMU）。
- fix83：补 EFLAGS（CF/ZF/SF/OF/PF）→ **SRE 跳升到 5034（≈2500 个 VM 步）**，这是目前最好成绩；随后死在 `0x58bd7d`（`add [eax+0xc4],0x20` 且 EAX=0xFFFFFF3C ⇒ `add [eax]=...` 读到地址 0），日志同时显示壳的处理器把 `Dr1` 设成了 `0xffce115f`（= −ESP），说明 EDI 已脏。
- fix84：修正 ALU 的**方向**（`0x23 and reg,[mem]`、`0x2B sub reg,[mem]` 这类原被当成“写内存”，实际应写寄存器）→ 反而只有 13 条 SRE 后挂住，说明新分支里还有选择条件的 bug（`op & 0xc6 == 0x02` 的判定需要逐条核对）。

**当前最佳：fix83（md5 21cf7c2c…），SRE=5034、未知指令=0。**

下一步（明确）：
1. 把 fix84 的方向修正做对（用指令字节表逐条验证 `reg_form` 判定），同时保留 fix83 的 EFLAGS。
2. 再跑，观察 EDI 是否还会变成 −ESP；若仍脏，用“每条模拟后打印 EAX/EDI/ESP”的窄窗口日志定位第一条出错的指令。

## 13.9 fix85/86：ALU 方向修正的反复（教训）

- fix85：把 ALU 组范围改成 `op<=0x3D && (op&7)<=5`，并区分 `r/m,reg` 与 `reg,r/m`（后者写寄存器）→ 只有 50 条 SRE。
- fix86：修掉 fix85 里把 `0x85 test [mem],reg` 误判为“无 ModRM 早退”的问题 → 仍然 50 条，且新的失败形态是 **EBX 变成了 0xC0000005（NTSTATUS 被当成指针）**。

对比：fix83（ALU 一律按“写内存”处理，方向其实是错的）反而有 5034 条 SRE。
⇒ 说明 5034 那条路径里，**有些指令的“错误方向”恰好被后面的代码掩盖了**，而方向一改就暴露了另一处更早的问题（EBX/EDI 被写成状态码/负栈指针）。
**下一步必须先定位“第一条把寄存器写脏的模拟指令”**，而不是继续整体改：
1. 加一个窄窗口日志：仅当模拟指令改动 EBX/EDI/EAX 且新值“像状态码(0xC0000xxx)或负栈指针”时，打印指令字节 + 前后值。
2. 拿到第一条后，对照 i386 ntdll 反汇编逐条核对语义（重点：有符号/无符号宽度、0x66 前缀、以及 `movzx/movsx(0F B6/B7/BE/BF)` 这类还没覆盖的两字节指令）。

## 13.10 窄窗口寄存器跟踪（fix87/88）的发现

给前 N 条模拟指令打印 `EAX/EBX/ECX/EDX/ESI/EDI/EBP/ESP/EFLAGS` 后确认：
- 壳的 #DB 处理器（0x58BC80）在模拟下**校验通过**：`add ebx,[eax+0xc]` → `0xD00`，`add ebx,[eax+0x10]` → `0x1400`，然后正常清零 Dr/EAX。寄存器在这一段都是健康的（EDI=ESP=0x31EED0 之类）。
- 但**最后一条被模拟的指令之后（`mov [eax+0xb0],ebx`，写 context 的 EAX 字段）就再没有模拟调用**，说明后续崩坏发生在**guest 自己执行的指令**里；再次进 int3 处理器时 EDI=1（`Dr1=0x31`）。
- 结论：问题不在“处理器那段模拟”，而在此前某次模拟改动了 guest 之后会读取的数据（最可能是 `pushad` 写入栈的 32 字节内容，之后被 `popad` 复原到寄存器）。

**下一步**：给 `pushad` 做单条受控实验（32 位 PE：保护页 + ESP 指向页顶 + 执行 `pushad`，在 VEH 里打印故障地址、ESP 和内存 32 字节），确认 box64 的入栈顺序到底是 EAX 先还是 EDI 先；据此校正 `pushad` 的布局，再跑。

设备：fix83（311be415…）为当前最佳。

## 14. 突破（fix89~fix98）：平板上游戏已可玩

**结论：VProtect 的硬件数据断点已在 Wine 里用软件实现，曹操传 v2.20 在平板上解包→主菜单→读档→战场，可操作。**

### 14.1 关键发现：guest TF 单步可用
受控实验 `tf3_test.exe`（32 位 PE，`pushfd; orl $0x100,(%esp); popfd` + VEH）证明 **box64 会按 guest 语义执行 TF 单步**：
`db=1 ... trap0 eip=401645`（陷阱正好落在下一条指令）。注意 **TF 不会跨异常保持**，每步都要在处理器里重新置位。

### 14.2 新方案（取代"自己模拟每条 x86 指令"）
1. arm 时保护 watch 页（不变）。
2. 故障来到时，若 `CS==0x23`：
   - 命中槽 → 页已 unprotect，**给 guest 上下文置 TF**，让 guest 自己把这条指令执行完；TF 陷阱正好落在"指令之后"，与真硬件数据断点的投递点一致。这时清 TF、设 Dr6、`hwbp_consume()`、投 `EXCEPTION_SINGLE_STEP`。
   - 未命中 → 同样 TF 单步，陷阱里重新上锁即可。
3. 这样**完全不需要 x86 指令模拟器**（fix80~fix88 那一整套可以退休）。

### 14.3 两个必须绕开的 box64 行为
- **push 类指令会二次减栈**：box64 在 store 故障时**已经把 ESP 减好**（故障地址 == 当前 RSP）。若让它 TF 重跑整条指令，就会再减一次。实测 `66 51 push cx` 造成 +2 字节漂移、`call` 造成 +4 字节漂移，累积后返回地址错位。
  解法：判据用 **`fa == RSP`**（**不要用异常 err 的读写位**——box64 会报反：`push [ebp+x]` 的**读**故障报成 err=2），命中则把 ESP 回填 `fa + 宽度`（0x66→2，否则→4）再 TF 单步，让 box64 整条重跑。
- **call 进 syscall dispatcher 会丢 TF**：`call ecx`（0x58fdb）之后没有陷阱。上面那条 push 规则里把 `e8`/`ff /2`/`ff /6` 一并按 push 处理即可（它们同样是 ESP 先减）。

### 14.4 逐事件对照原生
用 PC 原生 Wine 9.2 的日志（`/root/ccz_drexp.log`，780 对 int3/#DB）逐条比对：
- fix90（只修 ExceptionCode/Dr6）：前 50 条一致。
- fix93/94（push 模拟）：前 53 条。
- fix95（补 16 位 push）：前 53 条。
- **fix96（改成"ESP 回填 + 整条重跑"）：前 770/780 条完全一致**，之后分叉，但游戏已经跑通解包。

### 14.5 产物
`ntdll_fix98.so`（md5 `d22afe9275962219c0da6228a9a5e6e5`）= fix96 + 日志默认关闭。
默认把 fd2 指 /dev/null（**必须重定向**，否则 proot 管道不排水会卡容器启动）；`WINE_HWBP_LOG=1` 才写 `/home/xuser/hwbp.log`。

### 14.6 实测画面
解包约 2 分钟 → 标题 三国志曹操传 加强版 v2.20（星汉灿烂/契阔谈宴/歌以咏志/壮心暂歌）→ 读档（读取进度，存档来自更早的会话）→ 战场"宛郊外 曹操军主营" → 点武将出名牌、点空地弹「出兵/装备/买进/卖出」营地菜单。

## 15. 周边：启动方式、桌面快捷方式调研、清理（2026-11-02）

### 15.1 启动方式
- 容器外壳 `wfm.exe` 换成自建启动器（`build/wfm_launcher.c` → `wfm_launcher.exe`）：切 `D:\Download\ccz22` 后拉起 `Ekd5.exe` 并等待退出。原版 `wfm.exe.bak`、cmd 版 `wfm.exe.cmd` 都留着。
- Winlator 8 启动容器**只有 BTRun 选文件一条路**（容器行没有单独"启动"）。日常操作：
  `容器管理 → 容器行菜单 → 管理 → 磁盘(D:) → Download → PlayCCZ.exe 的 ▶`（实测 63.4 s 到标题；直接点 Ekd5.exe 是 67.0 s）。

### 15.2 平板桌面快捷方式：调研结论（结论：App 自身不支持，但可同签名重打包）
- 无 `ShortcutManager` / `requestPinShortcut` / `ACTION_CREATE_SHORTCUT`；manifest 只有 `MAIN/LAUNCHER`；`cmd shortcut` 也不能创建/钉住。
- **APK 用 AOSP testkey 签名**（公钥 SPKI sha256 `ef57b690…`，与公开 testkey.pk8 一致）⇒ 可同签名重打包 + `install -r`，容器数据不动。
- 工具链 apt 可得：apktool 2.4.0 / apksigner 0.9 / zipalign / openjdk-17-jre-headless。
- 路线：`res/xml/shortcuts.xml` 静态快捷方式 + manifest 挂 `android.app.shortcuts` + 一个接收「容器ID+exe」的 exported 入口，smali 复用 app 的 Container/Shortcut 启动逻辑。

### 15.3 清理
卸载 `com.winlator`(1.4G)、`com.termux`(5.4G)；删除 `/sdcard/Download` 内我们留下的构建/日志/中间产物（约 1.05G）。存储 85G → 76G。
保留 `ccz22`（游戏）、`ccz22_patch`（归档）、`PlayCCZ.exe`。app 私有 `files/ubuntu20`（3.0G）暂未动。
