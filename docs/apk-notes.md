# Winlator 8.0 主屏快捷方式改造

目标：给平板桌面加一个图标，一点直接进《曹操传》。本目录是改造用的全部材料。

## 结果
- 长按平板上的 **Winlator 图标** → 弹出菜单里多了一项 **"曹操传"**（就是本改造加的）。
- 点它 = 容器 `ccz22` 直接启动并运行 `D:\Download\ccz22\Ekd5.exe`（实测 60 多秒进标题）。
- 想要常驻桌面图标：长按 Winlator 图标 → 按住"曹操传"拖到主屏即可（手动拖，adb 的合成手势拖不动 MIUI 桌面）。

## 改了什么（就三处，没有改代码）

1. `AndroidManifest.xml`
   - `com.winlator.MainActivity` 里加 `<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts"/>`
   - `com.winlator.XServerDisplayActivity` 的 `android:exported` 由 `false` 改 `true`
     （桌面快捷方式由 launcher 进程启动，非导出 Activity 会被拒绝）
2. 新增 `res/xml/shortcuts.xml`：一条静态快捷方式 `ccz_game`，intent 直接指向
   `com.winlator.XServerDisplayActivity`，带两个 extra：
   - `container_id` = 1（int，容器 ccz22 的 id）
   - `exec_path` = `/storage/emulated/0/Download/ccz22/Ekd5.exe`（app 内部用 `unixToDOSPath` 转成 `D:\Download\ccz22\Ekd5.exe`，工作目录自动取该 exe 所在目录）
3. `res/values/strings.xml` 加两条文案：`shortcut_ccz_short`（曹操传）、`shortcut_ccz_long`（三国志曹操传 加强版）。

## 为什么能覆盖安装而不丢容器数据

这个 APK 是用 **AOSP testkey** 签的（证书 `CN=Android,O=Android,emailAddress=android@android.com`，
SHA-256 `a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`），
私钥是公开的（AOSP `build/target/product/security/testkey.pk8`）。
本目录的 `testkey.p12`（口令 `android`，别名 `testkey`）就是用它与原证书组装的，签名后证书指纹与原版**完全一致**，
所以 `adb install -r` 是"同签名升级"，`/data/data/com.winlatcn/files`（4.9G 容器）原样保留。

## 重建步骤（在 WSL，Ubuntu 20.04）

```bash
apt-get install -y openjdk-17-jre-headless apktool apksigner zipalign
# apktool 2.4 自带的 aapt 是坏的，要用系统的 aapt2，并且要换新框架：
curl -sL -o plat30.zip https://dl.google.com/android/repository/platform-30_r03.zip
python3 -c "import zipfile;zipfile.ZipFile('plat30.zip').extractall('plat30')"
cp plat30/android-*/android.jar /root/.local/share/apktool/framework/1.apk   # 替换 -28 的 framework-res

apktool d -f Winlator8.0.apk -o wn_out
python3 patch_apk.py            # manifest + shortcuts.xml + strings
python3 fix_res_names2.py       # apktool 2.4 会把 $avd_* 资源解成非法名，aapt/aapt2 都拒收 → 改名并同步 public.xml
python3 fix_styles.py           # windowBackground 的裸颜色改为 @color 引用（aapt2 要求 reference）
apktool b --use-aapt2 wn_out -o wn_patched.apk
zipalign -p -f 4 wn_patched.apk wn_aligned.apk
apksigner sign --ks testkey.p12 --ks-pass pass:android --ks-key-alias testkey \
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled false \
    --out wn_signed.apk wn_aligned.apk
adb install -r wn_signed.apk
```

坑位记录：
- `android:valueType` 只能是 `intType/floatType/colorType/pathType` 这几个（字符串 extra 直接省掉 valueType）。
- 框架必须是 API 30 的 android.jar，否则 manifest 里的 `appComponentFactory`、`allowAudioPlaybackCapture` 等属性报 "not found"。
- aapt（v1，Ubuntu 自带 8.1）会 SIGABRT，必须 `--use-aapt2`。

## 回滚

```bash
adb install -r winlator8.0_original.apk   # 同签名，数据不动
```

## 文件

| 文件 | 说明 |
|---|---|
| `winlator8.0_original.apk` | 原版（233 MB） |
| `winlator8.0_ccz_shortcut.apk` | 改好的、已用 testkey 签名的包（233 MB） |
| `testkey.p12` | 签名用 keystore（口令 android / 别名 testkey） |
| `patch_apk.py` | manifest / shortcuts.xml / strings 的改动 |
| `fix_res_names2.py` | `$avd_*` 非法资源名修复 |
| `fix_styles.py` | `windowBackground` 颜色引用修复 |

---

## 内置补丁版（winlator8.0_ccz_bundled.apk，2026-11-02）

目标：**装完即用**——不再需要往容器里拷 ntdll，也不再需要配环境变量。

### 做法
1. `ntdll` 补丁里把两个开关的默认值改掉（fix104）：
   - `thread.c`：32 位栈基址平移默认 `0xe00`（原来是 `0xe14`），`WINE_SS` 仍可覆盖；
   - `virtual.c`：`hwbp_update` 默认启用（只有显式设 `WINE_HWBP_SOFT=0` 才关闭）。
2. 把 fix104 直接烤进 APK 的内置镜像：
   ```
   xz -dc assets/imagefs.txz | tar -xf - -C imgsrc
   cp ntdll_fix104.so imgsrc/opt/wine/lib/wine/x86_64-unix/ntdll.so
   cd imgsrc && tar -cf - . | xz -T0 -6 > .../assets/imagefs.txz      # 132MB → 176MB
   ```
3. `apktool b --use-aapt2` 重打包 → `zipalign -p -f 4` → testkey 签名（`java -jar apksigner.jar`）。
4. 校验：从成品 APK 里再抽出 `assets/imagefs.txz`，解包核对 `opt/wine/.../ntdll.so` 的 md5 == fix104 ✔。

成品：`winlator8.0_ccz_bundled.apk`（276.8 MB，签名与原版同指纹 `a40da80a…`）。
实机 `adb install -r` 覆盖安装后，容器数据保留，启动到标题 56.3 s ✔。

### 注意
- 内置镜像只在 **/data 下没有 imagefs 时**（全新安装/换机）才会被解出来；已存在的容器不会被覆盖。
- 因此已有机器若要换成内置版，仍然建议手动把 `3-备份/ntdll_fix104.so` 拷到容器里（或删掉 files/imagefs 重新解一次，但那会清空容器）。

---

## fix108 + 输入魔改版（2026-11-02 21:2x）

### 内置 ntdll
- 换成 **fix108**（md5 `c3e15c393e3a63ffe2ab22be868f35bc`）：
  - box64 32 位 int3 的 RIP 归一化默认开启（`WINE_BPT_FIX=0` 可关）；
  - SEGV/STKDUMP/STKDEC/STKPAGE/GCTX/HWBP_PERF/ALLOCPROBE 等高频调试输出改为 `WINE_HWBP_LOG` 才输出。
- **设备差异**：手机需要默认（开）；平板必须 `WINE_BPT_FIX=0`（已写入平板容器）。若某台设备卡在启动，先试把它设 0/1。

### 输入魔改（2 处 smali，基于 8.0 反编译）
1. `InputControlsView.onTouchEvent`：
   原来循环里**任何**元素只要它的 `bindings[pointerId]` 是 `MOUSE_LEFT_BUTTON` 就会
   `setPointerButtonLeftEnabled(false)`（不管有没有点中）——而所有配置都带左键按钮，
   所以“显示按键”一开，触屏左键就被全局禁用。
   改为**只有该元素 `handleTouchDown` 返回 true（真的点中）时才禁用**。
2. `XServerDisplayActivity.showInputControls`：
   删掉 `setPointerButtonRightEnabled(false)`（改为 true），显示按键时双指轻点=右键仍然有效。

效果：显示按键时，空白区域 单指轻点=左键、双指轻点=右键；点中按键时按键优先。

### 产物
- `winlator8.0_ccz_bundled_fix108_input.apk`
  - 276,863,399 字节
  - md5 `daa34119e68cb61f10b29e796aeca0eb`
  - 签名与原版同指纹 `a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`
- 平板已 `adb install -r` 覆盖安装并冒烟：arms=1600 / faults=44381，标题 62.9s。
- 提醒：内置镜像只在全新安装（/data 下无 imagefs）时解出；已有容器本轮已手动换成 fix108。

### 两个变体（快捷方式路径不同，代码/补丁完全相同）
| 文件 | 适用 | 快捷方式 exec_path | md5 |
|---|---|---|---|
| `winlator8.0_ccz_bundled_fix108_input.apk` | 手机 | `/storage/emulated/0/Download/ccz/Ekd5.exe` | `daa34119e68cb61f10b29e796aeca0eb` |
| `winlator8.0_ccz_bundled_fix108_input_tablet.apk` | 平板 | `/storage/emulated/0/Download/ccz22/Ekd5.exe` | `e0ea790a1bbabf83e025dbfca5e4ba60` |

- 两台都已 `adb install -r` 覆盖安装：手机标题 77.6s；平板标题 66.7s（有一次 448 arms 的偶发卡住，重试即好）。
- 输入补丁对两台都生效：显示按键时空白区单指=左键、双指=右键。

### 2026-11-02 22:1x 平板路径调整
- 平板游戏目录：`/sdcard/Download/ccz22` → **`/sdcard/ccz`**（独立 ccz 目录，仍在 D: 盘映射 `D:/storage/emulated/0` 内，DOS 路径 `D:\ccz\Ekd5.exe`）。
- 平板变体 APK 的快捷方式 exec_path 同步改为 `/storage/emulated/0/ccz/Ekd5.exe`，md5 更新为 `c5acd575ad82474c5bb5fb9c8f58e4d6`。
- 容器 `drives` 仍是 `D:/storage/emulated/0`（无需改）；存档在容器 C: 内，未受影响。
- 平板覆盖安装并从新路径启动实测：arms=1536 / faults=44365，标题 66.6s。

### 2026-11-02 22:2x 手机路径同步
- 手机游戏目录：`/sdcard/Download/ccz` → **`/sdcard/ccz`**（与平板统一；`Ekd5.exe` md5 一致）。
- 两台设备现在共用同一版 APK 快捷方式路径 `/storage/emulated/0/ccz/Ekd5.exe`（md5 `c5acd575ad82474c5bb5fb9c8f58e4d6`）。
- 手机 `box64Preset` 从 PERFORMANCE 改回 **COMPATIBILITY**（实测更稳/更快）；如需 PERFORMANCE 可在 App 里自行切回。
- 手机重试后启动成功：标题 82.5s，arms=1536 / faults=44090。
- 备注：壳的偶发启动失败（192/704 arms 退出或 448 arms 停住）仍存在，重试即可；需要的话可加启动看门狗。

### 2026-11-05 性能 A/B（手机，战斗中实测）
方法：自动“契阔谈宴→读档 No.018→汜水关之战”，用 SurfaceFlinger --latency 统计呈现帧间隔（不杀进程时也可测；本轮为 A/B 重启）。

| 配置 | 战斗 FPS | 帧分布（<20ms / 20-80 / >80） |
|---|---|---|
| 基线 1280x720 + COMPATIBILITY | 37.6~38.7 | ~93 / ~28 / ~5 |
| cpuList 全 4-7 | 37.6 | 93 / 27 / 5（wineserver 仍 0-7，Winlator 的 cpuList 不钉它） |
| box64Preset=PERFORMANCE | 38.7 | 95 / 29 / 2 |
| WINEESYNC=0 | 36.1 | 98 / 25 / 3 |
| 640x480 桌面 | 启动 51s（更快），触控自动点击失效，未测战斗 |
| 960x540 桌面 | 启动 81s；窗口居中 |
| 固定 GL 表面 1866x840（smali 改 `XServerHolder.setFixedSize`） | **黑屏**，已回滚 |

关键观测：战斗时 GPU 空闲；`Ekd5` 0.3~0.4 核（89% 时间在 `read(pipe)` 等待）、`com.winlatcn` 0.4~0.6 核、
`proot` 0.1 核；窗口帧 60.2fps 时占多数，但约 26% 帧 20~200ms。⇒ 瓶颈是模拟的 x86/GDI 每帧计算与等待，
不是 GPU 填充率；上述配置都在噪声内（±3fps），无法实质提升。

未完成/待定：
- 系统刷新率被 vivo 智能刷新锁 60Hz（`settings`/`cmd display` 无效），需在系统设置里手动开 120/144；
- 平板同场景对照（平板当前未连接）；
- 如需大幅提速，考虑 32 位 Wine/box86 路线（本游戏纯 32 位，wow64 路径可能更慢），但需要重做 i386 ntdll 补丁与容器。

### 2026-11-05 高刷修复（手机）★
- 根因：Winlator 在前台时系统把显示从 120Hz 降到 **60Hz**（关掉 App 立刻回 120）——不是用户没开高刷。
- 改动：`XServerDisplayActivity.onCreate` 末尾给窗口设置 `WindowManager.LayoutParams.preferredRefreshRate = 120f`。
- 实测：App 前台保持 **120Hz**；同一场战斗 **35~38fps → 43.9fps**，中位帧间隔 **16.6ms → 8.3ms**。
- 请求 144Hz 时系统仍只给 120Hz（该机对 App 的上限）。
- 产物：`winlator8.0_ccz_120.apk`，md5 `ed879c0fa40682052058cd7c93eaec68`；手机已安装。
- 剩余：p90 仍 ~25ms、偶发 >80ms 尖峰（guest 模拟计算突发），属于模拟器算力上限。

### 2026-11-05 合帧渲染（手机）★
- 现象：PC 上所有角色的待机帧同一帧切换；手机（120Hz）一个接一个切。
- 原因：`GLRenderer.onUpdateWindowContent` 每次窗口内容更新就 `requestRender()`；游戏慢循环里逐角色的半成品更新在 120Hz 下被逐帧呈现。
- 改动：新增 `GLRendererCoalesce`（继承 GLRenderer）+ `CoalesceRunnable`：
  内容更新后先取消旧回调、延迟 **5ms** 静默再渲染，最长 **8ms** 强制渲染；XServerView 改用该类。
- 实测（同一场战斗）：**48.1fps**（120Hz 无合帧 43.9 / 60Hz 基线 35–38），中位 8.3ms；`Ekd5` 0.26 核、`com.winlatcn` 0.27 核（均下降）。
- 产物：`winlator8.0_ccz_coal.apk` md5 `ad8427664083ef45795a8639750a37dc`；手机已装。
- 该包同时包含：120Hz 窗口属性、RGB565 EGL、fix108、输入魔改、ccz 快捷方式。

### 2026-11-05 渲染专项复测（直出 vs 合帧）★ 结论修正
同一场战斗、静置、直播壁纸已停，用 SurfaceFlinger `--latency` **每秒新帧数**做尺子：

| 版本 | 出帧 | app CPU | 游戏 CPU | GL 线程 |
|---|---|---|---|---|
| 120Hz 直出（无合帧、无优先级） | **56.4 fps** | 0.42 | 0.36 | 0.20 |
| 合帧 5ms/8ms + 线程优先级 | **30.0 fps** | 0.35 | 0.36 | 0.15 |

- **合帧把出帧压掉约一半，已撤掉**（上一节的 48.1/43.9 是带轮询探针的干扰场景下测的，不能和本次对比）。
- 游戏待机约 **100 次内容更新/秒**；直出时 SF 约 56 帧/秒 ⇒ 瓶颈在呈现端，不是算力。
- 尝试 `Window.setFrameRate(120f)` 向系统声明帧率：这台 vivo 的 framework **没有该方法**（`NoSuchMethodError`，启动即崩），已撤；`Surface.setFrameRate` 未验证（后续可试，记得 catch Throwable）。
- 保留：`GLRenderer.onSurfaceCreated` 与 `XConnectorEpoll.run` 开头的 `Process.setThreadPriority(-4)`。
- 战斗期间 fix108 的 hwbp 计数不增长（战场无额外异常开销）。
- 环境干扰：`io.wallpaperengine.weclient` 直播壁纸常驻 ~56% CPU，mediaserver/硬解另 ~60%；停掉后系统空闲从 ~50% 提到 ~80%。玩之前建议停直播壁纸。
- 最终产物：`winlator8.0_ccz_final.apk`，md5 `5b6c2c9188cd4199b5e574ae7e7ef7e4`；手机已安装。
  - 直出渲染（`GLRenderer`）+ 线程优先级 + 120Hz 窗口属性 + RGB565 + fix108 + 输入魔改 + ccz 快捷方式。
  - `GLRendererCoalesce`/`CoalesceRunnable` 仍在 smali 里但不再实例化（未使用，可留作后续微合帧实验）。

### 2026-11-05 帧率声明 API 复测
- `Surface.setFrameRate(120f, 1)`（`surfaceCreated` 里调用，`catch Throwable`）：不崩，但同场景 **56.6fps**，与直出 56.4 无差别；未采用。
- `Window.setFrameRate(120f)`：这台 vivo 的 framework 没有该方法，直接崩，已撤。
⇒ 最终包不带任何 setFrameRate 调用；高刷仍靠 `preferredRefreshRate=144`（面板实际 120Hz）。

### 2026-11-05 box64 升级尝试（0.4.4 / 0.3.8）★ 不可用
- 为 glibc 2.31 交叉重编 box64 0.4.4/0.3.8（解释器指向 imagefs）；base64 分块传输（exec-in 会破坏二进制）。
- 0.4.4：游戏能起，但 hwbp arms/faults 持续暴涨（1600→9856，74s 仍黑屏），壳的反调试循环；`WINE_BPT_FIX=0` 无效。
- 0.3.8：Ekd5 直接退出。0.3.0：标题 ~60s、`arms=1600 faults=44106 hits=400` 正常。
- 结论：box64 0.3.0 之后 int3/#DB/Dr 语义变了，fix108 不能直接配新版；升级需重做 Wine 侧 hwbp 仿真。
- 备份：设备 `imagefs/usr/local/bin/box64.bak030`；PC `device_backup/box64_test_20261105/`；产物 `D:\ccz_winlator_patch\unpack_tools\box64_038_glibc231`、`box64_044_glibc231`。8.0 已恢复 0.3.0 并回到标题。

### 2026-11-06 渲染组件移植（11.2 → 8.0）
- `winlator8.0_ccz_swap0.apk`（md5 `d088396afbc996a22c13020bd72a9398`）：移植 11.2 的 `EGL14.eglSwapInterval(display, 0)`。战斗内 4 次采样 p90 91/125/125/141 ms，`>=80ms` 帧 16/24/21/28；final 同口径 58–100 ms、5–14。**判负，不采用。**
- `winlator8.0_ccz_rast11.apk`（md5 `c59b2bd36487d54732c8ae1ec275820a`，当前手机已装）：把 11.2 的 `Drawable.fillRect`/`copyArea` 原生机器码搬进 8.0 `libwinlator.so`。
  - JNI 签名两侧完全一致；`memcpy` 在 8.0 只有 `__memcpy_chk`，用 20 B cave 补 `x3=len` 后调用。
  - 验证：战斗、滚屏、对话框、攻击动画渲染正确；同场景 app CPU 约 0.21 core（final 约 0.31），帧间隔在噪声内。
  - 回退：`adb install -r winlator8.0_ccz_final.apk`（md5 `5b6c2c9188cd4199b5e574ae7e7ef7e4`）。
- 结论：渲染链不是算力瓶颈；帧率由游戏 GDI 出帧节奏决定。光栅移植只降 X 线程 CPU，不改变帧间隔。工具：`tools` 外的 `unpack_tools/transplant_drawable.py`（VA==文件偏移，cave 在函数 slot 尾部）。

### 2026-11-06 零增量指针事件修复
- 发现：`InputControlsView` 的鼠标移动定时器每 16 ms 调用 `xServer.injectPointerMoveDelta(0,0)`；`Pointer.setPosition()` 无论坐标是否变化都会 `triggerOnPointerMove` → `GLRenderer.onPointerMove` → `requestRender()`，即空闲时也强制 60 次/秒的无效渲染请求。
- 改动：`XServer.injectPointerMoveDelta` 开头加 `if ((dx|dy)==0) return;`（smali：`or-int v0,p1,p2; if-nez v0,:nonzero; return-void`）。
- 实测：战斗空闲 8.5 s，Timer-0 约 90 ms（约 1.0% 单核，改前约 1.4%）；GL/X/内容更新为主，空闲 app ≈30% 单核、game ≈34% 单核。
- 产物：`winlator8.0_ccz_nomove.apk`（md5 `c360a3a3e9e138bbb37818dd097f6872`，rast11 + 该修复），手机已装；回退 `winlator8.0_ccz_rast11.apk`。
- 同轮实验：容器 `cpuListWoW64=6,7` 全屏生效，但范围绘制成本 0.306→0.294 s（游戏）/0.345→0.322 s（App），在噪声内；单核 `7` 不会被应用（仍 0–7），原因待查。

### 2026-11-06 帧率提示 → DVFS 提升（fr60）
- 改动：`XServerView.surfaceCreated()` 里调 `Surface.setFrameRate(60f, 1)`（`FRAME_RATE_COMPATIBILITY_FIXED_SOURCE`），catch Throwable。
- 效果（战斗同场景 sysfs 采样）：
  - 大核 cpu4/5/6 中位频率 **600 MHz → 2.0–2.65 GHz**（此前 130s ftrace 里大核中位 0.6 GHz）。
  - 战斗空闲 CPU：游戏 **34%→22%**、App **30%→8%**（单核占比，与 4× 频率吻合）。
  - 自动战斗 30s：内容 **29.5 fps**、帧间隔中位 16.6 ms、p90 **49.9 ms**、>80 ms 占 6.9%。
- 残留：卡顿起点仍常见 **400–500 MHz**（游戏 1ms 小突发之间核心会掉频，再爬升），游戏线程 runqueue 仍 ~9.8%。
- 产物：`winlator8.0_ccz_fr60.apk`（md5 `efe61e1e6c9031e4aca662289ffec3a0`，nomove + 60fps 提示），当前手机已装。
- 取证明细：X 请求稳态 = MIT-SHM PutImage 92 次/秒（平均 28.5k 像素/次）+ SetClipRectangles 388 次/秒；像素转换只占 ~2–3%，成本在每次 blit 固定开销与低频率。

### 2026-11-06 fr60 + GL 线程钉大核
- 改动：`GLRenderer.onSurfaceCreated()` 末尾调用 `pinGlThread()`：`Runtime.exec(["/system/bin/taskset","-p","f0", myTid])`（f0 = cpu4–7，toybox 十六进制掩码），catch Throwable。
- 验证：ftrace 显示 GLThread 的 CPU 分布从 **cpu0–3 → cpu4–7**（{6:210,4:112,7:40,5:31}），GL runqueue 6s 内仅 61 ms（~1%）。
- 滚动负载（6 秒拖动）：游戏 **2.90→2.17 s**、App **2.90→2.54 s**（对比同场景 fr60 包）。
- 产物：`winlator8.0_ccz_fr60_glpin.apk` md5 `a923fd6e6ff5dd0c45af69005fc7a42e`，当前手机已装；回退 `winlator8.0_ccz_fr60.apk`。
- 注：`ClientSideGraphics=N`（Wine 注册表）实测会让 Winlator X 服务器花屏（标题栏/图标错乱），已回滚并删除该键。

### 2026-11-10 fr120（120 Hz）★ 当前现役
- 产物：`winlator8.0_ccz_fr120.apk`，md5 `e1bd0b4b9a1bbefb28cf529653497cdb`，276,863,399 B，2026-11-10 00:32 安装到手机。
- 与 `fr60_glpin` 的只读比对：dex 中 `Win32AppWorkarounds`(30) / `assignTaskAffinity`(1) / `applyStartupWorkarounds`(1) / `cpuListWoW64`(1) / `getAffinityMask`(1) / `preferredRefreshRate`(1) / `setFrameRate`(1) 计数完全一致；但 dex 版本 035 vs 038、整包重编带来 159 万字节差异，不能按字节断言逻辑等价。
- `fr60_glpin`（`a923fd6e…`）已在 2026-11-12 清理时删除，不再随包；回退用 `winlator8.0_original.apk`（`46246c35…`）。

### 2026-11-12 重装注意 ★
- **fr120 内置 imagefs 是 fix108 版**：`ntdll.so` = `c3e15c39…`（4,190,784 B）、`win32u.so` = `2726461e…`（1,511,552 B）、`wineserver` = `9ebc79db…`（710,336 B）。
  重装 APK / 重装系统文件后，**必须重推三件套**，否则启动慢约 20 s（fix108 的 maps 扫描）。
- box64 由 APK 在容器启动时从 `assets/box64/box64-0.3.0.tzst` 解出（`dd297b6d…`），不需要额外推。
- 容器创建默认是 Turnip/DXVK，需改成 `virgl + wined3d` 并写入生产 env（`WINE_SS=0xe00`、`WINE_HWBP_SOFT=1`…），见 `device_backup/device_state_20261112.md`。
