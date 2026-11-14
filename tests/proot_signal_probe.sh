#!/system/bin/sh
# P0-A final gap: does proot forward an external SIGTRAP to its tracee?
# Uses only toybox/mksh builtins - no glibc, no /proc/self/exe dependency.
R=/data/local/tmp/proot_sig_result.txt
P=/data/local/tmp/proot_sig_pid.txt
: > "$R"
echo $$ > "$P"
trap 'echo GOT_SIGTRAP=1 > "$R"; exit 0' 5
( sleep 1; kill -5 "$(cat "$P")" ) &
i=0
while [ "$i" -lt 80 ]; do
  sleep 0.1
  i=$(( i + 1 ))
  if [ -s "$R" ]; then break; fi
done
if [ ! -s "$R" ]; then echo GOT_SIGTRAP=0 > "$R"; fi
cat "$R"
