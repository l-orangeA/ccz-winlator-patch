#!/bin/bash
python3 /mnt/d/unpack_tools/wsl_fix_dbg.py
sed -n '1544,1560p' /root/wine0905/dlls/ntdll/unix/signal_x86_64.c
cd /root/wine0905
make -j8 dlls/ntdll/ntdll.so > /root/make_dbg.log 2>&1
echo "MAKE_RC=$?"
tail -3 /root/make_dbg.log
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_dbg.so
md5sum /mnt/d/unpack_tools/ntdll_dbg.so
