import zipfile, shutil, sys

src = "/mnt/d/ccz_winlator_patch/apk/winlator8.0_ccz_final.apk"
dex = "/root/probe/classes_probe.dex"
dst = "/root/probe/probe_unsigned.apk"

zin = zipfile.ZipFile(src, "r")
zout = zipfile.ZipFile(dst, "w", allowZip64=True)
n = 0
for info in zin.infolist():
    if info.filename == "classes.dex":
        continue
    if info.filename.startswith("META-INF/") and info.filename.split("/")[-1].upper().endswith((".RSA", ".SF", ".MF", ".DSA", ".EC")):
        continue
    ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    ni.compress_type = info.compress_type
    ni.external_attr = info.external_attr
    ni.internal_attr = info.internal_attr
    ni.create_system = info.create_system
    with zin.open(info, "r") as fsrc, zout.open(ni, "w") as fdst:
        shutil.copyfileobj(fsrc, fdst, 1024 * 1024)
    n += 1
ni = zipfile.ZipInfo("classes.dex", date_time=(2026, 11, 5, 21, 21, 0))
ni.compress_type = zipfile.ZIP_DEFLATED
with open(dex, "rb") as f, zout.open(ni, "w") as fdst:
    shutil.copyfileobj(f, fdst, 1024 * 1024)
n += 1
zout.close()
zin.close()
print("entries", n)
