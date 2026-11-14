import os

R = "/root/wn_out"
mp = os.path.join(R, "AndroidManifest.xml")
s = open(mp, encoding="utf-8").read()

old = """            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>"""
new = """            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
            <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts"/>
        </activity>"""
assert old in s, "main activity block not found"
s = s.replace(old, new, 1)

oldx = ('<activity android:configChanges="density|keyboard|keyboardHidden|navigation|orientation|'
        'screenLayout|screenSize|smallestScreenSize" android:exported="false" android:launchMode="singleTask" '
        'android:name="com.winlator.XServerDisplayActivity"')
newx = oldx.replace('android:exported="false"', 'android:exported="true"')
assert oldx in s, "XServerDisplayActivity not found"
s = s.replace(oldx, newx, 1)
open(mp, "w", encoding="utf-8").write(s)

sp = os.path.join(R, "res/values/strings.xml")
st = open(sp, encoding="utf-8").read()
if "shortcut_ccz_short" not in st:
    add = ('    <string name="shortcut_ccz_short">\u66f9\u64cd\u4f20</string>\n'
           '    <string name="shortcut_ccz_long">\u4e09\u56fd\u5fd7\u66f9\u64cd\u4f20 \u52a0\u5f3a\u7248</string>\n')
    st = st.replace("</resources>", add + "</resources>", 1)
    open(sp, "w", encoding="utf-8").write(st)

os.makedirs(os.path.join(R, "res/xml"), exist_ok=True)
open(os.path.join(R, "res/xml/shortcuts.xml"), "w", encoding="utf-8").write(
"""<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="ccz_game"
        android:enabled="true"
        android:icon="@mipmap/ic_launcher"
        android:shortcutShortLabel="@string/shortcut_ccz_short"
        android:shortcutLongLabel="@string/shortcut_ccz_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.winlatcn"
            android:targetClass="com.winlator.XServerDisplayActivity">
            <extra android:name="container_id" android:valueType="int" android:value="1"/>
            <extra android:name="exec_path" android:valueType="string" android:value="/storage/emulated/0/Download/ccz22/Ekd5.exe"/>
        </intent>
    </shortcut>
</shortcuts>
""")
print("patched ok")
