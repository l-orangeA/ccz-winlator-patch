import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if 'set_thread_wow64_context_inner' in s:
    print('already'); raise SystemExit
defn = 'NTSTATUS set_thread_wow64_context( HANDLE handle, const void *ctx, ULONG size )'
assert defn in s, 'defn'
s = s.replace(defn, 'static NTSTATUS set_thread_wow64_context_inner( HANDLE handle, const void *ctx, ULONG size )', 1)
# find the end of the function by brace matching
start = s.index('static NTSTATUS set_thread_wow64_context_inner')
brace = s.index('{', start)
depth = 0
i = brace
while i < len(s):
    if s[i] == '{': depth += 1
    elif s[i] == '}':
        depth -= 1
        if depth == 0: break
    i += 1
assert depth == 0
end = i + 1
wrapper = '''

NTSTATUS set_thread_wow64_context( HANDLE handle, const void *ctx, ULONG size )
{
    NTSTATUS ret = set_thread_wow64_context_inner( handle, ctx, size );
    hwbp_hold_end();   /* HWBP_TRAMP_SIG */
    return ret;
}
'''
s = s[:end] + wrapper + s[end:]
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('wrapper added')
