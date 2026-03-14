@echo off
REM Simple-Pic Restart Script for Windows

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

REM Stop the application
call stop.bat

REM Wait a moment
timeout /t 2 /nobreak >nul

REM Start the application
call start.bat