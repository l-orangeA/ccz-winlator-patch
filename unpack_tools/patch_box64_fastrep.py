"""box64 0.3.0 补丁：REP MOVS/STOS 原生 fast path（env BOX64_FASTREP）+ hotpage 命中计数（env BOX64_HOTPAGE_DUMP）。
在 WSL 跑: python3 patch_box64_fastrep.py /tmp/box64-0.3.0src/box64-0.3.0
设计要点：
- DF=1 保持原逐元素循环（罕见、避免重叠语义风险）；DF=0 且 RCX>=128 时调原生 C helper（memmove/memset/循环）
- 通过翻转 DF 分支顺序复用现有 MARK/MARK2/MARK3 三个槽位，不新增槽位
- 两个功能都有 env 门控，默认关闭 = 行为与原版一致
"""
import sys, os

ROOT = sys.argv[1] if len(sys.argv) > 1 else "/tmp/box64-0.3.0src/box64-0.3.0"

HELPER_C = r'''/* r111: native fast paths for REP MOVS/STOS + hotpage hit counter.
   BOX64_FASTREP=1 enables the dynarec fast paths; BOX64_HOTPAGE_DUMP=1 enables the counter dump. */
#include <stdio.h>
#include <string.h>
#include <stdint.h>

int box64_fastrep = 0;
int box64_hotpagedump = 0;

uintptr_t my_rep_movsb(uintptr_t dst, uintptr_t src, uintptr_t n) {
    memmove((void*)dst, (const void*)src, n);
    return n;
}
uintptr_t my_rep_movsd(uintptr_t dst, uintptr_t src, uintptr_t n) {
    memmove((void*)dst, (const void*)src, n*4);
    return n;
}
uintptr_t my_rep_movsq(uintptr_t dst, uintptr_t src, uintptr_t n) {
    memmove((void*)dst, (const void*)src, n*8);
    return n;
}
uintptr_t my_rep_stosb(uintptr_t dst, uintptr_t al, uintptr_t n) {
    memset((void*)dst, (int)(al & 0xff), n);
    return n;
}
uintptr_t my_rep_stosd(uintptr_t dst, uintptr_t val, uintptr_t n) {
    uint32_t* p = (uint32_t*)dst; uint32_t v = (uint32_t)val;
    for (uintptr_t i = 0; i < n; ++i) p[i] = v;
    return n;
}
uintptr_t my_rep_stosq(uintptr_t dst, uintptr_t val, uintptr_t n) {
    uint64_t* p = (uint64_t*)dst; uint64_t v = (uint64_t)val;
    for (uintptr_t i = 0; i < n; ++i) p[i] = v;
    return n;
}
static FILE* hp_f = NULL;
static unsigned long hp_n = 0;
void my_hotpage_hit(uintptr_t addr) {
    if (!box64_hotpagedump) return;
    ++hp_n;
    if (hp_n % 100000 == 0) {
        if (!hp_f) hp_f = fopen("/home/xuser/box64_hp.txt", "w");
        if (hp_f) { fprintf(hp_f, "hits=%lu last=%p\n", hp_n, (void*)addr); fflush(hp_f); }
    }
}
'''

CMAKE_ANCHOR = '    "${BOX64_ROOT}/src/box64context.c"'
CMAKE_ADD = CMAKE_ANCHOR + '\n    "${BOX64_ROOT}/src/my_repfast.c"'

CORE_ANCHOR = '    p = getenv("BOX64_DYNAREC_FORWARD");'
CORE_ADD = '''    p = getenv("BOX64_FASTREP");
    if(p && p[0] && p[0]!='0')
        box64_fastrep = 1;
    p = getenv("BOX64_HOTPAGE_DUMP");
    if(p && p[0] && p[0]!='0')
        box64_hotpagedump = 1;
''' + CORE_ANCHOR

DB_ANCHOR = '''    if(isInHotPage(addr))
        return NULL;'''
DB_ADD = '''    if(isInHotPage(addr)) {
        extern void my_hotpage_hit(uintptr_t addr);
        my_hotpage_hit(addr);
        return NULL;
    }'''

EXTERN_BLOCK = '''extern int box64_fastrep;
extern uintptr_t my_rep_movsb(uintptr_t dst, uintptr_t src, uintptr_t n);
extern uintptr_t my_rep_movsd(uintptr_t dst, uintptr_t src, uintptr_t n);
extern uintptr_t my_rep_movsq(uintptr_t dst, uintptr_t src, uintptr_t n);
extern uintptr_t my_rep_stosb(uintptr_t dst, uintptr_t al, uintptr_t n);
extern uintptr_t my_rep_stosd(uintptr_t dst, uintptr_t val, uintptr_t n);
extern uintptr_t my_rep_stosq(uintptr_t dst, uintptr_t val, uintptr_t n);
'''

A4_OLD = '''                MARK;   // Part with DF==0
                LDRB_S9_postindex(x1, xRSI, 1);
                STRB_S9_postindex(x1, xRDI, 1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_NEXT_nocond;
                MARK2;  // Part with DF==1
                LDRB_S9_postindex(x1, xRSI, -1);
                STRB_S9_postindex(x1, xRDI, -1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                // done'''
A4_NEW = '''                MARK;   // Part with DF==1 (r111: slow byte loop, DF1 kept slow)
                LDRB_S9_postindex(x1, xRSI, -1);
                STRB_S9_postindex(x1, xRDI, -1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_MARK3_nocond;
                // Part with DF==0
                if (box64_fastrep) {
                    SUBSx_U12(xZR, xRCX, 128);
                    B_MARK2(cLT);
                    MOVx_REG(x0, xRDI);
                    MOVx_REG(x1, xRSI);
                    MOVx_REG(x2, xRCX);
                    CALL_(my_rep_movsb, x4, 1);
                    ADDx_REG(xRDI, xRDI, x4);
                    ADDx_REG(xRSI, xRSI, x4);
                    MOVx_REG(xRCX, xZR);
                    B_NEXT_nocond;
                    MARK2;  // small count -> slow loop
                }
                LDRB_S9_postindex(x1, xRSI, 1);
                STRB_S9_postindex(x1, xRDI, 1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                MARK3;
                // done'''

A5_OLD = '''                MARK;   // Part with DF==0
                LDRxw_S9_postindex(x1, xRSI, rex.w?8:4);
                STRxw_S9_postindex(x1, xRDI, rex.w?8:4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_NEXT_nocond;
                MARK2;  // Part with DF==1
                LDRxw_S9_postindex(x1, xRSI, rex.w?-8:-4);
                STRxw_S9_postindex(x1, xRDI, rex.w?-8:-4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                // done'''
A5_NEW = '''                MARK;   // Part with DF==1 (r111: slow loop)
                LDRxw_S9_postindex(x1, xRSI, rex.w?-8:-4);
                STRxw_S9_postindex(x1, xRDI, rex.w?-8:-4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_MARK3_nocond;
                // Part with DF==0
                if (box64_fastrep) {
                    SUBSx_U12(xZR, xRCX, 128);
                    B_MARK2(cLT);
                    MOVx_REG(x0, xRDI);
                    MOVx_REG(x1, xRSI);
                    MOVx_REG(x2, xRCX);
                    CALL_(rex.w?my_rep_movsq:my_rep_movsd, x4, 1);
                    ADDx_REG(xRDI, xRDI, x4);
                    ADDx_REG(xRSI, xRSI, x4);
                    MOVx_REG(xRCX, xZR);
                    B_NEXT_nocond;
                    MARK2;  // small count -> slow loop
                }
                LDRxw_S9_postindex(x1, xRSI, rex.w?8:4);
                STRxw_S9_postindex(x1, xRDI, rex.w?8:4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                MARK3;
                // done'''

AA_OLD = '''                MARK;   // Part with DF==0
                STRB_S9_postindex(xRAX, xRDI, 1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_MARK3_nocond;
                MARK2;  // Part with DF==1
                STRB_S9_postindex(xRAX, xRDI, -1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                MARK3;
                // done'''
AA_NEW = '''                MARK;   // Part with DF==1 (r111: slow loop)
                STRB_S9_postindex(xRAX, xRDI, -1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_MARK3_nocond;
                if (box64_fastrep) {
                    SUBSx_U12(xZR, xRCX, 128);
                    B_MARK2(cLT);
                    MOVx_REG(x0, xRDI);
                    MOVx_REG(x1, xRAX);
                    MOVx_REG(x2, xRCX);
                    CALL_(my_rep_stosb, x4, 1);
                    ADDx_REG(xRDI, xRDI, x4);
                    MOVx_REG(xRCX, xZR);
                    B_NEXT_nocond;
                    MARK2;  // small count -> slow loop
                }
                STRB_S9_postindex(xRAX, xRDI, 1);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                MARK3;
                // done'''

AB_OLD = '''                MARK;   // Part with DF==0
                STRxw_S9_postindex(xRAX, xRDI, rex.w?8:4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_MARK3_nocond;
                MARK2;  // Part with DF==1
                STRxw_S9_postindex(xRAX, xRDI, rex.w?-8:-4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                MARK3;
                // done'''
AB_NEW = '''                MARK;   // Part with DF==1 (r111: slow loop)
                STRxw_S9_postindex(xRAX, xRDI, rex.w?-8:-4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK(xRCX);
                B_MARK3_nocond;
                if (box64_fastrep) {
                    SUBSx_U12(xZR, xRCX, 128);
                    B_MARK2(cLT);
                    MOVx_REG(x0, xRDI);
                    MOVx_REG(x1, xRAX);
                    MOVx_REG(x2, xRCX);
                    CALL_(rex.w?my_rep_stosq:my_rep_stosd, x4, 1);
                    ADDx_REG(xRDI, xRDI, x4);
                    MOVx_REG(xRCX, xZR);
                    B_NEXT_nocond;
                    MARK2;  // small count -> slow loop
                }
                STRxw_S9_postindex(xRAX, xRDI, rex.w?8:4);
                SUBx_U12(xRCX, xRCX, 1);
                CBNZx_MARK2(xRCX);
                MARK3;
                // done'''


def patch(path, pairs, optional=False):
    full = os.path.join(ROOT, path)
    src = open(full, encoding="utf-8", errors="replace").read()
    changed = False
    for name, old, new in pairs:
        if new in src:
            continue
        cnt = src.count(old)
        if cnt != 1:
            if optional:
                print("  SKIP %s (%s count=%d)" % (path, name, cnt))
                continue
            raise SystemExit("anchor %s in %s count=%d != 1" % (name, path, cnt))
        src = src.replace(old, new)
        changed = True
        print("  patched %s: %s" % (path, name))
    if changed:
        open(full, "w", encoding="utf-8", newline="\n").write(src)


def main():
    open(os.path.join(ROOT, "src", "my_repfast.c"), "w", encoding="utf-8", newline="\n").write(HELPER_C)
    print("wrote src/my_repfast.c")

    patch("CMakeLists.txt", [("box64context.c src line", CMAKE_ANCHOR, CMAKE_ADD)])
    patch("src/core.c", [("env parse", CORE_ANCHOR, CORE_ADD)])
    patch("src/dynarec/dynablock.c", [("hotpage counter", DB_ANCHOR, DB_ADD)])
    # externs for the dynarec file: insert right before "case 0xA4:" anchor block
    patch("src/dynarec/arm64/dynarec_arm64_00.c", [
        ("externs", "        case 0xA4:", EXTERN_BLOCK + "        case 0xA4:"),
        ("0xA4 rep movsb", A4_OLD, A4_NEW),
        ("0xA5 rep movsd", A5_OLD, A5_NEW),
        ("0xAA rep stosb", AA_OLD, AA_NEW),
        ("0xAB rep stosd", AB_OLD, AB_NEW),
    ])
    print("ALL PATCHED")


if __name__ == "__main__":
    main()
