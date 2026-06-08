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

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# Print usage
usage() {
    echo ""
    echo -e "${BOLD}Simple-Pic 管理脚本${NC}"
    echo ""
    echo -e "  ${CYAN}Usage:${NC}"
    echo -e "    ./simple-pic.sh ${GREEN}{start|stop|restart|status}${NC}"
    echo ""
    echo -e "  ${CYAN}Commands:${NC}"
    echo -e "    ${GREEN}start${NC}    启动应用"
    echo -e "    ${GREEN}stop${NC}     停止应用"
    echo -e "    ${GREEN}restart${NC}  重启应用"
    echo -e "    ${GREEN}status${NC}   查看运行状态"
    echo ""
    echo -e "  ${CYAN}Port:${NC}  编辑 application.yml 中的 server.port"
    echo -e "  ${CYAN}Env:${NC}   SERVER_PORT=9090 ./simple-pic.sh start"
    echo ""
    exit 1
}

# Get port from external application.yml
get_port() {
    if [ -f "application.yml" ]; then
        PORT=$(grep 'port:' application.yml | head -1 | sed 's/.*port:\s*//' | grep -oE '^[0-9]+' | head -1)
        echo "${PORT:-8080}"
    else
        echo "8080"
    fi
}

# Print banner
print_banner() {
    echo ""
    echo -e "${CYAN}┌─────────────────────────────┐${NC}"
    echo -e "${CYAN}│${NC}  ${BOLD}Simple-Pic${NC} ${DIM}图床管理脚本${NC}    ${CYAN}│${NC}"
    echo -e "${CYAN}└─────────────────────────────┘${NC}"
    echo ""
}

# Start
do_start() {
    print_banner

    if ! command -v java &> /dev/null; then
        echo -e "  ${RED}✗ 错误: 未检测到 Java 环境${NC}"
        echo -e "  ${DIM}请安装 JDK 8 或更高版本${NC}"
        echo ""
        return 1
    fi

    if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
        echo -e "  ${RED}✗ 错误: 未找到 JAR 文件${NC}"
        echo -e "  ${DIM}请确认 ${APP_NAME}-*.jar 存在于当前目录${NC}"
        echo ""
        return 1
    fi

    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            PORT=$(get_port)
            echo -e "  ${YELLOW}⚠ ${APP_NAME} 已在运行${NC}"
            echo ""
            echo -e "  ${DIM}PID:${NC}   ${PID}"
            echo -e "  ${DIM}Port:${NC}  ${PORT}"
            echo -e "  ${DIM}Log:${NC}   ${LOG_FILE}"
            echo ""
            return 0
        else
            rm -f "$PID_FILE"
        fi
    fi

    mkdir -p logs

    PORT=$(get_port)
    echo -e "  ${GREEN}▸${NC} 正在启动 ${BOLD}${APP_NAME}${NC}..."
    echo -e "  ${DIM}端口: ${PORT}${NC}"
    echo ""

    nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    PID=$!
    echo $PID > "$PID_FILE"

    # Animated wait
    echo -ne "  ${DIM}等待启动中${NC}"
    for i in $(seq 1 10); do
        sleep 1
        echo -ne "${DIM}.${NC}"
        if ! ps -p "$PID" > /dev/null 2>&1; then
            echo ""
            echo ""
            echo -e "  ${RED}✗ 启动失败${NC}"
            echo -e "  ${DIM}请查看日志: ${LOG_FILE}${NC}"
            echo ""
            tail -5 "$LOG_FILE" 2>/dev/null | sed 's/^/  /'
            rm -f "$PID_FILE"
            return 1
        fi
        # Check if port is listening
        if ss -tlnp 2>/dev/null | grep -q ":${PORT} "; then
            break
        fi
    done
    echo ""

    if ps -p "$PID" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓ ${APP_NAME} 启动成功！${NC}"
        echo ""
        echo -e "  ${DIM}PID:${NC}    ${BOLD}${PID}${NC}"
        echo -e "  ${DIM}Port:${NC}   ${BOLD}${PORT}${NC}"
        echo -e "  ${DIM}Log:${NC}    ${LOG_FILE}"
        echo -e "  ${DIM}Access:${NC} ${CYAN}http://localhost:${PORT}${NC}"
        echo ""
    else
        echo -e "  ${RED}✗ 启动失败${NC}"
        echo -e "  ${DIM}请查看日志: ${LOG_FILE}${NC}"
        echo ""
        tail -5 "$LOG_FILE" 2>/dev/null | sed 's/^/  /'
        rm -f "$PID_FILE"
        return 1
    fi
}

# Stop
do_stop() {
    print_banner

    if [ ! -f "$PID_FILE" ]; then
        echo -e "  ${YELLOW}⚠ ${APP_NAME} 未在运行${NC}"
        echo ""
        return 0
    fi

    PID=$(cat "$PID_FILE")

    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo -e "  ${YELLOW}⚠ ${APP_NAME} 未在运行 (残留 PID 文件已清理)${NC}"
        echo ""
        rm -f "$PID_FILE"
        return 0
    fi

    echo -e "  ${GREEN}▸${NC} 正在停止 ${BOLD}${APP_NAME}${NC} (PID: ${PID})..."
    echo ""
    kill "$PID"

    echo -ne "  ${DIM}等待停止中${NC}"
    for i in $(seq 1 30); do
        sleep 1
        echo -ne "${DIM}.${NC}"
        if ! ps -p "$PID" > /dev/null 2>&1; then
            echo ""
            rm -f "$PID_FILE"
            echo ""
            echo -e "  ${GREEN}✓ ${APP_NAME} 已停止${NC}"
            echo ""
            return 0
        fi
    done

    echo ""
    echo ""
    echo -e "  ${YELLOW}⚠ 正常停止超时，强制终止...${NC}"
    kill -9 "$PID" 2>/dev/null
    rm -f "$PID_FILE"
    echo -e "  ${GREEN}✓ ${APP_NAME} 已强制停止${NC}"
    echo ""
    return 0
}

# Status
do_status() {
    print_banner

    if [ ! -f "$PID_FILE" ]; then
        echo -e "  ${YELLOW}○ ${APP_NAME} 未在运行${NC}"
        echo ""
        echo -e "  ${DIM}使用 ./simple-pic.sh start 启动${NC}"
        echo ""
        return 0
    fi

    PID=$(cat "$PID_FILE")

    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo -e "  ${YELLOW}○ ${APP_NAME} 未在运行 (残留 PID 文件已清理)${NC}"
        echo ""
        rm -f "$PID_FILE"
        return 0
    fi

    PORT=$(get_port)
    UPTIME=$(ps -p "$PID" -o etime --no-headers 2>/dev/null | tr -d ' ')

    echo -e "  ${GREEN}● ${APP_NAME} 正在运行${NC}"
    echo ""
    echo -e "  ┌─────────────────────────────────────────┐"
    echo -e "  │ ${DIM}PID:${NC}     ${BOLD}${PID}${NC}"
    echo -e "  │ ${DIM}端口:${NC}    ${BOLD}${PORT}${NC}"
    echo -e "  │ ${DIM}运行时间:${NC} ${BOLD}${UPTIME}${NC}"
    echo -e "  │ ${DIM}访问地址:${NC} ${CYAN}http://localhost:${PORT}${NC}"
    echo -e "  └─────────────────────────────────────────┘"
    echo ""

    # Resource usage with header
    echo -e "  ${DIM}资源占用:${NC}"
    echo ""
    echo -e "  ${DIM}PID       内存(RSS)   CPU%  MEM%  运行时间${NC}"
    ps -p "$PID" -o pid,rss,pcpu,pmem,etime --no-headers 2>/dev/null | \
        awk '{printf "  %-9s %s MB     %-5s %-5s %s\n", $1, int($2/1024), $3, $4, $5}'
    echo ""
}

# Restart
do_restart() {
    print_banner

    echo -e "  ${GREEN}▸${NC} 正在重启 ${BOLD}${APP_NAME}${NC}..."
    echo ""

    # Stop (quiet mode - suppress banner and redundant messages)
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo -e "  ${DIM}停止中 (PID: ${PID})...${NC}"
            kill "$PID"
            for i in $(seq 1 30); do
                sleep 1
                if ! ps -p "$PID" > /dev/null 2>&1; then
                    rm -f "$PID_FILE"
                    break
                fi
            done
            # Force kill if still running
            if ps -p "$PID" > /dev/null 2>&1; then
                kill -9 "$PID" 2>/dev/null
                rm -f "$PID_FILE"
            fi
            echo -e "  ${GREEN}✓${NC} 已停止"
        else
            rm -f "$PID_FILE"
        fi
    else
        echo -e "  ${DIM}未在运行，直接启动${NC}"
    fi

    sleep 2

    # Start (without banner)
    do_start_quiet
}

# Start without banner (used by restart)
do_start_quiet() {
    if ! command -v java &> /dev/null; then
        echo -e "  ${RED}✗ 错误: 未检测到 Java 环境${NC}"
        return 1
    fi

    if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
        echo -e "  ${RED}✗ 错误: 未找到 JAR 文件${NC}"
        return 1
    fi

    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            PORT=$(get_port)
            echo -e "  ${YELLOW}⚠ ${APP_NAME} 已在运行${NC}"
            echo -e "  ${DIM}PID: ${PID}  Port: ${PORT}${NC}"
            echo ""
            return 0
        else
            rm -f "$PID_FILE"
        fi
    fi

    mkdir -p logs
    PORT=$(get_port)

    nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    PID=$!
    echo $PID > "$PID_FILE"

    echo -ne "  ${DIM}启动中${NC}"
    for i in $(seq 1 10); do
        sleep 1
        echo -ne "${DIM}.${NC}"
        if ! ps -p "$PID" > /dev/null 2>&1; then
            echo ""
            echo -e "  ${RED}✗ 启动失败，请查看日志: ${LOG_FILE}${NC}"
            echo ""
            tail -5 "$LOG_FILE" 2>/dev/null | sed 's/^/  /'
            rm -f "$PID_FILE"
            return 1
        fi
        if ss -tlnp 2>/dev/null | grep -q ":${PORT} "; then
            break
        fi
    done
    echo ""

    if ps -p "$PID" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓ ${APP_NAME} 启动成功${NC}"
        echo ""
        echo -e "  ${DIM}PID:${NC}    ${BOLD}${PID}${NC}"
        echo -e "  ${DIM}Port:${NC}   ${BOLD}${PORT}${NC}"
        echo -e "  ${DIM}Log:${NC}    ${LOG_FILE}"
        echo -e "  ${DIM}Access:${NC} ${CYAN}http://localhost:${PORT}${NC}"
        echo ""
    else
        echo -e "  ${RED}✗ 启动失败，请查看日志: ${LOG_FILE}${NC}"
        echo ""
        rm -f "$PID_FILE"
        return 1
    fi
}

# Main
case "${1:-}" in
    start)   do_start   ;;
    stop)    do_stop    ;;
    restart) do_restart ;;
    status)  do_status  ;;
    *)       usage      ;;
esac
