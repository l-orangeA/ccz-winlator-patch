#!/system/bin/sh
D=/data/local/tmp/cczcap
mkdir -p $D
LAYER=$(dumpsys SurfaceFlinger --list 2>/dev/null | sed -n 's/.*\(SurfaceView\[[^]]*\](BLAST)#[0-9]*\).*/\1/p' | grep XServerDisplayActivity | head -1)
{
  echo "layer=$LAYER"
  date
  echo "app=$(pidof com.winlatcn)"
  echo "game=$(pidof Ekd5.exe)"
  echo "wine=$(pidof wineserver)"
} > $D/meta.txt
n=0
while true; do
  ts=$(date +%s)
  echo "### $ts" >> $D/sf.log
  dumpsys SurfaceFlinger --latency "$LAYER" >> $D/sf.log 2>/dev/null
  {
    echo "### $ts"
    dumpsys display 2>/dev/null | grep -m1 'mActiveModeId'
    for c in 0 1 2 3 4 5 6 7; do
      echo "cpu$c=$(cat /sys/devices/system/cpu/cpu$c/cpufreq/scaling_cur_freq 2>/dev/null)"
    done
    echo "gpu=$(cat /sys/class/devfreq/13000000.mali/cur_freq 2>/dev/null)"
    for p in $(pidof com.winlatcn) $(pidof Ekd5.exe) $(pidof wineserver); do
      echo "pid$p=$(cat /proc/$p/stat 2>/dev/null | awk '{print $14" "$15}')"
    done
    grep -E 'MemFree|MemAvailable|SwapFree' /proc/meminfo
    ps -A -o PID,%CPU,NAME --sort=-%cpu 2>/dev/null | head -8
    if [ $((n % 3)) -eq 0 ]; then
      for z in /sys/class/thermal/thermal_zone*/temp; do
        echo "tz $z $(cat $z 2>/dev/null)"
      done
    fi
  } >> $D/sys.log 2>/dev/null
  n=$((n + 1))
  sleep 2
done
