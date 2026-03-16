#!/bin/bash

# Spring Boot Application Startup Script

APP_NAME="rdb-agent"
APP_VERSION="0.0.1-SNAPSHOT"
APP_JAR="${APP_NAME}-${APP_VERSION}.jar"

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

# Set directories
LIB_DIR="${BASE_DIR}/lib"
CONFIG_DIR="${BASE_DIR}/config"
LOGS_DIR="${BASE_DIR}/logs"
PID_FILE="${BASE_DIR}/.${APP_NAME}.pid"

# Create logs directory if not exists
mkdir -p "${LOGS_DIR}"

# Find the application jar
if [ -f "${LIB_DIR}/${APP_JAR}" ]; then
    APP_PATH="${LIB_DIR}/${APP_JAR}"
else
    # Try to find any jar in lib directory
    APP_PATH=$(find "${LIB_DIR}" -name "*.jar" | head -1)
    if [ -z "${APP_PATH}" ]; then
        echo "Error: Application JAR not found in ${LIB_DIR}"
        exit 1
    fi
fi

# JVM Options
JAVA_OPTS="${JAVA_OPTS:- -Xms256m -Xmx512m -XX:+UseG1GC -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9999}"

# Stop existing instance first
echo "Stopping existing ${APP_NAME} instance..."
if [ -f "${SCRIPT_DIR}/stop.sh" ]; then
    chmod +x "${SCRIPT_DIR}/stop.sh"
    "${SCRIPT_DIR}/stop.sh" > /dev/null 2>&1
    sleep 2
fi

# Check if already running
check_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

# Start the application
start() {
    if check_running; then
        echo "${APP_NAME} is already running (PID: $(cat "$PID_FILE"))"
        exit 0
    fi

    echo "Starting ${APP_NAME}..."

    # Run the application
    nohup java $JAVA_OPTS \
        -cp "${CONFIG_DIR}:${LIB_DIR}/*" \
        org.springframework.boot.loader.JarLauncher \
        > "${LOGS_DIR}/${APP_NAME}.out" 2>&1 &

    PID=$!
    echo $PID > "$PID_FILE"

    echo "${APP_NAME} started successfully, PID: ${PID}"
    echo "Log file: ${LOGS_DIR}/${APP_NAME}.out"
}

start
