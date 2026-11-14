V = "/root/wine0905/dlls/ntdll/unix/virtual.c"
s = open(V, encoding="utf-8").read()

a1 = "unsigned long hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit;"
assert a1 in s
s = s.replace(a1, a1 + "\nstatic double hwbp_t_first;", 1)

a2 = 'hits=%lu\\n"'
assert a2 in s, "fmt"
s = s.replace(a2, 'hits=%lu t=%.1f\\n"', 1)

a3 = "(int)getpid(), hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit );"
assert a3 in s, "args"
s = s.replace(a3,
              "(int)getpid(), hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit,\n"
              "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );", 1)

a4 = "            hwbp_cnt_arm++;\n            if (!(hwbp_cnt_arm & 63)) hwbp_write_stats();"
assert a4 in s, "arm"
s = s.replace(a4,
              "            if (!hwbp_cnt_arm) hwbp_t_first = hwbp_now();\n" + a4, 1)

# 时间函数放在计数器定义之后
a5 = "static double hwbp_t_first;"
s = s.replace(a5,
              a5 + "\nstatic double hwbp_now( void )\n{\n    struct timespec ts;\n"
                   "    clock_gettime( CLOCK_MONOTONIC, &ts );\n"
                   "    return ts.tv_sec + ts.tv_nsec / 1e9;\n}\n"
                   "static double hwbp_elapsed( void ) { return hwbp_now() - hwbp_t_first; }", 1)

open(V, "w", encoding="utf-8").write(s)
print("patched ok")
