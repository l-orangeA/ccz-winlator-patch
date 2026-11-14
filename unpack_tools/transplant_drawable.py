import struct, sys

lib8 = bytearray(open("/root/probe/lib8.so", "rb").read())
lib11 = open("/root/probe/lib11.so", "rb").read()
NOP = struct.pack("<I", 0x1F2003D5)

# 8.0 PLT targets
PLT8 = {
    "malloc": 0x4940,
    "free": 0x4950,
    "stackchk": 0x4960,
    "getinfo": 0x4980,
    "lockpix": 0x4990,
    "unlockpix": 0x49a0,
    "memcpy_chk": 0x4930,
}
# 11.2 PLT targets
PLT11 = {
    0xb9c0: "memcpy",
    0xb9d0: "malloc",
    0xb9e0: "free",
    0xb9f0: "getinfo",
    0xba00: "lockpix",
    0xba10: "unlockpix",
    0xba20: "stackchk",
}

def bl_enc(pc, target):
    return 0x94000000 | (((target - pc) >> 2) & 0x3FFFFFF)

def sign26(v):
    return v - (1 << 26) if v & (1 << 25) else v

# cave: str x30,[sp,#-16]! ; mov x3,x2 ; bl chk ; ldr x30,[sp],#16 ; ret
CAVE = [0xF81F0FFE, 0xAA0203E3, None, 0xF84107FE, 0xD65F03C0]

def patch_func(name, src, size, dst, slot):
    code = bytearray(lib11[src:src+size])
    need_cave = False
    cave_off = (size + 7) & ~7
    cave_addr = dst + cave_off
    for i in range(0, size, 4):
        w = struct.unpack_from("<I", code, i)[0]
        if (w >> 26) == 0x25:
            imm = sign26(w & 0x3FFFFFF) * 4
            tgt = src + i + imm
            if tgt not in PLT11:
                raise SystemExit("%s: unknown bl target 0x%x at +0x%x" % (name, tgt, i))
            kind = PLT11[tgt]
            if kind == "memcpy":
                need_cave = True
                neww = bl_enc(dst + i, cave_addr)
            else:
                neww = bl_enc(dst + i, PLT8[kind])
            struct.pack_into("<I", code, i, neww)
    if need_cave:
        cav = list(CAVE)
        cav[2] = bl_enc(cave_addr + 8, PLT8["memcpy_chk"])
        cavb = b"".join(struct.pack("<I", x) for x in cav)
        if cave_off + len(cavb) > slot:
            raise SystemExit("%s: cave does not fit" % name)
        code += b"\x00" * (cave_off - len(code)) + cavb
    if len(code) > slot:
        raise SystemExit("%s: code %d > slot %d" % (name, len(code), slot))
    code += NOP * ((slot - len(code)) // 4)
    lib8[dst:dst+len(code)] = code
    print("%s: copied %d bytes, slot %d, cave=%s" % (name, size, slot, need_cave))

# symbol: (src_addr_11, size_11, dst_addr_8, size_slot_8)
patch_func("fillRect", 0x60d0, 0x104, 0x2988, 420)
patch_func("copyArea", 0x5d60, 0x144, 0x241c, 492)

open("/root/probe/lib8_patched.so", "wb").write(lib8)
print("wrote lib8_patched.so", len(lib8))
