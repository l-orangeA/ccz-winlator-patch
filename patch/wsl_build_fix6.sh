#!/bin/bash
python3 /mnt/d/unpack_tools/wsl_fix4.py
python3 /mnt/d/unpack_tools/wsl_fix6_virtual.py
cd /root/wine0905
make -j8 dlls/ntdll/ntdll.so > /root/make_fix6.log 2>&1
echo "MAKE_RC=$?"
tail -4 /root/make_fix6.log
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_fix6.so
md5sum /mnt/d/unpack_tools/ntdll_fix6.so
