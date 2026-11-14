import subprocess,os,time,re,statistics,collections
ADB=r"C:/Users/MECHREV/AppData/Local/Temp/adbx/adb.exe";S="PHONE_SERIAL_REDACTED"
ENV=dict(os.environ,MSYS_NO_PATHCONV="1")
def sh(c,t=60):
    return subprocess.run([ADB,"-s",S,"shell",c],capture_output=True,env=ENV,timeout=t).stdout.decode("utf-8","ignore")
L=None
for l in sh("dumpsys SurfaceFlinger --list").splitlines():
    if "SurfaceView[" in l and "(BLAST)" in l and "XServerDisplayActivity" in l:
        i=l.find("SurfaceView[");j=l.find(" parentId");L=l[i:j].strip()
print("layer",L)
def mode():
    for l in sh("dumpsys display | grep -m1 mActiveModeId").splitlines():
        if "mActiveModeId" in l: return l.strip()
for i in range(4):
    raw=sh('dumpsys SurfaceFlinger --latency "%s"'%L)
    rows=[]
    for line in raw.splitlines():
        p=line.split()
        if len(p)==3:
            try:v=[int(x) for x in p]
            except:continue
            if v[1]>0: rows.append(v[1])
    d=[(rows[k+1]-rows[k])/1e6 for k in range(len(rows)-1)]
    d=[x for x in d if 0<x<1000]
    c=collections.Counter('<9.5' if x<9.5 else '<12' if x<12 else '<20' if x<20 else '<34' if x<34 else '<50' if x<50 else '<80' if x<80 else '>=80' for x in d)
    print("n=%d fps=%.1f med=%.2f p90=%.1f max=%.0f %s | %s"%(len(d)+1,(len(d))/(sum(d)/1000) if d else 0,statistics.median(d) if d else 0,sorted(d)[int(len(d)*.9)] if d else 0,max(d) if d else 0,dict(c),mode()))
    time.sleep(2)
