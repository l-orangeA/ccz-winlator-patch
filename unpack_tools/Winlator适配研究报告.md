# 曹操传加强版 2.2 → Winlator 适配研究报告

日期：2026-11-01
对象：小米平板 6S Pro（adb 序列号 TABLET_SERIAL_REDACTED）上的 Winlator，以及 D:\ccz\曹操传加强版2.2

---

## 一、结论摘要

1. **原版加壳 `Ekd5.exe` 在 Winlator（Wine 9.2、WOW64 + box64）下约 2 秒后以退出码 0 退出**：不弹窗、不留日志、不写任何数据文件、不产生崩溃转储。
2. **无壳重建版 `Ekd5_unpacked.exe` 在同一环境下以 `0xC0000005` 崩溃**，崩溃点 `0x46C0C9` 与 PC 上的 `Ekd5.exe-up.txt` 是同一个地址（引擎对话框过程的空指针）。
3. 能触达的模拟器配置层（系统版本、box64 预设、DX 包装器、分辨率、esync、音频、WOW64 字段）逐个单变量试过，**均不改变结果**。
4. Winlator 7.1（官方 v7.1.0）自带的 Wine **同样是 9.2 (Custom)**，`.container` 的 `wow64Mode:false` 被忽略，**并未得到真正的 32 位 Wine 栈**；原版 exe 在 7.1 里同样 EXIT 0。
5. 因此“仅靠改模拟器配置让加壳原版跑起来”这条路，在当前可拿到的 Winlator 8.0 / 7.1 上**没有走通**。剩下唯一没验的模拟器侧变量是更老的 Winlator 5.0（Wine 8.0.x，可能是 pre-WOW64 的 box86+wine32 老栈）。

---

## 二、环境清单

| 项 | 值 |
|---|---|
| 平板 | Xiaomi Pad 6S Pro（24018RPACC），Android 15（SDK 35），arm64-v8a，8 核，Adreno 740 |
| Winlator（主） | `com.winlatcn` 8.0（versionCode 18，**debuggable**） |
| 容器 | `ccz22`：1024x768、Turnip 24.1.0-22.2.5、CNC DDraw 6.6、PulseAudio、box64 0.3.0（COMPATIBILITY）、Wine 9.2 (Custom，WOW64)、`D:` = `/storage/emulated/0` |
| 测试副 | 官方 Winlator 7.1（`com.winlator`，已在收尾时卸载） |
| 游戏 | Windows 版 Ekd5 引擎（曹操传加强版 2.2），32 位，VProtect 加壳 |
| 原版 exe | `Ekd5_orig_backup.exe`，43,509,495 字节，md5 `a4dceaf9f4f29e6c66fcbc783f7e50c6` |
| 无壳重建 | `Ekd5_unpacked_installed.exe`，2,396,160 字节，md5 `a305587283e63e6cb9eae173e404eb31` |

---

## 三、可重复的取证方法（本次建立的）

### 3.1 主方法：容器文件管理器 BTRun（Winlator 8.0）

1. 把批处理放到游戏目录，例如：
   ```bat
   @echo off
   cd /d D:\Download\ccz22
   echo ==== START ==== > D:\Download\ccz22\run.log
   Ekd5.exe >> D:\Download\ccz22\run.log 2>&1
   echo EXIT %ERRORLEVEL% >> D:\Download\ccz22\run.log
   ```
2. Winlator 8.0：容器菜单 →「管理」→ 文件管理器 → `D:` → `Download` → `ccz22` → 点 `0run.bat` 条目的「运行」按钮（不是双击）。
3. 读平板 `/storage/emulated/0/Download/ccz22/run.log`。

> 这样完全绕开 Wine 界面点击，批处理的 stderr 能抓到 box64 的日志。

### 3.2 备用法：把 `wfm.exe` 换成 `cmd.exe`（得到交互式 Wine 控制台）

在任一 debuggable 的 Winlator 前缀里（本次在 7.1 上验证）：

```sh
cd files/imagefs/home/xuser-1/.wine/drive_c/windows
cp wfm.exe wfm.exe.bak
cp system32/cmd.exe wfm.exe
```

重启容器后，Winlator 启动的“文件管理器”变成 cmd 控制台；用 adb 键盘即可输入：

```sh
adb -s TABLET_SERIAL_REDACTED shell "input text 'D:\ccz22\0run71.bat'"
adb -s TABLET_SERIAL_REDACTED shell "input keyevent 66"   # Enter
```

实测可直接执行 `echo > 文件`、跑批处理、`dir`、`set` 等。
注意：`wfm.exe.bak` 是还原用的备份。

### 3.3 不可用的取证通道（已排除）

- `WINEDEBUG=+loaddll/+seh/+relay`：无论是容器 envVars 还是 cmd 里 `set`，都拿不到 Wine 调试输出。
  - 原因 1：Winlator 把 guest 的 stdout/stderr 收进自己的管道；
  - 原因 2：7.1 源码里 `if (MainActivity.DEBUG_LEVEL <= 1) envVars.put("WINEDEBUG", "-all")`，而 release 的 `DEBUG_LEVEL` 是编译期常量 0；
  - 原因 3：镜像里的 Wine 很可能没把调试通道编进 Unix 侧（`ntdll.so` 里只有 `file`，PE `ntdll.dll` 里才有 `loaddll/relay`）。
- `winedbg --auto`：报 “WineDbg was unable to attach to the process”，在 WOW64/box64 环境下不可用。
- `HKCU\...\Run` 注册表启动项、StartUp 文件夹里的 .bat：Wine 9.2 的 explorer 都不执行（已实测，无日志产生）。

---

## 四、实测证据

| 实验 | 结果 |
|---|---|
| 原版 `Ekd5.exe`（加壳） | 进程存活约 1.5–2.5 秒 → **EXIT 0**，无窗口、无文件写入、无 dump |
| 无壳 `Ekd5_unpacked.exe` | **EXIT -1073741819（0xC0000005）** |
| 原版在 7.1（同 Wine 9.2） | **EXIT 0**（同样无窗口） |
| 7.1 容器 `wow64Mode:false` | 不生效：wfm 进程仍加载 `x86_64-unix` 库（`grep -c i386-unix = 0`） |
| 运行期文件写入 | 游戏目录无任何新文件（引擎没有走到写配置/日志） |

### 崩溃点分析（无壳版 / PC dump 相同）

- 地址 `0x46C0C9`，位于窗口/对话框过程 `0x46CF31` 中。
- `0x46CF31` 在消息 0x110（WM_INITDIALOG）时把 lParam 存入全局 `0x4B68E0`；其它消息会 `mov ecx,[0x4B68E0]` 后 `call 0x46C065`。
- 崩溃时 `[ebp-4]（=ECX）= 0`，即 **`this` 为空**——WM_INITDIALOG 没走到，对话框对象没建立。
- PC 上的 `Ekd5.exe-up.txt` 也是 `0x46C0C9`，说明这是引擎自身的启动期缺陷（Windows 上偶发首启闪退，重开就好），Wine 下变成必现。

### 壳侧特征（内存转储 2.38MB 扫描）

- 壳用动态 `GetProcAddress` 解析 API；名字表里含 `IsDebuggerPresent`、`OutputDebugStringW`、`GetTickCount`、`SuspendThread`、`SetUnhandledExceptionFilter`、`VirtualProtect` 等。
- **没有** wine / VMware / VBox / SbieDll 等字符串。
- 壳自带 SEH 处理，具备“吞掉异常并 ExitProcess(0)”的能力。

---

## 五、已逐项验证无效的模拟器改动

| 改动（单变量） | 结果 |
|---|---|
| Windows 版本 Win10 ↔ XP SP3 | EXIT 0 |
| box64 稳定性：BIGBLOCK=0、STRONGMEM=3、SAFEFLAGS=2、CALLRET=0 | EXIT 0（参数已确认生效） |
| DX 包装器 CNC DDraw ↔ WineD3D | EXIT 0 |
| 分辨率 1024x768 ↔ 640x480（游戏原生） | EXIT 0 |
| WINEESYNC 1 ↔ 0 | EXIT 0 |
| 音频 PulseAudio ↔ 无，且关闭 DirectSound/DirectMusic | EXIT 0 |
| Winlator 7.1（同 Wine 9.2）+ CNC DDraw | EXIT 0 |
| 7.1 `.container` 的 `wow64Mode:false` | 字段被忽略，仍 64 位 WOW64 |

---

## 六、根因判断（两种假设，尚未用 Wine 轨迹最终区分）

**H1：壳反调试/反模拟后主动退出**
- 依据：EXIT 0（不是崩溃码）、壳具备 SEH/反调试 API、约 2 秒内退出、无窗口无文件。

**H2：引擎崩在对话框初始化，壳的 SEH 吞掉后退出 0**
- 依据：无壳版在同一地址 `0x46C0C9` 抛 AV；壳有 `SetUnhandledExceptionFilter`；EXIT 0 也符合“异常被处理”的表现。

两者在当前环境里表现完全一致。要区分需要 Wine 级调用轨迹或壳内断点，而这两条通道在 Winlator 里都被堵死（见 3.3）。

---

## 七、下一步选项与成本

| 选项 | 成本 | 成功概率 | 说明 |
|---|---|---|---|
| A. 再试 Winlator 5.0（Wine 8.0.x，可能 pre-WOW64） | 下载 88MB + 装第三个应用，约 10 分钟 | 中低 | 这是“模拟器路线”最后一个真正的变量（32 位 wine32 + box86 老栈）。若它也已升到 Wine 9.2，则该路线到顶 |
| B. 回无壳 exe 路线 | 已有 80% 基础 | 中高 | 用干净的 OEP 转储解决脏数据，再针对 `0x46C0C9` 的对话框初始化失败做修复（字体/窗口类/资源）。7.1 的交互控制台可做更细实验 |
| C. 打壳/打 exe 补丁 | 高（RE） | 取决于能否定位检测点 | 不建议优先；壳高度混淆 |

**建议**：如果目标必须“用原版加壳 exe”，选 A 做最后一次对照；如果目标是“能玩”，B 的期望更高，且与本次发现的引擎崩溃点直接对应。

---

## 八、设备与文件状态（收尾后）

**平板**
- `com.winlatcn`（8.0）已恢复：容器 `ccz22` 的 screenSize/envVars/dxwrapper/audio/wincomponents/box64Preset 均与初始一致；`user.reg` 中测试用的 Run 键已删除；Windows 版本已恢复 Windows 10（build 19043）。
- `com.winlator`（7.1）已卸载；`/sdcard/Download/Winlator_7.1.apk` 已删除。
- 游戏目录里的测试文件（`0run.bat`、`run*.log`、`dbg.log`、`Ekd5_unpacked.exe` 等）已删除；原始游戏文件未改动。

**PC（D:\ccz_winlator_patch\unpack_tools）**
- 保留：`winlator.apk`（8.0 安装包备份，233MB）、`Winlator适配研究报告.md`、`wl_ui.py`（uiautomator 驱动脚本）。
- 分析产物：`Ekd5_memdump*.bin`、`slotmap.txt`、`rebuild2.ps1` 等原有文件未动。

---

## 九、复现命令附录

```sh
# 查看设备
adb -s TABLET_SERIAL_REDACTED shell getprop ro.product.model
adb -s TABLET_SERIAL_REDACTED shell pm list packages | grep winlat

# 8.0 容器配置
adb -s TABLET_SERIAL_REDACTED shell "run-as com.winlatcn cat files/imagefs/home/xuser-1/.container"

# 8.0 启动日志（先建 0run.bat，再用容器文件管理器的“运行”按钮）
adb -s TABLET_SERIAL_REDACTED shell "cat /storage/emulated/0/Download/ccz22/run.log"

# 7.1 控制台技巧（需要 debuggable 的前缀）
adb -s TABLET_SERIAL_REDACTED shell "run-as com.winlator sh -c 'cd files/imagefs/home/xuser-1/.wine/drive_c/windows && cp wfm.exe wfm.exe.bak && cp system32/cmd.exe wfm.exe'"
# 重启容器后：
adb -s TABLET_SERIAL_REDACTED shell "input text 'D:\ccz22\0run71.bat'"
adb -s TABLET_SERIAL_REDACTED shell "input keyevent 66"
```

---

*本报告基于 2026-11-01 的实机测试；所有“无效”结论均为单变量对照后的实测结果，未改动游戏原始文件。*
