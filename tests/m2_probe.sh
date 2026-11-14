#!/system/bin/sh
# M2 end-to-end probe, runs as app uid (run-as com.winlatcn sh <this>)
B=/data/user/0/com.winlatcn/files
SOCK=$B/imagefs/tmp/hwbp.sock
LIB=/data/app/~~2-cHIP9edHUE7U4iEfTEVw==/com.winlatcn-9PdxA7RF9xev6guyO9XhcA==/lib/arm64

cd "$B" || { echo "NO_APPDIR"; exit 1; }
rm -f "$SOCK" helper.log

./perf_helper_probe "$SOCK" > helper.log 2>&1 &
H=$!
sleep 1

echo "---SOCKET---"
ls -l "$SOCK" 2>&1

echo "---CLIENT(app uid -> game tid)---"
P=$(pidof Ekd5.exe)
A=$(grep -m1 rw-p /proc/"$P"/maps | cut -d- -f1)
echo "P=$P A=$A"
./perf_helper_client "$SOCK" "$P" "0x$A" 1 3
echo "client_rc=$?"

echo "---PROOT_VISIBILITY---"
TINYPROOT_LOADER=$LIB/libtinyproot-loader.so "$LIB/libtinyproot.so" \
  --rootfs="$B/imagefs" --cwd=/ /system/bin/sh -c "ls -l /tmp/hwbp.sock" 2>&1

echo "---HELPER_LOG---"
cat helper.log 2>&1

kill $H 2>/dev/null
echo "---DONE---"
