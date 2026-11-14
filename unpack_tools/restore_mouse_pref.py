#!/usr/bin/env python3
"""Restore native drag-style mouse after auto_load.py forced touchpoint mode.

auto_load.set_pref() sets move_cursor_to_touchpoint=true for UI automation.
README (2026-11-07) says the user's normal setting is false (native drag).
Run this after every automated measurement round.
"""
import os
import subprocess
import sys

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = os.environ.get("WL_SER", "PHONE_SERIAL_REDACTED")
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PREF = "shared_prefs/com.winlatcn_preferences.xml"


def sh(args, inp=None):
    return subprocess.run([ADB, "-s", SER] + args, capture_output=True,
                          env=ENV, timeout=60, input=inp)


def read_pref():
    r = sh(["exec-out", "run-as", "com.winlatcn", "cat", PREF])
    return r.stdout.decode("utf-8", "replace")


def main():
    xml = read_pref()
    if not xml.strip():
        print("FAIL: empty prefs")
        return 2
    if 'move_cursor_to_touchpoint" value="false"' in xml:
        print("OK: already false (native drag)")
        return 0
    if 'move_cursor_to_touchpoint" value="true"' in xml:
        xml = xml.replace('move_cursor_to_touchpoint" value="true"',
                          'move_cursor_to_touchpoint" value="false"')
    else:
        xml = xml.replace("</map>",
                          '    <boolean name="move_cursor_to_touchpoint" value="false" />\n</map>')
    r = sh(["shell", "run-as", "com.winlatcn",
            "sh -c 'cat > " + PREF + "'"], inp=xml.encode())
    back = read_pref()
    ok = 'move_cursor_to_touchpoint" value="false"' in back
    print("OK: restored to false" if ok else "FAIL: verify mismatch")
    return 0 if ok else 3


if __name__ == "__main__":
    sys.exit(main())
