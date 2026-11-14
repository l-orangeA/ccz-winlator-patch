@echo off
cd /d D:\
echo === START %TIME% CWD=%CD% === >> D:\ekd5_run.log
D:\Ekd5.exe >> D:\ekd5_run.log 2>&1
echo === END %TIME% EXITCODE=%ERRORLEVEL% === >> D:\ekd5_run.log
