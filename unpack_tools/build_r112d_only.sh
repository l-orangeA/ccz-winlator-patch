#!/bin/bash
# 最小单变量：在作者版配置上只打 r112d 一处（SIGTRAP 不可延迟），不带 r112c/r113。
# 在 WSL 跑：bash /mnt/d/ccz_winlator_patch/unpack_tools/build_r112d_only.sh
set -e
OFF=/root/box64-0.4.4
OUT=/mnt/d/ccz_winlator_patch/build
BK=/root/official_src_backup_20261108
BASE=a7360b1e71ef26e515ce78dc40bc97e6
S=$OFF/src/libtools/signals.c

[ -f "$BK/signals.c" ] || { echo "缺少无补丁源码备份 $BK"; exit 1; }
cp "$BK/signals.c" "$S"
touch "$S"
restore_src() { cp "$BK/signals.c" "$S"; touch "$S"; }
trap restore_src EXIT

echo "=== 1/3 只插入 SIGTRAP 不可延迟 ==="
python3 - <<'PY'
p = '/root/box64-0.4.4/src/libtools/signals.c'
s = open(p, encoding='utf-8').read()
old = '        case X64_SIGABRT:\n            return 0;'
new = '        case X64_SIGABRT:\n        case X64_SIGTRAP:  /* r112d-only: hwbp sim needs immediate forwarding */\n            return 0;'
assert s.count(old) == 1, 'anchor count=%d' % s.count(old)
open(p, 'w', encoding='utf-8', newline='\n').write(s.replace(old, new, 1))
print('inserted 1 patch')
PY
grep -n "r112d-only" "$S"

echo "=== 2/3 用作者版配置编译 ==="
make -C "$OFF/build" -j8 box64 > /root/build_r112d_only.log 2>&1 || { tail -30 /root/build_r112d_only.log; exit 1; }
M=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  r112d-only md5=$M"
[ "$M" != "$BASE" ] || { echo "ERROR: 二进制未变"; exit 1; }
cp "$OFF/build/box64" "$OUT/box64_r112d_only.so"
file "$OUT/box64_r112d_only.so"

echo "=== 3/3 恢复官方源码并重编 ==="
restore_src
make -C "$OFF/build" -j8 box64 > /root/build_official_restore2.log 2>&1 || { tail -30 /root/build_official_restore2.log; exit 1; }
R=$(md5sum "$OFF/build/box64" | cut -d' ' -f1)
echo "  restored md5=$R (want $BASE)"
[ "$R" = "$BASE" ] || { echo "ERROR: 官方树未复原"; exit 1; }
trap - EXIT
echo "PASS: build/box64_r112d_only.so 已产出，官方树复原"
