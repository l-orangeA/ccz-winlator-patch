import io, os, shutil

BASE = "/root/probe/smali/com/winlator/renderer"

def patch(path, old, new):
    with io.open(path, encoding="utf-8") as f:
        s = f.read()
    if s.count(old) != 1:
        raise SystemExit("pattern count %d in %s" % (s.count(old), path))
    s = s.replace(old, new)
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(s)
    print("patched", path)

g = os.path.join(BASE, "GLRenderer.smali")
patch(g,
"""    .param p1, "gl"    # Ljavax/microedition/khronos/opengles/GL10;

    .line 98""",
"""    .param p1, "gl"    # Ljavax/microedition/khronos/opengles/GL10;

    invoke-static {}, Lcom/winlator/renderer/PerfStats;->onRender()V

    .line 98""")

patch(g,
"""    .param p1, "window"    # Lcom/winlator/xserver/Window;

    .line 161""",
"""    .param p1, "window"    # Lcom/winlator/xserver/Window;

    invoke-static {}, Lcom/winlator/renderer/PerfStats;->onUpdate()V

    .line 161""")

t = os.path.join(BASE, "Texture.smali")
patch(t,
"""    invoke-static/range {v0 .. v8}, Landroid/opengl/GLES20;->glTexSubImage2D(IIIIIIIILjava/nio/Buffer;)V

    .line 114""",
"""    invoke-static {}, Lcom/winlator/renderer/PerfStats;->onUploadStart()V

    invoke-static/range {v0 .. v8}, Landroid/opengl/GLES20;->glTexSubImage2D(IIIIIIIILjava/nio/Buffer;)V

    invoke-static {}, Lcom/winlator/renderer/PerfStats;->onUploadDone()V

    .line 114""")

shutil.copy("/mnt/d/ccz_winlator_patch/unpack_tools/PerfStats2.smali",
            os.path.join(BASE, "PerfStats.smali"))
print("copied PerfStats.smali")
