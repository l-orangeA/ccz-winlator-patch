#!/usr/bin/env bash
# 手机重连后运行：把 /sdcard/ccz 里的非游戏部件收进 /sdcard/ccz_patch
set -u
ADB=${ADB:-"D:/MuMuPlayer/nx_main/adb.exe"}
P=${P:-"PHONE_SERIAL_REDACTED"}
HERE=$(cd "$(dirname "$0")" && pwd)

"$ADB" -s "$P" shell "mkdir -p /sdcard/ccz_patch/artifacts /sdcard/ccz_patch/probes /sdcard/ccz_patch/game_tools/randomizer /sdcard/ccz_patch/tmp"

# 日志 / 解包残留
"$ADB" -s "$P" shell "cd /sdcard/ccz && for f in Ekd5.exe-up.txt tr_tail.bin uris.txt; do [ -f \"\$f\" ] && mv \"\$f\" /sdcard/ccz_patch/artifacts/; done"
# 探针
"$ADB" -s "$P" shell "cd /sdcard/ccz && [ -f waitwin.exe ] && mv waitwin.exe /sdcard/ccz_patch/probes/ || true"
# 随机工具（含规则）
"$ADB" -s "$P" shell "cd /sdcard/ccz && mv 2.10随机工具*.exe random_rules.json randomizer_settings.json /sdcard/ccz_patch/game_tools/randomizer/ 2>/dev/null || true"
# 临时传输件
"$ADB" -s "$P" shell "[ -f /sdcard/Download/wine92.tar ] && mv /sdcard/Download/wine92.tar /sdcard/ccz_patch/tmp/ || true"

# 设备侧说明
"$ADB" -s "$P" push "$HERE/../artifacts/device_ccz_patch_README.txt" /sdcard/ccz_patch/README.txt >/dev/null 2>&1 || true

"$ADB" -s "$P" shell "echo ==== /sdcard/ccz ====; ls -la /sdcard/ccz; echo ==== /sdcard/ccz_patch ====; find /sdcard/ccz_patch -maxdepth 3"
