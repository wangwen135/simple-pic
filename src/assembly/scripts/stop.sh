#!/bin/bash

# Simple-Pic Stop Script

APP_NAME="simple-pic"
PID_FILE="${APP_NAME}.pid"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "$APP_NAME is not running (no PID file found)"
    exit 0
fi

PID=$(cat "$PID_FILE")

# Check if process is running
if ! ps -p "$PID" > /dev/null 2>&1; then
    echo "$APP_NAME is not running (stale PID file)"
    rm -f "$PID_FILE"
    exit 0
fi

# Stop the process
echo "Stopping $APP_NAME (PID: $PID)..."
kill "$PID"

# Wait for process to stop
for i in {1..30}; do
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "$APP_NAME stopped successfully"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# Force kill if still running
echo "Force stopping $APP_NAME..."
kill -9 "$PID" 2>/dev/null
rm -f "$PID_FILE"
echo "$APP_NAME stopped (force killed)"