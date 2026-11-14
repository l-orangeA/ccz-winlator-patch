import ctypes, ctypes.wintypes as w, subprocess, time

k32 = ctypes.WinDLL('kernel32', use_last_error=True)
ntdll = ctypes.WinDLL('ntdll')
PQ, PVR = 0x0400, 0x0010

class MBI64(ctypes.Structure):
    _fields_ = [("BaseAddress", ctypes.c_ulonglong), ("AllocationBase", ctypes.c_ulonglong),
                ("AllocationProtect", w.DWORD), ("__alignment1", w.DWORD),
                ("RegionSize", ctypes.c_ulonglong), ("State", w.DWORD), ("Protect", w.DWORD),
                ("Type", w.DWORD), ("__alignment2", w.DWORD)]

exe = r"D:\ccz\曹操传加强版2.2\Ekd5_orig_backup.exe"
p = subprocess.Popen([exe], cwd=r"D:\ccz\曹操传加强版2.2")
time.sleep(8)
h = k32.OpenProcess(PQ | PVR, False, p.pid)
print("pid", p.pid, "handle", h)

NtWow64QueryVirtualMemory64 = ntdll.NtWow64QueryVirtualMemory64
NtWow64QueryVirtualMemory64.restype = ctypes.c_long
NtWow64QueryVirtualMemory64.argtypes = [w.HANDLE, ctypes.c_ulonglong, ctypes.c_ulong,
                                        ctypes.POINTER(MBI64), ctypes.c_size_t, ctypes.POINTER(ctypes.c_size_t)]

for addr in (0x7FFE0000, 0x80000000, 0x90000000, 0xd1cf0000, 0xd1cf02e0):
    m = MBI64(); ret = ctypes.c_size_t()
    st = NtWow64QueryVirtualMemory64(w.HANDLE(h), addr, 0, ctypes.byref(m), ctypes.sizeof(m), ctypes.byref(ret))
    print("%08x -> status=%08x base=%08x size=%x state=%x prot=%x type=%x" %
          (addr, st & 0xffffffff, m.BaseAddress, m.RegionSize, m.State, m.Protect, m.Type))

# also try reading 0xd1cf0000
buf = (ctypes.c_char * 16)(); got = ctypes.c_size_t()
ok = k32.ReadProcessMemory(w.HANDLE(h), ctypes.c_void_p(0xd1cf0000), buf, 16, ctypes.byref(got))
print("read 0xd1cf0000 ok=%d got=%d bytes=%s" % (ok, got.value, buf.raw.hex()))
ok = k32.ReadProcessMemory(w.HANDLE(h), ctypes.c_void_p(0x8af000), buf, 16, ctypes.byref(got))
print("read 0x8af000 ok=%d got=%d bytes=%s" % (ok, got.value, buf.raw.hex()))

k32.CloseHandle(w.HANDLE(h))
p.kill()
print("killed")
