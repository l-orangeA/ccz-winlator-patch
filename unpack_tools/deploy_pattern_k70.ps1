$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 停 app，备份我的 .container ==='
& $adb shell 'am force-stop com.winlator; sleep 1'
& $adb shell 'run-as com.winlator sh -c "cp files/rootfs/home/xuser-1/.container files/.container_stash && echo stash_ok"'
Write-Output '=== 2. 推 tar 并解包 .wine 骨架 ==='
& $adb push 'D:\ccz_winlator_patch\unpack_tools\pattern_push.tar' /data/local/tmp/pattern.tar 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/pattern.tar'
& $adb shell 'run-as com.winlator sh -c "rm -rf files/rootfs/home/xuser-1/.wine; cd files/rootfs/home/xuser-1 && tar -xf /data/local/tmp/pattern.tar && ls -la .wine/ | head -8; cat .wine/.update-timestamp"'
Write-Output '=== 3. 恢复终态 .container ==='
& $adb shell 'run-as com.winlator sh -c "cp files/.container_stash files/rootfs/home/xuser-1/.container && head -c 100 files/rootfs/home/xuser-1/.container"'
Write-Output ''
Write-Output '=== 4. wfm.exe = Ekd5 替换 ==='
& $adb push 'D:\ccz\曹操传加强版2.2\Ekd5.exe' /data/local/tmp/Ekd5.exe 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/Ekd5.exe'
& $adb shell 'run-as com.winlator sh -c "cp /data/local/tmp/Ekd5.exe files/rootfs/home/xuser-1/.wine/drive_c/windows/wfm.exe; ls -la files/rootfs/home/xuser-1/.wine/drive_c/windows/wfm.exe"'
Write-Output '=== 5. user.reg：Run 键 + DoubleClickSpeed ==='
& $adb shell 'run-as com.winlator sh -c "echo W1NvZnR3YXJlXFxNaWNyb3NvZnRcXFdpbmRvd3NcXEN1cnJlbnRWZXJzaW9uXFxSdW5dCiJFa2Q1VGVzdCI9IkQ6XFxla2Q1X3Rlc3QuYmF0Igo= | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; echo W0NvbnRyb2wgUGFuZWxcXERlc2t0b3BdDQoiRG91YmxlQ2xpY2tTcGVlZCI9IjIwMDAiCg== | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; tail -4 files/rootfs/home/xuser-1/.wine/user.reg"'
Write-Output '=== 6. 清理 ==='
& $adb shell 'run-as com.winlator sh -c "rm -f files/.container_stash /data/local/tmp/pattern.tar /data/local/tmp/Ekd5.exe"'
Write-Output '=== 7. 重启 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' 2>&1 | Out-Null
Write-Output 'setup complete - tap Run next'
