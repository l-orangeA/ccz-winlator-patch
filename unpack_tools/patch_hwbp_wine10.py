#!/usr/bin/env python3
"""把 fix110b 的 hwbp 软件模拟移植到 Wine 10.10（v11.2 用的同版本）。

步骤：
1. 从 src/fix110b/virtual.c 提取 HWBP_SOFT 块（4047-4620）
2. 适配 Wine 10 的 host_page_* API 名
3. 插入 /root/wine1010/dlls/ntdll/unix/virtual.c 的 virtual_handle_fault 前
4. 在 virtual_handle_fault 的 WRITEWATCH 分支插入 hwbp 触发
"""
import re
import sys

SRC = "/mnt/d/ccz_winlator_patch/src/fix110b/virtual.c"
DST = "/root/wine1010/dlls/ntdll/unix/virtual.c"
START = "/* HWBP_SOFT: software emulation of x86 hardware breakpoints (ARM hosts) */"
END = "/***********************************************************************\n *           virtual_handle_fault"


def main():
    s = open(SRC, encoding="utf-8", errors="replace").read()
    i = s.index(START)
    j = s.index(END, i)
    block = s[i:j]
    print("hwbp block: %d bytes, %d lines" % (len(block), block.count("\n")))

    # --- API 适配（Wine 10 用 host_page_*）---
    block = block.replace("get_page_vprot(", "get_host_page_vprot(")
    block = block.replace("page_mask", "host_page_mask")
    block = block.replace("page_size", "host_page_size")
    # 上面把 host_page_mask 里的 "page_mask" 也替换了，修回（避免 host_host_page_mask）
    block = block.replace("host_host_page_mask", "host_page_mask")
    block = block.replace("host_host_page_size", "host_page_size")

    d = open(DST, encoding="utf-8", errors="replace").read()
    if "HWBP_SOFT" in d:
        print("already patched, abort")
        return 1
    assert d.count(END) == 1, "anchor count %d" % d.count(END)
    d = d.replace(END, block + END, 1)

    # --- 在 WRITEWATCH 分支插入 hwbp 触发 ---
    old = """        if (vprot & VPROT_WRITEWATCH)
        {
            if (enable_write_exceptions && is_vprot_exec_write( vprot ) && !ntdll_get_thread_data()->allow_writes)"""
    new = """        if (vprot & VPROT_WRITEWATCH)
        {
            if (hwbp_page_watched( page )) { int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9; }
            hwbp_step_flag = 1;   /* HWBP_V2: trap after this store */
            if (enable_write_exceptions && is_vprot_exec_write( vprot ) && !ntdll_get_thread_data()->allow_writes)"""
    assert d.count(old) == 1, "writewatch anchor %d" % d.count(old)
    d = d.replace(old, new, 1)

    open(DST, "w", encoding="utf-8", newline="\n").write(d)
    print("patched:", DST)
    return 0


if __name__ == "__main__":
    sys.exit(main())
