#!/bin/bash
cd /root
rm -rf wine wine-9.2-custom-main
tar -xzf /mnt/d/unpack_tools/wine-src-proxy.tar.gz -C /root
mv /root/wine-9.2-custom-main /root/wine
cp /mnt/d/unpack_tools/signal_x86_64.c.new /root/wine/dlls/ntdll/unix/signal_x86_64.c
echo "patch check:"; grep -n mprotect /root/wine/dlls/ntdll/unix/signal_x86_64.c | head -2
cd /root/wine
./configure --enable-archs=i386,x86_64 \
  --without-x --without-freetype --without-fontconfig --without-gnutls \
  --without-alsa --without-pulse --without-dbus --without-unwind \
  --without-vulkan --without-sdl --without-udev --without-krb5 --without-cups \
  --without-netapi --without-sane --without-usb --without-gphoto --without-gstreamer \
  --without-oss --without-v4l2 --without-capi --without-pcap > /root/cfg.log 2>&1
echo "CFG_RC=$?"
tail -4 /root/cfg.log
make -j8 dlls/ntdll/ntdll.so > /root/make.log 2>&1
echo "MAKE_RC=$?"
tail -4 /root/make.log
ls -la dlls/ntdll/ntdll.so
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_x64.so
md5sum /mnt/d/unpack_tools/ntdll_x64.so
