import sys
p='/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s=open(p,encoding='utf-8',errors='replace').read()
if '#include <sys/mman.h>' not in s:
    s=s.replace('#include <sys/types.h>','#include <sys/types.h>\n#include <sys/mman.h>',1)
old="""    if (status) return status;
    stack = (struct exc_stack_layout *)((context->Rsp - sizeof(*stack)) & ~(ULONG_PTR)63);
    memmove( &stack->context, context, sizeof(*context) );"""
new="""    if (status) return status;
    stack = (struct exc_stack_layout *)((context->Rsp - sizeof(*stack)) & ~(ULONG_PTR)63);
    /* Windows delivers the exception frame even when the application made its
     * stack read-only (a classic anti-debug technique).  The guest vprot view is
     * left untouched; only the host mapping is made writable so that the frame
     * and the handler's own pushes don't fault during the dispatch. */
    {
        ULONG_PTR page_size = getpagesize();
        ULONG_PTR page = (ULONG_PTR)stack & ~(page_size - 1);
        ULONG_PTR end  = ((ULONG_PTR)stack + sizeof(*stack) + page_size - 1) & ~(page_size - 1);
        mprotect( (void *)page, end - page, PROT_READ | PROT_WRITE );
    }
    memmove( &stack->context, context, sizeof(*context) );"""
if 'mprotect( (void *)page' in s:
    print('already patched'); sys.exit(0)
if old not in s:
    print('PATTERN NOT FOUND'); sys.exit(1)
s=s.replace(old,new,1)
open(p,'w',encoding='utf-8',newline='\n').write(s)
print('patched 0905')
