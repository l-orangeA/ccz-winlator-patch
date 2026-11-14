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

pats = {
    "tbl_d90": bytes.fromhex("4205d6024205d702"),
    "tbl_000": bytes.fromhex("6c023f056d023f05"),
}

def scan():
    addr = 0
    hits = {k: [] for k in pats}
    while addr < 0x80000000:
        m = MBI()
        if not k32.VirtualQueryEx(w.HANDLE(h), ctypes.c_void_p(addr), ctypes.byref(m), ctypes.sizeof(m)):
            break
        base, size, state, prot = m.BaseAddress or 0, m.RegionSize, m.State, m.Protect
        if state == 0x1000 and prot & 0x66 and size < 0x8000000:
            buf = (ctypes.c_char * size)()
            n = ctypes.c_size_t()
            if k32.ReadProcessMemory(w.HANDLE(h), ctypes.c_void_p(base), buf, size, ctypes.byref(n)) and n.value == size:
                data = bytes(buf.raw)
                for k, pat in pats.items():
                    i = data.find(pat)
                    while i >= 0:
                        hits[k].append((base + i, prot, m.Type))
                        i = data.find(pat, i+1)
                        if len(hits[k]) > 8: break
        nxt = base + size
        if nxt <= addr: nxt = addr + 0x1000
        addr = nxt
    return hits

hits = scan()
for k, v in hits.items():
    print(k, [(hex(a), hex(pr), hex(t)) for a, pr, t in v])
k32.CloseHandle(w.HANDLE(h))
p.kill()
print("killed")
