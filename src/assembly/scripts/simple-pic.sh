#!/bin/bash

# Simple-Pic Management Script
# Usage: ./simple-pic.sh {start|stop|restart|status}

APP_NAME="simple-pic"
PID_FILE="${APP_NAME}.pid"
LOG_FILE="logs/${APP_NAME}.log"

# cd to script directory
cd "$(dirname "$0")"

# Find JAR file (don't hardcode version)
JAR_FILE=$(ls ${APP_NAME}-*.jar 2>/dev/null | head -1)

# Print usage
usage() {
    echo "Usage: ./simple-pic.sh {start|stop|restart|status}"
    exit 1
}

# Get port from external application.yml (Spring Boot will read it)
get_port() {
    if [ -f "application.yml" ]; then
        PORT=$(grep 'port:' application.yml | head -1 | sed 's/.*port:\s*//' | grep -oE '^[0-9]+' | head -1)
        echo "${PORT:-8080}"
    else
        echo "8080"
    fi
}

# Start
do_start() {
    if ! command -v java &> /dev/null; then
        echo "Error: Java is not installed. Please install Java 8 or higher."
        return 1
    fi

    if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
        echo "Error: JAR file not found."
        return 1
    fi

    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "$APP_NAME is already running (PID: $PID)"
            return 0
        else
            rm -f "$PID_FILE"
        fi
    fi

    mkdir -p logs

    echo "Starting $APP_NAME..."
    nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    PID=$!
    echo $PID > "$PID_FILE"

    sleep 3

    if ps -p "$PID" > /dev/null 2>&1; then
        PORT=$(get_port)
        echo "$APP_NAME started successfully (PID: $PID)"
        echo "Log file: $LOG_FILE"
        echo "Access: http://localhost:$PORT"
        return 0
    else
        echo "Failed to start $APP_NAME. Check log: $LOG_FILE"
        rm -f "$PID_FILE"
        return 1
    fi
}

# Stop
do_stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "$APP_NAME is not running"
        return 0
    fi

    PID=$(cat "$PID_FILE")

    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "$APP_NAME is not running (stale PID file)"
        rm -f "$PID_FILE"
        return 0
    fi

    echo "Stopping $APP_NAME (PID: $PID)..."
    kill "$PID"

    for i in $(seq 1 30); do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            echo "$APP_NAME stopped successfully"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done

    echo "Force stopping $APP_NAME..."
    kill -9 "$PID" 2>/dev/null
    rm -f "$PID_FILE"
    echo "$APP_NAME stopped (force killed)"
    return 0
}

# Status
do_status() {
    if [ ! -f "$PID_FILE" ]; then
        echo "$APP_NAME is not running"
        return 0
    fi

    PID=$(cat "$PID_FILE")

    if ps -p "$PID" > /dev/null 2>&1; then
        PORT=$(get_port)
        echo "$APP_NAME is running (PID: $PID)"
        echo ""
        ps -p "$PID" -o pid,vsz,rss,pcpu,pmem,etime --no-headers
        echo ""
        echo "Access: http://localhost:$PORT"
    else
        echo "$APP_NAME is not running (stale PID file)"
        rm -f "$PID_FILE"
    fi
    return 0
}

# Restart
do_restart() {
    do_stop
    sleep 2
    do_start
}

# Main
case "${1:-}" in
    start)   do_start   ;;
    stop)    do_stop    ;;
    restart) do_restart ;;
    status)  do_status  ;;
    *)       usage      ;;
esac
