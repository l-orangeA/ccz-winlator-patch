#!/usr/bin/env python3
"""从 Winlator 11.2 APK 提取 dex 并搜容器存储相关字符串"""
import re
import zipfile

z = zipfile.ZipFile("/mnt/d/ccz_winlator_patch/unpack_tools/winlator11/Winlator_11.2.apk")
pats = re.compile(rb"[\x20-\x7e]{6,}")
hits = set()
for n in z.namelist():
    if n.startswith("classes") and n.endswith(".dex"):
        data = z.read(n)
        for m in pats.finditer(data):
            s = m.group()
            try:
                t = s.decode("ascii")
            except Exception:
                continue
            low = t.lower()
            if ("container" in low and (".json" in low or "prefs" in low or "files/" in low or "key" in low or "containers" in low)) or "containers_json" in low or ("container" in low and low.startswith("container")):
                hits.add(t)
for t in sorted(hits)[:80]:
    print(t)
