#!/usr/bin/env python3
"""death_b64log.log 尾部分析: box64 自己的日志行 + 最后事件."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\death_b64log.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
print("total lines:", len(lines))
# box64 的行 = 以 "BOX64" / 含 "dynarec" / "SIGSEGV with Access" / "Repeated" / "Warning" 等
b64 = [ln for ln in lines if any(k in ln for k in (
    "SIGSEGV with Access", "Repeated SIGSEGV", "Warning, addr inside", "Dynarec block",
    "mprotect", "unprotectDB", "cleanDB", "Block is:", "Double SIGSEGV", "refreshProtection"))]
print("box64 interesting lines:", len(b64))
for ln in b64[-30:]:
    print("  " + ln[:190])
print("=== last 15 raw lines ===")
for ln in lines[-15:]:
    print("  " + ln[:190])
