#!/bin/bash
python3 /mnt/d/unpack_tools/wsl_fix2.py
sed -n '1435,1455p' /root/wine0905/dlls/ntdll/unix/signal_x86_64.c
cd /root/wine0905
make -j8 dlls/ntdll/ntdll.so > /root/make_fix2.log 2>&1
echo "MAKE_RC=$?"
tail -3 /root/make_fix2.log
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_fix2.so
md5sum /mnt/d/unpack_tools/ntdll_fix2.so
