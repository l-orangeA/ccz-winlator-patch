#!/bin/bash
cat files/imagefs/home/xuser-1/.wine/drive_c/tftest.txt 2>/dev/null || echo NO-FILE
echo ---
pidof Ekd5.exe
pidof com.winlatcn
