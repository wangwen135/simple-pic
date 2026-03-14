@echo off
REM Simple-Pic Startup Script for Windows

set APP_NAME=simple-pic
set JAR_FILE=%APP_NAME%-1.0.0.jar
set PID_FILE=%APP_NAME%.pid
set LOG_FILE=logs\%APP_NAME%.log
set CONFIG_FILE=config.yml

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: Java is not installed. Please install Java 8 or higher.
    exit /b 1
)

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo Error: JAR file '%JAR_FILE%' not found.
    exit /b 1
)

REM Check if already running
if exist "%PID_FILE%" (
    set /p PID=<%PID_FILE%
    tasklist /FI "PID eq %PID%" 2>nul | find /I /N "java.exe">nul
    if "%ERRORLEVEL%"=="0" (
        echo %APP_NAME% is already running ^(PID: %PID%^)
        exit /b 1
    )
    del "%PID_FILE%" 2>nul
)

REM Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

REM Generate config if not exists
if not exist "%CONFIG_FILE%" (
    echo Config file not found. It will be generated automatically on first start.
)

REM Start the application
echo Starting %APP_NAME%...
start /B java -jar "%JAR_FILE%" >> "%LOG_FILE%" 2>&1

REM Get the PID of the Java process
for /f "tokens=2" %%i in ('tasklist ^| findstr "java.exe"') do (
    set JAVA_PID=%%i
)

REM Wait a moment
timeout /t 2 /nobreak >nul

REM Save PID to file
if defined JAVA_PID (
    echo %JAVA_PID% > "%PID_FILE%"
    echo %APP_NAME% started successfully ^(PID: %JAVA_PID%^)
    echo Log file: %LOG_FILE%
    echo Access the application at: http://localhost:8080
) else (
    echo Failed to start %APP_NAME%. Check log file: %LOG_FILE%
    exit /b 1
)