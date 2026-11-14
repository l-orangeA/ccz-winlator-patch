"""box64 0.4.4 补丁（r113）：重复 ACCERR 毛刺分支恢复 0.3.0 的"落空穿透转发"行为。
0.4.4 在 repeated-glitch 分支加了 relockMutex/unlock_signal/return —— 信号永不转发给 guest，
导致 ntdll hwbp 监视页的 SEGV 被吞（faults 爆炸 / hits≈0 / 壳死循环）。
在已 no_tf-patched 的树 /root/box64-0.4.4ntf 上运行: python3 patch_box64_044_glitch.py
"""
P = "/root/box64-0.4.4ntf/src/libtools/signals.c"

OLD = '''            glitch_pc = NULL;
            glitch_addr = NULL;
            glitch_prot = 0;
            relockMutex(Locks);
            unlock_signal();
            return; // try again
    }'''

NEW = '''            glitch_pc = NULL;
            glitch_addr = NULL;
            glitch_prot = 0;
            /* r113: fall through to guest forwarding like 0.3.0 — the hwbp sim
               watch pages (manual mprotect) hit this path; swallowing the SEGV
               here starves the VProtect shell of its heartbeat */
    }'''

src = open(P, encoding="utf-8", errors="replace").read()
if NEW in src:
    print("already patched")
elif src.count(OLD) == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(OLD, NEW))
    print("patched")
else:
    raise SystemExit("anchor count=%d" % src.count(OLD))
