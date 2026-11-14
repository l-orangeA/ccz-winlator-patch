#!/bin/bash
cd /root
mkdir -p crossdebs
tar -xf crossdebs.tar -C crossdebs
n=0; f=0
for d in crossdebs/*.deb; do
  if dpkg-deb -x "$d" / >/dev/null 2>>/root/cross_extract.log; then n=$((n+1)); else f=$((f+1)); echo "FAIL $d" >> /root/cross_extract.log; fi
done
echo "extracted=$n fail=$f"
ls -la /usr/bin/x86_64-linux-gnu-gcc* 2>&1 | head -5
ls -la /usr/x86_64-linux-gnu/lib/libc.so.6 2>&1
