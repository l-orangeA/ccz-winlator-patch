import ctypes, ctypes.wintypes as w, subprocess, time, struct

k32 = ctypes.WinDLL('kernel32', use_last_error=True)
PQ, PVR = 0x0400, 0x0010

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

# locate table base 0x2E94400 (from previous scan: pattern at 0x2E94402)
BASE = 0x2E94400
data = rd(BASE, 0x1000)
print("table read:", data is not None)
if data:
    for off in (0xd80, 0xd90, 0xdb0, 0xdc0, 0xdd0, 0xde0, 0xdf0, 0xe00):
        vals = struct.unpack_from('<4I', data, off)
        print("+%03x: %08x %08x %08x %08x" % (off, *vals))
    e = struct.unpack_from('<I', data, 0xde4)[0]
    e2 = struct.unpack_from('<I', data, 0xde2)[0]
    print("dword@+de4 = %08x ; unaligned@+de2 = %08x" % (e, e2))

for a in (0x2E94400, 0x2E951E4, 0x2E951E2):
    m = ctypes.c_ulonglong()
    # quick region check via VirtualQueryEx
    class MBI(ctypes.Structure):
        _fields_ = [("BaseAddress", ctypes.c_void_p), ("AllocationBase", ctypes.c_void_p),
                    ("AllocationProtect", w.DWORD), ("RegionSize", ctypes.c_size_t),
                    ("State", w.DWORD), ("Protect", w.DWORD), ("Type", w.DWORD)]
    q = MBI()
    k32.VirtualQueryEx(w.HANDLE(h), ctypes.c_void_p(a), ctypes.byref(q), ctypes.sizeof(q))
    print("%08x in region %08x size=%x state=%x prot=%x type=%x" % (a, q.BaseAddress or 0, q.RegionSize, q.State, q.Protect, q.Type))

k32.CloseHandle(w.HANDLE(h))
p.kill()
print("killed")
