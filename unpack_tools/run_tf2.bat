@echo off
cd /d D:\Download\ccz22
tf_test2.exe > tf2_console.log 2>&1
echo EXIT %ERRORLEVEL% >> tf2_console.log
copy C:\tf2_result.txt . >nul 2>&1
