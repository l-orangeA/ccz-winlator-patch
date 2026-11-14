#!/bin/bash
cd /root
rm -rf wine0905 wine-9.2-custom-a48ee6df4a77
tar -xzf /mnt/d/unpack_tools/wine-20240905.tar.gz -C /root
mv /root/wine-9.2-custom-a48ee6df4a77 /root/wine0905
cd /root/wine0905
./configure --enable-archs=i386,x86_64 \
  --without-x --without-freetype --without-fontconfig --without-gnutls \
  --without-alsa --without-pulse --without-dbus --without-unwind \
  --without-vulkan --without-sdl --without-udev --without-krb5 --without-cups \
  --without-netapi --without-sane --without-usb --without-gphoto --without-gstreamer \
  --without-oss --without-v4l2 --without-capi --without-pcap > /root/cfg0905.log 2>&1
echo "CFG_RC=$?"
tail -3 /root/cfg0905.log
make -j8 dlls/ntdll/ntdll.so > /root/make0905.log 2>&1
echo "MAKE_RC=$?"
tail -3 /root/make0905.log
ls -la dlls/ntdll/ntdll.so
cp dlls/ntdll/ntdll.so /mnt/d/unpack_tools/ntdll_0905.so
md5sum /mnt/d/unpack_tools/ntdll_0905.so
