import re, subprocess, base64, os, sys

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SERIAL = "TABLET_SERIAL_REDACTED"
PID = sys.argv[1] if len(sys.argv) > 1 else "14810"
OUT = r"D:\ccz_winlator_patch\unpack_tools\guest_dump"
os.makedirs(OUT, exist_ok=True)

maps = open(r"D:\ccz_winlator_patch\unpack_tools\guest_maps.txt", encoding="utf-8", errors="replace").read().splitlines()
regions = []
for line in maps:
    m = re.match(r'([0-9a-f]+)-([0-9a-f]+) (\S+)', line.strip())
    if not m: continue
    a, b, perm = int(m.group(1), 16), int(m.group(2), 16), m.group(3)
    if a >= 0x10000000: continue
    if 'r' not in perm: continue
    size = b - a
    if size < 0x1000 or size > 0x400000: continue
    regions.append((a, b, perm))

print("regions:", len(regions), "total", hex(sum(b-a for a,b,_ in regions)))

patterns = {
    "clean": bytes.fromhex("4205d2024205d302"),
    "irreg": bytes.fromhex("22c902024205df02"),
    "irreg2": bytes.fromhex("0202d1d40202d1d2"),
    "tableD90": bytes.fromhex("4205d9024205d802")[::-1],
}

env = dict(os.environ); env["MSYS_NO_PATHCONV"] = "1"
def read_region(a, b):
    size = b - a
    cmd = ["-s", SERIAL, "exec-out",
           "run-as com.winlatcn sh -c 'dd if=/proc/%s/mem bs=4096 skip=%d count=%d 2>/dev/null | base64'" % (PID, a//4096, (size+4095)//4096)]
    r = subprocess.run([ADB]+cmd, capture_output=True, env=env, timeout=120)
    try:
        data = base64.b64decode(r.stdout, validate=False)
    except Exception as e:
        print("decode fail", hex(a), e, r.stderr[:100]); return None
    return data[:size]

hits = {}
for a, b, perm in regions:
    data = read_region(a, b)
    if data is None or not data:
        print("skip %08x-%08x %s (empty)" % (a,b,perm)); continue
    open(os.path.join(OUT, "r_%08x.bin" % a), "wb").write(data)
    for name, pat in patterns.items():
        i = data.find(pat)
        while i >= 0:
            hits.setdefault(name, []).append((a+i, perm, b-a))
            if len(hits[name]) > 20: break
            i = data.find(pat, i+1)
    print("dumped %08x-%08x %s %d bytes" % (a,b,perm,len(data)))

for k,v in hits.items():
    print(k, [(hex(x), p, hex(s)) for x,p,s in v][:10])
if not hits:
    print("no pattern hits")
