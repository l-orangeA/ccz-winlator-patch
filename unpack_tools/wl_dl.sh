#!/bin/bash
URL="https://ghfast.top/https://github.com/brunodev85/winlator/releases/download/v6.1.0/Winlator_6.1.apk"
OUT=/d/unpack_tools/Winlator_6.1.apk
TARGET=265585916
for i in $(seq 1 200); do
  SZ=$(stat -c %s "$OUT" 2>/dev/null || echo 0)
  if [ "$SZ" -ge "$TARGET" ]; then echo "DONE size=$SZ"; break; fi
  curl -sL -C - --max-time 300 -o "$OUT" "$URL"
  SZ2=$(stat -c %s "$OUT" 2>/dev/null || echo 0)
  echo "attempt $i size=$SZ2"
  if [ "$SZ2" -le "$SZ" ]; then sleep 3; fi
done
ls -l "$OUT"
