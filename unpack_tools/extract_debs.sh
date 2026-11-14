#!/bin/bash
cd /root
n=0; fail=0
for d in debs/*.deb; do
  if dpkg-deb -x "$d" / >>/root/extract.log 2>&1; then n=$((n+1)); else fail=$((fail+1)); echo "FAIL $d" >> /root/extract.log; fi
done
echo "extracted=$n fail=$fail" >> /root/extract.log
tail -5 /root/extract.log
ls -l /usr/bin/gcc* /usr/bin/g++* /usr/bin/make /usr/bin/flex /usr/bin/bison /usr/bin/x86_64-w64-mingw32-gcc 2>&1 | head
