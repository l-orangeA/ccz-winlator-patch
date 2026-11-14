"""切换 move_cursor_to_touchpoint（自动化需要 true，日常游玩用 false）。
用法: python set_mouse_drag.py <true|false>
"""
import subprocess, os, sys

ADB = os.environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SER = "PHONE_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
PREF = "shared_prefs/com.winlatcn_preferences.xml"


def sh(a, t=60, inp=None):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t, input=inp)


def txt(r):
    return r.stdout.decode("utf-8", "replace") if r else ""


val = sys.argv[1] if len(sys.argv) > 1 else "false"
out = txt(sh(["exec-out", "run-as com.winlatcn cat " + PREF]))
token_new = '<boolean name="move_cursor_to_touchpoint" value="%s" />' % val
token_old_true = '<boolean name="move_cursor_to_touchpoint" value="true" />'
token_old_false = '<boolean name="move_cursor_to_touchpoint" value="false" />'
if token_new in out:
    print("already", val)
    sys.exit(0)
if token_old_true in out:
    out = out.replace(token_old_true, token_new)
elif token_old_false in out:
    out = out.replace(token_old_false, token_new)
else:
    out = out.replace("</map>", '    %s\n</map>' % token_new)
r = sh(["shell", "run-as com.winlatcn sh -c 'cat > " + PREF + "'"], inp=out.encode())
back = txt(sh(["exec-out", "run-as com.winlatcn cat " + PREF]))
line = [l for l in back.splitlines() if "move_cursor_to_touchpoint" in l]
print("now:", line[0].strip() if line else "MISSING")
print("ok" if 'value="false"' in (line[0] if line else "") else "FAILED")
