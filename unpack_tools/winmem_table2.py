import ctypes, ctypes.wintypes as w, subprocess, time, struct

k32 = ctypes.WinDLL('kernel32', use_last_error=True)
PQ, PVR = 0x0400, 0x0010

class MBI(ctypes.Structure):
    _fields_ = [("BaseAddress", ctypes.c_void_p), ("AllocationBase", ctypes.c_void_p),
                ("AllocationProtect", w.DWORD), ("RegionSize", ctypes.c_size_t),
                ("State", w.DWORD), ("Protect", w.DWORD), ("Type", w.DWORD)]

exe = r"D:\ccz\曹操传加强版2.2\Ekd5_orig_backup.exe"
p = subprocess.Popen([exe], cwd=r"D:\ccz\曹操传加强版2.2")
time.sleep(8)
h = k32.OpenProcess(PQ | PVR, False, p.pid)
print("pid", p.pid, "handle", h)

def rd(a, n):
    buf = (ctypes.c_char * n)()
    got = ctypes.c_size_t()
    ok = k32.ReadProcessMemory(w.HANDLE(h), ctypes.c_void_p(a), buf, n, ctypes.byref(got))
    return bytes(buf.raw) if ok else None

pat = bytes.fromhex("6c023f056d023f05")   # table bytes at Wine 0x8af002
addr, found = 0, []
while addr < 0x80000000 and len(found) < 5:
    m = MBI()
    if not k32.VirtualQueryEx(w.HANDLE(h), ctypes.c_void_p(addr), ctypes.byref(m), ctypes.sizeof(m)):
        break
    base, size, state, prot = m.BaseAddress or 0, m.RegionSize, m.State, m.Protect
    if state == 0x1000 and (prot & 0xEE) and size < 0x4000000:
        d = rd(base, size)
        if d:
            i = d.find(pat)
            while i >= 0 and len(found) < 5:
                found.append(base + i - 2)   # page base = match - 2
                i = d.find(pat, i + 1)
    nxt = base + size
    if nxt <= addr: nxt = addr + 0x1000
    addr = nxt
print("candidate table bases:", [hex(x) for x in found])

for tb in found[:3]:
    d = rd(tb, 0x1000)
    if not d:
        print("no read", hex(tb)); continue
    print("table", hex(tb))
    for off in (0xd80, 0xd90, 0xdb0, 0xdc0, 0xdd0, 0xde0, 0xdf0):
        print("  +%03x: %08x %08x %08x %08x" % (off, *struct.unpack_from('<4I', d, off)))
    print("  @de4=%08x @de2=%08x" % (struct.unpack_from('<I', d, 0xde4)[0], struct.unpack_from('<I', d, 0xde2)[0]))
    q = MBI()
    k32.VirtualQueryEx(w.HANDLE(h), ctypes.c_void_p(tb), ctypes.byref(q), ctypes.sizeof(q))
    print("  region %08x size=%x state=%x prot=%x type=%x allocbase=%08x" % (q.BaseAddress or 0, q.RegionSize, q.State, q.Protect, q.Type, q.AllocationBase or 0))

k32.CloseHandle(w.HANDLE(h))
p.kill()
print("killed")
