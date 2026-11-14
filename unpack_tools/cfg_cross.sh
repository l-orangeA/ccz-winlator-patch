#!/bin/bash
cd /root
rm -rf wine-x64
cp -a wine-9.2-custom-main wine-x64
cd wine-x64
./configure --host=x86_64-linux-gnu --enable-archs=i386,x86_64 \
  --without-x --without-freetype --without-fontconfig --without-gnutls \
  --without-alsa --without-pulse --without-dbus --without-unwind \
  --without-vulkan --without-sdl --without-udev --without-krb5 --without-cups \
  --without-netapi --without-sane --without-usb --without-gphoto --without-gstreamer \
  --without-oss --without-v4l2 --without-capi --without-pcap > /root/cfg_cross.log 2>&1
echo "CFG_RC=$?" >> /root/cfg_cross.log
tail -20 /root/cfg_cross.log
