#!/bin/bash
set -e
WIN=/root/wine0905
WINPC=/mnt/d/ccz_winlator_patch
OUT="$WINPC/build/ntdll_fix110b_m3a.so"
[ -d "$WIN/dlls/ntdll/unix" ] || { echo "missing WSL wine tree"; exit 1; }
grep -q '^#define HAVE_FREETYPE 1' "$WIN/include/config.h" || { echo "config.h mismatch"; exit 1; }
BK=/root/ntdll_src_backup_m3a_$(date +%Y%m%d_%H%M%S)
mkdir -p "$BK"
cp "$WIN/dlls/ntdll/unix/signal_x86_64.c" "$WIN/dlls/ntdll/unix/virtual.c" "$WIN/dlls/ntdll/unix/thread.c" "$BK"/
for f in signal_x86_64.c virtual.c thread.c; do cp "$WINPC/src/fix110b_m3a/$f" "$WIN/dlls/ntdll/unix/$f"; done
touch "$WIN/dlls/ntdll/unix/signal_x86_64.c" "$WIN/dlls/ntdll/unix/virtual.c" "$WIN/dlls/ntdll/unix/thread.c"
cd "$WIN"
make -j8 dlls/ntdll/ntdll.so > /root/make_m3a_20261111.log 2>&1 || { tail -40 /root/make_m3a_20261111.log; exit 1; }
tail -2 /root/make_m3a_20261111.log
cp "$WIN/dlls/ntdll/ntdll.so" "$OUT"
GOT=$(md5sum "$OUT" | cut -d' ' -f1); PROD=$(md5sum "$WINPC/build/ntdll_fix110b.so" | cut -d' ' -f1)
echo "m3a  md5 = $GOT"; echo "prod md5 = $PROD"
[ "$GOT" != "$PROD" ] || { echo "ERROR identical"; exit 1; }
strings "$OUT" | grep -q '/tmp/hwbp.sock' && echo "SOCK_MARKER_OK"
echo "PASS: $OUT"
