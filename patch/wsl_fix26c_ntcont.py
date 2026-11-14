import subprocess
NL = chr(10)
BS = chr(92)
subprocess.run(['tar','-xzf','/mnt/d/unpack_tools/wine-20240905.tar.gz','-C','/tmp',
                'wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/server.c'], check=True)
src = '/tmp/wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/server.c'
dst = '/root/wine0905/dlls/ntdll/unix/server.c'
s = open(src, encoding='utf-8', errors='replace').read()
old = '    return signal_set_full_context( context );'
new = NL.join([
'    {',
'        static int ntc;',
'        if (ntc++ < 400)',
'        {',
'            char b[160]; int n = snprintf( b, sizeof(b),',
'                "NTCONT rip=%lx eflags=%lx tf=%d eax=%lx ecx=%lx edx=%lx' + BS + 'n",',
'                (unsigned long)context->Rip, (unsigned long)context->EFlags,',
'                (context->EFlags & 0x100) ? 1 : 0,',
'                (unsigned long)context->Rax, (unsigned long)context->Rcx,',
'                (unsigned long)context->Rdx );',
'            write( 2, b, n );',
'        }',
'    }',
'    return signal_set_full_context( context );'])
assert old in s
s = s.replace(old, new, 1)
open(dst, 'w', encoding='utf-8', newline=NL).write(s)
print('rewritten server.c with NTCONT')
