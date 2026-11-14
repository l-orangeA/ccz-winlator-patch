"""只启动容器（BTRun，不带 exe），触发 setupWineSystemFiles 打全补丁。不 force-stop。"""
import os, sys, time

os.environ["ADB"] = r"C:\Users\MECHREV\AppData\Local\Temp\adbx\adb.exe"
sys.path.insert(0, r"D:\ccz_winlator_patch\tools")
import wl_ui as U

r = U.go_root()
print("容器管理:", U.find_text(r, "容器管理") is not None)
bt = U.find_res(r, "BTRun")
print("BTRun:", bt is not None)
if bt is None:
    print("FAIL no BTRun")
    sys.exit(1)
print("tap:", U.tap_node(bt))
print("OK container starting")
