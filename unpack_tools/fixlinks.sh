#!/bin/bash
cd /usr/lib/aarch64-linux-gnu || exit 1
n=0
for f in *.so; do
  [ -L "$f" ] || continue
  t=$(readlink "$f" 2>/dev/null) || continue
  case "$t" in
    /lib/*)
      ln -sfn "../../../lib/${t#/lib/}" "$f"; n=$((n+1));;
  esac
done
echo "fixed=$n"
ls -la libpthread.so libm.so libdl.so libcrypt.so libc.so
