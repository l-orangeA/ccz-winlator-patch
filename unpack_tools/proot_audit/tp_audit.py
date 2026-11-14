#!/usr/bin/env python3
"""Static audit of Winlator libtinyproot.so (aarch64, stripped).
Parses ELF sections, resolves PLT -> imported symbol, disassembles .text,
and resolves adrp(+add/ldr) string references in .rodata.
"""
import sys, struct
from capstone import *
from capstone.arm64 import *

PATH = sys.argv[1] if len(sys.argv) > 1 else r"D:\ccz_winlator_patch\unpack_tools\proot_audit\lib\arm64-v8a\libtinyproot.so"
data = open(PATH, "rb").read()

# --- ELF64 parse (little endian) ---
assert data[:4] == b"\x7fELF" and data[4] == 2 and data[5] == 1
e_shoff, = struct.unpack_from("<Q", data, 0x28)
e_shentsize, e_shnum, e_shstrndx = struct.unpack_from("<HHH", data, 0x3A)
shdr = []
for i in range(e_shnum):
    off = e_shoff + i * e_shentsize
    name, typ, flags, addr, offset, size, link, info, align, entsize = struct.unpack_from("<IIQQQQIIQQ", data, off)
    shdr.append(dict(name=name, typ=typ, flags=flags, addr=addr, off=offset, size=size,
                     link=link, info=info, align=align, entsize=entsize))
shstr = shdr[e_shstrndx]
def cstr(buf, o):
    e = buf.find(b"\0", o)
    return buf[o:e].decode("latin1")
for s in shdr:
    s["sname"] = cstr(data[shstr["off"]:shstr["off"]+shstr["size"]], s["name"])

sec = {s["sname"]: s for s in shdr}
text = sec[".text"]; rodata = sec[".rodata"]; dynsym = sec[".dynsym"]; dynstr = sec[".dynstr"]
relaplt = sec[".rela.plt"]; plt = sec[".plt"]

# imports
syms = []
for o in range(0, dynsym["size"], 24):
    n, info, other, shndx, val, sz = struct.unpack_from("<IBBHQQ", data, dynsym["off"] + o)
    syms.append(cstr(data[dynstr["off"]:dynstr["off"]+dynstr["size"]], n) if n else "")
# plt relocations: r_offset, r_info, r_addend ; sym = r_info>>32
relocs = {}
for o in range(0, relaplt["size"], 24):
    r_off, r_info, r_add = struct.unpack_from("<QQq", data, relaplt["off"] + o)
    symi = r_info >> 32
    relocs[r_off] = syms[symi] if symi < len(syms) else "?"
# map GOT slot -> plt stub address. Find adrp/ldr/add patterns at start of each stub.
md = Cs(CS_ARCH_ARM64, CS_MODE_ARM)
md.detail = True

def dis(addr, size):
    va = addr
    off = addr  # for this file addr==off for .text/.plt
    return list(md.disasm(data[off:off+size], va))

plt_sym = {}
# PLT0 (resolver) is 0x20 bytes; each import stub is 16 bytes:
# adrp x16,page; ldr x17,[x16,#lo]; add x16,x16,#lo; br x17
for i in range(0x20, plt["size"], 16):
    ins = list(md.disasm(data[plt["off"]+i:plt["off"]+i+16], plt["addr"]+i))
    if len(ins) >= 2 and ins[0].mnemonic == "adrp":
        page = ins[0].operands[1].imm
        got = None
        if ins[1].mnemonic == "ldr" and len(ins[1].operands) >= 2 \
           and ins[1].operands[1].type == ARM64_OP_MEM:
            got = page + ins[1].operands[1].mem.disp
        if got is not None and got in relocs:
            plt_sym[plt["addr"] + i] = relocs[got]

# rodata string table
rd = {}
o = 0
raw = data[rodata["off"]:rodata["off"]+rodata["size"]]
while o < len(raw):
    e = raw.find(b"\0", o)
    if e < 0: break
    if e > o:
        s = raw[o:e]
        if all(32 <= c < 127 or c in (9,10) for c in s):
            rd[rodata["addr"] + o] = s.decode("latin1")
    o = e + 1

# disassemble text, build instruction list and xrefs
insns = []
for ins in md.disasm(data[text["off"]:text["off"]+text["size"]], text["addr"]):
    insns.append(ins)
byaddr = {i.address: i for i in insns}

def resolve_string(ins, idx):
    """If ins is adrp, look ahead for add/ldr with same reg to get address."""
    if ins.mnemonic != "adrp":
        return None
    rdst = ins.reg_name(ins.operands[0].reg)
    page = ins.operands[1].imm
    for j in range(idx+1, min(idx+7, len(insns))):
        n = insns[j]
        if n.address != insns[j-1].address + insns[j-1].size:
            break
        if n.mnemonic in ("add", "ldr", "ldrb", "ldrh") and len(n.operands) >= 3:
            if n.operands[0].type == ARM64_OP_REG and n.reg_name(n.operands[0].reg) == rdst \
               and n.operands[1].type == ARM64_OP_REG and n.reg_name(n.operands[1].reg) == rdst \
               and n.operands[2].type == ARM64_OP_IMM:
                return page + n.operands[2].imm
    return None

# calls to plt
calls = []
for idx, ins in enumerate(insns):
    if ins.mnemonic == "bl" and ins.operands[0].type == ARM64_OP_IMM:
        tgt = ins.operands[0].imm
        sym = plt_sym.get(tgt)
        if sym:
            calls.append((ins.address, sym))

# string xrefs
xrefs = {}
for idx, ins in enumerate(insns):
    if ins.mnemonic == "adrp":
        a = resolve_string(ins, idx)
        if a in rd:
            xrefs.setdefault((a, rd[a]), []).append(ins.address)

print("== imports ==")
print(", ".join(sorted(set(s for s in syms if s))))
print("\n== PLT symbols ==")
for a, s in sorted(plt_sym.items()):
    print(hex(a), s)

print("\n== calls to interesting imports ==")
for a, s in calls:
    if s in ("prctl", "ptrace", "execvp", "fork", "waitpid", "prlimit64", "process_vm_readv",
             "process_vm_writev", "kill", "getenv", "sigaction"):
        print(hex(a), s)

print("\n== string xrefs ==")
for (a, s), addrs in sorted(xrefs.items()):
    print(hex(a), repr(s[:90]), "->", " ".join(hex(x) for x in addrs))
