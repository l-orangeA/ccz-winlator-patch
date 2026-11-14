@echo off
cd /d D:\Download\ccz22
echo ==== START ==== > D:\Download\ccz22\run.log
Ekd5.exe >> D:\Download\ccz22\run.log 2>&1
echo EXIT %ERRORLEVEL% >> D:\Download\ccz22\run.log
