@echo off
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rk-capture.ps1" -Only experiments
echo Finished. You can close this window.
pause
