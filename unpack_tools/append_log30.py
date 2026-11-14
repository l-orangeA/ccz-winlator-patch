#!/usr/bin/env python3
"""追加 §30 到日志末尾."""
p = r"D:\ccz_winlator_patch\2026-11-06_渲染与卡顿攻坚日志.md"
data = open(p, encoding="utf-8").read()
if "30. ② 深挖现状检查点" in data:
    print("already appended")
    raise SystemExit(0)
add = """

## 30. ② 深挖现状检查点（2026-11-08 凌晨）

本轮完成的增量（按序）：
1. exec/fork 包装器静态对比：my_execve/my_fork/atfork 机制两版一致（结构性差异排除）；
   0.4.4 core.c 的 my_child_fork（ftrace 日志功能）消失=良性。
2. BIGBLOCK=0 降速实验：+27s 死（速度假说单因素不成立）。
3. RDTSC_1GHZ 实验：+22s 死（排除）。
4. 日志通道二分：+virtual 单独=死；DYNAREC_LOG 单独=死；双通道=存活 1 次（未复验）。
5. 设备侧出生监视器（watch_birth.sh，50ms 本地轮询）建成——但 0.4.4 下进程连出生都不稳定
   （有的 boot 从不出现）。
6. **设备 USB 在 0.4.4 反复压测后多次掉线**——本轮以设备掉线告终。

### ⚠️ 手机待恢复状态（重连后第一件事）
- box64 = 作者 0.4.4（a7360b1e）← 需回滚到 0.3.0（deploy_box64_044.py rollback）
- ntdll = fix110b（eddb74af）✔ 保留
- env = 生产 + 实验残留 ← 需 set_env_phone.py prod
- 恢复后验证：60s 内出标题 = 可交付

### ② 的剩余路线图
1. 死亡 boot 的 maps 抓取（设备侧监视器已建成，等连接稳定）
2. 镜像视图创建失败的 box64 0.4.4 mmap 定位层排查
3. 「启动期日志+解包后关闭」方案的可行性（若时序竞态确认）
4. 每层 2-4h，深度未知——保持耐心逐层推进"""
open(p, "w", encoding="utf-8", newline="\n").write(data + add)
print("appended")
