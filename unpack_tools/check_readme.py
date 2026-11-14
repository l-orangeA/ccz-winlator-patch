#!/usr/bin/env python3
data = open(r"D:\ccz_winlator_patch\README.md", encoding="utf-8").read()
print("has 0.4.4 section:", "0.4.4 兼容性深挖" in data)
print("README size:", len(data))
idx = data.find("0.4.4")
if idx > 0:
    print("first 0.4.4 context:", data[idx-20:idx+80].replace("\n", " | "))
