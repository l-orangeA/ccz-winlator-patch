"""box64 0.3.0 补丁 #2: env 解析放 main()（rcfile 表机制绕过），撤销 core.c 死代码插入。
在 WSL 跑: python3 patch_box64_main_env.py /tmp/box64-0.3.0src/box64-0.3.0
"""
import sys, os

ROOT = sys.argv[1] if len(sys.argv) > 1 else "/tmp/box64-0.3.0src/box64-0.3.0"

MAIN_OLD = '''int main(int argc, const char **argv, char **env) {

    x64emu_t* emu = NULL;'''
MAIN_NEW = '''int main(int argc, const char **argv, char **env) {
    {
        extern int box64_fastrep; extern int box64_hotpagedump;
        extern char* getenv(const char*);
        char* p = getenv("BOX64_FASTREP");
        if (p && p[0] && p[0] != '0') box64_fastrep = 1;
        p = getenv("BOX64_HOTPAGE_DUMP");
        if (p && p[0] && p[0] != '0') box64_hotpagedump = 1;
    }

    x64emu_t* emu = NULL;'''

CORE_NEW_BACK = '''    p = getenv("BOX64_DYNAREC_FORWARD");'''
CORE_OLD_DEAD = '''    p = getenv("BOX64_FASTREP");
    if(p && p[0] && p[0]!='0')
        box64_fastrep = 1;
    p = getenv("BOX64_HOTPAGE_DUMP");
    if(p && p[0] && p[0]!='0')
        box64_hotpagedump = 1;
    p = getenv("BOX64_DYNAREC_FORWARD");'''


def main():
    full = os.path.join(ROOT, "src", "main.c")
    src = open(full, encoding="utf-8", errors="replace").read()
    if "BOX64_FASTREP" not in src:
        if src.count(MAIN_OLD) != 1:
            raise SystemExit("main.c anchor count=%d" % src.count(MAIN_OLD))
        src = src.replace(MAIN_OLD, MAIN_NEW)
        open(full, "w", encoding="utf-8", newline="\n").write(src)
        print("main.c patched")
    else:
        print("main.c already patched")

    full2 = os.path.join(ROOT, "src", "core.c")
    src2 = open(full2, encoding="utf-8", errors="replace").read()
    if CORE_OLD_DEAD in src2:
        src2 = src2.replace(CORE_OLD_DEAD, CORE_NEW_BACK)
        open(full2, "w", encoding="utf-8", newline="\n").write(src2)
        print("core.c dead code reverted")
    else:
        print("core.c already clean")


if __name__ == "__main__":
    main()
