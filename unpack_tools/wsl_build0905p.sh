#!/bin/bash
python3 /mnt/d/unpack_tools/wsl_patch0905.py
grep -n mprotect /root/wine0905/dlls/ntdll/unix/signal_x86_64.c | head -2
cd /root/wine0905
make -j8 dlls/ntdll/ntdll.so > /root/make0905p.log 2>&1
echo "MAKE_RC=$?"
tail -3 /root/make0905p.log
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_0905_patched.so
md5sum /mnt/d/unpack_tools/ntdll_0905_patched.so
