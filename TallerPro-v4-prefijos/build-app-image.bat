@echo off
setlocal
cd /d "%~dp0" || exit /b 1

set "VERSION="
set "JAR="
for /f "tokens=1,2" %%A in ('powershell.exe -NoProfile -Command "$p = ([xml](Get-Content -Raw -LiteralPath 'pom.xml')).project; if ($p.version -notmatch '^[0-9]+\.[0-9]+\.[0-9]+$') { exit 1 }; Write-Output ($p.version + ' ' + $p.artifactId + '-' + $p.version + '.jar')"') do (
    set "VERSION=%%A"
    set "JAR=%%B"
)
if not defined VERSION (
    echo No se pudo leer la version de pom.xml.
    exit /b 1
)

call mvn clean verify %* || exit /b 1
mkdir "target\jpackage-input" || exit /b 1
copy /y "target\%JAR%" "target\jpackage-input\" >nul || exit /b 1

set "JPACKAGE=jpackage"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
set "NOMBRE=TallerPro"
set "CONSOLA="
if "%TALLERPRO_CONSOLA%"=="1" (
    set "NOMBRE=TallerProConsola"
    set "CONSOLA=--win-console"
)
"%JPACKAGE%" --type app-image --name "%NOMBRE%" --app-version "%VERSION%" ^
    --icon "icono\TallerPro.ico" --input "target\jpackage-input" --main-jar "%JAR%" ^
    --dest "target\app" %CONSOLA% || exit /b 1

echo Imagen lista: target\app\%NOMBRE%\%NOMBRE%.exe
