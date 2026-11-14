import struct, sys
from capstone import *

def load(path):
    d = open(path, 'rb').read()
    pe = struct.unpack_from('<I', d, 0x3c)[0]
    nsec = struct.unpack_from('<H', d, pe + 6)[0]
    optsz = struct.unpack_from('<H', d, pe + 20)[0]
    opt = pe + 24
    base = struct.unpack_from('<I', d, opt + 28)[0]
    secs = []
    so = opt + optsz
    for i in range(nsec):
        n, vs, va, rs, ra = struct.unpack_from('<8sIIII', d, so + i * 40)
        secs.append((n.rstrip(b'\0').decode(), vs, va, rs, ra))
    return d, base, secs

def rva2off(secs, rva):
    for n, vs, va, rs, ra in secs:
        if va <= rva < va + max(vs, rs):
            return ra + (rva - va)
    return None

if __name__ == '__main__':
    path = sys.argv[1]
    start = int(sys.argv[2], 16)
    count = int(sys.argv[3]) if len(sys.argv) > 3 else 40
    d, base, secs = load(path)
    print('imagebase', hex(base))
    rva = start - base if start >= base else start
    off = rva2off(secs, rva)
    print('rva', hex(rva), 'off', hex(off))
    md = Cs(CS_ARCH_X86, CS_MODE_32)
    md.detail = True
    code = d[off:off + 400]
    n = 0
    for ins in md.disasm(code, start):
        print('%08x  %-24s %s %s' % (ins.address, ins.bytes.hex(), ins.mnemonic, ins.op_str))
        n += 1
        if n >= count:
            break
