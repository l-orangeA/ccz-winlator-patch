#!/bin/bash
# 校验树的 primitives.c 相对官方 tarball 的差异（应只有我的 r109 系改动）
set -e
cd /tmp
rm -rf wine_check && mkdir wine_check
cd wine_check
# 只解出需要的文件
tar -xzf /mnt/d/ccz_winlator_patch/unpack_tools/wine-20240905.tar.gz wine-20240905/dlls/win32u/dibdrv/primitives.c 2>/dev/null || tar -xzf /mnt/d/ccz_winlator_patch/unpack_tools/wine-20240905.tar.gz --wildcards '*/dlls/win32u/dibdrv/primitives.c'
PRISTINE=$(find . -name primitives.c | head -1)
echo "pristine: $PRISTINE ($(wc -c < "$PRISTINE") bytes)"
diff -u "$PRISTINE" /root/wine0905/dlls/win32u/dibdrv/primitives.c | head -80
echo "=== diff line count ==="
diff "$PRISTINE" /root/wine0905/dlls/win32u/dibdrv/primitives.c | wc -l
