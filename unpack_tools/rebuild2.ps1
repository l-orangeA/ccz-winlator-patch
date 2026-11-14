$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 解包 .wine 骨架 ==='
& $adb shell 'run-as com.winlator sh -c "cd files/rootfs/home/xuser-1 && tar -xf /data/local/tmp/pattern.tar && ls -la | head -6; cat .wine/.update-timestamp; ls .wine/drive_c/windows/wfm.exe 2>&1"'
Write-Output '=== 2. wfm.exe = Ekd5 ==='
& $adb push 'D:\ccz\曹操传加强版2.2\Ekd5.exe' /data/local/tmp/Ekd5.exe 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/Ekd5.exe'
& $adb shell 'run-as com.winlator sh -c "cp /data/local/tmp/Ekd5.exe files/rootfs/home/xuser-1/.wine/drive_c/windows/wfm.exe; ls -la files/rootfs/home/xuser-1/.wine/drive_c/windows/wfm.exe"'
Write-Output '=== 3. user.reg 追加 ==='
& $adb shell 'run-as com.winlator sh -c "echo W1NvZnR3YXJlXFxNaWNyb3NvZnRcXFdpbmRvd3NcXEN1cnJlbnRWZXJzaW9uXFxSdW5dCiJFa2Q1VGVzdCI9IkQ6XFxla2Q1X3Rlc3QuYmF0Igo= | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; echo W0NvbnRyb2wgUGFuZWxcXERlc2t0b3BdDQoiRG91YmxlQ2xpY2tTcGVlZCI9IjIwMDAiCg== | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; tail -4 files/rootfs/home/xuser-1/.wine/user.reg"'
Write-Output '=== 4. 确认 .container 仍在 ==='
& $adb shell 'run-as com.winlator sh -c "head -c 100 files/rootfs/home/xuser-1/.container"'
