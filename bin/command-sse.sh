#!/usr/bin/env bash

set -e

# 自然语言命令内容
COMMAND='
Go to https://www.amazon.com/dp/B0C1H26C46
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
'

# API 接口
API_BASE="http://localhost:8182"
COMMAND_ENDPOINT="$API_BASE/api/commands/plain?mode=async"

# 发送命令
echo "Sending command to server..."
COMMAND_ID=$(curl -s -X POST \
  -H "Content-Type: text/plain" \
  --data "$COMMAND" \
  "$COMMAND_ENDPOINT")

# 检查 command ID 是否有效
if [[ -z "$COMMAND_ID" ]]; then
  echo "Error: Failed to get command ID from server."
  exit 1
fi
echo "Command ID: $COMMAND_ID"

# SSE 监听地址和结果地址
SSE_URL="$API_BASE/api/commands/$COMMAND_ID/stream"
RESULT_URL="$API_BASE/api/commands/$COMMAND_ID/result"

# 获取最终结果的函数
get_final_result() {
  echo ""
  echo "=== FETCHING FINAL RESULT ==="

  # 等待一下确保结果已准备好
  sleep 2

  local result=$(curl -s -X GET "$RESULT_URL" 2>/dev/null)

  if [[ -n "$result" ]]; then
    echo ""
    echo "=== FINAL RESULT ==="
    echo "Command ID: $COMMAND_ID"
    echo "Timestamp: $(date -u '+%Y-%m-%d %H:%M:%S') UTC"
    echo "Status: COMPLETED"
    echo ""
    echo "Result:"
    echo "$result" | jq . 2>/dev/null || echo "$result"
    echo ""
    echo "=== END OF RESULT ==="
  else
    echo "Warning: Failed to fetch final result"
    echo "You can manually check the result at: $RESULT_URL"
  fi
}

SSE_FIFO=$(mktemp -u)
mkfifo "$SSE_FIFO"

echo "Connecting to SSE stream..."
curl -N --no-buffer -H "Accept: text/event-stream" "$SSE_URL" > "$SSE_FIFO" &
CURL_PID=$!
echo "$CURL_PID" > /tmp/command_sse_curl_pid.txt

# 清理函数：在退出时删除 FIFO 并杀掉 curl 进程
cleanup() {
  [[ -p "$SSE_FIFO" ]] && rm -f "$SSE_FIFO"
  [[ -f /tmp/command_sse_curl_pid.txt ]] && rm -f /tmp/command_sse_curl_pid.txt
  [[ -n "$CURL_PID" ]] && kill "$CURL_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

# SSE 主循环
isDone=0
last_update=""

while read -r line; do
  # 跳过空行或注释
  if [[ -z "$line" || "$line" == :* ]]; then
    continue
  fi

  # 提取 data 字段
  if [[ "$line" == data:* ]]; then
    data="${line#data:}"
    data="${data#"${data%%[![:space:]]*}"}"  # 去除前导空白

    # 避免重复打印相同的更新
    if [[ "$data" != "$last_update" ]]; then
      echo "SSE update: $data"
      last_update="$data"
    fi

    # 检查是否已完成
    if [[ "$data" =~ \"isDone\"[[:space:]]*:[[:space:]]*true ]]; then
      isDone=1
      echo ""
      echo "Task completed! Fetching final result..."

      # 获取并打印最终结果
      get_final_result
      break
    fi
  fi
done < "$SSE_FIFO"

if [[ $isDone -eq 0 ]]; then
  echo "Warning: SSE stream ended but task may not be completed."
  echo "Attempting to fetch result anyway..."
  get_final_result
fi

echo ""
echo "Finished command-sse.sh script."