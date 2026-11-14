#!/system/bin/sh
# Wait-mode companion: runs as proot tracee, waits for an EXTERNAL SIGTRAP.
R=/data/local/tmp/proot_sig_result.txt
P=/data/local/tmp/proot_sig_pid.txt
: > "$R"
echo $$ > "$P"
trap 'echo GOT_SIGTRAP=1 > "$R"; exit 0' 5
i=0
while [ "$i" -lt 150 ]; do
  sleep 0.1
  i=$(( i + 1 ))
done
if [ ! -s "$R" ]; then echo GOT_SIGTRAP=0 > "$R"; fi
