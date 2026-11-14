import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
# remove hold_begin from set_thread_wow64_context
old = '''    hwbp_hold_begin();   /* HWBP_TRAMP_SIG: host reads the 32-bit context below */
'''
assert old in s, 'hold begin'
s = s.replace(old, '', 1)
# add hold_begin at setup_raise_exception entry
defn = 'static void setup_raise_exception( ucontext_t *sigcontext, EXCEPTION_RECORD *rec, struct xcontext *xcontext )'
assert defn in s, 'setup defn'
i = s.index(defn)
brace = s.index('{', i)
s = s[:brace+1] + '\n    hwbp_hold_begin();   /* HWBP_TRAMP_SIG: let the guest handler edit its context */' + s[brace+1:]
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('hold moved to setup_raise_exception')
