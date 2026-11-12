# 曹操传加强版 v2.20 · Winlator 8.0 底层补丁

> 让 VProtect 加壳的 `Ekd5.exe` 在 **Android / ARM 的 Winlator + Wine** 上跑起来。
> 核心是在 Wine `ntdll` 里用「页保护 + guest TF 单步 + push 栈回滚」**软件模拟 x86 硬件数据断点**，
> 骗过壳的反调试心跳 —— 解包、进主菜单、读档、进战场全部正常。

| 指标 | 数值 |
|---|---|
| 启动到标题画面 | **22.2 s**（优化前 44.5 s；最初 72.1 s） |
| 反调试档案 | `arms=1536 / hits=384`（满档 1600 / 400） |
| 待机 | 44.3 fps，帧间隔 p90 17.1 ms |
| 自动战斗 | 30.1 fps，p90 33.3 ms |

测试机：天玑 9200 / Mali-G715 MC11 / Android 15 / Winlator 8.0（Wine 9.2 + box64 0.3.0）。

## 下载

**➡️ [整合包（Releases）](../../releases/latest)** — 约 492 MB，**不含游戏本体**，只含安装所需文件。

| 包内 | 内容 |
|---|---|
| `2-模拟器\` | Winlator 8.0 改造 APK（120 Hz 版 `fr120`）、原版回滚 APK、testkey |
| `3-补丁\` | `ntdll` / `win32u` / `wineserver` 三件套 + 生产容器配置 + 一键部署脚本 + 部署说明 |
| `说明.txt` | 三步装机、md5 校验表、验收基线、已知坑 |

包内**不含**技术报告与工作日志（那些都在本仓库 `docs/` 里）。

游戏本体请自备（本项目与 Release 都**不包含**游戏文件）。

## 快速开始

1. 装 `2-模拟器\winlator8.0_ccz_fr120.apk`；
2. 打开 App → 容器管理 → 右上「添加」→ 直接「确认」，让它先把容器建出来；
3. 手机连电脑，运行 `3-补丁\部署补丁.ps1`（会 force-stop、推三件套、写容器配置、清 tmp）。

启动：容器管理 → `ccz` → 管理 → 磁盘(D:) → `ccz` → `Ekd5.exe` 的 ▶

> ⚠️ APK 内置的 imagefs 是 **fix108 版 ntdll**，每次重装 App / 重装系统文件后都必须重跑部署脚本，
> 否则启动会慢约 20 s（原因见下文「22.2 s 是怎么来的」）。

## 为什么需要这个补丁

VProtect 用 **x86 硬件数据断点**做反调试心跳：

- `Dr1 = ESP + 0xEC`，`Dr7 = 0x300104`（读写 1 字节监视）；
- 每次心跳都要确认 `Dr1` 处触发了 `#DB`，否则判定「被调试」→ 解包冻结。

ARM 上的 Wine / box64 没有 Dr 寄存器和调试异常，所以壳一定失败。
本补丁在 `ntdll` 侧把这一行为**软件模拟**出来。

## 实现思路

1. 壳 `arm` 时，把 `Dr1` 指向的栈页设成不可访问（`mprotect`）；
2. 任何访问都会 `SIGSEGV` → Wine 页故障处理判断地址是否等于被监视地址；
3. 在 guest 上下文里置 `EFLAGS.TF`，box64 会在**下一条指令之后**投一次 `SIGTRAP`；
4. `push/call` 类指令要先回滚 `ESP`（box64 在写故障时 ESP 已经减过），再整条重跑；
   真正命中监视地址的 400 次投 `#DB`，误报的 4.3 万次直接复保护。

细节与踩坑记录见 [`docs/`](docs/)：

- [页故障 19.8 s 全方位评估](docs/reports/页故障19秒-全方位评估-20261112.md) — 其中 §9 是整条链路最关键的发现
- [启动时间全模块优化计划](docs/reports/启动时间-全模块优化计划-20261112.md) — 五段全量账 + 判死清单
- [box64 重构评估](docs/reports/box64重构评估-20261112.md) — 为什么不重写 dynarec
- [完整 README（存档）](docs/README-完整版.md)

### 22.2 s 是怎么来的

44,000 次页故障里有 19.8 s 花在 `segv_handler` 中一段**遗留调试代码**上：
`if (RIP >= 0x10000000) { open/read /proc/self/maps + 逐行 sscanf }`。壳的 RIP 是 `0x31f014`，
每次页故障都命中；在 proot 下每次 `open/read` 还要 tracer 停一次，44,000 × 474 µs ≈ 19.8 s。
用 `WINE_HWBP_MODLOG` 门控（默认关）后单次从 474 µs 降到 **4.4 µs**。

## 仓库结构

```
src/fix110b/            fix110b 快照（相对 wine-9.2 的 dlls/ntdll/unix/）
src/fix110b_modfix/     当前部署版源码（fix110b + MODLOG 门控）
src/signal_x86_64.c.base88   本方案开始前的基线，便于 diff
build/                  重建配方（make_*_sources.py + build_*.sh）与部署脚本
tools/deploy/           整合包里的部署脚本、生产容器配置
tools/scripts/          测量 / 容器 / UI 自动化小工具
tools/probes/           成本探针（mprotect、SIGSEGV 往返、userfaultfd）
docs/                   全部报告、攻坚日志、设备快照
history/patch-scripts/  历次补丁脚本（过程记录，不是按序重放链）
```

## 构建

需要 Wine 9.2 源码树：

```bash
# 从当前树的源码快照生成 modfix 版源码
python3 build/make_modfix_sources_20261112.py
# 在 Wine 源码树里编译
bash build/build_ntdll_modfix_20261112.sh
# 校验产物特征串 / md5
python3 build/verify_fix110b_sources.py
```

| 产物 | md5 |
|---|---|
| `ntdll_fix110b_modfix.so` | `4e7d876de5f0d0defebd8a34b664e403` |
| `ntdll_fix110b.so` | `eddb74af96587b15589d430b1199a07b` |
| `win32u_r109g.so` | `2be1316c83d603c4defae8669fcc1dfb` |
| `wineserver_r112b.so` | `9df1d0c325f2b9c2f7e71c82fb5fab70` |

## 已知限制

- **只能配 box64 0.3.0**。0.4.4 / 0.3.8 会让壳的反调试循环冻结（已判定不可解，见报告）。
- **只适用于 Winlator 8.0 + Wine 9.2**。官方 11.2 换成新 wow64 架构，壳直接退出。
- 音频未解决：容器没有 gstreamer、游戏缺 `Mp3SERV.DAT`、`Koeia.dll` 是 170 字节空文件。
- 下面这些 box64 开关**不能开**（会让游戏空转）：`BOX64_DYNAREC_SAFEFLAGS=0`、
  `BOX64_DYNAREC_BIGBLOCK=2`、`BOX64_DYNAREC_FORWARD=1024`（部分组合）。
- 帧率上限受 `Surface.setFrameRate(60f)` 影响，系统 144 Hz 落不到游戏上。

## 免责声明

本项目是**个人研究用途**的补丁与工具，仓库与 Release **不包含游戏本体**；
请使用你自己合法拥有的《三国志曹操传》加强版。

整合包内的 APK 基于开源项目 [Winlator](https://github.com/brunodev85/winlator)（GPLv3）修改，
Wine 遵循 LGPL，box64 遵循 MIT。若相关权利方有异议，会立即下架。

## 致谢

Winlator / Wine / box64 / proot，以及所有在这些工具上做过兼容性探索的人。
