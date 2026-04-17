#!/bin/bash
# 预览服务停止脚本

APP_NAME="fileview-preview"
JAR_FILE="${APP_NAME}.jar"
PID_FILE="/opt/fileview/bin/preview/${APP_NAME}.pid"

# 查找 Java 服务进程 PID（排除 tail/grep 等非服务进程）
find_java_pid() {
    pgrep -f "java.*${JAR_FILE}" 2>/dev/null
}

# 优先从 PID 文件获取，若失效则通过 pgrep 兜底
PID=""
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    # 验证 PID 文件中的进程是否仍是目标 Java 进程
    if [ -n "$PID" ] && ps -p $PID > /dev/null 2>&1; then
        # PID 有效，检查是否确实是 Java 进程（防止 PID 被复用）
        if ! ps -p $PID -o args= 2>/dev/null | grep -q "java.*${JAR_FILE}"; then
            echo "[Fileview] PID 文件中的进程($PID)不是 $APP_NAME，尝试 pgrep 查找..."
            PID=$(find_java_pid)
        fi
    else
        echo "[Fileview] PID 文件中的进程($PID)已不存在，尝试 pgrep 查找..."
        PID=$(find_java_pid)
    fi
else
    PID=$(find_java_pid)
fi

# 未找到进程
if [ -z "$PID" ]; then
    echo "$APP_NAME is not running"
    rm -f "$PID_FILE"
    exit 0
fi

echo "Stopping $APP_NAME (PID: $PID)..."
kill $PID

# 等待最多30秒
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "$APP_NAME stopped"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# 强制停止
echo "Force stopping $APP_NAME..."
kill -9 $PID 2>/dev/null
rm -f "$PID_FILE"
echo "$APP_NAME force stopped"
