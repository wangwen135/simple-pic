@echo off
REM Simple-Pic Management Script
REM Usage: simple-pic.bat {start|stop|restart|status}

setlocal enabledelayedexpansion

set APP_NAME=simple-pic
set PID_FILE=%APP_NAME%.pid
set LOG_FILE=logs\%APP_NAME%.log

REM cd to script directory
cd /d "%~dp0"

REM Find JAR file
set JAR_FILE=
for %%f in (%APP_NAME%-*.jar) do (
    set JAR_FILE=%%f
)

REM Parse command
if "%~1"=="" goto usage
if "%~1"=="start"   goto do_start
if "%~1"=="stop"    goto do_stop
if "%~1"=="restart" goto do_restart
if "%~1"=="status"  goto do_status
goto usage

REM === Get port from application.yml ===
:get_port
set PORT=8080
if exist "application.yml" (
    for /f "tokens=2 delims=:" %%a in ('findstr /r "^ *port:" application.yml') do (
        set RAW_PORT=%%a
    )
    REM Strip spaces and ${SERVER_PORT:...} pattern
    if defined RAW_PORT (
        for /f "tokens=*" %%b in ("!RAW_PORT!") do set PORT=%%b
    )
)
set PORT=%PORT: =%
REM If port still contains ${...}, fallback to 8080
echo %PORT% | findstr /r "^[0-9]*$" >nul
if errorlevel 1 set PORT=8080
goto :eof

REM === Start ===
:do_start
where java >nul 2>nul
if errorlevel 1 (
    echo Error: Java is not installed. Please install Java 8 or higher.
    exit /b 1
)

if not defined JAR_FILE (
    echo Error: JAR file not found.
    exit /b 1
)

if exist "%PID_FILE%" (
    set /p PID=<%PID_FILE%
    tasklist /FI "PID eq !PID!" 2>nul | find /I /N "java.exe">nul
    if "!ERRORLEVEL!"=="0" (
        echo %APP_NAME% is already running ^(PID: !PID!^)
        exit /b 0
    )
    del "%PID_FILE%" 2>nul
)

if not exist "logs" mkdir logs

echo Starting %APP_NAME%...
start /B java -jar "%JAR_FILE%" >> "%LOG_FILE%" 2>&1

REM Get the PID of the Java process
set JAVA_PID=
for /f "tokens=2" %%i in ('tasklist ^| findstr "java.exe"') do (
    set JAVA_PID=%%i
)

timeout /t 3 /nobreak >nul

if defined JAVA_PID (
    echo !JAVA_PID! > "%PID_FILE%"
    call :get_port
    echo %APP_NAME% started successfully ^(PID: !JAVA_PID!^)
    echo Log file: %LOG_FILE%
    echo Access: http://localhost:%PORT%
) else (
    echo Failed to start %APP_NAME%. Check log: %LOG_FILE%
    exit /b 1
)
goto :eof

REM === Stop ===
:do_stop
if not exist "%PID_FILE%" (
    echo %APP_NAME% is not running
    exit /b 0
)

set /p PID=<%PID_FILE%

tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="1" (
    echo %APP_NAME% is not running ^(stale PID file^)
    del "%PID_FILE%" 2>nul
    exit /b 0
)

echo Stopping %APP_NAME% ^(PID: %PID%^)...
taskkill /PID %PID% /F

timeout /t 2 /nobreak >nul

tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="1" (
    echo %APP_NAME% stopped successfully
    del "%PID_FILE%" 2>nul
) else (
    echo Failed to stop %APP_NAME%
)
goto :eof

REM === Status ===
:do_status
if not exist "%PID_FILE%" (
    echo %APP_NAME% is not running
    exit /b 0
)

set /p PID=<%PID_FILE%

tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="0" (
    call :get_port
    echo %APP_NAME% is running ^(PID: %PID%^)
    echo.
    tasklist /FI "PID eq %PID%" /V
    echo.
    echo Access: http://localhost:%PORT%
) else (
    echo %APP_NAME% is not running ^(stale PID file^)
    del "%PID_FILE%" 2>nul
)
goto :eof

REM === Restart ===
:do_restart
call :do_stop
timeout /t 2 /nobreak >nul
call :do_start
goto :eof

REM === Usage ===
:usage
echo Usage: simple-pic.bat {start^|stop^|restart^|status}
exit /b 1
