import subprocess
subprocess.run(['tar','-xzf','/mnt/d/unpack_tools/wine-20240905.tar.gz','-C','/tmp','wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/virtual.c'],check=True)
p='/root/wine0905/dlls/ntdll/unix/virtual.c'
src='/tmp/wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/virtual.c'
s=open(src,encoding='utf-8',errors='replace').read()
nl=chr(10)
old=nl.join([
'    else if (stack < stack_info.limit)',
'    {',
'        mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'        if ((get_page_vprot( stack ) & VPROT_GUARD) &&',
'            grow_thread_stack( ROUND_ADDR( stack, page_mask ), &stack_info ))'])
new=nl.join([
'    else if (stack < stack_info.limit)',
'    {',
'        DWORD vprot;',
'        mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'        vprot = get_page_vprot( stack );',
'        /* Only treat a writable guard page as stack growth; a page the application',
'         * made read-only must still be delivered as an access violation. */',
'        if ((vprot & VPROT_GUARD) && (vprot & VPROT_WRITE) &&',
'            grow_thread_stack( ROUND_ADDR( stack, page_mask ), &stack_info ))'])
assert old in s, 'pattern missing'
s=s.replace(old,new,1)
open(p,'w',encoding='utf-8',newline=nl).write(s)
print('virtual.c patched')
