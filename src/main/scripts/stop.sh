#!/bin/bash

# Spring Boot Application Stop Script

APP_NAME="rdb-agent"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"
PID_FILE="${BASE_DIR}/.${APP_NAME}.pid"
STOP_TIMEOUT=30

# Check if running
check_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        fi
    fi

    MATCHING_PID=$(pgrep -f "${BASE_DIR}/lib/.*${APP_NAME}.*\.jar\|${BASE_DIR}/lib/\*\|${BASE_DIR}/config:${BASE_DIR}/lib" | head -1)
    if [ -n "$MATCHING_PID" ]; then
        PID="$MATCHING_PID"
        return 0
    fi

    return 1
}

# Stop the application
stop() {
    if ! check_running; then
        echo "${APP_NAME} is not running"
        exit 0
    fi

    if [ -f "$PID_FILE" ] && ps -p "$(cat "$PID_FILE")" > /dev/null 2>&1; then
        PID=$(cat "$PID_FILE")
    fi
    echo "Stopping ${APP_NAME} (PID: ${PID})..."

    # Graceful stop
    kill "$PID"

    # Wait for process to stop
    count=0
    while [ $count -lt $STOP_TIMEOUT ]; do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            break
        fi
        sleep 1
        count=$((count + 1))
        echo -n "."
    done

    echo ""

    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Process did not stop gracefully, force killing..."
        kill -9 "$PID"
        sleep 2
    fi

    # Remove PID file
    rm -f "$PID_FILE"

    echo "${APP_NAME} stopped successfully"
}

stop
