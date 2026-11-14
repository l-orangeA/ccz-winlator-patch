"""补 virtual_init 的闭合括号."""
P = "/root/wine0905/dlls/ntdll/unix/virtual.c"
OLD = """        }
    }


/***********************************************************************
 *           get_system_affinity_mask"""
NEW = """        }
    }
}


/***********************************************************************
 *           get_system_affinity_mask"""
src = open(P, encoding="utf-8", errors="replace").read()
n = src.count(OLD)
print("count:", n)
if n == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(OLD, NEW))
    print("fixed")
elif NEW in src:
    print("already ok")
else:
    raise SystemExit("unexpected")
