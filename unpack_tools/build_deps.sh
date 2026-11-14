#!/bin/bash
export DEBIAN_FRONTEND=noninteractive
PKGS="build-essential flex bison pkg-config gettext libx11-dev libxext-dev libxrender-dev libxi-dev libxcursor-dev libxrandr-dev libxcomposite-dev libxinerama-dev libxfixes-dev libxkbcommon-dev libfreetype6-dev libfontconfig1-dev libgnutls28-dev libunwind-dev libpulse-dev libasound2-dev libdbus-1-dev libxml2-dev libncurses-dev libgl-dev libegl-dev gcc-mingw-w64-i686 gcc-mingw-w64-x86-64"
apt-get install --print-uris -y --no-install-recommends $PKGS > /root/uris.txt 2>&1
echo "RC=$?" >> /root/uris.txt
grep -c "^'http" /root/uris.txt
