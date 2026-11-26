@echo off
setlocal
cd /d "%~dp0" || exit /b 1

if not defined ISCC for %%P in ("%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" "%ProgramFiles%\Inno Setup 6\ISCC.exe" "%LocalAppData%\Programs\Inno Setup 6\ISCC.exe") do (
    if exist "%%~P" set "ISCC=%%~P"
)
if not defined ISCC for /f "delims=" %%P in ('where ISCC.exe 2^>nul') do if not defined ISCC set "ISCC=%%P"
if not defined ISCC (
    echo Instala Inno Setup 6 o indica la ruta de ISCC.exe con la variable ISCC.
    exit /b 1
)

set "TALLERPRO_CONSOLA="
call build-app-image.bat %* || exit /b 1
"%ISCC%" "TallerPro.iss" || exit /b 1
echo Instalador listo: target\instalador\Setup.exe
