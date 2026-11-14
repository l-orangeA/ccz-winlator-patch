import ctypes, ctypes.wintypes as w, subprocess, time, struct

k32 = ctypes.WinDLL('kernel32', use_last_error=True)
PROCESS_QUERY_INFORMATION = 0x0400
PROCESS_VM_READ = 0x0010

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
h = None
for i in range(400):
    h = k32.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, False, p.pid)
    if h: break
    time.sleep(0.01)
print("handle", h)

def q(a):
    m = MBI()
    r = k32.VirtualQueryEx(w.HANDLE(h), ctypes.c_void_p(a), ctypes.byref(m), ctypes.sizeof(m))
    if not r: return None
    return (m.BaseAddress or 0, m.RegionSize, m.State, m.Protect)

seen = {}
caught = None
t0 = time.time()
last = None
while time.time() - t0 < 12:
    info = q(0x8af000)
    key = info
    if key != last:
        print("%.3fs %08x size=%x state=%x prot=%x" % (time.time()-t0, info[0], info[1], info[2], info[3]))
        last = key
    if info and info[2] == 0x1000:
        buf = (ctypes.c_char * 64)()
        n = ctypes.c_size_t()
        ok = k32.ReadProcessMemory(w.HANDLE(h), ctypes.c_void_p(0x8afde0), buf, 64, ctypes.byref(n))
        if ok:
            caught = buf.raw
            print("READ OK at %.3fs: %s" % (time.time()-t0, caught.hex()))
            break
    time.sleep(0.002)

if caught:
    print("dwords at 8afde0:", [hex(x) for x in struct.unpack('<16I', caught)])

# window check
u32 = ctypes.WinDLL('user32', use_last_error=True)
hWnd = u32.FindWindowW(None, None)
res = []
@ctypes.WINFUNCTYPE(w.BOOL, w.HWND, w.LPARAM)
def cb(hwnd, lp):
    pid = w.DWORD()
    u32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
    if pid.value == p.pid:
        n = u32.GetWindowTextLengthW(hwnd)
        b = ctypes.create_unicode_buffer(n+1)
        u32.GetWindowTextW(hwnd, b, n+1)
        res.append((hwnd, b.value))
    return True
u32.EnumWindows(cb, 0)
print("windows:", res)
k32.CloseHandle(w.HANDLE(h))
p.kill()
print("killed")
