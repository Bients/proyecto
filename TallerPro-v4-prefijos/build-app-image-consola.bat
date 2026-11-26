@echo off
setlocal
set "TALLERPRO_CONSOLA=1"
call "%~dp0build-app-image.bat" %*
exit /b %errorlevel%
