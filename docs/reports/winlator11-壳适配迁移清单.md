# Winlator 11.2 迁移：壳适配清单（2026-11-08 15:4x）

> 装 v11.2 后要逐项核对的「壳适配」，避免出现「新版本能跑别的游戏、但跑不了这个壳」。

---

## 1. 我们为这个壳做过的适配（全清单）

| # | 适配项 | 载体 | 现行值 | 迁移方式 |
|---|---|---|---|---|
| 1 | **hwbp 软件模拟 + 信号修复**（fix110b） | `ntdll.so` | `eddb74af…` | 取决于 v11.2 的 Wine 版本 |
| 2 | **跳过 PROT_NONE 视图注册**（fix110c） | `ntdll.so` | `44a0dcb0…` | 同上 |
| 3 | **视图注册修复**（fix110d） | `ntdll.so` | 已备份 | 同上 |
| 4 | **容器环境变量** | `.container` JSON | `WINEDEBUG=-all WINE_SS=0xe00 WINE_HWBP_SOFT=1` | ✅ 可直接填 |
| 5 | **Box64 作者版 0.4.4** | `box64` | `a7360b1e…` | v11.2 内置 0.4.4 正式版 ✔ |
| 6 | **分辨率 / DX / 驱动** | 容器 JSON | 1280×720 / DXVK / Turnip | 按 UI 重设 |
| 7 | **游戏文件** | 共享存储 | `/storage/emulated/0/ccz/Ekd5.exe` | ✅ 直接指向 |
| 8 | **存档** | 旧容器 imagefs | `com.winlatcn/files/imagefs/...` | 需要时复制 |

---

## 2. 迁移策略：三步走，先裸跑再逐项加

### 第 1 步：裸跑（先别搬任何补丁）

- v11.2 新建容器，只设分辨率 + DX；
- 直接指向 `/storage/emulated/0/ccz/Ekd5.exe`；
- 看**能否出标题**。
- 理由：v11.2 的 0.4.4 是正式版，作者可能已修好壳兼容；先确认它原生的能力，
  避免一上来就把旧补丁塞进去掩盖问题。

### 第 2 步：迁环境变量（成本最低）

若裸跑失败（卡解包/黑屏），把 #4 的 envVars 填进 v11.2 容器：

```
WINEDEBUG=-all
WINE_SS=0xe00
WINE_HWBP_SOFT=1
```

这三个是**壳适配的核心**：`WINE_HWBP_SOFT=1` 启用软件 hwbp 模拟（不依赖内核 perf），
`WINE_SS=0xe00` 控制栈保护行为。v11.2 的容器 UI 里「环境变量」可直接填。

### 第 3 步：迁 ntdll 补丁（最后手段）

先确认 v11.2 的 Wine 版本：

```bash
adb shell run-as com.winlator ls files/imagefs/opt/wine/lib/wine/x86_64-unix/
adb shell run-as com.winlator cat files/imagefs/opt/wine/version 2>/dev/null
```

| v11.2 Wine 版本 | 做法 |
|---|---|
| **9.2（与我们一致）** | 可直接把 `build/ntdll_fix110c.so` 复制进 v11.2 的 `opt/wine/.../ntdll.so`（先备份原文件） |
| 9.x 其它小版本 | 用 `src/fix110b` 源码 + `patch_fix110c.py` 对 v11.2 的 Wine 源码重打补丁、重编 |
| 10.x | 不能直接用；需按 §15–§20 的证据链在新源码上重新定位 Dr 传递（但 v11.2 的 wow64 可能已修） |

---

## 3. 兼容性风险

| 风险 | 说明 | 应对 |
|---|---|---|
| v11.2 Wine 版本不同 | 我们的 ntdll 补丁是给 Wine 9.2 编的 | 先查版本，再决定直接复制 / 重编 |
| 容器 JSON 格式变化 | v8.0 → v11.2 跨大版本 | 不迁移旧容器，新建 + 手动填 env |
| 0.4.4 在 v11.2 里仍跑不了壳 | 作者测试矩阵未必覆盖这个壳 | 退回第 2/3 步；仍不行则放弃 v11.2 |
| 存档路径不同 | 新容器 imagefs 独立 | 需要时从 `com.winlatcn` 复制 |

---

## 4. 验收

| 级 | 判据 |
|---|---|
| L1 | v11.2 能启动、能建容器、能跑 `wine notepad` |
| L2 | `Ekd5.exe` 出标题（不黑屏、不卡解包） |
| L3 | 读档、战斗正常 |
| L4 | 性能 A/B：出标题时间、待机 fps、战斗 fps、p90（基线 72 s / 44.3 fps / p90 17.1 ms） |

---

## 5. 当前状态

- APK 下载中（13 MB / 150 MB），完成后安装；
- 现有 `com.winlatcn` 生产态**不受影响**（包名不同，可共存）；
- 本清单将作为装完后的核对表，逐项勾选。
