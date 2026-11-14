import ctypes, ctypes.wintypes as w, subprocess, time

k32 = ctypes.WinDLL('kernel32', use_last_error=True)
ntdll = ctypes.WinDLL('ntdll')
PQ, PVR = 0x0400, 0x0010

class MBI(ctypes.Structure):
    _fields_ = [("BaseAddress", ctypes.c_void_p), ("AllocationBase", ctypes.c_void_p),
                ("AllocationProtect", w.DWORD), ("__a", w.DWORD), ("RegionSize", ctypes.c_size_t),
                ("State", w.DWORD), ("Protect", w.DWORD), ("Type", w.DWORD), ("__b", w.DWORD)]

exe = r"D:\ccz\曹操传加强版2.2\Ekd5_orig_backup.exe"
p = subprocess.Popen([exe], cwd=r"D:\ccz\曹操传加强版2.2")
time.sleep(8)
h = k32.OpenProcess(PQ | PVR, False, p.pid)
print("pid", p.pid, "handle", h)

NtQueryVirtualMemory = ntdll.NtQueryVirtualMemory
NtQueryVirtualMemory.restype = ctypes.c_long
NtQueryVirtualMemory.argtypes = [w.HANDLE, ctypes.c_void_p, ctypes.c_int,
                                 ctypes.POINTER(MBI), ctypes.c_size_t, ctypes.POINTER(ctypes.c_size_t)]

for addr in (0x00400000, 0x7F000000, 0x7FFF0000, 0x80000000, 0x90000000, 0xd1cf0000):
    m = MBI(); ret = ctypes.c_size_t()
    st = NtQueryVirtualMemory(w.HANDLE(h), ctypes.c_void_p(addr), 0, ctypes.byref(m),
                              ctypes.sizeof(m), ctypes.byref(ret)) & 0xffffffff
    print("%08x -> st=%08x base=%08x size=%x state=%x prot=%x" %
          (addr, st, m.BaseAddress or 0, m.RegionSize, m.State, m.Protect))

k32.CloseHandle(w.HANDLE(h))
p.kill()
print("killed")
