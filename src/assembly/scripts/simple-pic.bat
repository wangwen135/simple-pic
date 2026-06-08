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
    if defined RAW_PORT (
        for /f "tokens=*" %%b in ("!RAW_PORT!") do set PORT=%%b
    )
)
set PORT=%PORT: =%
echo %PORT% | findstr /r "^[0-9]*$" >nul
if errorlevel 1 set PORT=8080
goto :eof

REM === Print banner ===
:print_banner
echo.
echo   -------------------------------
echo   Simple-Pic Management Script
echo   -------------------------------
echo.
goto :eof

REM === Start ===
:do_start
call :print_banner

where java >nul 2>nul
if errorlevel 1 (
    echo   [ERROR] Java is not installed.
    echo   Please install JDK 8 or higher.
    echo.
    exit /b 1
)

if not defined JAR_FILE (
    echo   [ERROR] JAR file not found.
    echo.
    exit /b 1
)

if exist "%PID_FILE%" (
    set /p PID=<%PID_FILE%
    tasklist /FI "PID eq !PID!" 2>nul | find /I /N "java.exe">nul
    if "!ERRORLEVEL!"=="0" (
        call :get_port
        echo   [WARN] %APP_NAME% is already running
        echo.
        echo   PID:    !PID!
        echo   Port:   %PORT%
        echo   Log:    %LOG_FILE%
        echo.
        exit /b 0
    )
    del "%PID_FILE%" 2>nul
)

if not exist "logs" mkdir logs

call :get_port
echo   [*] Starting %APP_NAME%...
echo   Port: %PORT%
echo.

start /B java -jar "%JAR_FILE%" >> "%LOG_FILE%" 2>&1

REM Get the PID of the Java process
set JAVA_PID=
for /f "tokens=2" %%i in ('tasklist ^| findstr "java.exe"') do (
    set JAVA_PID=%%i
)

echo   Waiting for startup...
timeout /t 5 /nobreak >nul

if defined JAVA_PID (
    echo !JAVA_PID! > "%PID_FILE%"
    echo.
    echo   [OK] %APP_NAME% started successfully!
    echo.
    echo   PID:    !JAVA_PID!
    echo   Port:   %PORT%
    echo   Log:    %LOG_FILE%
    echo   Access: http://localhost:%PORT%
    echo.
) else (
    echo.
    echo   [ERROR] Failed to start %APP_NAME%.
    echo   Check log: %LOG_FILE%
    echo.
    exit /b 1
)
goto :eof

REM === Stop ===
:do_stop
call :print_banner

if not exist "%PID_FILE%" (
    echo   [INFO] %APP_NAME% is not running
    echo.
    exit /b 0
)

set /p PID=<%PID_FILE%

tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="1" (
    echo   [INFO] %APP_NAME% is not running ^(stale PID cleaned^)
    echo.
    del "%PID_FILE%" 2>nul
    exit /b 0
)

echo   [*] Stopping %APP_NAME% (PID: %PID%)...
echo.
taskkill /PID %PID% /F

timeout /t 2 /nobreak >nul

tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="1" (
    del "%PID_FILE%" 2>nul
    echo   [OK] %APP_NAME% stopped successfully.
    echo.
) else (
    echo   [WARN] Failed to stop %APP_NAME%.
    echo.
)
goto :eof

REM === Status ===
:do_status
call :print_banner

if not exist "%PID_FILE%" (
    echo   [INFO] %APP_NAME% is not running
    echo.
    echo   Use: simple-pic.bat start
    echo.
    exit /b 0
)

set /p PID=<%PID_FILE%

tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
if "%ERRORLEVEL%"=="0" (
    call :get_port
    echo   [RUNNING] %APP_NAME% is running
    echo.
    echo   -----------------------------------------
    echo   PID:     %PID%
    echo   Port:    %PORT%
    echo   Access:  http://localhost:%PORT%
    echo   -----------------------------------------
    echo.
    echo   Resource usage:
    echo.
    tasklist /FI "PID eq %PID%" /FO TABLE
    echo.
) else (
    echo   [INFO] %APP_NAME% is not running ^(stale PID cleaned^)
    echo.
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
echo.
echo   Simple-Pic Management Script
echo.
echo   Usage: simple-pic.bat {start^|stop^|restart^|status}
echo.
echo   Commands:
echo     start    Start the application
echo     stop     Stop the application
echo     restart  Restart the application
echo     status   Show running status
echo.
echo   Port: Edit server.port in application.yml
echo   Env:  set SERVER_PORT=9090 ^& simple-pic.bat start
echo.
exit /b 1
