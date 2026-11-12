# 部署补丁.ps1 —— 曹操传 v2.20 / Winlator 8.0 补丁一键部署（2026-11-12 版）
# 用法：powershell -ExecutionPolicy Bypass -File .\部署补丁.ps1
#      可加 -Serial <序列号> -AdbPath <adb.exe 路径>
param(
    [string]$Serial = "",
    [string]$AdbPath = ""
)
$ErrorActionPreference = "Continue"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$pkg  = "com.winlatcn"

function Fail($msg) { Write-Host "[失败] $msg" -ForegroundColor Red; exit 1 }
function Ok($msg)   { Write-Host "[OK]   $msg" -ForegroundColor Green }

# ---- 找 adb ----
$adb = $AdbPath
if (-not $adb) { $c = Get-Command adb -ErrorAction SilentlyContinue; if ($c) { $adb = $c.Source } }
if (-not $adb) {
    foreach ($p in @("D:\MuMuPlayer\nx_main\adb.exe", "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe", "C:\platform-tools\adb.exe")) {
        if (Test-Path $p) { $adb = $p; break }
    }
}
if (-not $adb) { Fail "找不到 adb.exe：请把 adb 加入 PATH，或用 -AdbPath 指定" }

function SH([string]$cmd)  { (& $adb -s $dev shell $cmd 2>&1 | Out-String) }
function SHc([string]$cmd) { (& $adb -s $dev shell $cmd 2>&1 | Out-String).Trim() }

# ---- 选设备 ----
$devs = @(& $adb devices | Select-String "device$" | ForEach-Object { ($_ -split "\s+")[0] })
if ($devs.Count -eq 0) { Fail "没有在线的设备（开 USB 调试并在手机上允许本电脑）" }
$dev = if ($Serial) { $Serial } else { $devs[0] }
if ($devs -notcontains $dev) { Fail "设备 $dev 不在线" }
Ok "设备 $dev"

# ---- 环境检查 ----
if (-not (SHc "pm path $pkg")) { Fail "手机没装 $pkg（先装 2-模拟器\winlator8.0_ccz_fr120.apk）" }
$ver = SHc "run-as $pkg cat files/imagefs/.winlator/.img_version"
if ($ver -ne "9") { Fail "系统文件还没装好：先打开一次 App 等进度条走完（当前=$ver）" }
$cfg = SHc "run-as $pkg sh -c 'test -f files/imagefs/home/xuser-1/.container && echo yes || echo no'"
if ($cfg -ne "yes") { Fail "还没有容器：先打开 App -> 容器管理 -> 右上添加 -> 确认，再重跑本脚本" }

# ---- 停 App ----
SH "am force-stop $pkg" | Out-Null
Start-Sleep -Milliseconds 800
Ok "已 force-stop"

# ---- 三件套 ----
$map = @(
    @{ src = "ntdll_fix110b_modfix.so"; dst = "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so";   md5 = "4e7d876de5f0d0defebd8a34b664e403" },
    @{ src = "win32u_r109g.so";        dst = "files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so"; md5 = "2be1316c83d603c4defae8669fcc1dfb" },
    @{ src = "wineserver_r112b.so";    dst = "files/imagefs/opt/wine/bin/wineserver";                  md5 = "9df1d0c325f2b9c2f7e71c82fb5fab70" }
)
foreach ($m in $map) {
    $local = Join-Path $here $m.src
    if (-not (Test-Path $local)) { Fail "缺少文件 $($m.src)" }
    & $adb -s $dev push $local "/data/local/tmp/$($m.src)" 2>&1 | Out-Null
    SH "run-as $pkg cp /data/local/tmp/$($m.src) $($m.dst)" | Out-Null
    SH "rm -f /data/local/tmp/$($m.src)" | Out-Null
    $got = SHc "run-as $pkg md5sum $($m.dst)"
    if ($got -notmatch $m.md5) { Fail "$($m.src) 校验失败（设备上=$got）" }
    Ok "$($m.src)  ->  $($m.dst)"
}

# ---- 容器配置（先备份原文件）----
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
SH "run-as $pkg cp files/imagefs/home/xuser-1/.container files/imagefs/home/xuser-1/.container.bak_$stamp" | Out-Null
& $adb -s $dev push (Join-Path $here "container.json") /data/local/tmp/container.json 2>&1 | Out-Null
SH "run-as $pkg cp /data/local/tmp/container.json files/imagefs/home/xuser-1/.container" | Out-Null
SH "rm -f /data/local/tmp/container.json" | Out-Null
$c2 = SHc "run-as $pkg cat files/imagefs/home/xuser-1/.container"
if (($c2 -notmatch "WINE_SS=0xe00") -or ($c2 -notmatch "WINE_HWBP_SOFT=1")) { Fail "容器配置写入异常" }
Ok "容器配置已写入（原文件备份：.container.bak_$stamp）"

# ---- 清 tmp ----
SH "run-as $pkg sh -c 'rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*'" | Out-Null
Ok "已清 tmp 残留"

Write-Host ""
Write-Host "部署完成。启动：" -ForegroundColor Cyan
Write-Host "  容器管理 -> ccz -> 管理 -> 磁盘(D:) -> ccz -> Ekd5.exe 的 ▶"
Write-Host ('  或：adb -s ' + $dev + ' shell am start -n ' + $pkg + '/com.winlator.XServerDisplayActivity --ei container_id 1 --es exec_path "/storage/emulated/0/ccz/Ekd5.exe"')
Write-Host "  验收基线：22.2 s 出标题；hwbp_stats arms=1536 faults=44090 hits=384"