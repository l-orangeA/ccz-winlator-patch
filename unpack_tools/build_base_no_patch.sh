#!/bin/bash
# Phase 0：编出「同配置、无补丁」对照二进制 base_no_patch
# 在 WSL 跑：bash /mnt/d/ccz_winlator_patch/unpack_tools/build_base_no_patch.sh
#
# 依据：/root/box64-0.4.4ntf 与官方 /root/box64-0.4.4 的 src 差异经 diff 确认
#       仅 signals.c / signal32.c 两文件、共 6 处补丁（r112c×4 + r113 + r112d）。
# 做法：把这两个文件覆盖成官方版 → 复用 ntf 的 build 目录（同 CMake 配置）重编
#       → 存出 base_no_patch → 恢复补丁版源码并重编回 r112d（md5 校验）。
# 验收：base md5 != r112d；恢复后 md5 == 18b1f670...
set -e
NTF=/root/box64-0.4.4ntf
OFF=/root/box64-0.4.4
OUT=/mnt/d/ccz_winlator_patch/build
BK=/root/ntf_patch_src_20261108
WANT=18b1f670ab3bd54f933a32ac0b7d6075
S=$NTF/src/libtools/signals.c
T=$NTF/src/libtools/signal32.c

mkdir -p "$BK"
cp "$S" "$BK/signals.c"
cp "$T" "$BK/signal32.c"
restore_src() {
    cp "$BK/signals.c" "$S"
    cp "$BK/signal32.c" "$T"
    touch "$S" "$T"
}
trap restore_src EXIT
echo "★ 补丁版源码已备份到 $BK（任何失败都会自动恢复）"

echo "=== 1/3 覆盖为官方无补丁源码 ==="
cp "$OFF/src/libtools/signals.c" "$S"
cp "$OFF/src/libtools/signal32.c" "$T"
touch "$S" "$T"
n=$({ grep -c "r112c\|r113\|r112d" "$S" "$T" || true; } | awk -F: '{s+=$NF} END{print s+0}')
echo "  补丁标记数=$n (want 0)"
[ "$n" = "0" ] || { echo "ERROR: 源码未清干净"; exit 1; }

echo "=== 2/3 复用 ntf build 目录编译（同 CMake 配置）==="
make -C "$NTF/build" -j8 box64 > /root/build_base.log 2>&1 || { tail -30 /root/build_base.log; exit 1; }
tail -3 /root/build_base.log
BASE=$(md5sum "$NTF/build/box64" | cut -d' ' -f1)
echo "  base md5=$BASE"
[ "$BASE" != "$WANT" ] || { echo "ERROR: 二进制未变，增量 make 假成功"; exit 1; }
cp "$NTF/build/box64" "$OUT/box64_base_no_patch.so"
file "$OUT/box64_base_no_patch.so"

echo "=== 3/3 恢复 r112d 源码并重新编译 ==="
restore_src
make -C "$NTF/build" -j8 box64 > /root/build_r112d_restore.log 2>&1 || { tail -30 /root/build_r112d_restore.log; exit 1; }
R=$(md5sum "$NTF/build/box64" | cut -d' ' -f1)
echo "  restored md5=$R (want $WANT)"
[ "$R" = "$WANT" ] || { echo "ERROR: 恢复后 md5 不一致，WSL 树状态需人工确认"; exit 1; }
trap - EXIT
echo "PASS: build/box64_base_no_patch.so 已产出，WSL 树已复原为 r112d"
