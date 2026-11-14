"""A0 分析：解析 hwbp_a0.log.gz 里的 +bitblt TRACE，输出 ROP/尺寸直方图。只读。
用法: python -I a0_analyze.py <hwbp_a0.log.gz>
"""
import gzip, re, sys, collections

BLIT = re.compile(
    r"dst 0x[0-9a-f]+ log=(-?\d+),(-?\d+) (\d+)x(\d+) "
    r"phys=(-?\d+),(-?\d+) (-?\d+)x(-?\d+) "
    r"vis=(\S+)  rop=([0-9a-f]{6})")
FLUSH = re.compile(r"flushing 0x[0-9a-f]+ (\d+)x(\d+) bounds (\S+)")

ROP_NAME = {
    "000042": "BLACKNESS", "100042": "NOTSRCMERGE?", "110046": "NOSRCMERGE",
    "550009": "DSTINVERT", "5A0049": "PATINVERT", "F00021": "PATCOPY",
    "Fb0a09": "MASKSPAN", "FF0062": "WHITENESS",
    "CC0020": "SRCCOPY", "8800C6": "SRCAND", "EE0086": "SRCPAINT",
    "660046": "DSPDxax", "7700E6": "SPDSxax", "220326": "DSa", "660047": "",
}
SRC_ROPS = {"CC0020", "8800C6", "EE0086", "660046", "7700E6", "220326", "660047"}

def bucket(w, h):
    a = w * h
    if a < 64: return "<8x8"
    if a < 576: return "<24x24"
    if a < 4096: return "<64x64"
    if a < 65536: return "<256x256"
    if a < 411520: return "<640x640"
    return ">=full"

def main():
    path = sys.argv[1]
    op = gzip.open if path.endswith(".gz") else open
    blits = collections.Counter()
    pix = collections.Counter()
    sizes = collections.Counter()
    exact = collections.Counter()
    src_or_pat = collections.Counter()
    flushes = collections.Counter()
    flush_pix = 0
    total_lines = 0
    other = collections.Counter()
    with op(path, "rt", encoding="utf-8", errors="replace") as f:
        for line in f:
            total_lines += 1
            m = BLIT.search(line)
            if m:
                lw, lh = int(m.group(3)), int(m.group(4))
                pw, ph = int(m.group(7)), int(m.group(8))
                rop = m.group(10)
                blits[rop] += 1
                pix[rop] += pw * ph
                sizes[bucket(pw, ph)] += 1
                exact["%dx%d" % (pw, ph)] += 1
                kind = "SRC(BitBlt)" if rop.upper() in SRC_ROPS else "PAT(PatBlt)"
                src_or_pat[kind] += 1
                continue
            m = FLUSH.search(line)
            if m:
                flushes["%dx%d" % (int(m.group(1)), int(m.group(2)))] += 1
                flush_pix += int(m.group(1)) * int(m.group(2))
                continue
            key = line.split(" ", 1)
            if key and key[0]:
                other[key[0][:16]] += 1

    print("total lines: %d" % total_lines)
    print("\n== blits by ROP (count, pixels, kind) ==")
    tot = sum(blits.values())
    for rop, c in blits.most_common():
        name = ROP_NAME.get(rop.upper(), "")
        kind = "SRC" if rop.upper() in SRC_ROPS else "PAT"
        print("  rop=%s %-10s %-3s cnt=%7d (%4.1f%%)  px=%12d" % (
            rop, name, kind, c, 100.0 * c / tot if tot else 0, pix[rop]))
    print("\n== kinds ==")
    for k, c in src_or_pat.most_common():
        print("  %-12s %7d (%.1f%%)" % (k, c, 100.0 * c / tot if tot else 0))
    print("\n== pixel-count bucket (phys area) ==")
    for k, c in sizes.most_common():
        print("  %-12s %7d" % (k, c))
    print("\n== top exact phys sizes ==")
    for k, c in exact.most_common(15):
        print("  %-12s %7d" % (k, c))
    print("\n== flush uploads ==")
    print("  total flushes: %d, total px: %d" % (sum(flushes.values()), flush_pix))
    for k, c in flushes.most_common(12):
        print("  %-12s %7d" % (k, c))
    print("\n== non-blit line prefixes (top noise sources) ==")
    for k, c in other.most_common(8):
        print("  %-18s %9d" % (k.encode("ascii", "backslashreplace").decode("ascii"), c))

if __name__ == "__main__":
    main()
