@echo off
cd /d "%~dp0"
echo Spring Lab RK: building and capturing evidence, this takes 3-5 minutes...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\rk-capture.ps1"
echo.
echo Finished. You can close this window.
pause
