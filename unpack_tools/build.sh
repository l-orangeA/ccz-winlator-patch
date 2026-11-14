#!/bin/bash
cd /root/wine-9.2-custom-main
make -j8 dlls/ntdll/ntdll.so > /root/make.log 2>&1
echo "MAKE_RC=$?" >> /root/make.log
tail -25 /root/make.log
