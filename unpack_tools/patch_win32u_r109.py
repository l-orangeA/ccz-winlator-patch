"""第二轮 win32u 补丁（r109）：A1 memset→libc + A6 -O3 自动向量化。
在 WSL 里跑: python3 patch_win32u_r109.py
改动文件: /root/wine0905/dlls/win32u/dibdrv/primitives.c（只动这一个文件）
1) 文件头加 #pragma GCC optimize("O3") —— copy_rect_bits_32 / do_rop_codes_line_16 /
   solid_rects_32 等逐像素 LOOP 是纯元素级操作，-O3 自动向量化成 SSE2 宽 store，
   box64 对 SSE2 1:1 映射 NEON（等价于 A3 手工加宽，但零语义风险）。
2) memset_32/memset_16 的 inline rep stos/stosw 替换为 libc memset/memcpy 调用
   （box64 对显式 libc 调用直调原生 ARM 实现，见 report §4 A1）。
"""
import sys

PATH = "/root/wine0905/dlls/win32u/dibdrv/primitives.c"

PRAGMA_ANCHOR = 'WINE_DEFAULT_DEBUG_CHANNEL(dib);'
PRAGMA = PRAGMA_ANCHOR + '\n\n/* r109: 本文件热循环自动向量化（box64 对 SSE2 1:1 映射 NEON） */\n#pragma GCC optimize("O3")'

MEMSET32_OLD = '''static inline void memset_32( DWORD *start, DWORD val, DWORD size )
{
#if defined(__GNUC__) && (defined(__i386__) || defined(__x86_64__))
    DWORD dummy;
    __asm__ __volatile__( "cld; rep; stosl"
                          : "=c" (dummy), "=D" (dummy)
                          : "a" (val), "0" (size), "1" (start) );
#else
    while (size--) *start++ = val;
#endif
}'''

MEMSET32_NEW = '''static inline void memset_32( DWORD *start, DWORD val, DWORD size )
{
    /* r109: inline rep stosl 在 box64 下只有 8B/迭代的翻译；改走 libc
       （box64 对显式 memset/memcpy 调用直调原生 ARM 实现） */
    if ((val & 0xff) == ((val >> 8) & 0xff) && (val & 0xff) == ((val >> 16) & 0xff) &&
        (val & 0xff) == ((val >> 24) & 0xff))
        memset( start, (int)(val & 0xff), (size_t)size * 4 );
    else
    {
        DWORD tile[16];
        int n;
        for (n = 0; n < 16; n++) tile[n] = val;
        while (size >= 16)
        {
            memcpy( start, tile, sizeof(tile) );
            start += 16;
            size -= 16;
        }
        while (size--) *start++ = val;
    }
}'''

MEMSET16_OLD = '''static inline void memset_16( WORD *start, WORD val, DWORD size )
{
#if defined(__GNUC__) && (defined(__i386__) || defined(__x86_64__))
    DWORD dummy;
    __asm__ __volatile__( "cld; rep; stosw"
                          : "=c" (dummy), "=D" (dummy)
                          : "a" (val), "0" (size), "1" (start) );
#else
    while (size--) *start++ = val;
#endif
}'''

MEMSET16_NEW = '''static inline void memset_16( WORD *start, WORD val, DWORD size )
{
    /* r109: 同 memset_32，16 位图案用 64B tile + libc memcpy */
    if ((val & 0xff) == (val >> 8))
        memset( start, (int)(val & 0xff), (size_t)size * 2 );
    else
    {
        WORD tile[32];
        int n;
        for (n = 0; n < 32; n++) tile[n] = val;
        while (size >= 32)
        {
            memcpy( start, tile, sizeof(tile) );
            start += 32;
            size -= 32;
        }
        while (size--) *start++ = val;
    }
}'''

def main():
    src = open(PATH, encoding="utf-8").read()
    if '#pragma GCC optimize("O3")' in src:
        print("already patched")
        return
    for name, old, new in [("pragma", PRAGMA_ANCHOR, PRAGMA),
                           ("memset_32", MEMSET32_OLD, MEMSET32_NEW),
                           ("memset_16", MEMSET16_OLD, MEMSET16_NEW)]:
        if src.count(old) != 1:
            sys.exit("anchor %s count=%d != 1" % (name, src.count(old)))
        src = src.replace(old, new)
    open(PATH, "w", encoding="utf-8", newline="\n").write(src)
    print("patched ok")

if __name__ == "__main__":
    main()
