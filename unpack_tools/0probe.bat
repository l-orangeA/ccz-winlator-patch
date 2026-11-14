@echo off
cd /d D:\Download\ccz22
tf_test8.exe > tf8.log 2>&1
echo EXIT %ERRORLEVEL% >> tf8.log
