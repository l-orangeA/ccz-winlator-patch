NL = chr(10)
BS = chr(92)

p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'ALLOCPROBE' in s:
    print('already'); raise SystemExit
old = NL.join([
'    if (!*ret)',
'        limit = get_zero_bits_limit( zero_bits );',
'    else',
'        limit = 0;',
'',
'    return allocate_virtual_memory( ret, size_ptr, type, protect, 0, limit, 0, 0 );',
'}'])
new = NL.join([
'    if (!*ret)',
'        limit = get_zero_bits_limit( zero_bits );',
'    else',
'        limit = 0;',
'',
'    {',
'        PVOID req = *ret;',
'        SIZE_T rsz = *size_ptr;',
'        NTSTATUS st = allocate_virtual_memory( ret, size_ptr, type, protect, 0, limit, 0, 0 );',
'        if ((ULONG_PTR)req >= 0x80000000 || (ULONG_PTR)*ret >= 0x80000000)',
'        {',
'            char b[192]; int n = snprintf( b, sizeof(b),',
'                "ALLOCPROBE req=%lu size=%lu type=%lx prot=%lx ret=%lu rsize=%lu st=%ld' + BS + 'n",',
'                (unsigned long)req, (unsigned long)rsz, (unsigned long)type, (unsigned long)protect,',
'                (unsigned long)*ret, (unsigned long)*size_ptr, (long)st );',
'            write( 2, b, n );',
'        }',
'        return st;',
'    }',
'}'])
assert old in s, 'alloc tail'
s = s.replace(old, new, 1)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched ALLOCPROBE')
