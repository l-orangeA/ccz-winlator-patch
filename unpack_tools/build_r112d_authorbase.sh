#!/bin/bash
# 在「作者版构建配置」上打 r112c+r113+r112d 六处补丁，编出能与 B 格严格单变量对照的二进制。
# 在 WSL 跑：bash /mnt/d/ccz_winlator_patch/unpack_tools/build_r112d_authorbase.sh
#
# 依据：/root/box64-0.4.4/build/box64 == 作者版 a7360b1e（ARM64=ON / ARM_DYNAREC=ON /
#       显式 dynamic-linker 指向 imagefs）；ntf 树配置不同，编出的二进制在容器里起不来。
#       两树 src 差异仅 signals.c / signal32.c 两文件共 6 处补丁（已 diff 确认）。
set -e
OFF=/root/box64-0.4.4
NTF=/root/box64-0.4.4ntf
OUT=/mnt/d/ccz_winlator_patch/build
BK=/root/official_src_backup_20261108
BASE=a7360b1e71ef26e515ce78dc40bc97e6
S=$OFF/src/libtools/signals.c
T=$OFF/src/libtools/signal32.c

mkdir -p "$BK"
cp "$S" "$BK/signals.c"
cp "$T" "$BK/signal32.c"
restore_src() {
    cp "$BK/signals.c" "$S"
    cp "$BK/signal32.c" "$T"
    touch "$S" "$T"
}
trap restore_src EXIT
echo "★ 官方树源码已备份到 $BK（任何失败自动恢复）"

echo "=== 1/3 覆盖为 ntf 补丁版源码 ==="
cp "$NTF/src/libtools/signals.c" "$S"
cp "$NTF/src/libtools/signal32.c" "$T"
touch "$S" "$T"
n=$({ grep -c "r112c\|r113\|r112d" "$S" "$T" || true; } | awk -F: '{s+=$NF} END{print s+0}')
echo "  补丁标记数=$n (want 6)"
[ "$n" = "6" ] || { echo "ERROR: 补丁未完整应用"; exit 1; }

echo "=== 2/3 用作者版 build 配置编译 ==="
make -C "$OFF/build" -j8 box64 > /root/build_authorbase.log 2>&1 || { tail -30 /root/build_authorbase.log; exit 1; }
tail -3 /root/build_authorbase.log
M=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  patched md5=$M"
[ "$M" != "$BASE" ] || { echo "ERROR: 二进制未变，增量 make 假成功"; exit 1; }
echo -n "  与 base 差异字节数: "
cmp -l "$OUT/../unpack_tools/box64_044_glibc231" "$OFF/build/box64" | wc -l
cp "$OFF/build/box64" "$OUT/box64_r112d_authorbase.so"
file "$OUT/box64_r112d_authorbase.so"

echo "=== 3/3 恢复官方源码并重编 ==="
restore_src
make -C "$OFF/build" -j8 box64 > /root/build_official_restore.log 2>&1 || { tail -30 /root/build_official_restore.log; exit 1; }
R=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
n2=$({ grep -c "r112c\|r113\|r112d" "$S" "$T" || true; } | awk -F: '{s+=$NF} END{print s+0}')
echo "  restored md5=$R  补丁标记数=$n2 (want 0)"
[ "$n2" = "0" ] || { echo "ERROR: 源码未复原"; exit 1; }
echo -n "  与原作者版差异字节数（构建元数据，应 <100）: "
cmp -l "$OUT/../unpack_tools/box64_044_glibc231" "$OFF/build/box64" | wc -l
trap - EXIT
echo "PASS: build/box64_r112d_authorbase.so 已产出，官方树已复原"
