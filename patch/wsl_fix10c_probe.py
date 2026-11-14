NL = chr(10)
BS = chr(92)

def sub(path, old, new, tag):
    s = open(path, encoding='utf-8', errors='replace').read()
    if new in s:
        print('already', tag); return
    assert old in s, 'pattern missing: ' + tag
    s = s.replace(old, new, 1)
    open(path, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched', tag)

sig = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
vir = '/root/wine0905/dlls/ntdll/unix/virtual.c'

old = '    stack = virtual_setup_exception( stack_ptr, stack_size, rec );' + NL
new = NL.join([
'    stack = virtual_setup_exception( stack_ptr, stack_size, rec );',
'    if ((ULONG_PTR)rec->ExceptionAddress < 0x10000000)',
'    {',
'        char b[160]; int n = snprintf( b, sizeof(b),',
'            "SRE_AFTER code=%lx stack=%lx stackptr=%lx' + BS + 'n",',
'            (unsigned long)rec->ExceptionCode, (unsigned long)stack,',
'            (unsigned long)stack_ptr );',
'        write( 2, b, n );',
'    }',
''])
sub(sig, old, new, 'SRE_AFTER')

old = NL.join([
'    mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'    vprot = get_page_vprot( page );',
'',
'#ifdef __APPLE__'])
new = NL.join([
'    mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'    vprot = get_page_vprot( page );',
'    if ((ULONG_PTR)addr < 0x10000000)',
'    {',
'        char b[192]; int n = snprintf( b, sizeof(b),',
'            "VHF_ENTRY addr=%lx err=%x stack=%lx page=%lx vprot=%x' + BS + 'n",',
'            (unsigned long)addr, err, (unsigned long)stack, (unsigned long)page, vprot );',
'        write( 2, b, n );',
'    }',
'',
'#ifdef __APPLE__'])
sub(vir, old, new, 'VHF_ENTRY')

old = NL.join([
'    mutex_unlock( &virtual_mutex );',
'    return ret;',
'}'])
new = NL.join([
'    if ((ULONG_PTR)addr < 0x10000000)',
'    {',
'        char b[128]; int n = snprintf( b, sizeof(b),',
'            "VHF_EXIT addr=%lx ret=%lx' + BS + 'n",',
'            (unsigned long)addr, (unsigned long)ret );',
'        write( 2, b, n );',
'    }',
'    mutex_unlock( &virtual_mutex );',
'    return ret;',
'}'])
s = open(vir, encoding='utf-8', errors='replace').read()
idx = s.index('virtual_handle_fault( void *addr, DWORD err, void *stack )')
end = s.index('virtual_setup_exception', idx)
seg = s[idx:end]
if 'VHF_EXIT' in seg:
    print('already VHF_EXIT')
else:
    assert old in seg, 'vhf exit missing'
    s = s[:idx] + seg.replace(old, new, 1) + s[end:]
    open(vir, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched VHF_EXIT')
