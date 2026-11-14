# 曹操传加强版 v2.20 · Winlator 8.0 魔改

基于 [Winlator](https://github.com/brunodev85/winlator) 8.0 魔改，用于让《三国志曹操传》加强版 v2.20 在 Android 设备上运行。

## 下载

➡️ [Releases](../../releases/latest) — 懒人包：一个 APK 装完即用，不需要 adb、电脑或部署脚本。

**不含游戏本体**，游戏请自备并放到 `/sdcard/ccz`。

## 已知事项

- **预计启动时间：20–40 秒**（首次启动、冷启动会更久，视机型、图形驱动与温度浮动）。
- **音频**：容器默认 ALSA 后端可能无声或导致容器启动异常；建议在容器设置里改用 **PulseAudio**。
- **天玑 / 骁龙需自行配置容器参数**：骁龙 / Adreno 用 **Turnip**；天玑 / Mali 建好容器后把图形驱动改成 **Virgl**，box64 预设与其余参数按机型自行调整。

## 开源协议

- 本项目的 APK 是 [Winlator](https://github.com/brunodev85/winlator)（**GPLv3**）的修改版。分发修改后的二进制须遵守 GPLv3：提供对应源码、以同协议开源，不得附加额外限制。
- [Wine](https://www.winehq.org/) 遵循 **LGPL**，[box64](https://github.com/ptitSeb/box64) 遵循 **MIT**，[proot](https://github.com/proot-me/proot) 遵循 **GPLv2**。
- 仓库与 Release **不包含游戏本体**；请使用你自己合法拥有的《三国志曹操传》加强版副本。
- 本项目仅供个人研究与技术交流，与游戏权利方无关；若相关权利方有异议会立即下架。
