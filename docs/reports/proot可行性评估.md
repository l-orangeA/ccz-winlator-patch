# tinyproot / proot 加速可行性评估

日期：2026-11-02
对象：Winlator 8.0 (`com.winlatcn`) 自带的 `libtinyproot.so`（APK `lib/arm64-v8a/`，75,192 字节，
NDK r22b，aarch64 PIE，stripped）
约束：只做静态分析 + 只读观测；未修改游戏、存档、容器配置。

## 结论（先给）

**没有可开的"更快模式"，也没有更快的官方版本可换。** 这颗 proot 已经默认启用
seccomp mode-2（`SECCOMP_RET_TRACE`）加速，这正是它的快路径；真正慢的
"每次系统调用都停"（`PTRACE_SYSCALL`）只是装过滤器失败时的静默回退。
换/改 proot 对手机提速的预期收益低，不建议作为主线。

**手机实测：快路径是生效的**（真实 App 域，见下表）——所以"手机 proot 退化成全量停点"
这个假设被否证。

## 1. 它是什么

`libtinyproot.so` = Winlator 作者对 PRoot 的裁剪版，源码公开：
`https://github.com/brunodev85/tinyproot`（`CMakeLists.txt` + `src/{tinyproot.c, syscall/, ptrace/, execve/, path/, tracee/, loader/}`）。
二进制中的关键字符串、结构、调用序列与源码逐一对上：

- 子进程启动顺序（对应 `src/tracee/event.c: launch_process`）：
  `fork()` → 子：`ptrace(PTRACE_TRACEME)` → `kill(getpid(), SIGSTOP)` →
  `enable_syscall_filtering()` → `execvp(exe, argv)`。
- `enable_syscall_filtering()`（`src/syscall/seccomp.c`）：
  - `prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0)`
  - `prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog)`（二进制里 0x85e8 / 0x8618 两条 `bl prctl`）
  - BPF 程序按架构分节：`AUDIT_ARCH_AARCH64 (0xC00000B7)` 与 `AUDIT_ARCH_ARM (0x40000028)`
    （二进制里正好是这两组局部描述符）；未知架构最终 `SECCOMP_RET_KILL`。
- **只拦白名单里的系统调用**（约 65 个，`filtered_sysnums`）：open/openat/stat/lstat/newfstatat/
  access/faccessat/readlink(at)/execve/chdir/getcwd/rename/unlink/mkdir/chmod/chown/xattr/statfs/
  prctl/prlimit64/ptrace/wait4/uname/utime/setrlimit 等"路径或状态类"；
  命中返回 `SECCOMP_RET_TRACE + flag`（`flag` 决定是否需要系统调用退出停点，即 FILTER_SYSEXIT）。
  **其余全部 `SECCOMP_RET_ALLOW`**——clock_gettime/gettimeofday/futex/mmap/mprotect/read/write/ioctl
  这类高频调用在 proot 这里**不产生停点**。
- tracer 侧 `PTRACE_SETOPTIONS` 先试 `0xff`（含 `PTRACE_O_TRACESECCOMP`），失败退回 `0x7f`；
  两者都失败才报 `ptrace(PTRACE_SETOPTIONS)` 并退出。首个 seccomp 事件到达时打印
  `ptrace acceleration (seccomp mode 2, new syscall order) enabled`（verbose ≥1）。

## 2. 有没有开关 / 更快版本

- 环境变量只有两个：`TINYPROOT_VERBOSE`（数值日志级别）、`TINYPROOT_LOADER`（loader 路径）。
  **不存在** `PROOT_NO_SECCOMP` 之类开关；`seccomp.c` 里没有任何 `getenv`。
- `enable_syscall_filtering()` 的返回值在子进程里**被忽略**：装不上就静默回退到
  `PTRACE_SYSCALL`（每个系统调用 2 次停点）。这是唯一"变慢"路径，且没有日志（除非 verbose）。
- 上游没有发布更快的构建；源码即全部，能做的只是自己改（GPL）后重编，收益见第 4 节。

## 3. 手机实测（真实 App 域，非 run-as）

启动容器后读 `/proc/<pid>/status`：

| 进程 | PID | 关系 | TracerPid | NoNewPrivs | Seccomp | Seccomp_filters |
|---|---|---|---|---|---|---|
| libtinyproot.so | 6652 | tracer | 0 | 0 | 2 | 1（zygote 自带） |
| start.exe (wine) | 6660 | tracee | 6652 | **1** | 2 | **2**（zygote + tinyproot） |
| wineserver | 6678 | tracee | 6652 | **1** | 2 | **2** |
| winedevice.exe | 6700/6786 | tracee | 6652 | **1** | 2 | **2** |

`NoNewPrivs=1` + `Seccomp_filters=2` ⇒ proot 的 BPF 过滤器确实装进了 tracee，
seccomp 快路径生效。另用同一个 .so 在 shell 身份下跑 `/system/bin/sh` 自检，也直接打印：
`proot:info: ptrace acceleration (seccomp mode 2, new syscall order) enabled`。

（附注：用 `run-as com.winlatcn` 跑同一个 .so 时，tracee 是 `NoNewPrivs=0 / Seccomp=0`、
proot 无任何日志——这是 `runas_app` 域与真实 `untrusted_app` 域的差异，**不能拿 run-as 代表 App**。）

## 4. 剩下的 proot 成本，以及值不值得改

快路径下每次停点来自两处：
1. 被白名单命中的路径类系统调用（启动期 open/stat 多，热循环里少）；
2. **信号投递**——tracer 会截停每一次信号转发，SIGTRAP/SIGSEGV 都算。

我们的 VProtect 补丁（页保护 + guest TF 单步）会在命中时产生 SEGV+TRAP 信号对；
这些信号即使 seccomp 也照样让 proot 停一次。因此"proot 变慢"只可能发生在
**信号风暴 + 调度变差**的组合里，而不是"每个系统调用都停"。

可做的改法（都不建议现在做）：
- 自编译 tinyproot，减少白名单项 / 去掉 FILTER_SYSEXIT 中不需要的项：只减少"路径类系统调用"的停点，
  对 CPU 密集的壳解密循环影响很小。
- 绕开 proot（bind-mount 或直接跑 wine）：会破坏 Winlator 的路径重定向与容器模型，工程量远大于收益。

## 5. ★ 今天新发现的方法学坑（比 proot 更可能解释"手机慢 100 倍"）

手机在无人操作时 **约 30~40 秒自动息屏并回锁屏（指纹锁）**。此时：

- `am start` 仍能启动 Activity（`mFocusedApp=XServerDisplayActivity`），但
  `topResumedActivity` 为空、`mCurrentFocus=null`、keyguard 在前台；
- App 进程落到 `cpuset:/background`、`cpu:/background`——本机 `background cpus = 0-3`
  （只有 4 个小核；`foreground = 0-7`）；
- 容器随后被冻结：`libtinyproot.so` / `Ekd5.exe` 出现 `State: T (stopped)`。

在这种状态下量到的任何"存活/心跳/CPU/上下文切换"都是假读数，且会把
"慢"放大成数量级。**复测前提：屏幕常亮（已 `svc power stayon true`，USB 供电时生效）+
设备已解锁 + XServerDisplayActivity 前台**。指纹锁 adb 解不开，必须由机主解锁。

## 6. 建议的下一步

1. **先不改 proot**。解锁后在有前台窗口的条件下复测一次（hwbp arms/s、Ekd5 的 utime/stime、
   proot 的 voluntary_ctxt_switches），确认历史"1 心跳/10~15s"是否是可复现的前台读数。
2. 若前台仍慢：再查信号转发成本（对比 `WINE_HWBP_SOFT=0` 与 =1 的前台读数），而不是查 seccomp。
3. 若确需动 proot：自编译同源 tinyproot（GPL），只做白名单/退出停点裁剪，并保留
   `TINYPROOT_VERBOSE` 作为回归判据；替换 APK 内 `.so` 后需重打包签名。

## 7. 复现命令（只读）

```
# 1) 看 tracee 是否装了 proot 的过滤器
adb -s <serial> shell 'for p in $(pidof libtinyproot.so) $(pidof Ekd5.exe); do \
  echo PID=$p; grep -E "Name|State|TracerPid|NoNewPrivs|Seccomp" /proc/$p/status; done'

# 2) 单独验证内核是否允许 seccomp 快路径（shell 域，秒级）
adb -s <serial> shell 'TINYPROOT_VERBOSE=2 \
  /data/app/~~*/com.winlatcn-*/lib/arm64/libtinyproot.so \
  --rootfs=/ --cwd=/ /system/bin/sh -c "echo OK"'
# 期望看到：ptrace acceleration (seccomp mode 2, new syscall order) enabled

# 3) 确认前台/后台调度分组
adb -s <serial> shell 'cat /dev/cpuset/background/cpus /dev/cpuset/foreground/cpus; \
  cat /proc/$(pidof Ekd5.exe)/cgroup'
```

---

# 追加：手机前台复测与崩溃链定位（2026-11-02 20:0x~20:2x）

## 前台不是"锁屏假读数"

设备已解锁、`svc power stayon true`、`XServerDisplayActivity` 在 `topResumedActivity`，
Ekd5 的 cgroup 是 `cpuset:/top-app`，跑在 **cpu7 @3.05GHz**：

| 采样 | 结果 |
|---|---|
| Ekd5 CPU | 0.63~0.66 core 持续 |
| Ekd5 状态 | R 75% / t(ptrace stop) 25% |
| **Ekd5 voluntary ctxt switches** | **30,000~43,800 次/秒** |
| 同容器 winedevice/wineserver/start/explorer | **0~117 次/秒** |
| hwbp_stats | 3 分钟后仍无文件（arms<64；平板为 1600 arms/30s） |

⇒ 慢是**游戏进程内部**的停点风暴，不是 proot 对所有进程的开销；同容器其它进程几乎不停。
关掉软件 Dr（`WINE_HWBP_SOFT=0`）风暴仍在（36k/s）⇒ 不是我们"arming"本身。
把**平板验证过的构建 fix99 + 平板配置(0xe00)** 部署到手机，同样约 10s 退出 ⇒ 不是 fix100~104 的构建回归。

## 崩溃链（诊断构建 fix105_diag：fd2 重定向到 /home/xuser/wine_diag.txt）

`WINE_SS=0xe00`（平板配置）：

```
SRE_ENTRY code=c0000005 addr=587057 rsp=8aefc0 cs=23      # 壳的只读栈 push
SRE_AFTER stack=8ae8c0 stackptr=8aefc0
MPROT_FAIL page=8ae000 sz=1000 errno=13                    # EACCES（mprotect 失败，属设计内）
→ guest 在 587058 继续，设置 Dr1=0x31f014 / Dr7=0x300104
→ perf_event_open(31f014) errno=13 → 走软件 Dr；page 31f000 被 PROT_NONE
SEGV rip=587073 addr=d12a01fa rsp=d12a0202 err=4            # 壳的 VM 栈 guard/未映射页
SRE_AFTER stack=d129fb00 stackptr=d12a0200
MPROT_FAIL page=d129f000 sz=2000 errno=12                   # ENOMEM：异常帧落在未映射页
旧回退：memcpy(tmp, page, sz) 从 d12a0000 读 → 主机侧 SIGSEGV @rip=0x50173
        （rdi=0x3f00…, rsi=0xd12a0000, rdx=0x2000）→ 进程死亡
```

`WINE_SS=0xe14`：不走到 587073，进程存活但在别处以 30~44k 停点/s 空转（arms 仍 <64）。

## fix106：主机崩溃已消除，但 guest 仍退出

补丁（`signal_x86_64.c` 的 MPROT_FALLBACK）：逐页 `process_vm_readv` 只拷已映射部分，未映射补零，
再 `mmap(MAP_FIXED)`。结果：
- 0xe00：不再出现 `rip=0x50173` 主机崩溃；但 guest 在 587073 的 AV 后走
  `KiUserExceptionDispatcher → loader`，约 11s 退出（表现为"未处理异常"）。
- 0xe14：行为不变（30~44k 停点/s，不前进）。

⇒ 剩余阻塞点：手机侧 `0x587073` 这次异常没有被 guest 的异常处理链接住；平板同样命中该 AV 却能继续。
下一步需要平板做正对照（同一 fix106_diag 构建），比较 `0x587073` 之后的 guest 路径。

## 产物与现场

- `build/ntdll_fix105_diag.so` md5 `4c0352dbc2fd109ce4d2b2106da25d10`（仅加 fd2 重定向，`WINE_HWBP_DIAG=1` 生效）
- `build/ntdll_fix106_diag.so` md5 `8d13bad57b5b0402712c44cc0c07fa80`（+ 未映射页安全回退）
- 手机已恢复：`ntdll.so` = fix104 `e92cb6dd608dd9226ed7619de0787df6`，
  容器 env 与备份逐键一致（`WINE_SS=0xe14`，无诊断变量）
- 诊断日志样本：`D:/ccz_winlator_patch/unpack_tools/diag_clean_0e00.txt`

---

# 追加2：平板正对照（2026-11-02 20:4x）

同一颗诊断构建 `ntdll_fix106_diag.so`（8d13bad5）分别跑两台设备：

| 指标 | 平板（正常，跑出标题） | 手机（0xe14，12s） | 手机（0xe00，11s） |
|---|---|---|---|
| int3 `80000003` | **386** | **2** | **0** |
| 单步 `80000004` | **386** | **0** | **0** |
| 监视页命中 `SEGV_ALL` | **44,367** | **0** | **0**（只有 VM 栈 AV） |
| `MPROT_FALLBACK` | 0 | 1 | 2 |
| `587057` 反调试探针 | **0** | 4 | 4 |
| `587073` VM 栈 AV | **0** | 0 | 有 → 退出 |
| hwbp_stats | arms=1536 faults=44365 steps=43265 hits=384 t=37.5s | <64 | <64 |
| 截图 | 1.6MB（标题） | 黑屏 | 黑屏 |

平板日志的 80000003/80000004 是交替的：`int3@587033 → 单步@587039`，正是 VProtect 心跳；
手机一台 int3 后 Dr1 被设成 0x31f004/0x31f014，然后**立刻被清掉**并进入 587057 探针。

⇒ **手机壳没有执行那次被监视的读**（没有任何监视页 SEGV、没有单步），不是异常投递的问题
（fix106 在平板上验证完全正常）。根因在 guest 侧的 int3 处理/返回上下文：
平板留在 fake stack（esp≈31ef14）继续心跳；手机返回后 ESP 跳到主栈（≈8aefb0），
`ESP+0xEC` 不再落在被保护的 31f000 页，于是壳判定"硬件断点没触发"→ 清 Dr → 反调试分支。

## 下一步（未做）

1. 给 guest 侧加日志：int3 handler 返回前后的 ESP/EIP/Dr、`NtGetThreadContext`/`NtSetThreadContext`
   的调用与返回值，确认手机在 int3 后为何回到主栈。
2. 对比 box64（两台设备同版本）对 32 位 int3 的 TF/SEH 上下文恢复差异；重点看 fake stack 的
   `esp` 是否被 `WINE_SS` 或异常帧布局改回主栈。
3. 平板已恢复 fix105(810d81c9)+原 env；手机已恢复 fix104(e92cb6dd)+原 env。

---

# 追加3：手机可用后的启动/流畅优化（2026-11-02 21:0x）

## 修复落地：fix108（BPT 归一化默认开启 + 去掉重调试输出）

- 根因：box64 的 32 位 int3 上报 `trap=0x80`、信号 RIP 指向 int3 **本身**（字节 0xCC）；
  Wine 对所有非单步 trap 按“RIP 在 int3 之后”处理（`ExceptionAddress=RIP-1` 且 `context->Rip--`），
  连退两次 → 壳拿到 `587032`，加 1 仍回到 int3 → Dr 单步永远不出现 → 判定调试器。
- 修复：在 `trap_handler` 的 switch 之前，若 `TRAP_sig != TRCTRAP` 且 RIP 处字节确实是 `0xCC`，
  把信号 RIP 归一到 `RIP+1`（等价于真实 CPU 的“int3 之后”）。默认开启，`WINE_BPT_FIX=0` 可关。
- 同时把 SEGV/STKDUMP/STKDEC/STKPAGE/GCTX/HWBP_PERF/ALLOCPROBE 等高频调试输出全部改成
  `WINE_HWBP_LOG` 才输出（此前每轮解包会往 fd2 写数百 MB）。
- 产物：`build/ntdll_fix108.so` md5 `c3e15c393e3a63ffe2ab22be868f35bc`。

## 手机启动时间（fix108，COMPATIBILITY）

| 项 | 数值 |
|---|---|
| 启动 intent → Ekd5 进程 | **10.4~10.6s** |
| 壳解包（hwbp 内部计时到 1536~1600 arms） | **40.3~43.2s** |
| 解包后到标题 | 0~24s（方差大） |
| 出标题（1280x720） | **51.1 / 74.7s**（另有 1920x1080 的 77.9s） |
| 出标题（1280x720 + PERFORMANCE） | **82.4s（更慢，弃用）** |
| 标题待机 CPU | **0.28 core** |

- 1280x720 相比 1920x1080 明显更好：桌面缓冲/合成量降低，且同样 640x480 的游戏窗口显示得更大。
- 观察到一个不稳定样本：一次解包到 1216 arms 时进程退出（未复现原因；疑似与 vivo 的
  “正在启动/再次滑动响应”系统弹层抢占前台有关）。启动成功率需多做几次确认。

## 当前手机配置（保留）

- 容器：`screenSize=1280x720`（extraData.desktopTheme 同步）、`box64Preset=COMPATIBILITY`、
  `WINE_SS=0xe00`、`WINE_HWBP_SOFT=1`；不再需要 `WINE_BPT_FIX`（已默认开启）。
- ntdll：fix108。游戏/存档未改。
- 平板（fix105 810d81c9）尚未做 fix108 回归——平板当前未连接。

---

# 追加4：平板回归（2026-11-02 21:2x）

## 结果：fix108 在平板上通过，但必须 `WINE_BPT_FIX=0`

| 配置 | 结果 |
|---|---|
| fix108 + 默认（BPT 归一化开） | **失败**：Ekd5 活着但 hwbp_stats 一直为空；日志里只有 1 条 `BPT_RIP_FIX rip=587033 -> 587034 trap=80`，之后卡住 |
| **fix108 + `WINE_BPT_FIX=0`** | **通过**：arms=1536 / faults=44365 / steps=43265 / hits=384，标题 62.9s（`BOX64_LOG=2` 的正常水平），截图确认标题 |

⇒ 这台平板的壳处理器依赖 Wine 原来的“RIP 再退一次”（Eip=587032）语义；
手机的壳处理器需要归一化后的 Eip=587033。**同一颗 exe、同一套 box64，但两台设备的执行路径对
这次 int3 的期望不同**，所以归一化只能按设备开关。

## 当前两台设备的状态

- **平板**：ntdll=fix108(c3e15c39)；容器 ccz22，1440x900、PERFORMANCE、`BOX64_LOG=2`、`WINE_BPT_FIX=0`；
  已跑出标题。配置备份：`unpack_tools/tablet_container_current.json`（回滚用，含修正前的 env）。
- **手机**：ntdll=fix108；1280x720、COMPATIBILITY、`WINE_SS=0xe00`、`WINE_HWBP_SOFT=1`、
  BPT 归一化走默认（开）；启动 51~78s、标题待机 0.28 core。

## 对安装包的含义

如果把 fix108 烤进 APK，默认容器 env 必须按设备给出 `WINE_BPT_FIX`：
- 手机/MTK 路径：默认开（或显式 1）
- 平板/这条 Snapdragon 路径：必须 0
建议在 APK 的默认容器里先设 0（对平板安全），文档里写明手机若卡在启动则改 1；
已有容器（两台设备）不会被覆盖，保持现状即可。
