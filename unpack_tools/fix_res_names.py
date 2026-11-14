import os, re, glob

R = "/root/wn_out"
renames = {}
for f in glob.glob(os.path.join(R, "res/drawable/$avd_*.xml")):
    base = os.path.basename(f)[1:-4]          # $avd_hide_password__0
    new = base.replace("__", "_")              # avd_hide_password_0
    renames[base] = new
    os.rename(f, os.path.join(R, "res/drawable", new + ".xml"))
print("renamed:", renames)

# public.xml
p = os.path.join(R, "res/values/public.xml")
s = open(p, encoding="utf-8").read()
for old, new in renames.items():
    s = s.replace('name="%s"' % old, 'name="%s"' % new)
open(p, "w", encoding="utf-8").write(s)

# 引用处
for f in glob.glob(os.path.join(R, "res/drawable/avd_*.xml")):
    s = open(f, encoding="utf-8").read()
    o = s
    for old, new in renames.items():
        s = s.replace("@drawable/%s" % old, "@drawable/%s" % new)
    if s != o:
        open(f, "w", encoding="utf-8").write(s)
        print("fixed refs in", os.path.basename(f))

# 其它任何地方（values/layout 等）里的残留
for f in glob.glob(os.path.join(R, "res/**/*.xml"), recursive=True):
    try:
        s = open(f, encoding="utf-8").read()
    except Exception:
        continue
    o = s
    for old, new in renames.items():
        s = s.replace("@drawable/%s" % old, "@drawable/%s" % new)
    if s != o:
        open(f, "w", encoding="utf-8").write(s)
        print("fixed", f)
print("done")
