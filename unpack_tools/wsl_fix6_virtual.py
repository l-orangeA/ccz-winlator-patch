import subprocess
subprocess.run(['tar','-xzf','/mnt/d/unpack_tools/wine-20240905.tar.gz','-C','/tmp','wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/virtual.c'],check=True)
p='/root/wine0905/dlls/ntdll/unix/virtual.c'
src='/tmp/wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/virtual.c'
s=open(src,encoding='utf-8',errors='replace').read()
bs=chr(92); nl=chr(10)
old=nl.join([
'    else if (stack < stack_info.limit)',
'    {',
'        mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'        if ((get_page_vprot( stack ) & VPROT_GUARD) &&',
'            grow_thread_stack( ROUND_ADDR( stack, page_mask ), &stack_info ))'])
new=nl.join([
'    else if (stack < stack_info.limit)',
'    {',
'        if (stack_info.is_wow)',
'        {',
'            static const char m[] = "WOW_SKIP_GUARD' + bs + 'n";',
'            write( 2, m, sizeof(m) - 1 );',
'        }',
'        else',
'        {',
'        mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'        if ((get_page_vprot( stack ) & VPROT_GUARD) &&',
'            grow_thread_stack( ROUND_ADDR( stack, page_mask ), &stack_info ))'])
assert old in s, 'pattern'
s=s.replace(old,new,1)
# close the extra else block after mutex_unlock
old2=nl.join([
'        }',
'        mutex_unlock( &virtual_mutex );',
'    }',
'#if defined(VALGRIND_MAKE_MEM_UNDEFINED)'])
new2=nl.join([
'        }',
'        mutex_unlock( &virtual_mutex );',
'        }',
'    }',
'#if defined(VALGRIND_MAKE_MEM_UNDEFINED)'])
assert old2 in s, 'close pattern'
s=s.replace(old2,new2,1)
open(p,'w',encoding='utf-8',newline=nl).write(s)
print('virtual.c fix6')
