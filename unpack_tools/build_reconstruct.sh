#!/bin/bash
# 路线 A 构建：bash build_reconstruct.sh a1|a2|a3
# 每级从官方无补丁源码累积应用，作者版配置编译，产物独立 md5，结束复原官方树。
set -e
STAGE=$1
case "$STAGE" in a1|a2|a3|a3probe|a3b|a3c|a3d) ;; *) echo "usage: build_reconstruct.sh a1|a2|a3|a3probe|a3b|a3c|a3d|a3b"; exit 1;; esac

OFF=/root/box64-0.4.4
OUT=/mnt/d/ccz_winlator_patch/build
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
BK=/root/official_signals_backup_20261108
BASE=a7360b1e71ef26e515ce78dc40bc97e6
S=$OFF/src/libtools/signals.c
UP=$(echo "$STAGE" | tr a-z A-Z)

mkdir -p "$BK"
cp "$S" "$BK/signals.c.$STAGE"
restore_src() { cp "$BK/signals.c.$STAGE" "$S"; touch "$S"; }
trap restore_src EXIT

echo "=== 1/3 应用 $UP 补丁 ==="
python3 "$TOOLS/apply_reconstruct.py" "$STAGE"
grep -c "A[123]:" "$S"
touch "$S"

echo "=== 2/3 作者版配置编译 ==="
make -C "$OFF/build" -j8 box64 > /root/build_$STAGE.log 2>&1 || { tail -40 /root/build_$STAGE.log; exit 1; }
M=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  $UP md5=$M"
[ "$M" != "$BASE" ] || { echo "ERROR: 二进制未变，增量 make 假成功"; exit 1; }
cp "$OFF/build/box64" "$OUT/box64_$UP.so"
file "$OUT/box64_$UP.so"

echo "=== 3/3 复原官方树并重编 ==="
restore_src
make -C "$OFF/build" -j8 box64 > /root/build_restore_$STAGE.log 2>&1 || { tail -30 /root/build_restore_$STAGE.log; exit 1; }
R=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  restored md5=$R (want $BASE)"
[ "$R" = "$BASE" ] || { echo "ERROR: 官方树未复原"; exit 1; }
trap - EXIT
echo "PASS: build/box64_$UP.so 已产出，官方树复原"
