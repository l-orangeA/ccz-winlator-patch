"""拉设备 i386 win32u.dll + 对比 device/ours 的 i386 syscall 桩编号。在 WSL 跑:
python3 pull_i386.py   （先在 PC 侧 push 到 /data/local/tmp 的逻辑省略——直接用 adb exec-out 由 PC 拉好后放 WSL 可见路径）
"""
import struct, subprocess, sys

DEVP = "/mnt/d/ccz_winlator_patch/device_backup/win32u_orig_20261106/win32u_i386.dll"
OURP = "/root/wine0905/dlls/win32u/i386-windows/win32u.dll"


def stubs(path):
    d = open(path, "rb").read()
    print(path.split("/")[-1], "size", len(d), "machine", "%04x" % struct.unpack_from("<H", d, struct.unpack_from("<I", d, 0x3c)[0] + 4)[0])
    ids = []
    i = 0
    n = len(d)
    while i < n - 6:
        if d[i] == 0xB8 and d[i+5] == 0xFF and d[i+6] == 0xD2:
            ids.append(struct.unpack_from("<I", d, i+1)[0])
            i += 7
        elif d[i] == 0xB8 and d[i+5] == 0xFF and d[i+6] == 0x12:
            ids.append(struct.unpack_from("<I", d, i+1)[0])
            i += 7
        else:
            i += 1
    return ids


a = stubs(DEVP)
b = stubs(OURP)
print("device i386 stubs:", len(a), a[:16])
print("ours   i386 stubs:", len(b), b[:16])
k = 0
while k < min(len(a), len(b)) and a[k] == b[k]:
    k += 1
print("first mismatch at #%d" % k)
diff = [j for j in range(min(len(a), len(b))) if a[j] != b[j]]
print("total diffs:", len(diff))
for j in diff[:12]:
    print("  #%d device=%s ours=%s" % (j, hex(a[j]), hex(b[j])))
