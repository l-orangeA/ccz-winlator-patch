#!/bin/bash
# 编译 X26_SYNC 引擎版，并把源码树复原。
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_engine.sh
set -e
OFF=/root/box64-0.4.4
OUT=/mnt/d/ccz_winlator_patch/build
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
BK=/root/engine_backup_20261108
BASE=a7360b1e71ef26e515ce78dc40bc97e6
ASM=$OFF/src/dynarec/arm64/arm64_set_flags.S

mkdir -p "$BK"
cp "$OFF/CMakeLists.txt" "$BK/CMakeLists.txt"
cp "$OFF/src/dynarec/dynarec.c" "$BK/dynarec.c"
restore_src() {
    cp "$BK/CMakeLists.txt" "$OFF/CMakeLists.txt"
    cp "$BK/dynarec.c" "$OFF/src/dynarec/dynarec.c"
    [ -f "$ASM" ] && mv "$ASM" "$BK/arm64_set_flags.S.last"
    touch "$OFF/CMakeLists.txt" "$OFF/src/dynarec/dynarec.c"
}
trap restore_src EXIT

echo "=== 1/3 应用 X26_SYNC（X1+X2+X3）==="
python3 "$TOOLS/patch_engine_x26.py"
tail -6 "$ASM"
grep -c "arm64_set_flags.S" "$OFF/CMakeLists.txt"
grep -c "X26_SYNC" "$OFF/src/dynarec/dynarec.c"
touch "$OFF/CMakeLists.txt" "$OFF/src/dynarec/dynarec.c"

echo "=== 2/3 作者版配置编译（CMake 会重跑）==="
make -C "$OFF/build" -j8 box64 > /root/build_engine.log 2>&1 || { tail -50 /root/build_engine.log; exit 1; }
tail -3 /root/build_engine.log
M=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  ENGINE md5=$M"
[ "$M" != "$BASE" ] || { echo "ERROR: 二进制未变"; exit 1; }
cp "$OFF/build/box64" "$OUT/box64_ENGINE.so"
file "$OUT/box64_ENGINE.so"
nm "$OUT/box64_ENGINE.so" 2>/dev/null | grep -c arm64_set_flags || true

echo "=== 3/3 复原官方树并重编 ==="
restore_src
make -C "$OFF/build" -j8 box64 > /root/build_restore_engine.log 2>&1 || { tail -40 /root/build_restore_engine.log; exit 1; }
R=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  restored md5=$R (want $BASE)"
[ "$R" = "$BASE" ] || { echo "ERROR: 官方树未复原"; exit 1; }
trap - EXIT
echo "PASS: build/box64_ENGINE.so 已产出，官方树复原"
