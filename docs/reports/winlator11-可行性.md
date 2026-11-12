# 直接用 Winlator 11.2 的可行性（2026-11-08 15:3x）

> 纯可行性研究 + 下载准备，**未安装、未动现有生产态**。

---

## 0. 结论：可行，而且是最省事的一条路

| 事实 | 值 |
|---|---|
| 官方最新版 | **v11.2.0（2026-08-19 发布）**，`brunodev85/winlator` GitHub Releases |
| **内置 Box64** | **v0.4.4 正式版**（官方更新日志明确列出） |
| 设备 | V2302A，**arm64-v8a**，Android 15（≥ minSdk 26）✔ |
| 当前安装 | Winlator **8.0**，包名 `com.winlatcn` |
| 官方包名 | `com.winlator` → **与现有版本可共存**，不会覆盖 |
| 游戏文件 | 在共享存储 `/storage/emulated/0/ccz/`，新版本可直接访问 |

**不需要改代码、不需要修 Dr、不需要换 Wine。** 作者已经把 0.4.4 打进正式版并跑通了
（否则不会写进 release notes）。

---

## 1. 为什么这能绕开我们踩的坑

我们手头是「**Winlator 8.0 的 Wine 9.2 + 单独替换作者版 box64 0.4.4**」：

- Wine 9.2 的 32 位上下文路径与 0.4.4 的 TEB/Dr 处理不自洽 → Dr1 被污染成 `ffce115f`
  → 壳的 hwbp 模拟失效 → `hits=25`（见 `box64-0.4.4-重构路线可行性研究.md` §15–§20）。

v11.2 是「**作者配套的 Wine + box64 0.4.4 + 全套 win32u/wow64 模块**」：

- 组件版本自洽，不存在「新 box64 配旧 Wine」的错位；
- 作者的测试矩阵覆盖了实际游戏，`0.4.4` 是作为正式版发布的。

---

## 2. 实施步骤（约 1 小时）

1. **下载** `Winlator_11.2.apk`（GitHub 官方 release，150.25 MB，
   SHA256 `ea5c81bf0e5b90a6b9ccb8007121ab6738c8646a5a60b341ce0ac2b33f1d100a`）；
2. **安装**（与 `com.winlatcn` 共存，不覆盖现有 8.0）；
3. **首次启动**：等待初始化（解压组件/OBB，几分钟）；
4. **新建容器**：参考现有 `ccz80` 的配置（分辨率、DX 版本、Box64 预设）；
5. **指向游戏**：`/storage/emulated/0/ccz/Ekd5.exe`（共享存储，无需复制）；
6. **实测**：出标题时间、待机 fps、自动战斗 fps、p90。

---

## 3. 风险与回退

| 风险 | 说明 | 回退 |
|---|---|---|
| v8.0 → v11.2 容器格式不兼容 | 不迁移，**新建容器**即可 | 无影响 |
| 存档在旧容器 imagefs 里 | 需要时从 `com.winlatcn/files/imagefs/...` 复制到新容器 | 可复制 |
| v11.2 跑不起来/更慢 | 直接卸载 | 卸载即可，`com.winlatcn` 生产态完全不受影响 |
| 下载源不可信 | 只从 GitHub 官方 release 取，核对 SHA256 | 删除 APK |

---

## 4. 与自研路线对比

| 路线 | 工作量 | 风险 | 当前结果 |
|---|---|---|---|
| 修 box64 0.4.4（信号/Dr/引擎） | 数天–数周 | 高 | ❌ 未成功（§20 到工具链边界） |
| 换 Wine 10.10 三件套 | 1–2 天 | 中 | ❌ 替换即崩（§11） |
| **装 Winlator 11.2** | **约 1 小时** | **低（可共存/可卸载）** | **作者已验证 0.4.4 可用** |
| 维持 0.3.0 | 0 | 0 | ✅ 当前可玩（72 s / 44.3 fps） |

---

## 5. 下一步

**下载 → 安装 → 建容器 → 实测**。若通过，直接做性能 A/B；若不通过，卸载回退，零损失。

这是目前唯一「不需要我们修任何底层」就能拿到 0.4.4 的路径。

---

## 6. 实测进展（2026-11-08 15:5x–16:1x）

### 6.1 已完成

| 步 | 结果 |
|---|---|
| 下载 | `Winlator_11.2.apk` 157,549,075 字节，**SHA256 MATCH**（`ea5c81bf…`） |
| 安装 | 成功，`versionCode=33`、`versionName=11.2`、**DEBUGGABLE**（可 `run-as`） |
| 初始化 | `files/rootfs` 解压完成；`shared_prefs` 记录 `box64_version=0.4.4` |
| 容器 | 新建 `Container-1`（1280×720 / Vulkan Vortek / DXVK / ALSA / Box64 PRESET=PERFORMANCE） |
| 壳适配 | 已写入 `rootfs/home/xuser-1/.container`：`WINEDEBUG=-all WINE_SS=0xe00 WINE_HWBP_SOFT=1` + 盘符 `F:/storage/emulated/0/ccz` |
| 启动 | `XServerDisplayActivity` 正常，Wine 桌面出现，**F: 盘（ccz）已挂载** |
| 32 位验证 | **notepad 正常启动** → 新 wow64 架构工作 |

### 6.2 关键发现：v11.2 是全新 wow64 架构

`/storage/emulated/0/wine.log`：

```
wowdiag: BTCpuProcessInit begin
wowdiag: process_init current_machine=014c native_machine=aa64
wowdiag: LdrLoadDll kind=cpu-backend module=...
wowdiag: cpu exports ... Simulate=... GetContext=... SetContext=...
wowdiag: LdrLoadDll kind=wow64win.dll
```

即：32 位程序由 **`wow64cpu`（cpu-backend）+ `wow64win.dll`** 模拟，
**不再走 Wine 9.2 的 `set_thread_wow64_context_inner`** —— 我们追了半天的 Dr 传递路径
在 v11.2 里已经不存在。这正是「换整套」的价值。

### 6.3 当前卡点：`Ekd5.exe` 启动即退出

- 在容器 cmd 里执行 `F:/Ekd5.exe`：**无输出、无进程**（`ps` 里没有 Ekd5）；
- `notepad` 同环境正常 → wow64 基础功能没问题；
- `wine.log` 只有 wow64 初始化日志，没有壳的报错；
- 推断：壳在新 wow64 架构下**自身兼容性问题**（anti-debug 检测架构/上下文行为），
  而不是我们之前修的 Dr 传递问题。

### 6.4 下一步（继续排查，不需要改底层）

1. **抓退出码**：cmd 里 `F:\Ekd5.exe & echo EXIT=%ERRORLEVEL%`（本轮因焦点误进 notepad，未抓到）；
2. **看壳自己的日志**：`F:\` 下可能有 `*.log`，或 `%TEMP%`；
3. **换 Box64 预设**：PERFORMANCE → STABILITY / COMPATIBILITY（容器 JSON 的 `box64Preset`）；
4. **关掉 Vortek 换 Gladio**（`graphicsDriver` 从 `vortek,gladio` 改 `gladio`），排除图形初始化失败；
5. **用 Winlator 的 Shortcuts** 建快捷方式启动，避免 cmd/焦点问题。

设备现状：`com.winlator`（v11.2）容器运行中；`com.winlatcn`（0.3.0 生产态）未受影响。

---

## 7. 容器配置对齐老 ccz（2026-08-08 16:1x）

按用户要求，把 v11.2 的 `Container-1` 逐字段对齐老容器 `ccz`：

| 字段 | 老 ccz | 新 ccz（对齐后） | 结果 |
|---|---|---|---|
| `envVars` | ZINK 全套 + `MESA_VK_WSI_*` + `DXVK_HUD` + `TU_DEBUG=sysmem,noconform` + `LC_ALL=zh_CN.utf8` + `WINEDEBUG=-all` + `BOX64_DYNAREC_BIGBLOCK=3` + `BOX64_DYNAREC_FORWARD=1024` + `BOX64_DYNAREC_CALLRET=1` + `WINE_SS=0xe00` + `WINE_HWBP_SOFT=1` | **完全一致** | ✅ |
| `cpuList` / `cpuListWoW64` | `0,1,2,3` / `4,5,6,7`（大小核分离） | 一致 | ✅ |
| `box64Preset` | `COMPATIBILITY` | 一致 | ✅ |
| `drives` | `D:/storage/emulated/0` | 一致（游戏路径 `D:\ccz\Ekd5.exe`） | ✅ |
| `wincomponents` | 老版（无 `vcrun2005=0`） | 一致 | ✅ |
| `name` | `ccz` | `ccz` | ✅ |
| `screenSize` | 1280×720 | 一致 | ✅ |
| `graphicsDriver` | `virgl` | **`vortek,gladio`** | ⚠️ v11.2 已移除 virgl |
| `dxwrapper` | `wined3d` | **`dxvk`** | ⚠️ v11.2 无 wined3d 选项 |
| `audioDriver` | `pulseaudio` | **`alsa`** | ⚠️ v11.2 只有 ALSA |

图形/音频栈三项**无法照搬**：v11.2 的 UI 只提供 Vulkan Vortek / OpenGL Gladio +
DXVK / VKD3D + ALSA，旧的 virgl/wined3d/pulseaudio 已被移除。其余项全部一致。

写入后回读校验通过（`回读一致: True`），app 重启生效；容器启动正常，
`wineserver`/`winedevice` 起来，D: 盘指向 `/storage/emulated/0`。

## 8. 鼠标模式：找到设置项，但被触控层拦截

`Input Controls` 对话框（uiautomator 精确 bounds）：

| 控件 | resource-id | bounds | 当前值 |
|---|---|---|---|
| Show Touchscreen Controls | `CBShowTouchscreenControls` | `[759,595][1676,707]` | **checked=true** |
| Relative Mouse Movement | `CBRelativeMouseMovement` | `[759,707][1639,819]` | checked=false |
| OK | `BTConfirm` | `[1732,882][2040,1029]` | — |

「拖拽」来自 **Show Touchscreen Controls=true**（触控层把触摸解释为拖拽）；
「跟随点击」= 取消该项（保持 Relative=false，绝对定位）。

**卡点**：该触控层会拦截 adb 的触摸点击 —— 按 uiautomator 的 bounds 点 `(1217,651)`
后 `checked` 仍为 `true`；交换坐标点击则把对话框点关。这是「要关触控层、但触控层拦点击」的死循环。

**解法（二选一）**：
1. 手动在手机上点一下：侧边栏 → Input Controls → 取消 `Show Touchscreen Controls` → OK；
2. 下轮用键盘导航：`TAB` 移到复选框 → `SPACE` 切换 → `TAB` 到 OK → `ENTER`
   （键盘事件不经过触控层，应能生效）。

---

## 9. 鼠标模式已解决：真正的开关是 `move_cursor_to_touchpoint`

### 9.1 老模拟器怎么设的

`com.winlatcn/shared_prefs/com.winlatcn_preferences.xml`：

```xml
<int name="inputcontrols_app_version" value="18" />
<boolean name="move_cursor_to_touchpoint" value="true" />
```

**`move_cursor_to_touchpoint=true` 才是「跟随点击」**（光标跟随触摸点）。
`Show Touchscreen Controls` 只是虚拟按键层，与鼠标模式无关 —— 所以之前取消它
「关了再点开还有」，因为它根本不是那个开关。

### 9.2 v11.2 的解法

v11.2 的 prefs 里**没有这个键** → 默认拖拽。直接写入：

```xml
<boolean name="move_cursor_to_touchpoint" value="true" />
```

（脚本 `set_mouse_mode.py`；写入后回读校验通过，app 重启。）

### 9.3 实测

在容器里单击 `C:` 盘 → **直接打开**（地址栏变 `C:`，列出 Program Files/users/windows）。
之前必须拖拽才能移动指针；现在是**点哪去哪** ✔。

### 9.4 附带发现

- v11.2 的 `D:` 盘挂载正常（`D:\` 列出 101 项：Android/DCIM/Download/...）；
- 但文件管理器列表里**没显示 ccz 文件夹**（`ls` 确认其存在），可能是缓存或过滤；
  下轮用 cmd 绝对路径 `D:\ccz\Ekd5.exe` 直接运行，绕开文件管理器。

### 9.5 资产

`set_mouse_mode.py`、`prefs11.xml`（写入前后的 prefs 备份）、
`shot30–35.png`（跟随点击实测截图）。
