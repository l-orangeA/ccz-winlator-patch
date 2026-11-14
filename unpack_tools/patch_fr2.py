import io, os
base = '/root/wn_out/smali/com/winlator/'

def patch(path, old, new):
    p = base + path
    s = io.open(p, encoding='utf-8').read()
    n = s.count(old)
    assert n == 1, (path, n)
    s = s.replace(old, new)
    io.open(p, 'w', encoding='utf-8').write(s)
    print('patched', path)

# 1) window frame rate hint
p = base + 'XServerDisplayActivity.smali'
s = io.open(p, encoding='utf-8').read()
anchor = '    invoke-virtual {v2, v0}, Landroid/view/Window;->setAttributes(Landroid/view/WindowManager$LayoutParams;)V\n\n    return-void'
if 'setFrameRate(F)V' not in s:
    assert s.count(anchor) == 1, s.count(anchor)
    s = s.replace(anchor,
        '    invoke-virtual {v2, v0}, Landroid/view/Window;->setAttributes(Landroid/view/WindowManager$LayoutParams;)V\n\n'
        '    const/high16 v1, 0x42f00000    # 120.0f\n\n'
        '    invoke-virtual {v2, v1}, Landroid/view/Window;->setFrameRate(F)V\n\n'
        '    return-void')
    io.open(p, 'w', encoding='utf-8').write(s)
    print('patched window setFrameRate')
else:
    print('window setFrameRate already present')

# 2) surface frame rate hint
p = base + 'widget/XServerView.smali'
s = io.open(p, encoding='utf-8').read()
if 'surfaceCreated' not in s:
    s = s.rstrip() + '''

.method public surfaceCreated(Landroid/view/SurfaceHolder;)V
    .locals 3

    invoke-super {p0, p1}, Landroid/opengl/GLSurfaceView;->surfaceCreated(Landroid/view/SurfaceHolder;)V

    :try_start_0
    invoke-interface {p1}, Landroid/view/SurfaceHolder;->getSurface()Landroid/view/Surface;
    move-result-object v0

    const/high16 v1, 0x42f00000    # 120.0f

    const/4 v2, 0x1

    invoke-virtual {v0, v1, v2}, Landroid/view/Surface;->setFrameRate(FI)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    return-void
.end method
'''
    io.open(p, 'w', encoding='utf-8').write(s)
    print('added surfaceCreated rate hint')
else:
    print('surfaceCreated already present')
