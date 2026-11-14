import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''                        amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);
                        {   /* deliver with our own ExceptionAddress (after the insn) */
                            struct xcontext xctx;
                            save_context( &xctx, ucontext );
                            setup_raise_exception( ucontext, &srec, &xctx );
                        }
                        return;'''
new = '''                        amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);
                        {   /* deliver with our own ExceptionAddress (after the insn) */
                            struct xcontext xctx;
                            save_context( &xctx, ucontext );
                            setup_raise_exception( ucontext, &srec, &xctx );
                        }
                        hwbp_consume();   /* HWBP_CONSUME: watch fired, page can stay open */
                        return;'''
assert old in s, 'match branch'
s = s.replace(old, new, 1)
old2 = '''extern void hwbp_reprotect_all( void );'''
new2 = '''extern void hwbp_reprotect_all( void );
extern void hwbp_consume( void );'''
s = s.replace(old2, new2, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('consume hook added')
