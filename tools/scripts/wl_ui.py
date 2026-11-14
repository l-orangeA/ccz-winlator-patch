
import subprocess, time, sys, xml.etree.ElementTree as ET, os
ADB = __import__("os").environ.get("ADB", r"D:\MuMuPlayer\nx_main\adb.exe")
SERIAL="TABLET_SERIAL_REDACTED"
env=dict(os.environ); env["MSYS_NO_PATHCONV"]="1"
def sh(args, timeout=30):
    return subprocess.run([ADB,"-s",SERIAL]+args, capture_output=True, env=env, timeout=timeout)
def tap(x,y):
    sh(["shell",f"input tap {int(x)} {int(y)}"])
def dump():
    sh(["shell","uiautomator dump /sdcard/_ui.xml"],timeout=40)
    p=sh(["exec-out","cat","/sdcard/_ui.xml"],timeout=40)
    return ET.fromstring(p.stdout.decode("utf-8","replace"))
def nodes(root):
    parent={}
    for n in root.iter():
        for c in n: parent[c]=n
    return parent
def center(b):
    import re
    m=re.findall(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',b)[0]
    x1,y1,x2,y2=map(int,m); return (x1+x2)//2,(y1+y2)//2
def find_text(root,text,exact=True):
    for n in root.iter():
        t=n.get('text','')
        if (t==text) if exact else (text in t):
            if n.get('bounds'): return n
    return None
def find_res(root,res_suffix):
    for n in root.iter():
        if n.get('resource-id','').endswith(res_suffix) and n.get('bounds'):
            return n
    return None
def tap_node(n):
    x,y=center(n.get('bounds')); tap(x,y); return x,y
def tile_run(root,title):
    parent=nodes(root)
    t=None
    for n in root.iter():
        if n.get('resource-id','').endswith('TVTitle') and n.get('text','')==title:
            t=n;break
    if t is None: return False
    # walk up to a node containing BTRun
    cur=t
    for _ in range(6):
        cur=parent.get(cur)
        if cur is None: return False
        for d in cur.iter():
            if d.get('resource-id','').endswith('BTRun'):
                tap_node(d); return True
    return False
def open_drawer(root):
    for n in root.iter():
        if n.get('resource-id','').endswith('Toolbar'):
            for c in n.iter():
                if c.get('class','').endswith('ImageButton'):
                    tap_node(c); return True
    return False
def wait_for(pred, tries=12, delay=0.5):
    for i in range(tries):
        r=dump()
        v=pred(r)
        if v is not None: return r,v
        time.sleep(delay)
    return None,None
def start_game():
    sh(["shell","am force-stop com.winlatcn"]); time.sleep(1)
    sh(["shell","monkey -p com.winlatcn -c android.intent.category.LAUNCHER 1"],timeout=30)
    time.sleep(3)
    r=None
    for attempt in range(8):
        r=dump()
        if find_text(r,"容器管理") is not None:
            break
        open_drawer(r); time.sleep(1.0)
        r=dump()
        if find_text(r,"容器管理") is not None:
            break
        sh(["shell","input keyevent 4"]); time.sleep(1.2)
    n=find_text(r,"容器管理")
    if n is None: print("ERR drawer"); return False
    tap_node(n); time.sleep(1.5); r=dump()
    # container list -> menu -> 管理
    menu=find_res(r,"BTMenu")
    if menu is None: print("ERR no BTMenu"); return False
    tap_node(menu); time.sleep(1)
    r=dump(); n=find_text(r,"管理")
    if n is None: print("ERR no 管理"); return False
    tap_node(n); time.sleep(2)
    # file manager: D: -> Download -> ccz22 -> 0run.bat
    for title in ["磁盘 (D:)","Download","ccz22","0run.bat"]:
        r=dump()
        if not tile_run(r,title):
            print("ERR tile",title); return False
        time.sleep(1.6)
    return True

def go_root(tries=10):
    for i in range(tries):
        r=dump()
        if find_text(r,"容器管理") is not None or find_text(r,"快捷方式") is not None:
            return r
        sh(["shell","input keyevent 4"]); time.sleep(1.0)
    return dump()
def open_settings():
    r=go_root()
    if find_text(r,"Box64版本") is not None: return r
    open_drawer(r); time.sleep(1.0); r=dump()
    n=find_text(r,"wine设置")
    if n is None:
        open_drawer(r); time.sleep(1.0); r=dump(); n=find_text(r,"wine设置")
    if n is None: return None
    tap_node(n); time.sleep(1.5); return dump()
def set_box64(ver):
    r=open_settings()
    if r is None: return "ERR settings"
    sp=find_res(r,"SBox64Version") or find_text(r,"0.3.0 (Default)")
    if sp is None: return "ERR spinner"
    tap_node(sp); time.sleep(1.2); r=dump()
    n=find_text(r,ver)
    if n is None: return "ERR option"
    tap_node(n); time.sleep(3)
    return "OK"


def open_edit():
    r=go_root()
    if r is None: return None
    m=find_res(r,"BTMenu")
    if m is None: return None
    tap_node(m); time.sleep(1.0)
    r=dump(); n=find_text(r,"编辑")
    if n is None: return None
    tap_node(n); time.sleep(2.0); return dump()
def spinner_text(r):
    for n in r.iter():
        if n.get("resource-id","").endswith("SWinVersion"):
            for c in n.iter():
                if c.get("class","").endswith("TextView") and c.get("text"): return c.get("text")
    return None
def set_winver(label):
    r=open_edit()
    if r is None: return "ERR edit"
    n=find_text(r,"高级")
    if n is None: return "ERR adv"
    tap_node(n); time.sleep(1.2); r=dump()
    n=find_res(r,"SWinVersion")
    if n is None: return "ERR winver"
    tap_node(n); time.sleep(1.5); r=dump()
    o=find_text(r,label)
    print("option node", o.get("bounds") if o is not None else None)
    if o is None: return "ERR option"
    tap_node(o); time.sleep(1.5); r=dump()
    print("spinner now", spinner_text(r))
    ok=find_res(r,"BTConfirm")
    if ok: tap_node(ok); time.sleep(2.0)
    r=dump(); print("after confirm spinner", spinner_text(r))
    return "OK"

if __name__=="__main__":
    print("OK" if start_game() else "FAIL")
