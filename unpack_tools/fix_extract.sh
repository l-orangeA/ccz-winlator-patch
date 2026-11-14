#!/bin/bash
cd /root/fixdata
rm -f /root/fix.log
for f in *.xz; do
  tar -xJf "$f" --hard-dereference -C / >/dev/null 2>>/root/fix.log
  echo "RC=$? $f" >> /root/fix.log
done
tail -4 /root/fix.log
/usr/bin/pkg-config --version
/usr/bin/x86_64-w64-mingw32-gcc --version 2>&1 | head -1
/usr/bin/i686-w64-mingw32-gcc --version 2>&1 | head -1
