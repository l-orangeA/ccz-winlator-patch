#!/bin/bash
export DEBIAN_FRONTEND=noninteractive
apt-get install --print-uris -y --no-install-recommends mawk > /root/mawk.txt 2>&1
grep "^'http" /root/mawk.txt | head -3
