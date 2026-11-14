import glob, os

R = "/root/wn_out"
PAIRS = [
    ("$avd_hide_password__0", "avd_hide_password_0"),
    ("$avd_hide_password__1", "avd_hide_password_1"),
    ("$avd_hide_password__2", "avd_hide_password_2"),
    ("$avd_show_password__0", "avd_show_password_0"),
    ("$avd_show_password__1", "avd_show_password_1"),
    ("$avd_show_password__2", "avd_show_password_2"),
]

def fix(path):
    if os.path.getsize(path) > 4_000_000:
        return False
    with open(path, encoding="utf-8", errors="ignore") as f:
        s = f.read()
    o = s
    for a, b in PAIRS:
        s = s.replace(a, b)
    if s != o:
        with open(path, "w", encoding="utf-8") as f:
            f.write(s)
        return True
    return False

n = 0
for f in glob.glob(os.path.join(R, "res", "**", "*.xml"), recursive=True):
    try:
        if fix(f):
            print("fixed", os.path.relpath(f, R)); n += 1
    except Exception as e:
        print("skip", f, e)
print("total fixed", n)
