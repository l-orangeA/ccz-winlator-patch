#!/usr/bin/env python3
"""分段并行下载 Winlator_11.2.apk，合并后校验 SHA256。"""
import concurrent.futures as cf
import hashlib
import os
import sys
import urllib.request

URL = ("https://gh-proxy.com/https://github.com/brunodev85/winlator/releases/download/"
       "v11.2.0/Winlator_11.2.apk")
TOTAL = 157549075
WANT = "ea5c81bf0e5b90a6b9ccb8007121ab6738c8646a5a60b341ce0ac2b33f1d100a"
PARTS = 8
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Winlator_11.2.apk")


def fetch(i):
    start = TOTAL * i // PARTS
    end = TOTAL * (i + 1) // PARTS - 1
    for attempt in range(3):
        try:
            req = urllib.request.Request(URL, headers={"Range": "bytes=%d-%d" % (start, end)})
            with urllib.request.urlopen(req, timeout=120) as r:
                data = r.read()
            if len(data) == end - start + 1:
                print("part %d ok %d bytes" % (i, len(data)), flush=True)
                return i, data
            print("part %d short %d/%d" % (i, len(data), end - start + 1), flush=True)
        except Exception as e:
            print("part %d attempt %d err %s" % (i, attempt + 1, e), flush=True)
    return i, None


def main():
    parts = {}
    with cf.ThreadPoolExecutor(max_workers=PARTS) as ex:
        for i, data in ex.map(fetch, range(PARTS)):
            if data is None:
                raise SystemExit("part %d failed" % i)
            parts[i] = data
    buf = b"".join(parts[i] for i in range(PARTS))
    assert len(buf) == TOTAL, "size %d != %d" % (len(buf), TOTAL)
    open(OUT, "wb").write(buf)
    got = hashlib.sha256(buf).hexdigest()
    print("size", len(buf))
    print("sha256", got)
    print("MATCH" if got == WANT else "MISMATCH (want %s)" % WANT)
    return 0 if got == WANT else 1


if __name__ == "__main__":
    sys.exit(main())
