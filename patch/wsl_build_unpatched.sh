#!/bin/bash
cd /root/wine
cp /mnt/d/unpack_tools/wc_signal_x86_64.c dlls/ntdll/unix/signal_x86_64.c
make -j8 dlls/ntdll/ntdll.so > /root/make2.log 2>&1
echo "MAKE_RC=$?"
tail -3 /root/make2.log
ls -la dlls/ntdll/ntdll.so
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_x64_unpatched.so
md5sum /mnt/d/unpack_tools/ntdll_x64_unpatched.so
