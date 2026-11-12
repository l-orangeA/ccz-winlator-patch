#!/bin/bash
# canonical 配方：重建当前部署版 build/ntdll_fix110b.so
#
# 在 WSL Ubuntu2004x64 里执行：
#   bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_fix110b.sh
#
# 输入 : src/fix110b/{signal_x86_64.c,virtual.c,thread.c}   （make_fix110b_snapshot.py 生成的源码快照）
# 输出 : /root/wine0905/dlls/ntdll/ntdll.so → build/ntdll_fix110b.so
# 验收 : md5 == eddb74af96587b15589d430b1199a07b
# 状态 : 2026-11-08 12:42 在 WSL Ubuntu2004x64 实跑 PASS，got == want；
#        产物 / WSL 树 / 手机实机三方 md5 一致。
#
# 关键前提（缺一个 md5 就对不上）：
#   1) 源码树必须是 wine-20240905.tar.gz（wine-9.2-custom-a48ee6df4a77）。
#   2) configure 必须带 --with-freetype --with-fontconfig —— 这棵树 11-06 为 r109g 重配过，
#      fix110b.so 是在该 config.h 下编的。旧的 wsl_build0905.sh 用 --without-*，
#      config.h 不同 → 产物不同。且旧脚本里的 /mnt/d/unpack_tools 路径已失效。
#   3) 顶层 Makefile 里 r109g 注入的 -DNDEBUG 只匹配 dlls/win32u/ 命令行，不影响 ntdll。
#   4) 编译器/工具链需与 11-07 相同（Ubuntu 20.04 + 同一套交叉工具链）。
#
# 若 md5 不一致：不要覆盖 build/ 里的部署版，先 diff include/config.h、Makefile 与编译日志。

set -e
WIN=/root/wine0905
WINPC=/mnt/d/ccz_winlator_patch
WANT=eddb74af96587b15589d430b1199a07b

echo "=== 1/4 源码快照自检 ==="
python3 "$WINPC/unpack_tools/verify_fix110b_sources.py"

if [ ! -d "$WIN/dlls/ntdll/unix" ]; then
    echo "=== 2/4 未发现 $WIN，从 tar 解干净树并 configure ==="
    if [ -e /root/wine-9.2-custom-a48ee6df4a77 ]; then
        echo "已存在 /root/wine-9.2-custom-a48ee6df4a77，先手工确认再跑"; exit 1
    fi
    tar -xzf "$WINPC/unpack_tools/wine-20240905.tar.gz" -C /root
    mv /root/wine-9.2-custom-a48ee6df4a77 "$WIN"
    cd "$WIN"
    ./configure --enable-archs=i386,x86_64 --with-freetype --with-fontconfig \
        --without-x --without-gnutls \
        --without-alsa --without-pulse --without-dbus --without-unwind \
        --without-vulkan --without-sdl --without-udev --without-krb5 --without-cups \
        --without-netapi --without-sane --without-usb --without-gphoto --without-gstreamer \
        --without-oss --without-v4l2 --without-capi --without-pcap > /root/cfg_fix110b.log 2>&1 || {
            tail -30 /root/cfg_fix110b.log; exit 1; }
else
    echo "=== 2/4 复用现有 $WIN，检查配置 ==="
fi

cd "$WIN"
grep -q '^#define HAVE_FREETYPE 1' include/config.h || {
    echo "config.h 缺 HAVE_FREETYPE：需要 --with-freetype --with-fontconfig（r109g 配方）"; exit 1; }
grep -q 'SONAME_LIBFONTCONFIG' include/config.h || {
    echo "config.h 缺 SONAME_LIBFONTCONFIG：配置不对"; exit 1; }
echo "  config.h OK (freetype + fontconfig)"

echo "=== 3/4 覆盖 fix110b 源码并编译 ==="
# 树当前可能是 fix110d，覆盖前先留一份，绝不删改原树其它内容
BK=/root/ntdll_src_backup_$(date +%Y%m%d_%H%M%S)
mkdir -p "$BK"
cp dlls/ntdll/unix/signal_x86_64.c dlls/ntdll/unix/virtual.c dlls/ntdll/unix/thread.c "$BK"/
echo "  现有源码已备份到 $BK"
for f in signal_x86_64.c virtual.c thread.c; do
    cp "$WINPC/src/fix110b/$f" "dlls/ntdll/unix/$f"
done
touch dlls/ntdll/unix/signal_x86_64.c dlls/ntdll/unix/virtual.c dlls/ntdll/unix/thread.c
make -j8 dlls/ntdll/ntdll.so > /root/make_fix110b.log 2>&1 || {
    tail -40 /root/make_fix110b.log; exit 1; }
tail -3 /root/make_fix110b.log

echo "=== 4/4 验收 ==="
GOT=$(md5sum dlls/ntdll/ntdll.so | cut -d' ' -f1)
echo "  got  $GOT"
echo "  want $WANT"
if [ "$GOT" != "$WANT" ]; then
    echo "  md5 不一致：保留 build/ 里的原部署版不动。"
    echo "  排查顺序：include/config.h → 顶层 Makefile → 交叉工具链版本 → /root/make_fix110b.log"
    exit 1
fi
cp dlls/ntdll/ntdll.so "$WINPC/build/ntdll_fix110b.so"
echo "  PASS：build/ntdll_fix110b.so 已更新（内容与原部署版一致）"
