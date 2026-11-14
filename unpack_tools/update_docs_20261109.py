#!/usr/bin/env python3
"""Append the 2026-11-09 findings to the project docs and workspace memory.

Written as a file (not an inline heredoc) because the text contains markdown
backticks, which Bash would interpret as command substitution.
"""
import os

ROOT = "D:/ccz_winlator_patch"


def append(rel, text):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "a", encoding="utf-8") as f:
        f.write(text)
    print("APPENDED", rel, len(text))


LOG36 = r"""

---

## 36. 2026-11-09：11.2 新 wow64 攻坚 + 8.0 生产态长线/帧率基线（补录）

### 【要点摘要】

- ★ **设备归属纠正**：8.0 生产态 = 手机 `PHONE_SERIAL_REDACTED`（V2302A / MT6985 天玑 9200 / Mali-G715 MC11 / Android 15）；11-09 攻坚机 = 平板 `TABLET_SERIAL_REDACTED`（SM8550 / Adreno 740），装官方 11.2 `com.winlator`。README §1 终态 72.1s / 44.3fps / p90 17.1ms 是**天玑手机**实测。
- ★ **11.2 轻量方案整类判死**：v3–v8、A1、①②③ 开关、R-B、R-E 最小版共 14 个单变量全灭；根因收敛到 Wine 10 new wow64 的 32 位恢复/机器帧重建，需天级工程。详见 `report/wow64-壳兼容-魔改重构研究.md`。
- ★ **生产态可信基线**：自动战斗 60s → avg 39.7 fps（剔除首秒 38.2）、p50 16.63 ms、p90 49.88 ms、p99 266.05 ms、max 548.81 ms、≥100 ms 135 次。基础 60Hz 节奏稳，长尾尖峰是主要问题。
- ★ **测量口径铁律**：`dumpsys SurfaceFlinger --latency` 必须取 `(BLAST)` 子层，并过滤 LONG_MAX 哨兵（`1e12 < v < 1e18`）。本日新脚本漏了上界，导致「0 帧 / 游戏静止」误判；已全部补回，**此前相关结论作废**。
- · 系统 144 + 应用单独 144 未落到 app：全程 `frameRateOverride = 60.000004 Hz`。

### 36.1 11.2 攻坚：容器修复与补丁落点

- 点 ▶ 秒退根因 = `xuser-1` home 骨架残缺（`pattern_push.tar` 只塞了 `.wine`，缺 `.config/.cache/.local`）；用健康容器克隆后 wineserver/wfm 正常。**不是** `.container` 配置、不是 `wfm.exe`、不是补丁。
- 补丁其实从未部署（设备 md5 为官方原版）→ 重新部署 hwbp5 (`fcd4c7e3…`) + skipint3 i386 (`857bccc5…`)；`WINE_DBG_LOG=1` 传达成功，`wine_dbg_<pid>.log` 按 PID 落盘。
- v3 证伪 i386 PE ntdll 不在 32 位 SEH 路径；v4 改打 `wow64cpu` + 64 位 PE ntdll 后 `Eip 587033→587034` 生效；v5 拦在 SEH 前吞掉 int3 仍崩在恢复路径。
- A1 `BOX64_IGNOREINT3=1` 让 `80000003` 消失但崩点不变 ⇒ **int3 不是根因**；`0x3f0004xxxx` 是 box64 `custommem.c:2929` 的 ELF 47-bit 加载区附近。
- ② 只读探针：首次异常 `trap=3` SIGTRAP(int3)，随后 `0x587057` 写违例 `trap=14`，再到 `0x3f0004b31b` nested SIGSEGV。
- ③ 开关 A/B：`WINE_HWBP_SOFT=0`、`BOX64_DYNAREC_INTERP_SIGNAL=1`、`BOX64_DYNAREC=0` 异常序列逐字相同；`DYNAREC=0` 是全集关闭，故 dynarec 子开关整类判死。
- R-B：`PROBE-RB flags_before=1`（RESET_STATE 本来就置着）；R-E 最小版：`R13` 本来就是 `cpu_area+4`。两者均是 no-op。
- 结论：恢复所需上下文本来就对，崩在 `syscall_32to64` prolog / `Wow64SystemServiceEx` 对 wow_frame 的污染；剩余 R-D（降级 Wine 9.2，2–3 天）/ R-E 完整版（3–5 天）或停在 8.0。

### 36.2 生产态长线/帧率基线（本日新增，可信）

- 口径：60 秒自动战斗、只读、`--latency` BLAST 子层按 max 时间戳数新增帧。
- 逐秒 12–65 帧、60 秒全活跃；`gfxinfo` 的 `Total frames/Janky/99th` 是 **HWUI UI 层**，180 秒一格没动 ⇒ 不能当游戏 GL 帧判据（此前误用已纠正）。
- 亲和：`Ekd5.exe Cpus_allowed_list=4-7`（生效），App/wineserver/proot `0-7`；Ekd5 0.3–0.4 core、App 0.2–0.4、wineserver 0.15–0.23。
- 长尾与日志 §2.3 的「1ms 突发 + 大核 DVFS 400–600MHz + 跨核迁移」一致；P1（亲和收窄）对准 p99 / ≥100ms 次数，P3（帧率声明解锁 + 渲染节拍）才冲 90/120。
- 目标修正：当前 app 锁 60Hz 且连 60 都不稳，「稳定 90/120」需先解 `Surface.setFrameRate(60f, FIXED_SOURCE)` 锚定，再压长尾尖峰。
- 详见 `report/长线流畅度与帧率-源码分析.md` §8。

### 36.3 工具、备份与归档

- 新增脚本：`unpack_tools/phone_boot.py`、`phone_measure.py`、`phone_probe_readonly.py`、`phone_long_probe.py`、`phone_final_probe.py`、`restore_mouse_pref.py`、`probe_first_exception.py`、`patch_skipint3_v3..v8.py`、`patch_rb_resetstate.py`、`patch_re_r13.py`。
- 备份：`_归档_20261109/backup_20261109/`（124 文件）+ `backup_20261109.zip`（2.9 MB），含设备容器/prefs/rootfs md5、报告、脚本、日志、可信基线数字。
- 清理：按项目惯例**只移不删**，146 个中间产物 / 141.7 MB 移入 `_归档_20261109/中间产物/`，清单 `归档清单_20261109.md`。
- 未触碰：官方 `Winlator_11.2.apk`（与 `apk/winlator11.2_mod.apk` md5 不同，唯一副本）、`apk/*.apk`、生产三件套、`device_backup/`、报告、README、攻坚日志、`.workbuddy/`。
- 设备收尾状态：手机停在自动战斗界面、鼠标模式 `move_cursor_to_touchpoint=false`、未改任何容器配置。
"""

REPORT8 = r"""

---

## 8. 实测补充（2026-11-09 21:4x）：可信基线与口径修订

> 前面 §0–§7 是源码分析；本节为后续实机实测。§7 末尾「未动日志与记忆文件」仅指撰写当时，
> 用户随后明确要求更新日志与记录，已在攻坚日志 §36 与工作区记忆中补记。

### 8.1 可信基线（60 秒自动战斗，只读）

| 指标 | 值 |
|---|---|
| 逐秒出帧 | 12–65 帧，60 秒全活跃 |
| 平均 | **39.7 fps**（剔除首秒缓冲初值 38.2） |
| 帧间隔 | p50 **16.63 ms**、p90 **49.88 ms**、p99 **266.05 ms**、max **548.81 ms** |
| 长尾 | ≥80 ms 169 次、≥100 ms 135 次（2381 间隔中 5.7%） |
| 刷新率 | `frameRateOverride = 60.000004 Hz` 全程；系统 144 / 应用单独 144 未落到 app |
| 亲和/CPU | Ekd5 `4-7`（生效）、其余 `0-7`；Ekd5 0.3–0.4 core |

### 8.2 ★ 口径修订（重要，前面若干判断因此作废）

1. `dumpsys SurfaceFlinger --latency` 环形缓冲用 `9223372036854775807`（LONG_MAX）填充空位。
   **必须过滤 `1e12 < v[1] < 1e18`**；否则 `max(ts)` 被顶到 LONG_MAX，之后所有真实帧判为「已见过」，
   得到假的 0 fps。历史 `fps_probe.py` 有此上界，本日新脚本漏了，现已给全部脚本补回。
2. 必须取 `SurfaceView[...](BLAST)#NNN` **子层**；父层 `SurfaceView[...]#NNN` 的缓冲全 0。
3. `gfxinfo` 的 `Total frames / Janky / 99th` 统计的是 **HWUI UI 层**，不是游戏 GL 内容帧
   （180 秒内一格没动即证明）。游戏帧率只能用 BLAST 子层 `--latency`。
4. **作废结论**：本日早些时候「游戏静止 177 秒 / 0 帧 / 11 fps / p90 216ms / 5.4fps」等全部作废。
   两轮长采样里 35 次截图 md5 全不同，画面一直在动（自动战斗确实在跑）。

### 8.3 对 §4 候选与 §6 顺序的修正

- 基础节奏稳定 60Hz（p50 16.63ms），问题在 **p99 266ms / 每秒约 2.2 次 ≥100ms 尖峰**，
  与 §2.3 的 DVFS 掉频再爬升一致 ⇒ P1（亲和收窄）判据明确为 **p99 与 ≥100ms 次数**，辅以 p90 / avg fps。
- 「稳定 90/120」当前不可达：app 锁 60Hz 且 60 都不稳。P3 解 `Surface.setFrameRate(60f, FIXED_SOURCE)`
  锚定是前提；P1 压尖峰是第一步。
- 新增硬知识：生产态为天玑 9200（MT6985 / Mali-G715 MC11 / Android 15），
  容器用 `virgl + wined3d + pulseaudio` 通用栈；`cpuListWoW64=4,5,6,7` 下发到游戏进程**确实生效**。

### 8.4 备份与归档

- `_归档_20261109/backup_20261109/` + `.zip`：设备快照、报告、脚本、日志、基线数字。
- `_归档_20261109/中间产物/`：146 个中间产物 / 141.7 MB，**只移不删**，清单 `归档清单_20261109.md`。
"""

README = r"""

## 2026-11-09 补记（11.2 攻坚结论 + 生产态基线）

> 本节只记结论与指路，完整实证见 `report/wow64-壳兼容-魔改重构研究.md`、
> `report/长线流畅度与帧率-源码分析.md` 与攻坚日志 §36。

- **设备归属**：8.0 生产态在手机 `PHONE_SERIAL_REDACTED`（MT6985 天玑 9200 / Mali-G715 MC11 / Android 15）；
  11-09 的 11.2 攻坚在平板 `TABLET_SERIAL_REDACTED`（SM8550 / Adreno 740，包名 `com.winlator`）。
  **两者不是同一台、不是同一个 app、不是同一套容器配置**。§1 终态数据来自天玑手机。
- **11.2 结论**：容器秒退已修（home 骨架残缺），补丁已真正部署，但 v3–v8 / A1 / 开关 A/B /
  R-B / R-E 共 14 个单变量全灭；根因是 Wine 10 new wow64 的 32 位异常恢复/机器帧重建，
  属天级工程。**维持 0.3.0 + 8.0 生产态**。
- **生产态可信基线**（60s 自动战斗）：avg 39.7 fps、p50 16.63 ms、p90 49.88 ms、p99 266.05 ms、
  ≥100 ms 135 次；`frameRateOverride` 全程 60 Hz（系统 144 未落到 app）。
- **★ 测量铁律**：`SurfaceFlinger --latency` 取 `(BLAST)` 子层并过滤 LONG_MAX 哨兵
  （`1e12 < v < 1e18`）；`gfxinfo` 是 UI 层，不能当游戏帧率。此前「0 帧」结论作废。
- 备份 `_归档_20261109/backup_20261109.zip`；中间产物按惯例只移不删，见 `归档清单_20261109.md`。
"""

DAILY = r"""# 2026-11-09 工作日志

## 【要点摘要】
- ★ 纠正设备归属：8.0 生产态 = 天玑 9200 手机 `PHONE_SERIAL_REDACTED`；11.2 攻坚 = 骁龙平板 `TABLET_SERIAL_REDACTED`。README §1 数据是天玑实测。
- ★ 11.2 新 wow64 轻量方案整类判死（14 个单变量），维持 8.0 生产态；结论入 `report/wow64-壳兼容-魔改重构研究.md` 与攻坚日志 §36。
- ★ 生产态自动战斗基线：39.7 fps / p90 49.88ms / p99 266.05ms / ≥100ms 135 次；app 锁 60Hz。
- ★ 测量铁律：BLAST 子层 + 过滤 LONG_MAX 哨兵；gfxinfo 是 UI 层。此前 0 帧结论作废。
- ★ 按用户要求完成备份与归档（只移不删）：备份 zip 2.9MB / 124 文件，归档 146 文件 / 141.7MB。

## · 过程
1. 11.2 攻坚：修容器秒退（home 骨架）、重新部署 hwbp5+skipint3、v3–v8 补丁链、A1 IGNOREINT3、①②③ 探针与开关、R-B/R-E，全部单变量实证。
2. 转生产态：源码分析出三条链（渲染节拍 / DVFS 调度 / Wine X 客户端），产出 `report/长线流畅度与帧率-源码分析.md`。
3. 实机基线：发现并修复 `--latency` 哨兵 bug 后拿到可信数据；建立 `phone_*` 系列只读测量脚本。
4. 备份、归档、更新日志与记录（用户明确要求）。

## - 参考
- 攻坚日志 §36、README 2026-11-09 补记、两份报告、归档清单与备份 MANIFEST。
- 下一步：P1 亲和 A/B（60s 自动战斗，判据 p99 / ≥100ms 次数）。
"""

MEMORY = r"""# 项目长期笔记（ccz_winlator_patch）

## 设备与版本（务必分清）
- **8.0 生产态（可玩基线）**：手机 `PHONE_SERIAL_REDACTED` / V2302A / **MT6985 天玑 9200** / Mali-G715 MC11 / Android 15，包名 `com.winlatcn`。容器 `virgl + wined3d + pulseaudio`（通用栈，非 Adreno 专用）。README §1 的 72.1s / 44.3fps / p90 17.1ms 出自这台。
- **11.2 攻坚机**：平板 `TABLET_SERIAL_REDACTED` / 24018RPACC / **SM8550 骁龙 8 Gen 2** / Adreno 740，包名 `com.winlator` 官方 11.2，容器 `turnip,gladio + dxvk + alsa`。壳始终跑不通。
- 11-02 平板阶段（README §3–§10）是历史记录，不是当前主线。

## 技术铁律
- `dumpsys SurfaceFlinger --latency`：必须取 `SurfaceView[...](BLAST)#NNN` 子层；必须过滤 LONG_MAX 哨兵 `9223372036854775807`（用 `1e12 < v[1] < 1e18`），否则得到假 0 fps。`gfxinfo` 统计 HWUI UI 层，不是游戏 GL 帧。
- box64 0.3.0 无完整可重建源码树（`/root/box64-0.3.0src` 只有安装物）；fix110b 补丁链只认 0.3.0，0.4.4/0.3.8 破坏 VProtect 反调试。
- Wine 10 new wow64 的 32 位异常恢复/机器帧问题是结构性冲突，轻量补丁（异常分发、RESET_STATE、R13、env 开关）全部无效。
- 生产态亲和下发确实生效：`Ekd5.exe Cpus_allowed_list=4-7`（`Win32AppWorkarounds.java:130-157` 经 UDP 7946 → winhandler）。
- 系统设 144 / 应用单独 144 未落到 app：游戏运行时 `frameRateOverride=60.000004 Hz`（嫌疑 `Surface.setFrameRate(60f, FIXED_SOURCE)`）。

## 当前基线（2026-11-09，60s 自动战斗）
avg 39.7 fps（剔除首秒 38.2）、p50 16.63ms、p90 49.88ms、p99 266.05ms、max 548.81ms、≥100ms 135 次 / 2381 间隔。基础 60Hz 稳，长尾尖峰来自 DVFS 掉频再爬升。

## 用户惯例
- 清理只移不删（移到 `_归档_YYYYMMDD/`），真删要先列清单确认；备份先行。
- 日志/记忆没有明确要求不动；本日用户已明确要求更新。
- 单变量 A/B、结论必须实证、失败明说。

## 指路
- 攻坚日志 §36、`report/wow64-壳兼容-魔改重构研究.md`、`report/长线流畅度与帧率-源码分析.md`、`_归档_20261109/`。
- 下一步 P1：亲和收窄 A/B，判据 p99 / ≥100ms 次数（同存档同回合 60s 自动战斗）。
"""


def main():
    append("2026-11-06_渲染与卡顿攻坚日志.md", LOG36)
    append("report/长线流畅度与帧率-源码分析.md", REPORT8)
    append("README.md", README)
    mdir = os.path.join(ROOT, ".workbuddy", "memory")
    os.makedirs(mdir, exist_ok=True)
    append(".workbuddy/memory/2026-11-09.md", DAILY)
    with open(os.path.join(mdir, "MEMORY.md"), "w", encoding="utf-8") as f:
        f.write(MEMORY)
    print("WROTE .workbuddy/memory/MEMORY.md")


if __name__ == "__main__":
    main()
