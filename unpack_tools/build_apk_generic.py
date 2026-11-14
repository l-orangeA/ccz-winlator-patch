import zipfile, shutil, sys, os

src, dex, dst = sys.argv[1], sys.argv[2], sys.argv[3]
zin = zipfile.ZipFile(src, "r")
zout = zipfile.ZipFile(dst, "w", allowZip64=True)
skip = ("classes.dex",)
for info in zin.infolist():
    if info.filename in skip:
        continue
    if info.filename.startswith("META-INF/") and info.filename.upper().endswith((".RSA", ".SF", ".MF", ".DSA", ".EC")):
        continue
    ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    ni.compress_type = info.compress_type
    ni.external_attr = info.external_attr
    ni.internal_attr = info.internal_attr
    ni.create_system = info.create_system
    with zin.open(info, "r") as fsrc, zout.open(ni, "w") as fdst:
        shutil.copyfileobj(fsrc, fdst, 1024 * 1024)
ni = zipfile.ZipInfo("classes.dex", date_time=(2026, 11, 6, 13, 0, 0))
ni.compress_type = zipfile.ZIP_DEFLATED
with open(dex, "rb") as f, zout.open(ni, "w") as fdst:
    shutil.copyfileobj(f, fdst, 1024 * 1024)
zout.close(); zin.close()
print("built", dst, os.path.getsize(dst))
