#!/bin/bash

# Simple-Pic Startup Script

APP_NAME="simple-pic"
JAR_FILE="${APP_NAME}-1.0.0.jar"
PID_FILE="${APP_NAME}.pid"
LOG_FILE="logs/${APP_NAME}.log"
CONFIG_FILE="config.yml"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed. Please install Java 8 or higher."
    exit 1
fi

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file '$JAR_FILE' not found."
    exit 1
fi

# Check if already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "$APP_NAME is already running (PID: $PID)"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# Create logs directory if it doesn't exist
mkdir -p logs

# Start the application
echo "Starting $APP_NAME..."

# Generate config if not exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Config file not found. It will be generated automatically on first start."
fi

nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
PID=$!

# Save PID
echo $PID > "$PID_FILE"

# Wait a moment to check if the process started successfully
sleep 2

if ps -p "$PID" > /dev/null 2>&1; then
    echo "$APP_NAME started successfully (PID: $PID)"
    echo "Log file: $LOG_FILE"
    echo "Access the application at: http://localhost:8080"
else
    echo "Failed to start $APP_NAME. Check log file: $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
fi