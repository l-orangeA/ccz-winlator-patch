"""r109d: win32u 加 -DNDEBUG（对齐设备原版构建——原版无任何 assert 引用）。
r109c 的崩溃根因：primitives.c 的 assert(!IsRectEmpty(rc)) 在我们构建里未被裁掉，
标题 GUI 初始化遇到退化矩形即 abort。原版该路径安全跳过。
在 WSL 跑: python3 patch_win32u_r109d.py && cd /root/wine0905 && make -j8 dlls/win32u/win32u.so
"""
import subprocess, sys

MI = "/root/wine0905/dlls/win32u/Makefile.in"

src = open(MI, encoding="utf-8").read()
if "EXTRADEFS = -DNDEBUG" in src:
    print("Makefile.in already has NDEBUG")
else:
    lines = src.split("\n")
    out = []
    inserted = False
    for ln in lines:
        out.append(ln)
        if not inserted and ln.startswith("MODULE"):
            out.append("EXTRADEFS = -DNDEBUG")
            inserted = True
    if not inserted:
        raise SystemExit("MODULE line not found in Makefile.in")
    open(MI, "w", encoding="utf-8", newline="\n").write("\n".join(out))
    print("Makefile.in patched")

r = subprocess.run(["/root/wine0905/config.status", "dlls/win32u/Makefile"],
                   cwd="/root/wine0905", capture_output=True, text=True)
print("config.status rc=%d" % r.returncode)
if r.returncode != 0:
    print(r.stdout[-500:], r.stderr[-500:])
    sys.exit(1)
mk = open("/root/wine0905/dlls/win32u/Makefile", encoding="utf-8", errors="replace").read()
print("Makefile has -DNDEBUG:", "-DNDEBUG" in mk)
