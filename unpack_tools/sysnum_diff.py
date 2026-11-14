#!/usr/bin/env python3
"""i386 syscall 桩对比 v2: 模式 B8 <id32> BA <ptr32> FF D2"""
import struct

DEVP = "/mnt/d/ccz_winlator_patch/device_backup/win32u_orig_20261106/win32u_i386.dll"
OURP = "/root/wine0905/dlls/win32u/i386-windows/win32u.dll"


def stubs(path):
    d = open(path, "rb").read()
    ids = []
    i = 0
    n = len(d)
    while i < n - 12:
        if d[i] == 0xB8 and d[i+5] == 0xBA and d[i+10] == 0xFF and d[i+11] == 0xD2:
            ids.append(struct.unpack_from("<I", d, i+1)[0])
            i += 12
        else:
            i += 1
    return ids


a = stubs(DEVP)
b = stubs(OURP)
print("device stubs:", len(a))
print("ours   stubs:", len(b))
print("device 前 16:", [hex(x) for x in a[:16]])
print("ours   前 16:", [hex(x) for x in b[:16]])
diff = [j for j in range(min(len(a), len(b))) if a[j] != b[j]]
print("total diffs:", len(diff))
for j in diff[:15]:
    print("  #%d device=%s ours=%s" % (j, hex(a[j]), hex(b[j])))
if len(a) != len(b):
    print("COUNT MISMATCH device=%d ours=%d" % (len(a), len(b)))
