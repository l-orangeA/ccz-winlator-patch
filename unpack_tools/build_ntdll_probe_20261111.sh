#!/bin/bash
# Build P0-B read/write-split counter variant. Does NOT touch build/ntdll_fix110b.so.
# Run in WSL Ubuntu2004x64:
#   bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_probe_20261111.sh
set -e
WIN=/root/wine0905
WINPC=/mnt/d/ccz_winlator_patch
OUT="$WINPC/build/ntdll_fix110b_probe.so"

[ -d "$WIN/dlls/ntdll/unix" ] || { echo "missing WSL wine tree $WIN"; exit 1; }
grep -q '^#define HAVE_FREETYPE 1' "$WIN/include/config.h" || { echo "config.h mismatch"; exit 1; }

BK=/root/ntdll_src_backup_probe_$(date +%Y%m%d_%H%M%S)
mkdir -p "$BK"
cp "$WIN/dlls/ntdll/unix/signal_x86_64.c" \
   "$WIN/dlls/ntdll/unix/virtual.c" \
   "$WIN/dlls/ntdll/unix/thread.c" "$BK"/
echo "existing WSL sources backed up to $BK"

for f in signal_x86_64.c virtual.c thread.c; do
    cp "$WINPC/src/fix110b_probe/$f" "$WIN/dlls/ntdll/unix/$f"
done
touch "$WIN/dlls/ntdll/unix/signal_x86_64.c" \
      "$WIN/dlls/ntdll/unix/virtual.c" \
      "$WIN/dlls/ntdll/unix/thread.c"

cd "$WIN"
make -j8 dlls/ntdll/ntdll.so > /root/make_probe_20261111.log 2>&1 || {
    tail -40 /root/make_probe_20261111.log; exit 1; }
tail -3 /root/make_probe_20261111.log

cp "$WIN/dlls/ntdll/ntdll.so" "$OUT"
GOT=$(md5sum "$OUT" | cut -d' ' -f1)
PROD=$(md5sum "$WINPC/build/ntdll_fix110b.so" | cut -d' ' -f1)
echo "probe md5 = $GOT"
echo "prod  md5 = $PROD"
[ "$GOT" != "$PROD" ] || { echo "ERROR: probe identical to production build"; exit 1; }
strings "$OUT" | grep -q 'rfault=%lu wfault=%lu' && echo "PROBE_MARKER_OK"
echo "PASS: $OUT"
