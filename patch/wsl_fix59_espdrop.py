import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '        wow_context->Esp = get_wow_teb( teb )->Tib.StackBase - 16;'
new = '        { unsigned long drop = 0; const char *e = getenv("WINE_ESP_DROP"); if (e) drop = strtoul( e, NULL, 0 ); wow_context->Esp = get_wow_teb( teb )->Tib.StackBase - 16 - drop; }  /* ESP_DROP */'
assert old in s, 'esp line'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('esp drop ok')
