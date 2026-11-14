#!/bin/bash
# 在作者版构建配置上编译 NOTF_E10，并把源码树复原。
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_notf_author.sh
set -e
OFF=/root/box64-0.4.4
OUT=/mnt/d/ccz_winlator_patch/build
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
BK=/root/official_signals_backup_20261108
BASE=a7360b1e71ef26e515ce78dc40bc97e6
S=$OFF/src/libtools/signals.c

mkdir -p "$BK"
cp "$S" "$BK/signals.c.notf_e10"
restore_src() { cp "$BK/signals.c.notf_e10" "$S"; touch "$S"; }
trap restore_src EXIT

echo "=== 1/3 删除两处 no_tf 写点 ==="
python3 "$TOOLS/patch_box64_044_notf_author.py"
grep -c NOTF_E10 "$S"
touch "$S"

echo "=== 2/3 作者版配置编译 ==="
make -C "$OFF/build" -j8 box64 > /root/build_notf_e10.log 2>&1 || { tail -30 /root/build_notf_e10.log; exit 1; }
M=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  NOTF_E10 md5=$M"
[ "$M" != "$BASE" ] || { echo "ERROR: 二进制未变，增量 make 假成功"; exit 1; }
cp "$OFF/build/box64" "$OUT/box64_notf_author.so"
file "$OUT/box64_notf_author.so"

echo "=== 3/3 复原官方树并重编 ==="
restore_src
make -C "$OFF/build" -j8 box64 > /root/build_official_restore7.log 2>&1 || { tail -30 /root/build_official_restore7.log; exit 1; }
R=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  restored md5=$R (want $BASE)"
[ "$R" = "$BASE" ] || { echo "ERROR: 官方树未复原"; exit 1; }
trap - EXIT
echo "PASS: build/box64_notf_author.so 已产出，官方树复原"
