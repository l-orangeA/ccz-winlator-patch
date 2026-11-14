"""补漏: signal32.c:798 的 no_tf=1。在 WSL 跑。"""
P = "/root/box64-0.4.4ntf/src/libtools/signal32.c"
OLD = "    if(ACCESS_FLAG(F_TF)) emu->flags.no_tf = 1;"
NEW = "    /* r112c: removed no_tf=1 (line 798) */"
src = open(P, encoding="utf-8", errors="replace").read()
n = src.count(OLD)
print("count:", n)
if n == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(OLD, NEW))
    print("patched")
elif NEW in src:
    print("already patched")
else:
    raise SystemExit("unexpected count")
