@echo off
cd /d D:\Download\ccz22
cpuid_test.exe > cpuid.log 2>&1
echo EXIT %ERRORLEVEL% >> cpuid.log
tsc_test.exe > tsc.log 2>&1
