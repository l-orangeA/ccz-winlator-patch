#!/bin/bash
nm -D --defined-only /root/wine/dlls/ntdll/ntdll.so | awk '{print $NF}' | sort -u > /tmp/new.txt
nm -D --defined-only /mnt/d/unpack_tools/ntdll.so | awk '{print $NF}' | sort -u > /tmp/old.txt
echo "new=$(wc -l < /tmp/new.txt) old=$(wc -l < /tmp/old.txt)"
echo "-- only in old:"; comm -23 /tmp/old.txt /tmp/new.txt | head -15
echo "-- only in new:"; comm -13 /tmp/old.txt /tmp/new.txt | head -15
echo "-- needed libs old:"; readelf -d /mnt/d/unpack_tools/ntdll.so | grep NEEDED
echo "-- needed libs new:"; readelf -d /root/wine/dlls/ntdll/ntdll.so | grep NEEDED
