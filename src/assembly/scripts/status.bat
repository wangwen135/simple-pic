@echo off
REM Simple-Pic Status Script for Windows

set APP_NAME=simple-pic
set PID_FILE=%APP_NAME%.pid

REM Check if PID file exists
if not exist "%PID_FILE%" (
    echo %APP_NAME% is not running
    exit /b 0
)

set /p PID=<%PID_FILE%

REM Check if process is running
tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="0" (
    echo %APP_NAME% is running ^(PID: %PID%^)
    echo.
    echo Memory usage:
    tasklist /FI "PID eq %PID%" /V
    echo.
    echo Access the application at: http://localhost:8080
) else (
    echo %APP_NAME% is not running ^(stale PID file^)
    del "%PID_FILE%" 2>nul
)