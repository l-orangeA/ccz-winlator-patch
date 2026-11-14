import os, re

R = "/root/wn_out"
# 1) 加一个颜色资源
cp = os.path.join(R, "res/values/colors.xml")
s = open(cp, encoding="utf-8").read()
if "ccz_black" not in s:
    s = s.replace("</resources>", '    <color name="ccz_black">#ff000000</color>\n</resources>', 1)
    open(cp, "w", encoding="utf-8").write(s)

# 2) 把 windowBackground 的裸颜色换成引用
for rel in ("res/values/styles.xml", "res/values-v27/styles.xml"):
    p = os.path.join(R, rel)
    if not os.path.exists(p):
        continue
    s = open(p, encoding="utf-8").read()
    n = s.count('name="android:windowBackground">#ff000000<')
    s = s.replace('name="android:windowBackground">#ff000000<',
                  'name="android:windowBackground">@color/ccz_black<')
    open(p, "w", encoding="utf-8").write(s)
    print(rel, "replaced", n)
print("ok")
