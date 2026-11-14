#!/usr/bin/env python3
"""分析 Ekd5.exe 的壳特征：PE 头、section 熵、导入表、anti-debug API。"""
import math
import sys

import pefile

P = r"D:\ccz_winlator_patch\unpack_tools\Ekd5.exe"
pe = pefile.PE(P, fast_load=False)

print("=== 基本信息 ===")
print("machine    : 0x%x" % pe.FILE_HEADER.Machine)
print("subsystem  : %d (2=GUI,3=CUI)" % pe.OPTIONAL_HEADER.Subsystem)
print("timestamp  : 0x%x" % pe.FILE_HEADER.TimeDateStamp)
print("entry RVA  : 0x%x" % pe.OPTIONAL_HEADER.AddressOfEntryPoint)
print("image base : 0x%x" % pe.OPTIONAL_HEADER.ImageBase)
print("sections   :", pe.FILE_HEADER.NumberOfSections)
print("DLL chars  : 0x%x" % pe.OPTIONAL_HEADER.DllCharacteristics)


def entropy(data):
    if not data:
        return 0.0
    c = [0] * 256
    for b in data:
        c[b] += 1
    e = 0.0
    for v in c:
        if v:
            p = v / len(data)
            e -= p * math.log2(p)
    return e


print("\n=== Sections ===")
ep = pe.OPTIONAL_HEADER.AddressOfEntryPoint
for s in pe.sections:
    name = s.Name.rstrip(b"\x00").decode("latin1", "replace")
    data = s.get_data()
    print("  %-8s VA=0x%-8x vsize=%-8d raw=%-8d ent=%.2f %s" % (
        name, s.VirtualAddress, s.Misc_VirtualSize, s.SizeOfRawData, entropy(data),
        "<== EP" if s.VirtualAddress <= ep < s.VirtualAddress + s.Misc_VirtualSize else ""))

print("\n=== 导入表 ===")
total = 0
suspect = []
WATCH = ["debug", "context", "thread", "process", "virtual", "protect", "exception",
         "ntdll", "query", "setinformation", "continue", "outputdebug", "hardware",
         "dr", "breakpoint", "trace", "timing", "tick", "queryperformance"]
if hasattr(pe, "DIRECTORY_ENTRY_IMPORT"):
    for dll in pe.DIRECTORY_ENTRY_IMPORT:
        names = []
        for imp in dll.imports:
            n = imp.name.decode("latin1", "replace") if imp.name else ("ord_%d" % imp.ordinal)
            names.append(n)
            total += 1
            low = n.lower()
            if any(w in low for w in WATCH):
                suspect.append("%s!%s" % (dll.dll.decode("latin1", "replace"), n))
        print("  %-14s %3d imports" % (dll.dll.decode("latin1", "replace"), len(names)))
else:
    print("  (无导入表 — 壳的特征)")

print("\n=== anti-debug / 敏感 API ===")
for s in suspect:
    print("  " + s)
print("\n总导入数:", total)
