import ctypes, ctypes.wintypes as w, subprocess, time

k32 = ctypes.WinDLL('kernel32', use_last_error=True)
PQ, PVR = 0x0400, 0x0010
MEM_RESERVE, MEM_RELEASE = 0x2000, 0x8000
PAGE_NOACCESS = 0x01

exe = r"D:\ccz\曹操传加强版2.2\Ekd5_orig_backup.exe"
p = subprocess.Popen([exe], cwd=r"D:\ccz\曹操传加强版2.2")
time.sleep(8)
h = k32.OpenProcess(PQ | PVR | 0x0020, False, p.pid)  # + PROCESS_VM_OPERATION
print("pid", p.pid, "handle", h)

k32.VirtualAllocEx.restype = ctypes.c_void_p
k32.VirtualAllocEx.argtypes = [w.HANDLE, ctypes.c_void_p, ctypes.c_size_t, w.DWORD, w.DWORD]

for addr in (0x70000000, 0x7F000000, 0x80000000, 0x90000000, 0xB0000000, 0xD0000000, 0xF0000000):
    r = k32.VirtualAllocEx(w.HANDLE(h), ctypes.c_void_p(addr), 0x10000, MEM_RESERVE, PAGE_NOACCESS)
    err = ctypes.get_last_error() if not r else 0
    print("VirtualAllocEx(%08x) -> %s err=%d" % (addr, hex(r) if r else "FAIL", err))
    if r:
        k32.VirtualFreeEx(w.HANDLE(h), ctypes.c_void_p(r), 0, MEM_RELEASE)

k32.CloseHandle(w.HANDLE(h))
p.kill()
print("killed")
