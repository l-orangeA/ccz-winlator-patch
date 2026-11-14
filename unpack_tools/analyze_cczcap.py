import re, collections, statistics

PID = {"app":12005,"game":16000,"wine":15643,"sf":1163,"comp":1025,"kswapd":111,
       "render":15537,"gl":15565,"xthr":15592}
WANT = set(PID.values())

switch_re = re.compile(r'.*?\[(\d+)\]\s+\S+\s+([\d.]+): sched_switch: prev_comm=(.*?) prev_pid=(\d+) prev_prio=\S+ prev_state=(\S+) ==> next_comm=(.*?) next_pid=(\d+)')
wake_re = re.compile(r'.*?\[(\d+)\]\s+\S+\s+([\d.]+): sched_wakeup: comm=(.*?) pid=(\d+) prio=\S+ target_cpu=(\d+)')
freq_re = re.compile(r'.*?([\d.]+): cpu_frequency: state=(\d+) cpu_id=(\d+)')
mark_re = re.compile(r'.*?([\d.]+): tracing_mark_write: B\|12005\|queueBuffer')

sw=[]; wakes=[]; freqs=[]; marks=[]
nlines=0; tmin=None; tmax=None
with open("tail1g.txt", "r", encoding="utf-8", errors="ignore") as f:
    for line in f:
        nlines += 1
        if "sched_switch:" in line:
            m = switch_re.match(line)
            if m:
                pp = int(m.group(4)); np = int(m.group(7))
                if pp in WANT or np in WANT:
                    t = float(m.group(2))
                    sw.append((t, int(m.group(1)), m.group(3), pp, m.group(5), m.group(6), np))
        elif "sched_wakeup:" in line:
            m = wake_re.match(line)
            if m and int(m.group(4)) in WANT:
                wakes.append((float(m.group(2)), m.group(3), int(m.group(4))))
        elif "cpu_frequency:" in line:
            m = freq_re.match(line)
            if m:
                freqs.append((float(m.group(1)), int(m.group(2)), int(m.group(3))))
        elif "queueBuffer" in line and "|12005|" in line:
            m = mark_re.match(line)
            if m:
                marks.append(float(m.group(1)))
sw.sort(); wakes.sort(); freqs.sort(); marks.sort()
print("lines=%d events: sw=%d wakes=%d freqs=%d marks=%d" % (nlines, len(sw), len(wakes), len(freqs), len(marks)))
if not sw:
    raise SystemExit("no switch events")
T0=sw[0][0]; T1=sw[-1][0]
print("span %.1fs (%.2f min)" % (T1-T0, (T1-T0)/60))

def analyze(pid, name, thr=8.0):
    segs=[]; last=None
    for t,cpu,pc,pp,ps,nc,np in sw:
        if pp==pid:
            if last is not None:
                segs.append((last,t,ps,cpu))
            last=None
        if np==pid:
            last=t
    wk=collections.defaultdict(list)
    for t,comm,p in wakes:
        if p==pid: wk[comm].append(t)
    rq=slp=0.0; slow=[]
    cpus=collections.Counter()
    for a,b,st,cpu in segs:
        d=(b-a)*1000
        cpus[cpu]+=1
        if st in ('R','R+'): rq+=d
        else: slp+=d
        if d>thr:
            src='-'
            if st not in ('R','R+'):
                cand=[x for x in wk.get(pid,[]) if a-0.05<=x[0]<=b]
                if cand: src=cand[-1][1]
            slow.append((d,st,cpu,a,src))
    slow.sort(reverse=True)
    print("\n== %-8s pid=%-6d segs=%-6d runq=%6.1fms(%.1f%%) sleep=%8.1fms slow>%.0fms=%d cpus=%s" % (
        name,pid,len(segs),rq,rq/((T1-T0)*10),slp,thr,len(slow),dict(cpus.most_common(4))))
    print("   top wakers:", collections.Counter({k:len(v) for k,v in wk.items()}).most_common(5))
    for d,st,cpu,a,src in slow[:12]:
        print("   %8.1fms %-3s cpu%d t=%.3f waker=%s" % (d,st,cpu,a-T0,src))
    return segs

segs_game = analyze(16000,'game',8)
segs_gl = analyze(15565,'gl',8)
segs_x = analyze(15592,'xthr',8)
analyze(12005,'appmain',8)
analyze(15643,'wineserver',8)
analyze(1163,'SF',20)
analyze(1025,'composer',20)
analyze(111,'kswapd0',20)

# frames
if marks:
    q=[]
    last=None
    for t in marks:
        if last is None or (t-last)*1000>2.0:
            q.append(t)
        last=t
    d=[(q[i+1]-q[i])*1000 for i in range(len(q)-1)]
    d=[x for x in d if 0<x<2000]
    print("\n== frames(queueBuffer clusters) n=%d span=%.1fs fps=%.1f med=%.1f p90=%.1f max=%.0f" % (
        len(q), q[-1]-q[0], len(q)/(q[-1]-q[0]) if len(q)>1 else 0,
        statistics.median(d) if d else 0, sorted(d)[int(len(d)*.9)] if d else 0, max(d) if d else 0))

# cpu freq summary
bycpu=collections.defaultdict(list)
for t,f,c in freqs: bycpu[c].append(f)
for c in sorted(bycpu):
    v=bycpu[c]
    print("cpu%d freq MHz: min=%.0f med=%.0f max=%.0f" % (c,min(v)/1000,statistics.median(v)/1000,max(v)/1000))
