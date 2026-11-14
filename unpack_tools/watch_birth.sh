#!/bin/bash
# 设备侧: 每 50ms 轮询 Ekd5 出生, 出生瞬间抓 maps 到 /data/local/tmp
for i in $(seq 1 800); do
  p=$(pidof Ekd5.exe 2>/dev/null)
  if [ -n "$p" ]; then
    echo "CAUGHT pid=$p at $(date +%s.%N)"
    cat /proc/$p/maps > /data/local/tmp/dying_maps.txt 2>/dev/null
    echo "maps saved ($(wc -l < /data/local/tmp/dying_maps.txt) lines)"
    # 等死亡
    n=0
    while [ -n "$(pidof Ekd5.exe 2>/dev/null)" ] && [ $n -lt 200 ]; do
      sleep 0.1; n=$((n+1))
    done
    echo "process gone or 20s"
    exit 0
  fi
  sleep 0.05
done
echo "NEVER BORN in 40s"
