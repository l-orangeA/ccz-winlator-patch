#!/bin/bash
cd /root/wine0905
echo '=== Makefile mtime ==='
ls -la Makefile
echo '=== line 197110-197125 ==='
sed -n '197110,197125p' Makefile | cat -A | head -30
