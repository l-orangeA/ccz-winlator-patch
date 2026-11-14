#!/usr/bin/env python3
"""v6: 对比 r109g 读档对话框与原版基准。"""
from PIL import Image

A = "/mnt/d/ccz_winlator_patch/unpack_tools/a0_trace"
im = Image.open(f"{A}/dlg_r109g.png")
print("dlg_r109g size:", im.size)
# 读档对话框+存档列表区域（视觉坐标按 2800 宽归一：visual*1.458）
crop = im.crop((700, 550, 2200, 1100))
crop = crop.resize((crop.size[0] * 2, crop.size[1] * 2), Image.LANCZOS)
crop.save(f"{A}/font_cmp_r109g.png")
print("saved", crop.size)
