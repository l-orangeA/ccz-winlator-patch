import json, subprocess, os, sys

ADB = r"D:\MuMuPlayer\nx_main\adb.exe"
SER = "TABLET_SERIAL_REDACTED"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")

def sh(a, t=60):
    return subprocess.run([ADB, "-s", SER] + a, capture_output=True, env=ENV, timeout=t)

def main(args):
    p = sh(["exec-out", "run-as com.winlatcn cat files/imagefs/home/xuser-1/.container"], 40)
    d = json.loads(p.stdout.decode("utf-8", "replace"))
    kv = {}
    for item in d["envVars"].split():
        if "=" in item:
            k, v = item.split("=", 1)
            kv[k] = v
    for spec in args:
        if spec.startswith("@"):
            k, v = spec[1:].split("=", 1)
            d[k] = v if v not in ("true", "false") else (v == "true")
        elif "=" in spec:
            k, v = spec.split("=", 1)
            if v == "":
                kv.pop(k, None)
            else:
                kv[k] = v
        else:
            kv.pop(spec, None)
    d["envVars"] = " ".join("%s=%s" % (k, v) for k, v in kv.items())
    out = json.dumps(d, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    open(r"D:\ccz_winlator_patch\unpack_tools\container_new.json", "wb").write(out)
    sh(["push", r"D:\ccz_winlator_patch\unpack_tools\container_new.json", "/data/local/tmp/cnew.json"], 60)
    r = sh(["shell", 'run-as com.winlatcn cp /data/local/tmp/cnew.json files/imagefs/home/xuser-1/.container && echo ok'], 40)
    print(r.stdout.decode().strip() or r.stderr.decode().strip())
    print("preset=%s screen=%s cpuWoW64=%s gfx=%s dx=%s" % (
        d.get("box64Preset"), d.get("screenSize"), d.get("cpuListWoW64"),
        d.get("graphicsDriver"), d.get("dxwrapper")))

if __name__ == "__main__":
    main(sys.argv[1:])
