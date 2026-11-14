#!/bin/bash
set -e
cat /etc/os-release | head -2
uname -m
sed -i 's|http://archive.ubuntu.com/ubuntu/|http://mirrors.tuna.tsinghua.edu.cn/ubuntu/|g; s|http://security.ubuntu.com/ubuntu/|http://mirrors.tuna.tsinghua.edu.cn/ubuntu/|g' /etc/apt/sources.list
apt-get update > /tmp/apt.log 2>&1 || (tail -20 /tmp/apt.log; exit 1)
DEBIAN_FRONTEND=noninteractive apt-get install -y build-essential flex bison pkg-config gettext gcc-mingw-w64-i686 gcc-mingw-w64-x86-64 >> /tmp/apt.log 2>&1
echo "APT_RC=$?"
gcc --version | head -1
x86_64-w64-mingw32-gcc --version | head -1
