@echo off
chcp 65001 >nul
cd /d "%~dp0"
javac -encoding UTF-8 *.java
if errorlevel 1 (
    echo Error al compilar. Revisa que tengas el JDK instalado.
    pause
    exit /b 1
)
java Main
