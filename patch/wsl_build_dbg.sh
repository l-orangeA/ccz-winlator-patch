#!/bin/bash
python3 /mnt/d/unpack_tools/wsl_dbg0905.py
grep -n WINE_MPROTECT_STACK /root/wine0905/dlls/ntdll/unix/signal_x86_64.c | head -2
cd /root/wine0905
make -j8 dlls/ntdll/ntdll.so > /root/make_dbg.log 2>&1
echo "MAKE_RC=$?"
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_dbg.so
md5sum /mnt/d/unpack_tools/ntdll_dbg.so
