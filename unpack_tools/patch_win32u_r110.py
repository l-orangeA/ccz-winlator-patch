"""r110: 在 r109f（freetype+NDEBUG 兼容基线）之上加 A1 memset→libc 优化。不带 pragma O3。
在 WSL 跑: python3 patch_win32u_r110.py && cd /root/wine0905 && make -j8 dlls/win32u/win32u.so
前置: primitives.c 必须是 pristine（r109e 还原后的状态）。
"""
import sys

PATH = "/root/wine0905/dlls/win32u/dibdrv/primitives.c"

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
    /* r110(A1): inline rep stosl 在 box64 下只有 8B/迭代的翻译；改走 libc
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
    /* r110(A1): 同 memset_32，16 位图案用 64B tile + libc memcpy */
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

src = open(PATH, encoding="utf-8").read()
if "r110(A1)" in src:
    print("already r110")
    sys.exit(0)
for name, old, new in [("memset_32", MEMSET32_OLD, MEMSET32_NEW),
                       ("memset_16", MEMSET16_OLD, MEMSET16_NEW)]:
    if src.count(old) != 1:
        sys.exit("anchor %s count=%d != 1（primitives.c 不是 pristine？）" % (name, src.count(old)))
    src = src.replace(old, new)
open(PATH, "w", encoding="utf-8", newline="\n").write(src)
print("r110 memset patch applied")
