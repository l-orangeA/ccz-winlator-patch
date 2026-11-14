import ctypes, ctypes.wintypes as w, subprocess, time, sys

k32 = ctypes.WinDLL('kernel32', use_last_error=True)

PROCESS_QUERY_INFORMATION = 0x0400
PROCESS_VM_READ = 0x0010
MEM_COMMIT = 0x1000

class MEMORY_BASIC_INFORMATION(ctypes.Structure):
    _fields_ = [("BaseAddress", ctypes.c_void_p),
                ("AllocationBase", ctypes.c_void_p),
                ("AllocationProtect", w.DWORD),
                ("RegionSize", ctypes.c_size_t),
                ("State", w.DWORD),
                ("Protect", w.DWORD),
                ("Type", w.DWORD)]

exe = r"D:\ccz\曹操传加强版2.2\Ekd5_orig_backup.exe"
cwd = r"D:\ccz\曹操传加强版2.2"
p = subprocess.Popen([exe], cwd=cwd)
print("started pid", p.pid)
time.sleep(6)

h = k32.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, False, p.pid)
print("handle", h)
if not h:
    print("open failed", ctypes.get_last_error()); p.kill(); sys.exit(1)

addr = 0
regions = []
while addr < 0x100000000:
    mbi = MEMORY_BASIC_INFORMATION()
    r = k32.VirtualQueryEx(w.HANDLE(h), ctypes.c_void_p(addr), ctypes.byref(mbi), ctypes.sizeof(mbi))
    if not r:
        break
    base = mbi.BaseAddress or 0
    regions.append((base, mbi.RegionSize, mbi.State, mbi.Protect, mbi.Type))
    addr = base + mbi.RegionSize
    if mbi.RegionSize == 0:
        break

high = [r for r in regions if r[0] >= 0xb0000000 and r[2] == MEM_COMMIT]
print("total regions", len(regions), "high committed", len(high))
for base, size, state, prot, typ in high[:40]:
    print("  %08x-%08x prot=%08x type=%x" % (base, base+size, prot, typ))

def region_of(a):
    for base, size, state, prot, typ in regions:
        if base <= a < base + size:
            return (base, size, state, prot, typ)
    return None

for target in (0x8af000, 0x8afdbe, 0xd1cf0000, 0xd1cf02ea, 0x05420000):
    print("region for %08x:" % target, region_of(target))

buf = (ctypes.c_char * 32)()
n = ctypes.c_size_t()
for target in (0x8afde0, 0xd1cf02e0):
    ok = k32.ReadProcessMemory(w.HANDLE(h), ctypes.c_void_p(target), buf, 32, ctypes.byref(n))
    print("read %08x ok=%d n=%d bytes=%s" % (target, ok, n.value, buf.raw.hex()))

k32.CloseHandle(w.HANDLE(h))
print("terminating game")
p.kill()
