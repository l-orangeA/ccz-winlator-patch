@echo off
cd /d D:\Download\ccz22
echo ==== START ==== > game.log
Ekd5.exe >> game.log 2>&1
echo EXIT %ERRORLEVEL% >> game.log
