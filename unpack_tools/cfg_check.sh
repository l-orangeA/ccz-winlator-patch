#!/bin/bash
cd /root/wine0905
echo '=== configure 命令行 ==='
head -20 config.log | grep -A3 'configure command\|running configure' || grep -m1 -B2 -A8 'configure' config.log | head -20
echo '=== 关键可选组件 ==='
grep -E 'freetype|vulkan|opengl|GL_X11|xinerama|fontconfig' config.log | grep -iE 'checking|yes|no' | head -20
echo '=== 我们的 win32u UND 符号清单 ==='
readelf --dyn-syms -W dlls/win32u/win32u.so | grep ' UND ' | awk '{print $8}' | sort > /tmp/und_new.txt
wc -l /tmp/und_new.txt
echo '=== 原版 UND 清单 ==='
readelf --dyn-syms -W /mnt/d/ccz_winlator_patch/device_backup/win32u_orig_20261106/win32u.so | grep ' UND ' | awk '{print $8}' | sort > /tmp/und_old.txt
wc -l /tmp/und_old.txt
echo '=== 共有/独有统计 ==='
comm -12 /tmp/und_old.txt /tmp/und_new.txt | wc -l
echo '--- orig only ---'
comm -23 /tmp/und_old.txt /tmp/und_new.txt
echo '--- new only ---'
comm -13 /tmp/und_old.txt /tmp/und_new.txt
