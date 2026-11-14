# -*- coding: utf-8 -*-
"""只替换 APK 内的 assets/imagefs.txz（换成 XZ 格式版），其余条目原样复制。
不回编译、不动 dex；输出未签名 APK，交给 zipalign + apksigner 重签。
"""
import os
import shutil
import zipfile

ROOT = r"D:\ccz_winlator_patch"
SRC = os.path.join(ROOT, "build", "ccz_lazy_signed.apk")
TXZ = os.path.join(ROOT, "build", "_imagefs_lazy_xz.txz")
OUT = os.path.join(ROOT, "build", "ccz_lazy_v2_unsigned.apk")
TARGET = "assets/imagefs.txz"
SKIP_EXT = (".SF", ".RSA", ".MF", ".DSA", ".EC")


def main():
    n_skip = 0
    with zipfile.ZipFile(SRC) as zin, zipfile.ZipFile(OUT, "w", allowZip64=True) as zout:
        for item in zin.infolist():
            name = item.filename
            if name.startswith("META-INF/") and name.upper().endswith(SKIP_EXT):
                n_skip += 1
                continue
            zi = zipfile.ZipInfo(name, date_time=item.date_time)
            zi.compress_type = item.compress_type
            zi.external_attr = item.external_attr
            zi.internal_attr = item.internal_attr
            zi.create_system = item.create_system
            if name == TARGET:
                src = open(TXZ, "rb")
                print("替换", name, "->", os.path.getsize(TXZ), "B (XZ)")
            else:
                src = zin.open(item)
            with zout.open(zi, "w", force_zip64=True) as dst, src:
                shutil.copyfileobj(src, dst, 1 << 20)
    print("跳过旧签名文件", n_skip, "个")
    print("输出", OUT, "%.1f MB" % (os.path.getsize(OUT) / 1048576.0))


if __name__ == "__main__":
    main()
