#!/bin/bash
cd /root/wine-9.2-custom-main
./configure --enable-archs=i386,x86_64 > /root/configure.log 2>&1
echo "CFG_RC=$?" >> /root/configure.log
tail -20 /root/configure.log
