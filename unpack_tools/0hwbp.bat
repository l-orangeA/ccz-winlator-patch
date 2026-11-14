@echo off
cd /d D:\Download\ccz22
hwbp_test.exe > hwbp_box64.log 2>&1
echo EXIT %ERRORLEVEL% >> hwbp_box64.log
