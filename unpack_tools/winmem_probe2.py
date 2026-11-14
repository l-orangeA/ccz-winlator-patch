import ctypes, ctypes.wintypes as w, subprocess, time, sys

k32 = ctypes.WinDLL('kernel32', use_last_error=True)
PROCESS_QUERY_INFORMATION = 0x0400
PROCESS_VM_READ = 0x0010
MEM_COMMIT = 0x1000

class MBI(ctypes.Structure):
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
print("pid", p.pid)
time.sleep(0.3)
h = k32.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, False, p.pid)
print("handle", h)

def q(a):
    m = MBI()
    r = k32.VirtualQueryEx(w.HANDLE(h), ctypes.c_void_p(a), ctypes.byref(m), ctypes.sizeof(m))
    if not r:
        return None
    return (m.BaseAddress or 0, m.RegionSize, m.State, m.Protect, m.Type, m.AllocationBase or 0)

snap = None
t0 = time.time()
while time.time() - t0 < 20:
    info = q(0x8af000)
    if info and info[2] == MEM_COMMIT:
        buf = (ctypes.c_char * 4096)()
        n = ctypes.c_size_t()
        ok = k32.ReadProcessMemory(w.HANDLE(h), ctypes.c_void_p(0x8af000), buf, 4096, ctypes.byref(n))
        if ok:
            snap = bytes(buf.raw)
            print("caught page at %.2fs region=%s" % (time.time()-t0, info))
            break
    time.sleep(0.05)

if snap:
    import struct
    for off in range(0xd90, 0xe10, 0x10):
        vals = struct.unpack_from('<4I', snap, off)
        print("8af%03x: %u %u %u %u" % (off, *vals))
    open(r"D:\ccz_winlator_patch\unpack_tools\win_8af000.bin", "wb").write(snap)
else:
    print("page never committed/readable within 20s")

for a in (0x8af000, 0xd1cf0000, 0xd1cf02ea, 0x05420000, 0x852000, 0x8b0000):
    print("region %08x:" % a, q(a))

k32.CloseHandle(w.HANDLE(h))
time.sleep(0.5)
print("terminating")
p.kill()
