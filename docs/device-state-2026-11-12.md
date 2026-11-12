# 设备生产态快照（2026-11-12 23:59，手机 <PHONE_SERIAL>）

> 2026-11-12 23:22 用户卸载 `com.winlatcn` 后，23:37–23:59 从零重装并重新部署。
> 本文件是重装后的**唯一可信生产态**，覆盖 11-02 平板时期的旧说明。

| 层 | 值 |
|---|---|
| APK | `winlator8.0_ccz_fr120.apk`（`e1bd0b4b9a1bbefb28cf529653497cdb`，276,863,399 B） |
| imagefs | `.img_version=9`，706 MB；**自带 ntdll `c3e15c39`（fix108）**、原版 win32u / wineserver |
| 容器 | `home/xuser-1/.container`（见 `phone_container_20261112_live.json`，1,203 B，`box64Preset=COMPATIBILITY`） |
| ntdll | `4e7d876de5f0d0defebd8a34b664e403`（fix110b_modfix） |
| win32u | `2be1316c83d603c4defae8669fcc1dfb`（r109g） |
| wineserver | `9df1d0c325f2b9c2f7e71c82fb5fab70`（r112b） |
| box64 | `dd297b6d3b0966f6f8acf0d60495f82c`（0.3.0，APK 启动时自动解出） |
| 游戏 | `/sdcard/ccz/Ekd5.exe` = `2225d9da…`（v2.20；817 文件，`SV` 459 个） |

验收（干净一轮）：**22.2 s 出标题**（ws 1.7 / ekd 6.0 / hwbp t 7.4 / title 21.3），
`arms=1536 faults=44090 hits=384`；冷装首轮 `arms=1600 faults=44106 hits=400` 也正常。

## 重装/恢复步骤（照抄即可）

1. `adb install -r apk\winlator8.0_ccz_fr120.apk`，授予 READ/WRITE_EXTERNAL_STORAGE；
2. 启动一次，等 `files/imagefs/.winlator/.img_version` 出现且内容为 `9`（约 60 s，706 MB）；
3. 建容器（默认值即可，会解出 `container_pattern.tzst`），把本目录的
   `phone_container_20261112_live.json` 拷到 `files/imagefs/home/xuser-1/.container`；
4. 推三件套（`run-as cp`，目标路径见上表的 md5）：
   `opt/wine/lib/wine/x86_64-unix/ntdll.so`、`opt/wine/lib/wine/x86_64-unix/win32u.so`、
   `opt/wine/bin/wineserver`；
5. 清 `files/imagefs/tmp/.wine-*`、`tmp/.X11-unix/*`、`tmp/shm/*`；
6. `am start -n com.winlatcn/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"`。

注意：**改动 imagefs / 重装 APK 后，三件套必须重推**；`fr120` 自带的是 fix108。
