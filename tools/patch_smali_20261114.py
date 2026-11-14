# -*- coding: utf-8 -*-
"""在 WSL 里跑：修改 apktool 反编译出的 smali，根治 box64 预设坑。"""
import sys

BASE = "/root/ccz_lazy/smali/com/winlator"


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def write(p, s):
    with open(p, "w", encoding="utf-8", newline="") as f:
        f.write(s)


# ---- 1) 新建容器强制 COMPATIBILITY（忽略全局 SharedPreferences） ----
p = BASE + "/ContainerDetailFragment.smali"
s = read(p)
old = '''    const-string v1, "box64_preset"

    move-object/from16 v22, v3

    .end local v3    # "cbShowFPS":Landroid/widget/CheckBox;
    .local v22, "cbShowFPS":Landroid/widget/CheckBox;
    const-string v3, "COMPATIBILITY"

    invoke-interface {v13, v1, v3}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1
'''
new = '''    const-string v1, "COMPATIBILITY"

    move-object/from16 v22, v3

    .end local v3    # "cbShowFPS":Landroid/widget/CheckBox;
    .local v22, "cbShowFPS":Landroid/widget/CheckBox;
'''
if s.count(old) != 1:
    sys.exit("ERR: ContainerDetailFragment 目标块匹配 %d 次" % s.count(old))
write(p, s.replace(old, new))
print("[1/3] 新建容器 box64Preset 强制 COMPATIBILITY（忽略 shared_prefs）")

# ---- 2) PERFORMANCE 预设去掉会破坏 VProtect 壳的参数 ----
p = BASE + "/box64/Box64PresetManager.smali"
lines = read(p).split("\n")
start = next(i for i, l in enumerate(lines)
             if i > 430 and 'const-string v2, "PERFORMANCE"' in l)
end = next(i for i in range(start, len(lines)) if lines[i].strip() == "goto :goto_1")
seg = lines[start:end]
repls = [
    ("invoke-virtual {v1, v14, v3},", "invoke-virtual {v1, v14, v13},"),  # SAFEFLAGS 1 -> 2
    ("invoke-virtual {v1, v12, v3},", "invoke-virtual {v1, v12, v15},"),  # FASTNAN   1 -> 0
    ("invoke-virtual {v1, v11, v3},", "invoke-virtual {v1, v11, v15},"),  # FASTROUND 1 -> 0
    ("invoke-virtual {v1, v10, v15},", "invoke-virtual {v1, v10, v3},"),  # X87DOUBLE 0 -> 1
    ("invoke-virtual {v1, v8, v15},", "invoke-virtual {v1, v8, v3},"),    # STRONGMEM 0 -> 1
    ('const-string v2, "512"', 'const-string v2, "1024"'),                  # FORWARD 512 -> 1024
]
for old, new in repls:
    hits = sum(1 for l in seg if old in l)
    if hits != 1:
        sys.exit("ERR: PERFORMANCE 段 %r 命中 %d 次" % (old, hits))
    seg = [l.replace(old, new) if old in l else l for l in seg]
lines[start:end] = seg
write(p, "\n".join(lines))
print("[2/3] PERFORMANCE 预设改为安全值（STRONGMEM=1 / SAFEFLAGS=2 / FASTNAN=0 / FASTROUND=0 / X87DOUBLE=1 / FORWARD=1024）")

# ---- 3) 容器默认驱动贴近已验证生产态 ----
p = BASE + "/container/Container.smali"
s = read(p)
pairs = [
    ('    .line 36\n    const-string v0, "dxvk"', '    .line 36\n    const-string v0, "wined3d"'),
    ('    .line 41\n    const-string v0, "alsa"', '    .line 41\n    const-string v0, "pulseaudio"'),
]
for old, new in pairs:
    if s.count(old) != 1:
        sys.exit("ERR: Container 目标块匹配 %d 次: %r" % (s.count(old), old[:40]))
    s = s.replace(old, new)
write(p, s)
print("[3/3] 容器默认 dxwrapper=wined3d / audioDriver=pulseaudio（graphicsDriver 保持 turnip）")
print("SMALI PATCH OK")
