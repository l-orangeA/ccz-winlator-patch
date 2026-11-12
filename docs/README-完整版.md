# 曹操传加强版 v2.20 —— Winlator 底层补丁存档

本文件夹集中存放为「让加壳的 `Ekd5.exe` 在 Winlator 8.0 平板上跑起来」而**新增/修改**的全部东西。
游戏本体、Wine 原始包、日志等不在这里。

---

## 1. 一句话结论

VProtect 的反调试心跳用的是 **x86 硬件数据断点**（`Dr1 = ESP+0xEC`，`Dr7 = 0x300104` 读写 1 字节），
ARM 上 Wine/box64 没有 Dr 实现。本补丁在 Wine 的 `ntdll` 里用「页保护 + guest TF 单步 + push 栈回滚」
把这个行为软件模拟出来，使壳的反调试通过，游戏可以解包、进主菜单、读档、进战场并操作。

### ★ 当前终态（2026-11-12 23:55 从零重装后核验，手机 `<PHONE_SERIAL>` 在线）

| 层 | 版本 / 文件 | md5 |
|---|---|---|
| APK | `apk\winlator8.0_ccz_fr120.apk`（120 Hz） | `e1bd0b4b…` |
| box64 | 原版 **0.3.0** | `dd297b6d…` |
| ntdll | **fix110b**（`build\ntdll_fix110b.so`） | `eddb74af…` |
| **ntdll（2026-11-12 起，推荐）** | **fix110b + modfix**（`build\ntdll_fix110b_modfix.so`） | **`4e7d876d…`** |
| win32u | **r109g**（`build\win32u_r109g.so`） | `2be1316c…` |
| wineserver | **r112b**（`build\wineserver_r112b.so`） | `9df1d0c3…` |
| env | `device_backup\phone_container_20261112_live.json`（现网，1,205 B） | — |
| 游戏 | v2.20（`/sdcard/ccz`，SV 存档 459 个全保留） | — |

实测：**22.2 s 出标题**（11-12 优化后；11-08 时为 72.1 s）、`arms=1536 / hits=384`；
待机 44.3 fps / p90 17.1 ms，自动战斗 30.1 fps / p90 33.3 ms。
回滚锚点：旧的 `/sdcard/ccz_backup_20261107/` 已不存在；**2026-11-13 00:10 已重建 `/sdcard/ccz_backup_20261113/`**
（817 文件 / 342 MB，`Ekd5.exe` 与 `SV/SV001.E5S` 的 md5 与源目录一致）。

> **★ 2026-11-12 更新：启动时间 72.1 / 44.5 s → 22.2 s（−50%）**
> 根因是 `segv_handler` 里一段**遗留调试代码**：`if (RIP >= 0x10000000) { open/read /proc/self/maps（最多 256 KB）+ 逐行 sscanf }`。
> 壳的 RIP 是 `0x31f014`，**每次页故障都命中**，44,000 次 × ~474 µs ≈ 19.8 s。
> 已用 `WINE_HWBP_MODLOG` 门控（默认关），`init` 段 474 µs → **4.4 µs**。
> 部署 `build\ntdll_fix110b_modfix.so`（`4e7d876d`）即可；源码 `src/fix110b_modfix/`，重建脚本 `unpack_tools/make_modfix_sources_20261112.py` + `build_ntdll_modfix_20261112.sh`。
> 详见 `report/页故障19秒-全方位评估-20261112.md`（§9）与 `report/启动时间-全模块优化计划-20261112.md`。

> **★ 2026-11-12 23:55 设备从零重装并重新部署（用户主动卸载后）**
> 23:22 用户卸载 `com.winlatcn`（应用数据全清，`/sdcard/ccz` 游戏未动）→ 重装 `fr120`
> → imagefs 解包（`.img_version=9`，706 MB）→ UI 建容器 `id=1 / name=ccz` → 写生产 `.container`（886 B，首启后被 app 重写成 1,203 B）
> → `run-as cp` 三件套。验收：四条 md5 全对；干净一轮 **22.2 s 出标题**
> （`ws=1.7 / ekd=6.0 / hwbp t=7.4 / title=21.3`），`arms=1536 faults=44090 hits=384`，可玩。
> 全过程见 `unpack_tools\winlator11\deploy_20261112_2355.md`，标题截图 `tools\deploy_final.png`。

两点容易混：
- §3–§10 是 **11-02 平板阶段**（平板 `<TABLET_SERIAL>`）的历史记录，不是当前主线设备；
  平板/手机 env 不同（平板 `BOX64_LOG=2`，手机 `WINEDEBUG=-all`）。
- Wine 三件套（ntdll / win32u / wineserver）是**单独 push 进手机**的，不在任何 APK 里；
  APK 层当前是 11-10 的 `fr120`（原来的 `fr60_glpin` 已在 11-12 清理时删除）。
  **APK 内置 imagefs 里自带的是 fix108 版 ntdll + 原版 win32u/wineserver**，
  所以每次「重装 APK / 重装系统文件」后，三件套都必须重新推一遍（见 §1 下方重装记录）。

---

## 2. 目录

```
build\     可直接部署的 so（当前部署版：**ntdll_fix110b_modfix.so** / win32u_r109g.so / wineserver_r112b.so）
  ntdll_fix99.so   fix99：11-02 早期部署版（md5 00b7e14d…）；不是当前版
  ntdll_fix110b.so fix110b（md5 eddb74af…）：含 maps 扫描，启动慢 19.8 s，只作回退/对照
  ntdll_fix110b_modfix.so **当前部署版**（md5 4e7d876d…）：fix110b + `WINE_HWBP_MODLOG` 门控，见 §1
src\       改动后的 Wine 源码（相对 wine-9.2 的 dlls/ntdll/unix/）
  *.c               **fix99 时代快照（11-02）**；重建当前版不要用这三个文件
  fix110b\          **当前部署版 fix110b 的源码快照**（2026-11-08 从 WSL 树实读生成）
  signal_x86_64.c.base88   本方案开始前的基线（便于 diff）
patch\     历次打补丁脚本（wsl_fixXX_*.py）与构建脚本；按编号是过程记录，止于 fix99/fix101（11-02）
             fix104 之后的增量补丁在 unpack_tools\（patch_fix108/110/110b/110c/110d.py 等），
             全部就地修改 WSL 的 /root/wine0905 树，不是按序重放链
tools\     小工具
  measure.py       计时：从点开 exe 到标题画面出现（性能回归用）
  set_env.py       改容器 envVars（.container 里的那串）
  set_container.py 改容器顶层字段（box64Preset / screenSize / showFPS …）与 envVars
  run_bat.py       通过 BTRun 启动某个 exe/bat
  wl_ui.py         adb + uiautomator 的 UI 自动化基础库
  dis_pe.py        32 位 PE 反汇编小工具（capstone）
tests\     验证 box64 行为的探针（源码 + 已编译 exe）
  tf3_test.c/.exe  证明 box64 支持 guest TF 单步（陷阱落在下一条指令）
  hwbp_test.c/.exe 硬件断点语义探针
  stkfault*_test.c 栈页保护/异常投递探针
report\    《Wine底层修复-只读栈异常投递.md》完整技术记录（第 14 节是本方案的最终说明）；
             11-12 三份新报告：页故障19秒-全方位评估 / 启动时间-全模块优化计划 / box64重构评估
apk\       现役 fr120、原版回滚 APK、testkey.p12，以及 APK 重建说明（apk\README.md）
deliverables\ 交付整合包：曹操传Winlator整合包_20261112.zip（2026-11-12 重打包）
```

---

## 3. 部署（平板，11-02 历史）

> 本节与 §4 回滚、§7 配置现状都是**平板早期记录**。当前主线是手机，终态见 §1。
> 手机侧部署/回滚用 `unpack_tools\deploy_win32u.py`、`deploy_wineserver.py`、
> `restore_prod_state.py`、`final_restore.py`。

目标机：小米平板 6S Pro，`adb` 序列号 `<TABLET_SERIAL>`，Winlator 8（包名 `com.winlatcn`），容器名 `ccz22`。

```bash
adb push ntdll_fix99.so /data/local/tmp/ntdll.so
adb shell run-as com.winlatcn cp /data/local/tmp/ntdll.so \
    files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so
# 重启容器前清掉残留，否则卡「正在启动…」
adb shell run-as com.winlatcn sh -c \
    "rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*"
```

启动：在 Winlator 的 BTRun 里直接点 `D:\Download\ccz22\Ekd5.exe`。
**不要用 `0game.bat` 包一层 cmd** —— 控制台窗口会盖住游戏窗口并抢焦点。
解包约 45~70 秒出标题画面（视配置/温度）。

容器 envVars 必须包含：

```
WINE_SS=0xe00        # 栈基址平移，决定 watch 落在页内什么位置
WINE_HWBP_SOFT=1     # 启用软件 Dr 模拟
```

推荐同时保留：`WINEDEBUG=-all`、`BOX64_LOG=0`（关日志，实测启动从 68s 降到 46s）。

调试时再加 `WINE_HWBP_LOG=1`，事件日志写 `/home/xuser/hwbp.log`（很啰嗦，一轮可到 800MB）。
**注意：无论开不开日志，`ntdll` 构造器都必须把 stderr 重定向走**（日志→文件，否则→/dev/null），
否则 proot 的管道不排水，容器会卡在启动。

---

## 4. 回滚

平板上的原始备份都在 `com.winlatcn` 的 `files/` 下（另有一份副本见平板侧 `ccz22_patch\`）：

| 文件 | 原始 md5 | 说明 |
|---|---|---|
| `files/ntdll.so.orig` | `dbd94d1f0643cdaa556b22631b36bf0b` | 原始 x86_64-unix/ntdll.so |
| `files/ntdll_i386.dll.orig` | `215bdd9cd02b67178f1efa48aabd0387` | 原始 i386-windows/ntdll.dll（本方案未改它，但历史轮次换过构建） |
| `files/container.bak` | – | 原始 .container |
| `files/imagefs/home/xuser-1/.container.bak2` | – | 新一轮调参前的 .container |
| `.wine/drive_c/windows/wfm.exe.bak` | `7d0f7c1ff50b75bbc56bf0fcc7b52a85` | 原始 wfm.exe（当前被换成了 cmd，用于控制台启动路径） |

回滚一条命令即可（以 ntdll 为例）：

```bash
adb shell run-as com.winlatcn cp files/ntdll.so.orig \
    files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so
```

---

## 5. 这套实现依赖的 box64 行为（换版本必须复核）

1. **guest TF 单步**：在 32 位 guest 上下文里置 `EFL.TF`，box64 会在**下一条指令之后**投一次 SIGTRAP。
   实测见 `tests/tf3_test.exe`。
2. **push 类指令的 ESP 先减**：`push/call` 在内存写故障时，`RSP` 已经是**新的栈顶**，故障地址 == RSP。
   所以不能让它单步重跑（会二次减栈），要把 ESP 回填 `故障地址 + 操作数宽度`（`0x66` 前缀 = 2，否则 = 4）。
3. **异常 err 的读写位不可信**：box64 会把 `push [ebp+x]` 的**读**故障报成 `err=2`（写）。
   判据只能用 `故障地址 == RSP`。
4. `call` 进入 64 位 syscall dispatcher 后会**丢 TF**，所以 call/ret 也走上面第 2 条的「回填 ESP + 整条重跑」。

已验证**不能开**的 box64 开关：`BOX64_DYNAREC_SAFEFLAGS=0`、`BOX64_DYNAREC_BIGBLOCK=2`、
`BOX64_DYNAREC_FORWARD=1024`（任一都会让游戏进程空转不出窗口）。

---

## 6. 重建（2026-11-08 更新：当前部署版 = fix110b）

**旧说明（`cp src/*.c` → `make`）只能重建 fix99，不是当前版。** `src\` 根目录三个 `.c`
停在 11-02 的 fix99 时代；fix110b 是后来在 WSL 树里逐层就地打出来的。

canonical 配方（在 WSL `Ubuntu2004x64` 里执行）：

```bash
bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_fix110b.sh
```

| 项 | 内容 |
|---|---|
| 输入源码 | `src\fix110b\signal_x86_64.c` / `virtual.c` / `thread.c` |
| 源码出处 | `unpack_tools\make_fix110b_snapshot.py`：从 WSL `/root/wine0905` 实读，精确回退 fix110c/d 两处增量（取证：WSL 树 `ntdll.so` md5 == `build\ntdll_fix110d.so`） |
| 源码自检 | `<venv>\python.exe unpack_tools\verify_fix110b_sources.py`（校验源码 md5 + 产物特征串，Windows/WSL 都能跑） |
| 编译树 | `unpack_tools\wine-20240905.tar.gz`（wine-9.2-custom-a48ee6df4a77），`/root/wine0905` |
| configure | **必须 `--with-freetype --with-fontconfig`**（r109g 重配后的 config.h）。旧 `wsl_build0905.sh` 的 `--without-*` 与已失效的 `/mnt/d/unpack_tools` 路径都不适用 |
| 验收 | `dlls/ntdll/ntdll.so` md5 == `eddb74af96587b15589d430b1199a07b`；不一致则**不覆盖** `build\` 里的部署版，先 diff `include/config.h` / `Makefile` / 工具链 |
| 状态 | **已实机验证 PASS（2026-11-08 12:42，WSL `Ubuntu2004x64`）**：`got == want == eddb74af…`；产物 / WSL 树 / 手机实机三方 md5 一致 |
| 安全 | 覆盖 WSL 树现有源码前，脚本先备份到 `/root/ntdll_src_backup_<时间戳>/`（只备份不删除）；实跑前的 fix110d 源码另存 `_归档_20261108\virtual.c.fix110d` |

同类配方：`unpack_tools\build_win32u_r109g.sh`（win32u，已实机验证）。
wineserver r112b 目前只有 `build\wineserver_r112b.so` + `patch_wineserver_affinity2.py`，无 canonical 重建脚本。

`patch\` 里 fix2…fix101 是**过程记录**，不是必须按序重放链；fix104 之后的增量补丁
（`wsl_fix104_defaults.py`、`patch_fix108.py`、`patch_fix110.py`、`patch_fix110b.py`…）
都在 `unpack_tools\`，依赖「当时那棵 WSL 树」的上下文，重建请走上面的快照配方。

---

## 7. 配置现状（2026-11-02 00:5x，平板历史快照）

> 当前手机生产配置见 `device_backup\phone_container_20261106_prod.txt`
> （`WINEDEBUG=-all`、`WINE_SS=0xe00`、`WINE_HWBP_SOFT=1`、BIGBLOCK=3 / FORWARD=1024 / CALLRET=1）。
> 本节数值只适用于平板，不要拿来套手机。

容器 `ccz22`：`分辨率=1920x1080`、`Windows 版本=Windows 11`、`Box64预设=兼容模式(COMPATIBILITY)`、
`BOX64_LOG=2`（已按要求回退，代价是启动慢约 20s）、`WINE_SS=0xe00`、`WINE_HWBP_SOFT=1`。
完整值见 `device_backup/container.current`。
**与补丁本身有关、不能动的只有 envVars 里的 `WINE_SS=0xe00` 和 `WINE_HWBP_SOFT=1`。**

实测（Win11 + 1920x1080 + 兼容预设 + BOX64_LOG=2）：点开 exe 到标题画面 **67.0 s**，
截图见 `report/win11_1920x1080_title.png`。

### 容器外壳 wfm.exe 已换成启动器
`wfm.exe` 是容器 Windows 那边的"我的电脑"/外壳程序。现在换成了 `build/wfm_launcher.exe`
（源码 `build/wfm_launcher.c`）：设置工作目录 `D:\Download\ccz22` 后启动 `Ekd5.exe` 并等待其退出，
即"外壳一起来就是游戏"。容器内保留了：

| 文件 | 内容 |
|---|---|
| `wfm.exe` | 启动器（md5 a2e82ae869fc50091bd339a1541b3326） |
| `wfm.exe.cmd` | 上一版（就是 cmd.exe 的副本，控制台路径用，md5 20ef4f7a…） |
| `wfm.exe.bak` | 最初的原版（md5 7d0f7c1f…） |

回退成控制台版：`cp wfm.exe.cmd wfm.exe`。

注：Winlator 8 启动容器的唯一入口是 **BTRun 选一个文件跑**（容器行没有单独的"启动"）；
所以"默认启动即开游戏"实际生效的两个场合是：①外壳(wfm)启动时；②你自己做的启动方式。
BTRun 直接选 `Ekd5.exe` 依旧是最稳的一条路。

### 直接开游戏的两种操作（已实测）

1. **最省事（3 步导航）**：Winlator → 容器管理 → 容器行右侧菜单 → **管理** → **磁盘 (D:)** → **Download** → 点 `PlayCCZ.exe` 那一行的 ▶。
   `PlayCCZ.exe` 就是 `build/wfm_launcher.exe`（已放到 `/sdcard/Download/PlayCCZ.exe`）：自己切到 `D:\Download\ccz22` 再拉 `Ekd5.exe`，无控制台窗口。实测 **63.4 s** 出标题。
2. **原路径**：… → Download → **ccz22** → `Ekd5.exe` 的 ▶。实测 **67.0 s**。

另：Windows 桌面 `C:\users\xuser\Desktop\ccz\PlayCCZ.exe` 也放了一份。
应用的「快捷方式」列表由桌面/开始菜单生成，**改完要容器重启一次**才会刷新出新条目。

以下数值是调参时测过的，供参考：

补充事实（对调参有用）：
- 游戏**不用** DirectDraw/D3D/OpenGL，运行时只加载 GDI/USER32 等 —— `dxwrapper` 设 cnc-ddraw 还是 wined3d 都不生效。
- 容器 `screenSize` 只决定 X 桌面分辨率，游戏窗口是 640x480 类的小窗口，会被整体放大到屏幕。
- 引擎是较新的 MSVC/UCRT 构建（加载 `ucrtbase.dll`、`uxtheme.dll`、`dbghelp.dll` 等）。
- 标题画面待机时：Ekd5.exe ≈ 50% 单核，Winlator(X server) ≈ 30%，surfaceflinger/composer 也不低。
- 已测：`BOX64_LOG=2 → 0` 启动 68.4s → 45.9s；去掉钩子日志 46.3s（噪声内）；
  `box64Preset=COMPATIBILITY → PERFORMANCE` 46.0s（无实质差别，但可用）。
- Winlator 自带的 `showFPS` 开关在这台机器上不出叠加层（试过，屏幕无变化）。

---

## 8. 平板桌面快捷方式（2026-11-02 已完成）

给 Winlator APK 加了一条静态快捷方式：**长按平板上的 Winlator 图标 → 菜单里多出"曹操传" → 点它直接启动容器 ccz22 并运行 `D:\Download\ccz22\Ekd5.exe`**。
把它拖到主屏就是常驻图标。实测可用（60 多秒进标题）。

- 为什么能覆盖安装不丢数据：原 APK 用 **AOSP testkey** 签名，私钥公开；我们用同一把钥匙重签，证书指纹一致，`adb install -r` 即同签名升级。
- 改动只有 manifest 两处 + `res/xml/shortcuts.xml` + 两条字符串，**没改任何 smali**。
- 全部材料、重建命令、四个构建坑位、回滚方法都在 **`apk\README.md`**。
- 回滚：`adb install -r apk\winlator8.0_original.apk`。

---

## 9. 性能调参实测（2026-11-02）

计时口径：`am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path ...`
→ 截图检测到标题画面（含黑底比例判定），脚本 `tools/measure2.py`。单次约 50~56s，**波动 ±2s**，所以只认 >3s 的差异。

| 配置 | 到标题 | 结论 |
|---|---|---|
| 基线（兼容预设 + 无额外 DYNAREC 变量） | 56.1 s | — |
| `BOX64_DYNAREC_SAFEFLAGS=0` | 不出窗口 | ❌ 破坏反调试（TF/标志语义承重） |
| `BIGBLOCK=2/FORWARD=1024/CALLRET=1` 一起开 | 不出窗口 | ❌ 同上（当时含 SAFEFLAGS，未单独复测 BIGBLOCK=2） |
| `BOX64_DYNAREC_BIGBLOCK=3` | 53.4 s | ✅ 可用 |
| `+ FORWARD=1024` | 52.7 s | ✅ 可用 |
| `+ CALLRET=1` | 53.1 s | ✅ 可用（噪声内，保留） |
| `+ FASTNAN=1 FASTROUND=1` | 不出窗口 | ❌ |
| `+ BLEEDING_EDGE=1` | 53.0 s | 无收益，已撤 |

**当前保留**：`BOX64_DYNAREC_BIGBLOCK=3`、`BOX64_DYNAREC_FORWARD=1024`、`BOX64_DYNAREC_CALLRET=1`
（两次复核 55.7 / 53.5 s）。相对基线的实际收益约 **2~5%**，主要瓶颈仍是「解包期每次访存都要过一遍
页保护 + TF 单步」的异常风暴，属于补丁机制本身的成本。

### 其它排查过、结论是「不是瓶颈」的
- **文件 I/O**：游戏资源在 /sdcard（FUSE）上。实测顺序读 494 MB/s、小文件 262 MB/s，内部 ext4 608 MB/s —— 差距不足以解释卡顿，不值得把游戏搬进容器内部。
- **fsync**：内核 5.15.167，`futex_waitv` 是 5.16 才有 ⇒ fsync 用不了，`WINEESYNC=1` 是正确选择。
- **刷新率**：屏幕跑在 144 Hz（SurfaceFlinger/composer 各吃 ~30% CPU）。`settings put system peak_refresh_rate 60` 在 MIUI 上**不生效**（renderFrameRate 仍 144），已还原。
- **Box64 预设**：兼容/性能两档实测无差别（性能模式可用，无风险）。
- **游戏侧**：`dxwrapper`（cnc-ddraw / wined3d）对这款 GDI 游戏完全无效；应用自带的「帧数限制」只在 DXVK 对话框里，对 GDI 也不适用。

### 9.1 容器「原生设置」逐项实测（2026-11-02）

指标：`tools/measure_render.py` —— 让鼠标持续移动 12 秒，量 `com.winlatcn`（X 服务/合成）与 `Ekd5.exe`（box64 里的游戏）各自吃了多少 CPU。

| 配置 | 到标题 | app CPU | game CPU |
|---|---|---|---|
| 1920x1080 / VirGL / cpu4-7（原始） | 52.9~56.3 s | 4.0~4.3 s | 6.9~7.0 s |
| 1600x900 | 53.3 s | 4.24 s | 7.07 s |
| 800x600 | 57.0 s | 4.07 s | 6.95 s |
| 图形驱动 Turnip（代替 VirGL） | 55.6 s | 4.60 s | 6.57 s |
| WoW64 只钉 cpu7（X3 大核） | 55.6 s | 6.24 s | 5.19 s |
| WoW64 钉 cpu6,7 | 52.8 s | 4.76 s | 6.59 s |

**结论：容器原生设置全都不改变量级**，差异都在噪声内。
- 分辨率完全无关（800x600 与 1080p 的渲染 CPU 一模一样）⇒ 瓶颈不是像素，是**每帧 GDI 绘制被 box64 翻译**的固定开销。
- 图形驱动 VirGL/Turnip 无差别（游戏不走 GL/D3D）。
- CPU 亲和：钉到单核会让游戏自身 −26% 但 X 服务 +45%（帧数变多），总 CPU 不变、启动更慢；钉 6,7 与默认持平。
- 唯一有量级意义的现象：**持续移动鼠标时游戏进程稳定吃 57~58% 单核**——这就是"卡不卡"的实际上限。

已把这些测试值全部还原成原始配置（1920x1080 / VirGL / cpu4-7）。

### 9.2 最小优化 + 异常计数（fix103，2026-11-02）

改动（`ntdll_fix103.so`，md5 `99f59eb69ee7a2babd4a3ce6970f91e8`）：
1. `hwbp_arm_one` 里去掉与 `mprotect_range` 重复的 `mprotect(PROT_NONE)`（每次 arm 省一次系统调用）；
2. 命中路径 `hwbp_step_end_ex(keep)`：本次单步马上就要 `consume` 时不再"复保护→再解锁"，省一对 mprotect；
3. 加了计数器（arm / fault / step / hit），每 64 次 arm 落盘一次到容器内
   `/home/xuser/hwbp_stats.txt`（= `files/imagefs/home/xuser-1/hwbp_stats.txt`）。

**实测一次完整启动（到标题）的事件量：**

```
pid=18785  arms=1600  faults=44381  steps=43281  hits=400
```

推论（这是关键）：
- 整条启动约 53 s，异常事件只有 **4.4 万次**。即便按每次（信号往返 + 2 次 mprotect + Wine 分发）
  100 µs 的高估计算，也只占 **~4.4 s**；按 30 µs 算是 ~1.3 s。
- 也就是说 **我们这套补丁的开销只占启动时间的 3%~8%**；剩下 ~45 s 是 box64 在老老实实跑壳的 VM 解密循环。
- 因此：**Wine 侧再怎么压，天花板也就几秒；方案 1（box64 原生 Dr）同理**——它最多把这 4 万次异常换成更便宜的路径，
  省不出 5 秒以上，却要承担 dynarec 的回归风险。

结论修正：**方案 1 不值得做**（原报告里"收益只在启动期"的判断成立，但"启动期收益"本身也被高估了）。

---

## 10. 自带补丁的安装包（2026-11-02 收尾）

> **2026-11-12 起**：整合包改为「原版 APK + 补丁文件 + 一键部署脚本」的方式
> （`deliverables\曹操传Winlator整合包_20261112.zip`，491.9 MB，不含游戏），不再把补丁烘焙进 APK；
> 下面这个 bundled APK 只作历史与对照。

`apk\winlator8.0_ccz_bundled.apk`（276.8 MB）——装完即用，不需要再往容器拷 ntdll、也不需要配环境变量。

> 注：该包已不在 `apk\` 目录，只在 `_归档_20261113\曹操传Winlator整合包_20261102.zip` 的
> `2-模拟器\` 内（2026-11-13 收尾时从 `deliverables\` 移入归档）；且是 11-02 的 **fix104 版**，
> 落后于 §1 终态（fix110b / r109g / r112b / v2.20），不要拿它当当前交付物。

- ntdll 侧：fix104 把 `WINE_SS` 默认改成 `0xe00`、`WINE_HWBP_SOFT` 默认开启（仍可用环境变量覆盖/关闭）。
- 安装包侧：把 fix104 直接替换进 APK 内置镜像 `assets/imagefs.txz` 的
  `opt/wine/lib/wine/x86_64-unix/ntdll.so`，重打包并用同一把 testkey 签名（证书指纹与原版一致）。
- 已核对：从成品 APK 里抽出的 ntdll.so md5 = `e92cb6dd…`（fix104）；实机 `install -r` 后容器数据保留、56.3 s 进标题。
- 注意：内置镜像只在 `/data` 下没有 imagefs 时（全新安装/换机）才解出来；已有机器请直接拷 `build\ntdll_fix104.so`。

桌面整合包 `D:\ccz_winlator_patch\_归档_20261113\曹操传Winlator整合包_20261102.zip`（817,657,978 B）= 游戏本体（含最新存档）+ 内置补丁 APK + 原版回滚 APK + ntdll 备份 + 文档。**2026-11-13 已移入归档**，当前交付物是 `deliverables\曹操传Winlator整合包_20261112.zip`。

## 目录结构（2026-11-05 整理）
- **游戏本体**：`D:\ccz\曹操传加强版2.2\` 只保留游戏文件（E5/Map/RS/SV/SoundTrk/WAV、*.e5、*.dll、`Ekd5_orig_backup.exe`、`AutoDLL.ini`、`CczCustom.ini`）；活动 `Ekd5.exe` 在设备 `/sdcard/ccz`。
- **非游戏部件**统一在 `D:\ccz_winlator_patch\`：
  - `unpack_tools\`（原 `D:\unpack_tools`：脚本、探针、构建产物、venv）
  - `artifacts\from_game_dir\`（Ekd5.exe-up.txt、脱壳进度日志.md、tr_tail.bin、uris.txt）
  - `artifacts\game_tools\`（随机工具+规则、新建文件夹里的工具/表格）
  - `deliverables\`（当前：曹操传Winlator整合包_20261112.zip；旧的 20261102 包在 `_归档_20261113\`）
  - `_归档_20261113\`（11-02 旧整合包、11-12 的临时测试截图/日志、设备 `/data/local/tmp` 残留打包）
  - `apk\ build\ patch\ report\ src\ tests\ tools\ device_backup\`
- 所有脚本/文档里的 `D:\unpack_tools` 已替换为新路径；`auto_load.py` 改为相对自身目录；venv 移动后 `import PIL` 验证通过。
- **设备**：`/sdcard/ccz` 只放游戏；`/sdcard/ccz_patch` 收 artifacts/probes/game_tools/tmp。手机当前未连接，重连后运行 `tools\device_organize.sh` 完成设备侧整理。

## 版本升级评估（2026-11-05）
- 官方 11.2（`com.winlator`）内置 Wine 10.10 + box64 0.4.4；游戏在无 fix108 时退出；用 8.0 Wine 9.2 混合也起不来（11.2 的启动器/winhandler 与 9.2 不兼容）。
- 11.2 的 Mali/天玑优化在 Vortek/Gladio（D3D/GL），本游戏纯 GDI 用不上；渲染器源码仍是同一套直出 requestRender + 整张纹理上传。
- box64 0.4.4/0.3.8 均破坏 VProtect 反调试（0.4.4 循环、0.3.8 退出）；0.3.0 正常（标题 ~60s，hwbp `arms=1600/faults=44106/hits=400`）。详见 `apk/README.md`。
- 结论：**不升级 11.2，留在 8.0 分支**（当时基线 md5 `5b6c2c9188cd4199b5e574ae7e7ef7e4`；
  后续 8.0 内的迭代见下一节，当前 APK 是 `fr60_glpin`）。

## 渲染组件对比与移植实验（2026-11-06）
源码对照：8.0 `unpack_tools/out8`（jadx）+ `w8src/cpp/winlator/drawable.c`；11.2 `w11src`（官方 11.2 源码）+ `w112_out`。

### 8.0 vs 11.2 渲染差异
| 组件 | 8.0 | 11.2 | 结论 |
|---|---|---|---|
| GLRenderer | 无 swap 设置 | `EGL14.eglSwapInterval(display, 0)` | 已移植测试，**变差**，不采用 |
| Texture | `GL_BGRA` + `UNPACK_ALIGNMENT=4` | 同为 BGRA、alignment 1、owner API | 8.0 已是 BGRA，无需改 |
| 原生 `fillRect` | 每像素 `__memcpy_chk` 拼行 + 每行 `__memcpy_chk`；420 B | 32 位存储拼行 + 每行 `memcpy`；260 B | **可移植**，已搬 |
| 原生 `copyArea` | 仅"全零偏移且等长"才整块 memcpy，否则逐行 `__memcpy_chk`；492 B | 满宽(strides==width)整块 `memcpy`，否则逐行 `memcpy`；324 B | **可移植**，已搬 |
| 其它 | drawBitmap/copyAreaOp/drawAlpha 未动 | — | 风险高、无证据，先不搬 |

### 实验结果
- **swap0（11.2 原样）**：战斗内 4 次采样 p90 91/125/125/141 ms、`>=80ms` 帧 16/24/21/28；final 同口径 p90 58–100 ms、`>=80ms` 5–14。判负，弃用。APK：`apk\winlator8.0_ccz_swap0.apk`（md5 `d088396a…`）。
- **原生光栅移植**：把 11.2 的 `fillRect`/`copyArea` 机器码搬进 8.0 `libwinlator.so`（JNI 签名两侧完全一致；memcpy 用 20 B cave 转 `__memcpy_chk`，`x3=len`）。战斗/滚屏/对话框/攻击动画渲染正确；战斗内 app CPU 实测约 0.21 core（final 历史同类战斗约 0.31，非严格同场景）；帧间隔与 final 在噪声内。
  - APK：`apk\winlator8.0_ccz_rast11.apk`（md5 `c59b2bd36487d54732c8ae1ec275820a`），当时已装，现已被 `fr60_glpin` 取代。
  - 回退：`adb install -r apk\winlator8.0_ccz_final.apk`。
- **判定**：渲染链本身不是 CPU/GPU 瓶颈；战斗帧率由游戏 GDI 出帧节奏（box64）决定，GL 线程/ X 线程均有余量（GLThread ~17%、X 线程 ~7%，runqueue <5%）。渲染侧可拿的只有光栅 CPU，帧间隔收益不明显。

### 工具教训
- smali 的 `if-gtz` 是"**大于零跳转**"，不是"跳到零"；写探针时用它当"除数为零则跳过"会在上传计数为 0 时执行除法，把 GLThread 除零打崩，症状表现为"探针不写文件"，极易误判为"代码没执行"。


### 2026-11-06 晚：60fps 帧率提示 → 大核 DVFS 提升
- `XServerView.surfaceCreated()` 加 `Surface.setFrameRate(60f, 1)` 后，大核 cpu4/5/6 中位频率 **600 MHz → 2.0–2.65 GHz**；战斗空闲 CPU 游戏 **34%→22%**、App **30%→8%**；自动战斗 30 秒内容 29.5fps、p90 49.9ms。
- 残留卡顿起点仍见 400–500 MHz（游戏 1ms 小突发之间核心掉频再爬升），游戏线程 runqueue ~9.8%。
- 协议层取证：稳态 MIT-SHM PutImage 92 次/秒（均 28.5k 像素）+ SetClipRectangles 388 次/秒；逐像素转换只占 2–3%，成本在每次 blit 固定开销与低频率。
- 产物：`apk\winlator8.0_ccz_fr60.apk`（md5 `efe61e1e6c9031e4aca662289ffec3a0`），中间包。

### 2026-11-06 深夜：GL 线程钉大核
- `GLRenderer.onSurfaceCreated` 调 `Runtime.exec(["/system/bin/taskset","-p","f0",myTid])`（f0=cpu4–7），SELinux 放行。
- ftrace：GLThread 从 cpu0–3 移到 **cpu4–7**，runqueue 仅 ~1%（原 3.4%、14–17 ms 等待）。
- 滚动 6 秒拖动：游戏 **2.90→2.17 s**、App **2.90→2.54 s**（相对 fr60 包同场景）。
- 产物：`apk\winlator8.0_ccz_fr60_glpin.apk`（md5 `a923fd6e6ff5dd0c45af69005fc7a42e`），**当前 APK，见 §1 终态表**。
- 负结果：Wine `ClientSideGraphics=N` → Winlator X 服务器花屏，已回滚。

## 2026-11-06 完整攻坚日志
- 详见 `2026-11-06_渲染与卡顿攻坚日志.md`（源码对照/23 分钟 ftrace/协议层直方图/有效与失败实验/构建步骤/文件位置）。
- **日志已整理（2026-11-08）**：原 §32 / §33 各两段同号章节已合并去重（编号唯一、事实零删减），
  末尾换行补齐、顶部加阅读导航；原文归档在 `_归档_20261108\2026-11-06_渲染与卡顿攻坚日志.md.bak`。
  结论链：**§31 终止判决 → §32 fix110c/d 边界 → §33 终态确认 → §34 当前手机最优态**。
  整理脚本 `unpack_tools\consolidate_log_32_33.py`（幂等，重跑即复验）。

## 组件优化调研（2026-11-06 晚）
- 详见 `report\流畅度与画范围-组件优化-研究.md`。4 路源码精读（Java 管线/原生 C/Wine GDI 全路径/box64 dynarec）后的结论：
  画范围的大头在游戏进程侧 win32u.so 像素循环被 box64 翻译执行（0.42s vs App 侧 0.06s 上限）；
  唯一量级杠杆 = 把 Wine 热循环改成显式 memcpy/memset（box64 直调原生 NEON）；另确认 388 次/秒
  SetClipRectangles 的来源是每帧 GetDC 的 `update_dc` → wineserver 往返 + 客户端组装（服务器端
  opcode 59 直接丢弃）。

## A0 实测完成 + 重大发现（2026-11-06 深夜）
- **`WINEDEBUG=+dib` 是启动杀手**：手机容器里遗留的上次调试 env 让壳连续 3 次启动失败
  （arms 停在 960/1152），换 `-all` 后一次成功（82.7s）。手机容器已固化 `WINEDEBUG=-all`
  （快照 `device_backup/phone_container_20261106_prod.txt`）。部分「启动偶发黑屏」与此相关。
- **A0 ROP 直方图**（26,649 blits，报告 §8）：SRCCOPY 83.3%（已快）；PATCOPY 10.9% → memset
  rep stos 慢路径（改 libc，A1）；DSa/SRCAND/SRCPAINT 4.2% → 逐像素（加宽，A3）；
  **DSTINVERT 0 命中 → 原计划 A2 取消**；1x3 像素 blit 有 18,973 次 → A5（update_dc 缓存）坐实；
  上传 93% 是 992x736 整窗口（24.3 亿 px，维持不动）。
- B1 wineserver 钉核：shell/run-as/su 三路全被权限挡死 → 第二轮走 smali 或 Wine server/main.c 补丁。
- C2 hotpage：box64 0.3.8 无日志输出，无法排查 → 搁置。
- 工具：`unpack_tools/a0_rop_trace.py`（采集/恢复）、`a0_analyze.py`（解析）、`b1_pin_test.py`（钉核实验）；
  原始数据 `unpack_tools/a0_trace/hwbp_a0.log.gz`（4.8MB）。
- 下一步：第二轮重编 win32u（A1+A3+A6 一批），在 WSL `/root/wine0905` 做。

## 第二轮 win32u 重编收官（2026-11-07 凌晨）
- **兼容基线 r109f 已上机固化**（md5 `d4e71a61…`，`deploy_win32u.py orig` 一条命令回滚）：
  重编 win32u 的两个必要条件——`--with-freetype`（freetype-less 构建在建字体时崩）+ `-DNDEBUG`
  （assert 在退化矩形 abort）。canonical 配方 `unpack_tools/build_win32u_r109f.sh`。
- **A1（memset→libc）同协议 A/B 判零收益**（0.447 vs 0.442 s/次，±1% 噪声内）：画范围 CPU 大头是
  SRCCOPY（83%，已原生）+ 游戏逻辑 + 翻译税，填充循环不是瓶颈。A3/O3 由此不再单独尝试。
- 组件级光栅优化到此到头；剩余方向（A5 update_dc 缓存、wineserver 钉核、box64 rep-movs）单项收益
  都在 ~5% 以下，需单独立项。
- 踩坑全记录见日志 §14/§15：启动检测假阳性（壁纸误判）、壳禁用一切日志调试（只能二分 A/B）、
  游戏窗口几何多变（toggle 需按窗口边界自适应 + CPU 自校验）。

## 字体修复（r109g，2026-11-07）
- r109f 反馈「字体不对」（宋体点阵变粗体描边）。根因：重编版缺 **fontconfig**（原配置
  `--without-fontconfig`）→ 字体族匹配回退到错误文件。原版二进制含 Fc* + libfontconfig.so.1，
  imagefs 里 ARM64 libfontconfig + /etc/fonts 都在（box64 原生桥接，原版运行时是活的）。
- 修复：r109g = 重编加 `--with-fontconfig`（md5 `2be1316c…`），读档对话框截图字节数与原版
  当年完全一致（643271）= 字体渲染复原。**已固化手机**。
- **重编 win32u 三条件定版**：`--with-freetype` + `--with-fontconfig` + `-DNDEBUG`；
  canonical 配方 `unpack_tools/build_win32u_r109g.sh`，部署/回滚 `deploy_win32u.py <build|orig>`。
- 想换 Android 原生字体（NotoSansCJK 等）：数据级操作（字体文件进 imagefs + /etc/fonts/fonts.conf
  加条目），无需再编 wine。详见日志 §16。

## 第二梯队实验（2026-11-07 收官）
- **B1 wineserver 钉核 ★ 已固化（r112b）**：env 不穿透 wineserver（environ 取证，启动器给的是精简
  env），改为 wineserver 启动时读 `/home/xuser/.container` 的 cpuListWoW64 自钉——亲和 0-7→4-7
  验证生效。wineserver 自身 CPU **-26%**（0.226 vs 0.305）；游戏侧收益被 boot 间方差淹没无法证实
  （机制上 ~1-2%）。补丁 `patch_wineserver_affinity2.py`，产物 `build/wineserver_r112b.so`，
  回滚 `deploy_wineserver.py orig`。
- **最终状态战斗实测**：44.3 fps、帧间隔中位 16.6ms、**p90 17.1ms**（历史最好档）。
- **box64 重编路线正式判死**（重启后干净复测）：之前"138s 判死"是卡死窗口假象——r111 实际能跑
  （78.9s 出标题），但同窗口 A/B：原版 ≈0.59、r111=0.849（**慢 44%**，作者配方差距）、
  FASTREP 开关零差异（热路径 rep 拷贝都是 <128 字节小块，够不着原生阈值）。已回滚原版 box64。
  产物留档 `build/box64_r111.so`，补丁可复用（`patch_box64_fastrep.py`）。日志 §19。
- **重大运维发现**：多轮部署实验后出现内核/FUSE 级确定性卡死（连已知良好组合都 100% 失败、停滞点
  计数相同）→ **adb reboot 复位**（重启后需人工解锁 FBE 锁屏才能 run-as）。以后卡死簇出现即重启，
  不要在卡死状态下下结论。详见日志 §17.4/§18。
- 设备最终固化：原版 box64 + r109g win32u + r112b wineserver + prod env。

## ② box64 0.4.4 兼容性深挖：终止结论（2026-11-08，日志 §21-34）

> **研究重启（2026-11-08 12:4x）**：`report\box64-0.4.4-重构升级方案.md` 实读源码与死亡现场后，
> 发现「box64 侧 r112d 与 wine 侧 fix110c/d **从未交叉验证**」的实验矩阵空洞（r112d 那轮配的是
> fix110b，死在镜像视图；fix110c/d 那轮配的是原版 box64，hits=25）。本节结论降级为
> **上一轮实验矩阵的结果**，P0 组合矩阵补全后再判路线是否终止。原结论与资产全部保留。
> **P0 已于 11-08 13:1x 实跑完毕**：交叉组合未通关（`hits` 恒为 25），且发现上一轮 r11x
> 二进制构建配置错误（`ARM_DYNAREC=OFF` / linker 为空，容器内根本起不来）。按止损条件暂停升版，
> 维持 0.3.0 终态；设备已恢复并复核。详见 `report\box64-0.4.4-重构升级方案.md` §7。
> **13:2x 根因定位（§8）**：`hits=25` = 前 25 次命中后 TF 清不掉 → guest 永久单步；
> 已排除 Dr/CONTEXT 污染、页保护冲突主因、defer_signal。一次 EFL 桥接补丁因作者版
> `BOX32=OFF`（`signal32.c` 未编译）作废并归档。设备本轮未动，仍是生产态。
> **13:4x 探针与修复（§9）**：只读探针坐实「Wine 清 TF 生效，但 0.4.4 双层 `old_eflags`
> 把 emu EFLAGS 恢复回去」；内外两层最小修复 E7/E9 实跑均无效（`hits` 恒 25），
> 该方向证伪。按止损条件暂停；设备已恢复生产态。
> **13:4x 源码终审（§10）**：确认 `ACCESS_FLAG(F_TF)` 读 `emu->eflags`、宿主 `regs[26]`
> 只在 3 个 dynarec 特殊分支写回；E7/E9 补丁点分别冗余/被 siglongjmp 绕过。
> 唯一未在正确配置下验证的机制是 0.4.4 新增的 `no_tf`，下一步 E10 单变量定性。设备未动。
> **13:5x E10 终局（§11）**：删 `no_tf` 反而 +45 s 早死（连 stats 都没有）；
> B/E4/E7/E9/E10 单变量链全灭，按止损条件**正式终止 0.4.4 升版**，维持 0.3.0 终态。
> 设备已恢复生产态，四件套 md5 复核通过。
> **13:5x 重构可行性研究**：`report\box64-0.4.4-重构路线可行性研究.md` ——
> A 信号路径重构 ★★★（A0–A6 改造点）、B 局部 backport ★（硬阻塞：生产 0.3.0 无可重建源码树）、
> C 架构替代 ✗。纯研究轮，设备未动。
> **14:0x A0–A3 实施（可行性研究 §10）**：E11/E12/E13 与 B 的 final stats 逐字相同
> （`arms=2048 faults=18091 steps=16790 hits=25`），「改 EFLAGS 生命周期」证伪；
> 按止损暂停 A4/A5，设备已恢复生产态。
> **14:2x A3 取证（可行性研究 §11）**：探针证明信号走对函数、defer 零命中、A3 的宿主
> `xFlags` 写回生效，但 **siglongjmp 恢复 x26 又把它覆盖回 TF=1**（解释 3 成立）；
> A3B（绕过 siglongjmp）因寄存器未同步 +45s 早死。下一步 A3-full 需先评估
> `copyEmu2USignalCTXreg` + `native_epilog` 的 PC 语义。
> **14:3x A3-full 终局（可行性研究 §12）**：A3B 只同步 xFlags → +45s 早死；
> A3C 完整同步 + `native_epilog` → 存活但黑屏无 stats（宿主栈帧未重建）。
> A 路线 E11–E16 六个单变量实验全灭，**正式终止**，维持 0.3.0 终态。
> **14:3x A3D 与魔改边界（§13）**：A3D（全量同步 + 原 PC）仍黑屏无 stats；
> A3B/A3C/A3D 三种替换 siglongjmp 全灭。`signals.c` 层 8 个实验到头，
> 下一步只能改 `dynarec.c` 的 `EmuRun` 恢复路径（2–5 天、高风险）。设备已恢复。
> **14:3x 开工改引擎（§14）**：用户拍板，目标提速/流畅。实施 X1 `arm64_set_flags.S` +
> X2 CMake + X3 `EmuRun` setjmp 分支同步 x26；日志 §35 已记录。
> **14:4x 引擎版结果（§14.4）**：`box64_ENGINE.so` `054efb71` 实跑仍 `hits=25`，
> TF/x26 假设证伪；但查到 `ffce115f` 在 WCTX 0 次、Wine 收到 803 次
> → **Dr 值传递被污染**，是更根本的根因。下一步查 32 位 CONTEXT 桥接（§14.5）。
> **15:0x Dr 污染定位（§15）**：探针证明 `ffce115f` 来自 `get_thread_wow64_context`，
> 且 `GCTX2` 里 server 端已是 `ffce115f`（144 次）；壳的 32 位 WCTX 里 0 次
> → 污染在**无日志的 64 位 Set 路径**。下一步加 `WCTX64` 日志定性。
> **15:1x WCTX64 定性（§16）**：64 位 Set 里含 DEBUG 位的调用 Dr 全 0、无 `ffce115f`
> → 排除。根因收窄到 `restore_context` 用 64 位布局读 32 位 CONTEXT（Dr1 落在 FloatSave 区）。
> 下一步 W1：32 位 guest 跳过 Dr 覆盖，验收 E19 `hits≈384`。
> **15:1x W1 结果（§17）**：`hits=25` 不变，`w1_probe.log` **文件未创建** → RCTX32 分支
> 从未执行，32 位信号返回不走 `restore_context`，该路径排除。剩余唯一无日志的缓存写入是
> `get_thread_wow64_context:1413-1418`，下一步 GCTXW 探针。
> **15:2x GCTXW/SETPROBE（§18）**：Get 的 `before==ctx`（不自写）、Set（32/64 位）
> 都没有 `ffce115f` → 所有已知写入点排除。剩余假设：TEB 布局错位 / 未覆盖 API / 初始垃圾。
> 下一步比对 0.3.0/0.4.4 的 `amd64_thread_data()` 地址计算。
> **15:2x 静态终审（§19）**：全树 `dr1` 写入点全部排除；`amd64_thread_data()` 是
> `ntdll_get_thread_data()->cpu_data` + 固定偏移（dr1@0x2f8），只剩「TEB/cpu_data 尺寸
> 或对齐不匹配」这一种解释。下一步打印地址核对 + guard 字段验证。
> **15:2x 终审（§20）**：`C_ASSERT` 保证布局契约、box64 不提供 TEB → 布局假设也不成立；
> `ffce115f` 只能来自非显式写入，需 **gdb 硬件观察点**，proot+Android 无权限附加。
> **到达工具链边界**：建议接受 0.3.0 / 上报上游 / 换可 root 的调试环境。
> **15:2x 替代路线（`report\box64-0.4.4-替代路线研究.md`）**：不修根因、改走
> A「Dr 白名单过滤」（半天，★★★）或 B「换 Wine 10.10 新 wow64 架构」（1–2 天，★★）；
> C backport / D patch 壳 / E 双引擎均不推荐。下一步执行 A，E22 验收 `hits≈384`。
> **15:3x A 失败 / B 启动（§9–§10）**：A（`ntdll_drf.so`）E22 `hits=25`，白名单过滤证伪；
> 关键纠正「设备 Wine 是 x86-64 由 box64 翻译」，Wine 10.10 可 WSL 原编译，
> configure 成功、后台 make 中。编译完替换测试 E23。
> **15:3x B 结果（§11）**：Wine 10.10 编译成功（17s），但 E23 三件套替换后**进程直接退出**
> —— 缺 `libwine.so`、新 wow64 PE 模块、Wine 10 前缀。需**整套替换 + wineboot 重建**，
> 1–2 天工程。设备已恢复 0.3.0 生产态。
> **15:3x Winlator 11.2 路线（`report\winlator11-可行性.md`）**：官方 v11.2.0 明确内置
> **Box64 0.4.4 正式版**；包名 `com.winlator` 与现有 `com.winlatcn` **可共存**；
> 设备 arm64-v8a / Android 15 满足。APK 已后台下载（157,549,075 字节）。
> 下一步：安装 → 新建容器 → 实测，零代码改动、可卸载回退。
> **15:5x 迁移清单（`report\winlator11-壳适配迁移清单.md`）**：8 项壳适配（fix110b/c/d、
> env、Box64、分辨率、游戏/存档）逐项列明；策略「裸跑 → 迁 env → 迁 ntdll」，
> ntdll 能否直接搬取决于 v11.2 的 Wine 版本。APK 换 gh-proxy + 8 线程分段下载中。
> **15:5x v11.2 实测（§6）**：装好、容器建好、F: 盘挂载、**notepad 32 位正常**；
> **关键**：v11.2 用全新 `wow64cpu + wow64win` 架构，不再走 Wine 9.2 的 Dr 路径。
> 壳 `Ekd5.exe` 启动即退出（无进程），属新架构下的壳兼容问题，下一步抓退出码/换预设。
> **16:1x 容器对齐（§7）**：env 全套 / cpuList 0-3·4-7 / COMPATIBILITY / D 盘 / wincomponents
> 已与老 ccz 完全一致；virgl、wined3d、pulseaudio 因 v11.2 移除旧驱动无法照搬。
> **鼠标模式（§8）**：`Show Touchscreen Controls=true` 是拖拽层，需取消；该层会拦截
> adb 点击，下轮用 TAB+SPACE 键盘导航或手动点一下。
> **16:2x 鼠标已解决（§9）**：真正开关是老 prefs 里的 `move_cursor_to_touchpoint=true`
> （不是 Show Touchscreen Controls）。写入 v11.2 prefs 后实测单击即打开，跟随点击生效。

- **结论：终止，不再投入。**（上一轮实验矩阵口径；状态见上方注记） 六层修复（①毛刺吞掉 r113 ②KUSD 冲突 fix110b ③预存在映射遗忘 fix110b
  ④⑤镜像视图未建立 / 多阶段壳 ⑥壳入口无映射与 SIGTRAP 不可延迟）穷尽后，实验矩阵
  （作者版/自编译版 × fix108/fix110b × 纯 env/各类日志 × FUSE/ext4 × BIGBLOCK/RDTSC/INTERP_SIGNAL）
  **均无法稳定通过解包**；唯一一次存活（双通道日志）1/1 不可复现。
- **根因**：0.4.4 特有的 `defer_signal`（延迟 SIGTRAP → hwbp 精确 RIP 丢失）、`signal32.c`
  （独立 32 位 sigframe，EIP/EFL/ERR/TRAPNO 格式差异）、`protectDB/unprotectDB` 的
  PROT_DYNAREC 页保护（与 hwbp 的 mprotect 争同一物理页）三者与 hwbp 模拟系统性冲突，
  需 box64 上游重构 32 位 wow64 信号路径，黑盒补丁不可解。
- **进展**：fix110c（跳过 PROT_NONE 保留区）/ fix110d（跳过低地址）让壳**首次在 0.4.4 上完整解包**
  （arms=1600），但 Dr 命中率仅 25（正常 400），anti-debug 循环冻结。
  fix110b 在 0.3.0 上无回归，**保留在生产态**。
- **重启条件**：box64 上游重构 32 位信号路径，或拿到 Winlator 作者的 0.4.4 编译补丁集。
- **终态**：0.3.0 + fix110b + r109g + r112b + 生产 env + 游戏 v2.20（PC 已同步换装），
  72.1 s 出标题、arms=1600。
- **资产**：`build\box64_r112c_044.so` / `box64_r112d_044.so` / `box64_r113_044.so`、
  `build\ntdll_fix110b.so` / `fix110c.so` / `fix110d.so`、
  `unpack_tools\patch_fix110*.py` / `patch_box64_044_*.py` 与死亡现场日志，全部留档。

## 后续大幅提速候选（2026-11-07 评估存档，详见日志 §20-26）

> **状态注记（2026-11-08）**：以下是 11-07 的判断依据存档，**不是待办清单**。
> ② 路线已于 11-08 终止（结论见上一节）；当时推测的「时序敏感竞态」被后续六层深挖推翻，
> 实际是信号基础设施的结构性冲突。③ 已结案（零收益），④ 未动。

1. **② box64 0.4.4 路线（11-07 评估 → 11-08 终止）**：
   - 已修 3 层：毛刺吞掉（r113 glitch fall-through）、KUSD 冲突（fix110b 视图注册跳过）、
     预存在映射遗忘（fix110b virtual_init 重扫描）——**0.3.0+fix110b 无回归（保留）**
   - 当时现状：0.4.4 纯 env 下壳仍 +23s 死亡；重日志下壳能活 → 当时判为「时序敏感竞态」（**已推翻**）
   - 当时计划的下一层：壳退出检测点定位（t≈20s）—— 已由日志 §21-34 执行完毕并判死。
2. **③ ext4 迁移已结案（零收益）**；**④ 纯 32 位路线**未动（月级）。
3. 免费项：散热背夹+插电。
- **最终态**：0.3.0 + fix110b ntdll + r109g win32u + r112b wineserver + 生产 env + **新游戏 v2.20**
  （PC 已同步）。战斗实测：待机 44.3fps/p90 17.1ms；自动战斗 30.1fps/p90 33.3ms。

## 清理与输入设置（2026-11-07）
- 删除本战役死产物（约 460MB）：build 的 r109/r109c/r109d/r109e/r109f/r110/box64_r111/wineserver_r112、
  `phone_diag_bpt2.txt`(374MB)、`ph_b64.log`(81MB)、a0_trace 临时截图/json。保留：ntdll 全历史链、
  r109g/r112b（部署版）、tar 备份、A0 数据（`a0_trace/hwbp_a0.log.gz`）、字体取证截图。
- 鼠标已改回**原生拖拽式**（`move_cursor_to_touchpoint=false`；之前自动化时改成 touchpoint 模式）。
  工具 `unpack_tools/set_mouse_drag.py`（true/false 可切；重启 App 生效）。
  **注意**：跑 `boot_test/panel_retry` 等自动化脚本前需临时改回 true（auto_load.set_pref 会自动设）。

## 游戏更新换装（2026-11-07）★
- 网盘新包 `曹操传加强版v2.20.rar`（184MB，11-05 下载）→ PC 解压 → 换入 `/sdcard/ccz`。
- 变更集：**Ekd5.exe**（新壳/新游戏逻辑）、AutoDLL.ini、Imsg.e5、Logo.avi、Itemicon.dll（新增）、
  36 个 RS 战役脚本（R_XX/S_XX = 更新说明的 bug 修复战役）。存档 SV*.E5S 零风险（新包无同名存档）。
- **兼容性验证通过**：新壳在 **fix110b**+r109g+r112b 上直接跑通——64.2s 出标题、arms=1536/hits=384
  完美档案；读档 No.018 → 战斗 → 范围开关全正常（存档完好，SV 共 456 文件保留）。
- **回滚锚点**：`/sdcard/ccz_backup_20261107/`（整目录）；PC 侧 `build/newgame/`（新包解压）、
  `build/newgame.rar`。
- 教训补充：换装后 3 连败=手机卡死簇（当日 30+ 次启动累积），**adb reboot 后一次成功（53.9s）**——
  又一次验证"卡死簇→重启复位"的处理流程。
- 工具沉淀：`boot_test.py`（自适应验收）、`deploy_win32u.py`（部署/回滚）、`sysnum_diff.py`/`symdiff.py`
  （二进制差异取证）、`build_win32u_r109f.sh`/`patch_win32u_r110.py`（构建配方）。


## 2026-11-09 补记（11.2 攻坚结论 + 生产态基线）

> 本节只记结论与指路，完整实证见 `report/wow64-壳兼容-魔改重构研究.md`、
> `report/长线流畅度与帧率-源码分析.md` 与攻坚日志 §36。

- **设备归属**：8.0 生产态在手机 `<PHONE_SERIAL>`（MT6985 天玑 9200 / Mali-G715 MC11 / Android 15）；
  11-09 的 11.2 攻坚在平板 `<TABLET_SERIAL>`（SM8550 / Adreno 740，包名 `com.winlator`）。
  **两者不是同一台、不是同一个 app、不是同一套容器配置**。§1 终态数据来自天玑手机。
- **11.2 结论**：容器秒退已修（home 骨架残缺），补丁已真正部署，但 v3–v8 / A1 / 开关 A/B /
  R-B / R-E 共 14 个单变量全灭；根因是 Wine 10 new wow64 的 32 位异常恢复/机器帧重建，
  属天级工程。**维持 0.3.0 + 8.0 生产态**。
- **生产态可信基线**（60s 自动战斗）：avg 39.7 fps、p50 16.63 ms、p90 49.88 ms、p99 266.05 ms、
  ≥100 ms 135 次；`frameRateOverride` 全程 60 Hz（系统 144 未落到 app）。
- **★ 测量铁律**：`SurfaceFlinger --latency` 取 `(BLAST)` 子层并过滤 LONG_MAX 哨兵
  （`1e12 < v < 1e18`）；`gfxinfo` 是 UI 层，不能当游戏帧率。此前「0 帧」结论作废。
- 备份 `_归档_20261109/backup_20261109.zip`；中间产物按惯例只移不删，见 `归档清单_20261109.md`。

## 2026-11-12 深夜：设备从零重装 + 整合包重建

> 前情：用户 23:22 主动卸载 `com.winlatcn`，应用数据全清；`/sdcard/ccz` 未动。
> 23:37–23:55 完成重装与部署，全过程见 `unpack_tools/winlator11/deploy_20261112_2355.md`。

| 项 | 值 |
|---|---|
| APK | `apk\winlator8.0_ccz_fr120.apk`（`e1bd0b4b…`，276,863,399 B） |
| imagefs | 解包 706 MB，`.img_version=9`；**内置 ntdll 是 fix108（`c3e15c39`）**、win32u/wineserver 为原版 |
| 容器 | `id=1 / name=ccz`，`.container` 886 B（生产 env + virgl/glVersion4.0 + `drives=D:/storage/emulated/0`） |
| 三件套 | `4e7d876d`（ntdll modfix）/ `2be1316c`（win32u r109g）/ `9df1d0c3`（wineserver r112b），权限 `-rwxrwx--x` |
| box64 | 启动时自动解出 0.3.0（`dd297b6d`） |
| 验收 | **22.2 s 出标题**（ws 1.7 / ekd 6.0 / hwbp t 7.4 / title 21.3），`arms=1536 faults=44090 hits=384` |

- 已知坑：APK 内置 imagefs 是 fix108 版，**重装 APK 或重装系统文件后必须重推三件套**。
- 遗留：`/sdcard/ccz_backup_20261107` 已不存在 → **11-13 00:10 已重建为 `/sdcard/ccz_backup_20261113`**（342 MB / 817 文件）；`/data/local/tmp` 的 1.1 GB 残留 **11-13 00:09 已清**（清单与打包留档在 `_归档_20261113\`）。
- 整合包：`deliverables\曹操传Winlator整合包_20261112.zip`（491.9 MB：fr120/原版 APK + 三件套 + container.json + 部署脚本 + 文档；**不含游戏**）。
