#!/bin/bash
cd /root
mkdir -p debs
tar -xf debs.tar -C debs
dpkg -i debs/*.deb > /root/dpkg.log 2>&1
echo "DPKG_RC=$?" >> /root/dpkg.log
dpkg --configure -a >> /root/dpkg.log 2>&1
echo "CONF_RC=$?" >> /root/dpkg.log
tail -6 /root/dpkg.log
