#!/bin/bash
# 检查 wineserver 进程的 environ（由 adb shell 调用，run-as 已在外层）
cat /proc/14124/environ 2>/dev/null | tr '\0' '\n' | grep -E 'WINE_|BOX64_|WINEDEBUG' | head -15
echo ---
ls -la /proc/14124/exe 2>/dev/null
grep Cpus_allowed_list /proc/14124/status 2>/dev/null
