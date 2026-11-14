#!/usr/bin/env python3
"""把 v11.2 的鼠标模式改成「跟随点击」：写入 move_cursor_to_touchpoint=true。

依据：老模拟器 com.winlatcn 的 shared_prefs 里就是这一项 = true。
v11.2 的 prefs 没有该键 → 默认拖拽。
"""
import os
import subprocess
import time

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PKG = "com.winlator"
PREFS = "shared_prefs/com.winlator_preferences.xml"
KEY = "move_cursor_to_touchpoint"


def sh(a, t=120):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)


def cat(p):
    return sh(["exec-out", "run-as %s cat %s" % (PKG, p)]).stdout.decode("utf-8", "replace")


def main():
    xml = cat(PREFS)
    print("原 prefs:\n" + xml, flush=True)
    if KEY in xml:
        print("已存在 %s，改为 true" % KEY, flush=True)
        import re
        xml = re.sub(r'<boolean name="%s" value="[^"]*" />' % KEY,
                     '<boolean name="%s" value="true" />' % KEY, xml)
    else:
        xml = xml.replace("</map>", '    <boolean name="%s" value="true" />\n</map>' % KEY)
    print("新 prefs:\n" + xml, flush=True)

    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "prefs11.xml")
    open(p, "w", encoding="utf-8").write(xml)
    sh(["push", p, "/data/local/tmp/p11.xml"])
    sh(["shell", "run-as %s cp /data/local/tmp/p11.xml %s" % (PKG, PREFS)])
    sh(["shell", "rm -f /data/local/tmp/p11.xml"])
    got = cat(PREFS)
    print("回读含 key:", KEY in got and 'value="true"' in got, flush=True)

    sh(["shell", "am force-stop " + PKG])
    time.sleep(2)
    sh(["shell", "monkey -p %s -c android.intent.category.LAUNCHER 1" % PKG])
    time.sleep(6)
    print("app 已重启", flush=True)


if __name__ == "__main__":
    main()
