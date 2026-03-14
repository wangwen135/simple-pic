#!/bin/bash

# Simple-Pic Status Script

APP_NAME="simple-pic"
PID_FILE="${APP_NAME}.pid"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "$APP_NAME is not running"
    exit 0
fi

PID=$(cat "$PID_FILE")

# Check if process is running
if ps -p "$PID" > /dev/null 2>&1; then
    echo "$APP_NAME is running (PID: $PID)"
    echo ""
    echo "Memory usage:"
    ps -p "$PID" -o pid,vsz,rss,pcpu,pmem,cmd
    echo ""
    echo "Access the application at: http://localhost:8080"
else
    echo "$APP_NAME is not running (stale PID file)"
    rm -f "$PID_FILE"
fi