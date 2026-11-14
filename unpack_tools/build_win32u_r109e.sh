#!/bin/bash
# r109e: 撤销 primitives.c 全部 r109 改动（还原官方 tarball 版本），保留 Makefile 的 -DNDEBUG，
# 重建"树原味" win32u —— 用于判定崩溃源于我的改动还是树的构建配置。
set -e
cd /tmp/wine_check
PRISTINE=$(find . -name primitives.c | head -1)
cp "$PRISTINE" /root/wine0905/dlls/win32u/dibdrv/primitives.c
echo "primitives.c reverted to pristine"
cd /root/wine0905
rm -f dlls/win32u/*.o dlls/win32u/dibdrv/*.o dlls/win32u/i386-windows/*.o dlls/win32u/x86_64-windows/*.o
make -j8 dlls/win32u/win32u.so 2>&1 | tail -3
echo '=== assert refs ==='
readelf --dyn-syms -W dlls/win32u/win32u.so | grep '__assert_fail' && echo 'HAS ASSERT' || echo 'clean (NDEBUG ok)'
cp dlls/win32u/win32u.so /mnt/d/ccz_winlator_patch/build/win32u_r109e.so
md5sum /mnt/d/ccz_winlator_patch/build/win32u_r109e.so
ls -la /mnt/d/ccz_winlator_patch/build/win32u_r109e.so
