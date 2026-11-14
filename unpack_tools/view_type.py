#!/usr/bin/env python3
"""对比两版日志中 0x400000-0x600000 的视图类型."""
import gzip, re

for tag, path, gz in [("0.3.0", r"D:\ccz_winlator_patch\unpack_tools\a0_trace\v030_full.log.gz", True),
                      ("0.4.4-r113+fix110b", r"D:\ccz_winlator_patch\unpack_tools\a0_trace\f110bv_virtual.log", False)]:
    if gz:
        data = gzip.open(path, "rt", encoding="utf-8", errors="replace").read()
    else:
        data = open(path, "rb").read().decode("utf-8", "replace")
    lines = data.splitlines()
    views = [(i, ln) for i, ln in enumerate(lines) if re.search(r"View: 0x0{0,2}4[0-6][0-9a-f]{4}", ln)]
    print("%s: %d image-region view dumps" % (tag, len(views)))
    for i, ln in views[:6]:
        print("  @%d  %s" % (i, ln[:140]))
    print()
