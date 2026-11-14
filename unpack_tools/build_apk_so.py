import zipfile, shutil, sys, os

src, new_so, dst = sys.argv[1], sys.argv[2], sys.argv[3]
so_name = "lib/arm64-v8a/libwinlator.so"
zin = zipfile.ZipFile(src, "r")
zout = zipfile.ZipFile(dst, "w", allowZip64=True)
replaced = False
for info in zin.infolist():
    if info.filename.startswith("META-INF/") and info.filename.upper().endswith((".RSA", ".SF", ".MF", ".DSA", ".EC")):
        continue
    if info.filename == so_name:
        print("so compress_type", info.compress_type, "size", info.file_size)
        ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
        ni.compress_type = info.compress_type
        ni.external_attr = info.external_attr
        ni.internal_attr = info.internal_attr
        ni.create_system = info.create_system
        data = open(new_so, "rb").read()
        with zout.open(ni, "w") as fdst:
            fdst.write(data)
        replaced = True
        continue
    ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    ni.compress_type = info.compress_type
    ni.external_attr = info.external_attr
    ni.internal_attr = info.internal_attr
    ni.create_system = info.create_system
    with zin.open(info, "r") as fsrc, zout.open(ni, "w") as fdst:
        shutil.copyfileobj(fsrc, fdst, 1024 * 1024)
zout.close(); zin.close()
print("built", dst, os.path.getsize(dst), "replaced", replaced)
