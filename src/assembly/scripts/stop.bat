@echo off
REM Simple-Pic Stop Script for Windows

set APP_NAME=simple-pic
set PID_FILE=%APP_NAME%.pid

REM Check if PID file exists
if not exist "%PID_FILE%" (
    echo %APP_NAME% is not running ^(no PID file found^)
    exit /b 0
)

set /p PID=<%PID_FILE%

REM Check if process is running
tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="1" (
    echo %APP_NAME% is not running ^(stale PID file^)
    del "%PID_FILE%" 2>nul
    exit /b 0
)

REM Stop the process
echo Stopping %APP_NAME% ^(PID: %PID%^)...
taskkill /PID %PID% /F

REM Wait a moment
timeout /t 2 /nobreak >nul

REM Check if stopped
tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="1" (
    echo %APP_NAME% stopped successfully
    del "%PID_FILE%" 2>nul
) else (
    echo Failed to stop %APP_NAME%
)