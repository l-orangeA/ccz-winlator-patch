# -*- coding: utf-8 -*-
"""生成 2026-11-14 精简整合包：安装包 + 部署脚本(+素材) + 使用说明。
文本一律 GBK + CRLF，供 Windows 双击/系统 PowerShell 5.1 直接使用。
"""
import hashlib
import os
import shutil
import zipfile

ROOT = r"D:\ccz_winlator_patch"
SRC = os.path.join(ROOT, "build", "_pkg_new_20261114")
OUT = os.path.join(ROOT, "deliverables", "曹操传Winlator整合包_20261114_精简版.zip")
TOP = "曹操传Winlator整合包_20261114"

PS1 = r'''# 一键部署 —— 曹操传加强版 v2.20 / Winlator 8.0（2026-11-14 版）
# 双击同目录的「一键部署.bat」即可，无需手工敲命令。
param(
    [string]$Serial = "",
    [string]$AdbPath = ""
)
$ErrorActionPreference = "Continue"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$pkg  = "com.winlatcn"
$apk  = "winlator8.0_ccz_fr120.apk"

function Fail($m) { Write-Host ""; Write-Host "[失败] $m" -ForegroundColor Red; Write-Host ""; exit 1 }
function Ok($m)   { Write-Host "[OK]   $m" -ForegroundColor Green }
function Info($m) { Write-Host ".      $m" -ForegroundColor Gray }
function Step($m) { Write-Host ""; Write-Host "== $m ==" -ForegroundColor Cyan }

# ---------- 找 adb ----------
$adb = $AdbPath
if (-not $adb) { $c = Get-Command adb -ErrorAction SilentlyContinue; if ($c) { $adb = $c.Source } }
if (-not $adb) {
    foreach ($p in @(
        (Join-Path $here "adb.exe"),
        "D:\MuMuPlayer\nx_main\adb.exe",
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"),
        "C:\platform-tools\adb.exe",
        "D:\platform-tools\adb.exe"
    )) { if (Test-Path $p) { $adb = $p; break } }
}
if (-not $adb) { Fail "找不到 adb.exe：请装 platform-tools，或运行时加 -AdbPath 路径" }

function SH([string]$cmd)  { (& $adb -s $dev shell $cmd 2>&1 | Out-String) }
function SHc([string]$cmd) { (& $adb -s $dev shell $cmd 2>&1 | Out-String).Trim() }

# ---------- 选设备 ----------
$devs = @(& $adb devices | Select-String "device$" | ForEach-Object { ($_ -split "\s+")[0] })
if ($devs.Count -eq 0) { Fail "没有在线设备：打开 USB 调试，并在设备上允许本电脑" }
$dev = if ($Serial) { $Serial } else { $devs[0] }
if ($devs -notcontains $dev) { Fail "设备 $dev 不在线" }
Ok "设备 $dev"

# ---------- 1. 安装 APK / 等系统文件解包 ----------
Step "1/6 检查 APK 与系统文件"
if (-not (SHc "pm path $pkg")) {
    $localApk = Join-Path $here $apk
    if (-not (Test-Path $localApk)) { Fail "缺少安装包 $apk" }
    Info "未检测到 Winlator，正在安装（约 30 秒）"
    & $adb -s $dev install -r $localApk 2>&1 | Out-String | Write-Host
    SH "am start -n $pkg/com.winlator.MainActivity" | Out-Null
}
$ver = ""
for ($i = 0; $i -lt 60; $i++) {
    $ver = SHc "run-as $pkg cat files/imagefs/.winlator/.img_version"
    if ($ver -eq "9") { break }
    SH "am start -n $pkg/com.winlator.MainActivity" | Out-Null
    Start-Sleep -Seconds 2
}
if ($ver -ne "9") { Fail "系统文件未就绪（img_version=$ver）：先打开一次 App 等进度条走完" }
Ok "imagefs 就绪 (img_version=9)"

# ---------- 2. 等容器（不代建，避免跳过补丁） ----------
Step "2/6 检查容器"
$cfgPath = "files/imagefs/home/xuser-1/.container"
$has = SHc "run-as $pkg sh -c 'test -f $cfgPath && echo yes'"
if ($has -ne "yes") {
    Write-Host "请在平板上：容器管理 -> 右上「添加」-> 直接点「确认」" -ForegroundColor Yellow
    Info "脚本会在这里等你，最多 3 分钟"
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 2
        $has = SHc "run-as $pkg sh -c 'test -f $cfgPath && echo yes'"
        if ($has -eq "yes") { break }
    }
}
if ($has -ne "yes") { Fail "仍没有容器：必须先由 App 自己建容器，否则补丁不会解压" }
Ok "容器已存在"

# ---------- 3. 备份 + 推三件套 ----------
Step "3/6 部署 Wine 补丁（三件套）"
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
SH "run-as $pkg cp $cfgPath $cfgPath.bak_$stamp" | Out-Null

$map = @(
    @{ src = "ntdll.so";   dst = "files/imagefs/opt/wine/lib/wine/x86_64-unix/ntdll.so";   md5 = "4e7d876de5f0d0defebd8a34b664e403" },
    @{ src = "win32u.so";  dst = "files/imagefs/opt/wine/lib/wine/x86_64-unix/win32u.so";  md5 = "2be1316c83d603c4defae8669fcc1dfb" },
    @{ src = "wineserver"; dst = "files/imagefs/opt/wine/bin/wineserver";                  md5 = "9df1d0c325f2b9c2f7e71c82fb5fab70" }
)
foreach ($m in $map) {
    $local = Join-Path $here $m.src
    if (-not (Test-Path $local)) { Fail "缺少补丁文件 $($m.src)" }
    & $adb -s $dev push $local "/data/local/tmp/$($m.src)" 2>&1 | Out-Null
    SH "run-as $pkg cp /data/local/tmp/$($m.src) $($m.dst)" | Out-Null
    SH "rm -f /data/local/tmp/$($m.src)" | Out-Null
    $got = SHc "run-as $pkg md5sum $($m.dst)"
    if ($got -notmatch $m.md5) { Fail "$($m.src) 校验失败（设备上=$got）" }
    Ok "$($m.src) 已部署并校验"
}

# ---------- 4. 按芯片生成容器配置（关键：删 extraData） ----------
Step "4/6 写容器配置（按设备区分）"
$plat = SHc "getprop ro.board.platform"
$soc  = SHc "getprop ro.soc.model"
$hw   = SHc "getprop ro.hardware"
$chip = "$plat $soc $hw".ToLower()
if ($chip -match "kalama|qcom|sm8|sm7|sdm|msm|adreno") {
    $driver = "turnip"; $gcfg = ""; $gpu = "Adreno (骁龙)"
} elseif ($chip -match "mt6|mtk|mediatek|dimensity|mali") {
    $driver = "virgl"; $gcfg = "glVersion=4.0,disableVertexArrayBGRA=1"; $gpu = "Mali (天玑)"
} else {
    $driver = "turnip"; $gcfg = ""; $gpu = "未知，按 Turnip 处理"
}
Ok "识别 GPU: $gpu -> graphicsDriver=$driver"

$raw = SHc "run-as $pkg cat $cfgPath"
$cfg = $raw | ConvertFrom-Json
# extraData 里的 appVersion/imgVersion 会让 App 误判「补丁已打过」，
# 从而跳过 applyGeneralPatches，导致 winhandler.exe 等 97 个文件缺失、游戏永远起不来。
if ($cfg.PSObject.Properties.Name -contains "extraData") {
    $cfg.PSObject.Properties.Remove("extraData")
}
$cfg.name = "ccz"
$cfg.screenSize = "1280x720"
$cfg.envVars = "LC_ALL=zh_CN.utf8 WINEDEBUG=-all WINE_SS=0xe00 WINE_HWBP_SOFT=1 BOX64_DYNAREC_BIGBLOCK=3 BOX64_DYNAREC_FORWARD=1024 BOX64_DYNAREC_CALLRET=1 ZINK_DESCRIPTORS=lazy MESA_VK_WSI_PRESENT_MODE=mailbox"
$cfg.cpuList = "0,1,2,3"
$cfg.cpuListWoW64 = "4,5,6,7"
$cfg.graphicsDriver = $driver
$cfg.graphicsDriverConfig = $gcfg
$cfg.dxwrapper = "wined3d"
$cfg.audioDriver = "pulseaudio"
$cfg.drives = "D:/storage/emulated/0"
$cfg.box64Preset = "COMPATIBILITY"
$cfg.showFPS = $false
$json = $cfg | ConvertTo-Json -Compress -Depth 10

function WriteCfg([string]$text) {
    $tmp = Join-Path $env:TEMP "ccz_container_$PID.json"
    [System.IO.File]::WriteAllText($tmp, $text, (New-Object System.Text.UTF8Encoding($false)))
    & $adb -s $dev push $tmp /data/local/tmp/ccz_container.json 2>&1 | Out-Null
    SH "run-as $pkg cp /data/local/tmp/ccz_container.json $cfgPath" | Out-Null
    SH "rm -f /data/local/tmp/ccz_container.json" | Out-Null
    Remove-Item $tmp -Force
}
WriteCfg $json
Ok "配置已写入（已清除 extraData，box64Preset=COMPATIBILITY）"

# ---------- 5. 冷启一次容器，让 App 把 97 个补丁文件解压出来 ----------
Step "5/6 触发首启补丁"
SH "am force-stop $pkg" | Out-Null
for ($n = 1; $n -le 2; $n++) {
    SH "pkill -9 -x libtinyproot.so" | Out-Null
    Start-Sleep -Seconds 1
    SH "pkill -9 -x wineserver" | Out-Null
    SH "pkill -9 -x winedevice.exe" | Out-Null
}
SH "run-as $pkg sh -c 'rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*'" | Out-Null
SH "am start -n $pkg/com.winlator.XServerDisplayActivity --ei container_id 1" | Out-Null

$ready = $false
for ($i = 0; $i -lt 45; $i++) {
    $w = SHc "run-as $pkg sh -c 'test -f files/imagefs/home/xuser-1/.wine/drive_c/windows/winhandler.exe && echo yes'"
    if ($w -eq "yes") { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ready) { Fail "首启补丁未落地（winhandler.exe 缺失）：重跑本脚本一次" }
Ok "winhandler.exe / wfm.exe 等补丁已解压"

# ---------- 6. 改回 box64Preset（App 首启会偷偷改成 PERFORMANCE） ----------
Step "6/6 锁定 box64Preset=COMPATIBILITY"
# PERFORMANCE -> BOX64_DYNAREC_STRONGMEM=0，实测会让 VProtect 壳在 arms=256~448 卡死。
$raw2 = SHc "run-as $pkg cat $cfgPath"
$cfg2 = $raw2 | ConvertFrom-Json
$cfg2.box64Preset = "COMPATIBILITY"
WriteCfg ($cfg2 | ConvertTo-Json -Compress -Depth 10)
Ok "box64Preset=COMPATIBILITY（STRONGMEM=1）"

SH "am force-stop $pkg" | Out-Null
SH "run-as $pkg sh -c 'rm -rf files/imagefs/tmp/.wine-* files/imagefs/tmp/.X11-unix/* files/imagefs/tmp/shm/*'" | Out-Null

Write-Host ""
Write-Host "=========== 部署完成 ===========" -ForegroundColor Green
Write-Host "启动游戏：容器管理 -> ccz -> 管理 -> 磁盘(D:) -> ccz -> Ekd5.exe 的 ▶"
Write-Host "验收基线：约 20~60 秒出标题；hwbp_stats arms=1536 faults=44090 hits=384"
Write-Host "存档目录：/sdcard/ccz/SV（本包不含游戏本体）"
Write-Host ""
Write-Host "三个必须知道：" -ForegroundColor Yellow
Write-Host "  1. 重装 APK / 重装系统文件后，必须重跑本脚本"
Write-Host "  2. App 每次首启都可能把 box64Preset 改成 PERFORMANCE，卡住就重跑本脚本"
Write-Host "  3. 测试时别熄屏，熄屏会造成假死（adb shell svc power stayon true）"
Write-Host ""
'''

BAT = r'''@echo off
chcp 936 >nul
title 曹操传 Winlator 一键部署
cd /d "%~dp0"
echo.
echo   曹操传加强版 v2.20 / Winlator 8.0 一键部署
echo   请先用 USB 连接设备并打开 USB 调试
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0一键部署.ps1"
echo.
pause
'''

README = r'''曹操传加强版 v2.20 —— Winlator 8.0 整合包（2026-11-14 精简版）
================================================================

【这个包里只有三类东西】
  winlator8.0_ccz_fr120.apk    安装包（唯一一个 APK）
  一键部署.bat                 双击即可运行（入口）
  一键部署.ps1                 部署脚本（由 bat 调用）
  ntdll.so / win32u.so / wineserver
                               部署脚本要用到的 Wine 补丁文件，别删
  使用说明.txt                 本文件

不含游戏本体、不含原版 APK、不含签名证书、不含技术文档。


【三步装好】
1. 用 USB 连上手机/平板，打开「USB 调试」并允许本电脑。
2. 双击「一键部署.bat」。
   - 没装 APK 会自动安装；
   - 没建容器会提示你：容器管理 -> 右上「添加」-> 直接「确认」，脚本会等你；
   - 之后自动推补丁、按芯片选驱动、校验 md5、清残留，约 2~3 分钟。
3. 打开 App：容器管理 -> ccz -> 管理 -> 磁盘(D:) -> ccz -> Ekd5.exe 的 ▶

首次启动 20~60 秒出标题画面（视机型）。看到「三国志曹操传 加强版 v2.20」
和四个菜单项就是成功。


【游戏本体与存档】
- 本包不含游戏。游戏目录固定为 /sdcard/ccz（容器内 D:\ccz）。
- 存档在 /sdcard/ccz/SV。SV.E5S 是空索引，SV001.E5S 起才是存档。
- 要全新开局：删掉 SV 目录里的 SV*.E5S，只留 SV.E5S。
- 升级/回滚前先整目录备份 /sdcard/ccz。


【设备区分（脚本自动做，不用管）】
  骁龙 / Adreno（如 8 Gen 2）  -> graphicsDriver = turnip
  天玑 / Mali（如 9200）        -> graphicsDriver = virgl
两套都用同一份补丁；差异只在图形驱动，脚本按 ro.board.platform 自动判断。


【卡住怎么办】
1. 先重跑一次「一键部署.bat」。
   App 每次首启可能把 box64Preset 改成 PERFORMANCE（= STRONGMEM=0），
   这会让加壳程序在解包早期卡死（黑屏、无标题）。脚本会改回 COMPATIBILITY。
2. 保持屏幕常亮。熄屏会造成「假死」：容器看着不动，其实是被系统挂起。
   可在电脑执行：adb shell svc power stayon true
3. 卡过后先清后台：重跑脚本会强制停止 App 并清掉 proot 残留。
4. 重装 APK / 在设置里「重装系统文件」后，必须重跑本脚本，
   因为 APK 内置的是旧版 ntdll，补丁会被覆盖。


【验收与校验值】
  干净启动：hwbp_stats arms=1536 faults=44090 steps=42990 hits=384
  APK    e1bd0b4b9a1bbefb28cf529653497cdb
  ntdll  4e7d876de5f0d0defebd8a34b664e403
  win32u 2be1316c83d603c4defae8669fcc1dfb
  wineserver 9df1d0c325f2b9c2f7e71c82fb5fab70

【适用】仅 Winlator 8.0 + Wine 9.2 + box64 0.3.0；官方 11.2 不适用。
【回滚】需要原版 APK 或技术资料：https://github.com/l-orangeA/ccz-winlator-patch
'''


def write_text(path, text):
    with open(path, "w", encoding="gbk", newline="\r\n") as f:
        f.write(text)
    print("  text", os.path.basename(path), os.path.getsize(path), "B (GBK/CRLF)")


def md5(path):
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    os.makedirs(SRC, exist_ok=True)
    for fn in os.listdir(SRC):
        p = os.path.join(SRC, fn)
        if os.path.isfile(p):
            os.remove(p)

    print("== 生成文本 ==")
    write_text(os.path.join(SRC, "一键部署.ps1"), PS1)
    write_text(os.path.join(SRC, "一键部署.bat"), BAT)
    write_text(os.path.join(SRC, "使用说明.txt"), README)

    print("== 复制二进制 ==")
    copies = [
        (os.path.join(ROOT, "apk", "winlator8.0_ccz_fr120.apk"), "winlator8.0_ccz_fr120.apk"),
        (os.path.join(ROOT, "build", "ntdll_fix110b_modfix.so"), "ntdll.so"),
        (os.path.join(ROOT, "build", "win32u_r109g.so"), "win32u.so"),
        (os.path.join(ROOT, "build", "wineserver_r112b.so"), "wineserver"),
    ]
    for src, dst in copies:
        if not os.path.isfile(src):
            raise SystemExit("缺少源文件: " + src)
        shutil.copy2(src, os.path.join(SRC, dst))
        print("  copy", dst, os.path.getsize(os.path.join(SRC, dst)), "B")

    print("== 打包 ==")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_STORED, allowZip64=True) as z:
        for fn in ["winlator8.0_ccz_fr120.apk", "一键部署.bat", "一键部署.ps1",
                   "ntdll.so", "win32u.so", "wineserver", "使用说明.txt"]:
            z.write(os.path.join(SRC, fn), TOP + "/" + fn)
            print("  zip ", fn)
    print("== 结果 ==")
    print(OUT)
    print("大小 %.1f MB" % (os.path.getsize(OUT) / 1048576.0))
    print("md5  " + md5(OUT))


if __name__ == "__main__":
    main()
