#!/bin/bash
cd /root/fix2
for f in *.tar; do
  tar -xf "$f" -C /
done
echo EXTRACT_DONE
pkg-config --version
x86_64-w64-mingw32-gcc --version 2>&1 | head -1
i686-w64-mingw32-gcc --version 2>&1 | head -1
perl --version 2>&1 | head -2
