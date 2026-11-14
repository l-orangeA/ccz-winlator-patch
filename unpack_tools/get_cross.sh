#!/bin/bash
export DEBIAN_FRONTEND=noninteractive
apt-get install --print-uris -y --no-install-recommends gcc-9-x86-64-linux-gnu libc6-dev-amd64-cross linux-libc-dev-amd64-cross > /root/cross.txt 2>&1
echo "RC=$?" >> /root/cross.txt
grep "^'http" /root/cross.txt | wc -l
grep "^'http" /root/cross.txt | head -5
