#!/usr/bin/env bash

# Test script for curl commands from README.md
# All curl commands are explicitly listed below for easy maintenance
# Author: Auto-generated for platonai
# Date: 2025-06-11 17:29:59

# =========================
# CURL 命令与描述变量定义
# =========================

# System Health Checks (Quick tests first)
CURL_DESC_HEALTH_CHECK="Health Check Endpoint"
CURL_CMD_HEALTH_CHECK='curl -X GET "http://localhost:8182/actuator/health"'

CURL_DESC_QUERY_PARAMS="Query Parameters Test"
CURL_CMD_QUERY_PARAMS='curl -X GET "http://localhost:8182/actuator/health?details=true"'

CURL_DESC_WEBUI="WebUI Command Interface"
CURL_CMD_WEBUI='curl -X GET "http://localhost:8182/command.html"'

CURL_DESC_CUSTOM_HEADERS="Custom Headers Test"
CURL_CMD_CUSTOM_HEADERS='curl -X GET "http://localhost:8182/actuator/health" -H "Accept: application/json" -H "User-Agent: PulsarRPA-Test-Suite/1.0"'

# Simple Data Extraction Tests
CURL_DESC_SIMPLE_LOAD="Simple Page Load Test"
CURL_CMD_SIMPLE_LOAD='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_base_uri(dom) as url,
dom_first_text(dom, '\''title'\'') as page_title
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

CURL_DESC_HTML_PARSE="HTML Parsing Test"
CURL_CMD_HTML_PARSE='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, '\''h1'\'') as heading,
dom_all_texts(dom, '\''p'\'') as paragraphs
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

CURL_DESC_COMPLEX_XSQL="Complex X-SQL Query"
CURL_CMD_COMPLEX_XSQL='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, '\''title'\'') as page_title,
dom_first_text(dom, '\''h1,h2'\'') as main_heading,
dom_base_uri(dom) as base_url
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

CURL_DESC_FORM_DATA="Form Data Test"
CURL_CMD_FORM_DATA='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select dom_first_text(dom, '\''title'\'') as title from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

# Advanced API Tests (Longer running)
CURL_DESC_PLAIN_API="Plain Text Command API - Amazon Product"
CURL_CMD_PLAIN_API='curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

After browser launch: clear browser cookies.
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
"'

CURL_DESC_JSON_API="JSON Command API - Amazon Product"
CURL_CMD_JSON_API='curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '\''{"url": "https://www.amazon.com/dp/B0C1H26C46", "pageSummaryPrompt": "Provide a brief introduction of this product.", "dataExtractionRules": "product name, price, and ratings", "linkExtractionRules": "all links containing /dp/ on the page", "onPageReadyActions": ["click #title", "scroll to the middle"]}'\'''

CURL_DESC_XSQL_LLM="X-SQL API - LLM Data Extraction"
CURL_CMD_XSQL_LLM='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
llm_extract(dom, '\''product name, price, ratings'\'') as llm_extracted_data,
dom_base_uri(dom) as url,
dom_first_text(dom, '\''#productTitle'\'') as title,
dom_first_slim_html(dom, '\''img:expr(width > 400)'\'') as img
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

CURL_DESC_ASYNC_MODE="Async Command Mode Test"
CURL_CMD_ASYNC_MODE='curl -X POST "http://localhost:8182/api/commands/plain?mode=async" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

Extract the page title and all text content.
"'

# 系统测试优先的命令数组
declare -A CURL_COMMANDS
declare -a CURL_ORDER

CURL_ORDER=(
  "CURL_DESC_HEALTH_CHECK"
  "CURL_DESC_QUERY_PARAMS"
  "CURL_DESC_WEBUI"
  "CURL_DESC_CUSTOM_HEADERS"
  "CURL_DESC_SIMPLE_LOAD"
  "CURL_DESC_HTML_PARSE"
  "CURL_DESC_COMPLEX_XSQL"
  "CURL_DESC_FORM_DATA"
  "CURL_DESC_ASYNC_MODE"
  "CURL_DESC_PLAIN_API"
  "CURL_DESC_JSON_API"
  "CURL_DESC_XSQL_LLM"
)

CURL_COMMANDS["$CURL_DESC_HEALTH_CHECK"]="$CURL_CMD_HEALTH_CHECK"
CURL_COMMANDS["$CURL_DESC_QUERY_PARAMS"]="$CURL_CMD_QUERY_PARAMS"
CURL_COMMANDS["$CURL_DESC_WEBUI"]="$CURL_CMD_WEBUI"
CURL_COMMANDS["$CURL_DESC_CUSTOM_HEADERS"]="$CURL_CMD_CUSTOM_HEADERS"
CURL_COMMANDS["$CURL_DESC_SIMPLE_LOAD"]="$CURL_CMD_SIMPLE_LOAD"
CURL_COMMANDS["$CURL_DESC_HTML_PARSE"]="$CURL_CMD_HTML_PARSE"
CURL_COMMANDS["$CURL_DESC_COMPLEX_XSQL"]="$CURL_CMD_COMPLEX_XSQL"
CURL_COMMANDS["$CURL_DESC_FORM_DATA"]="$CURL_CMD_FORM_DATA"
CURL_COMMANDS["$CURL_DESC_ASYNC_MODE"]="$CURL_CMD_ASYNC_MODE"
CURL_COMMANDS["$CURL_DESC_PLAIN_API"]="$CURL_CMD_PLAIN_API"
CURL_COMMANDS["$CURL_DESC_JSON_API"]="$CURL_CMD_JSON_API"
CURL_COMMANDS["$CURL_DESC_XSQL_LLM"]="$CURL_CMD_XSQL_LLM"

# =============================================================================
# SECTION: GLOBAL CONFIGURATION AND INITIALIZATION
# =============================================================================

DEFAULT_BASE_URL="http://localhost:8182"
TEST_RESULTS_DIR="./target/test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${TEST_RESULTS_DIR}/curl_tests_${TIMESTAMP}.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 默认选项
PULSAR_BASE_URL="$DEFAULT_BASE_URL"
TIMEOUT_SECONDS=120
FAST_MODE=false
SKIP_SERVER_CHECK=false
VERBOSE_MODE=false
USER_NAME="platonai"

# 计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# 确保结果目录存在
mkdir -p "$TEST_RESULTS_DIR"

# =============================================================================
# SECTION: UTILITY FUNCTIONS
# =============================================================================

log() {
  local message="$1"
  local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  local log_message="[$timestamp] $message"
  echo -e "$log_message"
  echo "$log_message" >> "$LOG_FILE"
}

vlog() {
  if [ "$VERBOSE_MODE" = true ]; then
    log "[VERBOSE] $1"
  fi
}

show_progress() {
  local current="$1"
  local total="$2"
  local percent=$((current * 100 / total))
  local filled=$((percent / 2))
  local empty=$((50 - filled))
  local progress=""

  for ((i=0; i<filled; i++)); do
    progress+="="
  done

  for ((i=0; i<empty; i++)); do
    progress+="-"
  done

  printf "\r[PROGRESS] [%s] %d%% (%d/%d)" "$progress" "$percent" "$current" "$total"
}

substitute_urls() {
  local command="$1"
  echo "$command" | sed "s|http://localhost:8182|$PULSAR_BASE_URL|g"
}

check_server() {
  log "${BLUE}[INFO] Checking PulsarRPA server at $PULSAR_BASE_URL...${NC}"

  # 检查健康端点
  local response
  response=$(curl -s -o /dev/null -w "%{http_code}" "$PULSAR_BASE_URL/actuator/health" --connect-timeout 5 2>/dev/null)
  if [[ "$response" -ge 200 && "$response" -lt 300 ]]; then
    log "${GREEN}[SUCCESS] PulsarRPA server is healthy and responding${NC}"
    return 0
  fi

  # 健康端点失败，尝试基础URL
  response=$(curl -s -o /dev/null -w "%{http_code}" "$PULSAR_BASE_URL/" --connect-timeout 5 2>/dev/null)
  if [[ "$response" -ge 200 && "$response" -lt 300 ]]; then
    log "${YELLOW}[WARNING] Server responding but health check endpoint unavailable${NC}"
    return 0
  fi

  log "${RED}[ERROR] PulsarRPA server not accessible at $PULSAR_BASE_URL${NC}"
  log "${CYAN}[HINT] Start PulsarRPA with:${NC}"
  log "    java -DDEEPSEEK_API_KEY=\${DEEPSEEK_API_KEY} -jar PulsarRPA.jar"
  return 1
}

# =============================================================================
# SECTION: TEST EXECUTION FUNCTIONS
# =============================================================================

run_curl_test() {
  local test_name="$1"
  local curl_command="$2"
  local test_number="$3"

  TOTAL_TESTS=$((TOTAL_TESTS+1))
  log ""
  log "${PURPLE}[TEST $test_number/${#CURL_ORDER[@]}] $test_name${NC}"

  # 显示命令预览
  if [ "$VERBOSE_MODE" = true ]; then
    log "${CYAN}[COMMAND]${NC}"
    echo "$curl_command"
  else
    local short_cmd="${curl_command:0:80}..."
    short_cmd=$(echo "$short_cmd" | tr -d '\n\r')
    log "${CYAN}[COMMAND] $short_cmd${NC}"
  fi

  # 替换URL
  local final_command=$(substitute_urls "$curl_command")
  local response_file=$(mktemp)
  local error_file=$(mktemp)
  local meta_file=$(mktemp)

  local full_command="$final_command --max-time $TIMEOUT_SECONDS -w '%{http_code}\n%{time_total}\n%{size_download}\n%{url_effective}' -o \"$response_file\" -s"
  vlog "Executing: ${full_command:0:150}..."

  # 执行命令
  local start_time=$(date +%s)
  local success=false

  eval "$full_command" > "$meta_file" 2> "$error_file"
  local exit_code=$?
  if [ $exit_code -eq 0 ]; then
    success=true
  fi

  local end_time=$(date +%s)
  local duration=$((end_time - start_time))

  if [ "$success" = true ]; then
    local http_status="000"
    local time_total="0.000"
    local size_download="0"
    local url_effective="N/A"

    if [ -f "$meta_file" ]; then
      IFS=$'\n' read -r -d '' http_status time_total size_download url_effective < "$meta_file" || true
    fi

    log "${BLUE}[RESPONSE] Status: $http_status | Time: ${time_total}s | Size: ${size_download}B | Duration: ${duration}s${NC}"

    # 检查成功
    if [[ "$http_status" =~ ^[23][0-9][0-9]$ ]]; then
      log "${GREEN}[PASS] ✅ Test completed successfully${NC}"
      PASSED_TESTS=$((PASSED_TESTS+1))
      cp "$response_file" "$TEST_RESULTS_DIR/test_${test_number}_success.json" 2>/dev/null

      if [ "$size_download" -gt 0 ] && [ "$size_download" -lt 3000 ]; then
        if [ -s "$response_file" ]; then
          local preview=$(head -c 250 "$response_file" | tr -d '\n\r' | sed 's/  */ /g')
          log "${CYAN}[PREVIEW] $preview...${NC}"
        fi
      elif [ "$size_download" -gt 3000 ]; then
        log "${CYAN}[INFO] Large response (${size_download}B) saved to results directory${NC}"
      fi
    else
      log "${RED}[FAIL] ❌ HTTP Status: $http_status${NC}"
      FAILED_TESTS=$((FAILED_TESTS+1))
      cp "$response_file" "$TEST_RESULTS_DIR/test_${test_number}_error_${http_status}.txt" 2>/dev/null

      if [ -s "$response_file" ]; then
        local error_preview=$(head -c 200 "$response_file" | tr -d '\n\r')
        log "${RED}[ERROR RESPONSE] $error_preview${NC}"
      fi

      if [ -s "$error_file" ]; then
        local curl_error=$(head -c 200 "$error_file" | tr -d '\n\r')
        log "${RED}[CURL ERROR] $curl_error${NC}"
      fi
    fi
  else
    log "${RED}[FAIL] ❌ Command execution failed${NC}"
    FAILED_TESTS=$((FAILED_TESTS+1))

    {
      echo "Command: $final_command"
      echo "Error output:"
      cat "$error_file"
    } > "$TEST_RESULTS_DIR/test_${test_number}_exec_error.txt"

    if [ -s "$error_file" ]; then
      local exec_error=$(head -c 200 "$error_file" | tr -d '\n\r')
      log "${RED}[EXECUTION ERROR] $exec_error${NC}"
    fi
  fi

  rm -f "$response_file" "$error_file" "$meta_file"
  if [ "$test_number" -lt "${#CURL_ORDER[@]}" ]; then
    show_progress "$TOTAL_TESTS" "${#CURL_ORDER[@]}"
  fi
}

run_all_tests() {
  log "${BLUE}[INFO] Starting test execution...${NC}"
  log "${BLUE}[INFO] Total commands to test: ${#CURL_ORDER[@]}${NC}"

  local test_counter=0
  for desc_var in "${CURL_ORDER[@]}"; do
    test_counter=$((test_counter+1))
    local test_name="${!desc_var}"
    local test_command="${CURL_COMMANDS[$test_name]}"
    run_curl_test "$test_name" "$test_command" "$test_counter"

    if [ "$FAST_MODE" = false ]; then
      sleep 1
    fi
  done

  echo ""
}

print_summary() {
  log ""
  log "=============================================="
  log "${BLUE}[FINAL SUMMARY] Test Results${NC}"
  log "=============================================="
  log "${BLUE}Test Session: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
  log "${BLUE}User: $USER_NAME${NC}"
  log "${BLUE}Server: $PULSAR_BASE_URL${NC}"
  log "${BLUE}Total Commands: ${#CURL_ORDER[@]}${NC}"
  log "${BLUE}Tests Executed: $TOTAL_TESTS${NC}"
  log "${GREEN}Passed: $PASSED_TESTS${NC}"
  log "${RED}Failed: $FAILED_TESTS${NC}"
  log "${YELLOW}Skipped: $SKIPPED_TESTS${NC}"

  if [ "$TOTAL_TESTS" -gt 0 ]; then
    local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    log "${BLUE}Success Rate: $success_rate%${NC}"
  fi

  log "${BLUE}Log File: $LOG_FILE${NC}"
  log "${BLUE}Results Directory: $TEST_RESULTS_DIR${NC}"
  log "=============================================="

  if [ "$TOTAL_TESTS" -eq 0 ]; then
    log "${YELLOW}[INFO] No tests were executed${NC}"
    exit 0
  elif [ "$FAILED_TESTS" -eq 0 ]; then
    log "${GREEN}[SUCCESS] All tests passed! 🎉${NC}"
    exit 0
  else
    log "${YELLOW}[PARTIAL SUCCESS] Some tests failed. Check logs for details.${NC}"
    exit 1
  fi
}

usage() {
  cat << EOF
Usage: $0 [OPTIONS]

Test curl commands from README.md against PulsarRPA server.

OPTIONS:
-u, --url URL         PulsarRPA base URL (default: $DEFAULT_BASE_URL)
-f, --fast            Fast mode - minimal delays between tests
-s, --skip-server     Skip server connectivity check
-t, --timeout SEC     Request timeout in seconds (default: 120)
-v, --verbose         Enable verbose output
-h, --help            Show this help message

EXAMPLES:
$0                              # Run all tests with defaults
$0 -u http://localhost:8080     # Use custom server URL
$0 -f -t 60                     # Fast mode with 60s timeout
$0 -s -v                        # Skip server check with verbose output

REQUIREMENTS:
- curl command available
- PulsarRPA server running (unless --skip-server)

UPDATING COMMANDS:
Edit the CURL_COMMANDS array to add/modify tests.
EOF
}

# =============================================================================
# SECTION: MAIN EXECUTION LOGIC
# =============================================================================

parse_args() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      -u|--url)
        PULSAR_BASE_URL="$2"
        shift 2
        ;;
      -f|--fast)
        FAST_MODE=true
        shift
        ;;
      -s|--skip-server)
        SKIP_SERVER_CHECK=true
        shift
        ;;
      -t|--timeout)
        TIMEOUT_SECONDS="$2"
        shift 2
        ;;
      -v|--verbose)
        VERBOSE_MODE=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "Unknown option: $1"
        usage
        exit 1
        ;;
    esac
  done
}

main() {
  log "${BLUE}[INFO] PulsarRPA Curl Command Test Suite${NC}"
  log "${BLUE}[INFO] User: $USER_NAME${NC}"
  log "${BLUE}[INFO] Timestamp: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
  log "${BLUE}[INFO] Server URL: $PULSAR_BASE_URL${NC}"
  log "${BLUE}[INFO] Timeout: ${TIMEOUT_SECONDS}# 我将把这个 PowerShell 脚本转换成 Bash 版本

```bash
#!/usr/bin/env bash

# Test script for curl commands from README.md
# All curl commands are explicitly listed below for easy maintenance
# Author: Auto-generated for platonai
# Date: 2025-06-11 17:29:59

# =========================
# CURL 命令与描述变量定义
# =========================

# System Health Checks (Quick tests first)
CURL_DESC_HEALTH_CHECK="Health Check Endpoint"
CURL_CMD_HEALTH_CHECK='curl -X GET "http://localhost:8182/actuator/health"'

CURL_DESC_QUERY_PARAMS="Query Parameters Test"
CURL_CMD_QUERY_PARAMS='curl -X GET "http://localhost:8182/actuator/health?details=true"'

CURL_DESC_WEBUI="WebUI Command Interface"
CURL_CMD_WEBUI='curl -X GET "http://localhost:8182/command.html"'

CURL_DESC_CUSTOM_HEADERS="Custom Headers Test"
CURL_CMD_CUSTOM_HEADERS='curl -X GET "http://localhost:8182/actuator/health" -H "Accept: application/json" -H "User-Agent: PulsarRPA-Test-Suite/1.0"'

# Simple Data Extraction Tests
CURL_DESC_SIMPLE_LOAD="Simple Page Load Test"
CURL_CMD_SIMPLE_LOAD='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_base_uri(dom) as url,
dom_first_text(dom, '"'"'title'"'"') as page_title
from load_and_select('"'"'https://www.amazon.com/dp/B0C1H26C46'"'"', '"'"'body'"'"');
"'

CURL_DESC_HTML_PARSE="HTML Parsing Test"
CURL_CMD_HTML_PARSE='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, '"'"'h1'"'"') as heading,
dom_all_texts(dom, '"'"'p'"'"') as paragraphs
from load_and_select('"'"'https://www.amazon.com/dp/B0C1H26C46'"'"', '"'"'body'"'"');
"'

CURL_DESC_COMPLEX_XSQL="Complex X-SQL Query"
CURL_CMD_COMPLEX_XSQL='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, '"'"'title'"'"') as page_title,
dom_first_text(dom, '"'"'h1,h2'"'"') as main_heading,
dom_base_uri(dom) as base_url
from load_and_select('"'"'https://www.amazon.com/dp/B0C1H26C46'"'"', '"'"'body'"'"');
"'

CURL_DESC_FORM_DATA="Form Data Test"
CURL_CMD_FORM_DATA='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select dom_first_text(dom, '"'"'title'"'"') as title from load_and_select('"'"'https://www.amazon.com/dp/B0C1H26C46'"'"', '"'"'body'"'"');
"'

# Advanced API Tests (Longer running)
CURL_DESC_PLAIN_API="Plain Text Command API - Amazon Product"
CURL_CMD_PLAIN_API='curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

After browser launch: clear browser cookies.
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
"'

CURL_DESC_JSON_API="JSON Command API - Amazon Product"
CURL_CMD_JSON_API='curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '"'"'{
"url": "https://www.amazon.com/dp/B0C1H26C46",
"pageSummaryPrompt": "Provide a brief introduction of this product.",
"dataExtractionRules": "product name, price, and ratings",
"linkExtractionRules": "all links containing /dp/ on the page",
"onPageReadyActions": ["click #title", "scroll to the middle"]
}'"'"''

CURL_DESC_XSQL_LLM="X-SQL API - LLM Data Extraction"
CURL_CMD_XSQL_LLM='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
llm_extract(dom, '"'"'product name, price, ratings'"'"') as llm_extracted_data,
dom_base_uri(dom) as url,
dom_first_text(dom, '"'"'#productTitle'"'"') as title,
dom_first_slim_html(dom, '"'"'img:expr(width > 400)'"'"') as img
from load_and_select('"'"'https://www.amazon.com/dp/B0C1H26C46'"'"', '"'"'body'"'"');
"'

CURL_DESC_ASYNC_MODE="Async Command Mode Test"
CURL_CMD_ASYNC_MODE='curl -X POST "http://localhost:8182/api/commands/plain?mode=async" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

Extract the page title and all text content.
"'

# 命令数组
declare -a CURL_COMMANDS=(
  "$CURL_DESC_HEALTH_CHECK|$CURL_CMD_HEALTH_CHECK"
  "$CURL_DESC_QUERY_PARAMS|$CURL_CMD_QUERY_PARAMS"
  "$CURL_DESC_WEBUI|$CURL_CMD_WEBUI"
  "$CURL_DESC_CUSTOM_HEADERS|$CURL_CMD_CUSTOM_HEADERS"
  "$CURL_DESC_SIMPLE_LOAD|$CURL_CMD_SIMPLE_LOAD"
  "$CURL_DESC_HTML_PARSE|$CURL_CMD_HTML_PARSE"
  "$CURL_DESC_COMPLEX_XSQL|$CURL_CMD_COMPLEX_XSQL"
  "$CURL_DESC_FORM_DATA|$CURL_CMD_FORM_DATA"
  "$CURL_DESC_ASYNC_MODE|$CURL_CMD_ASYNC_MODE"
  "$CURL_DESC_PLAIN_API|$CURL_CMD_PLAIN_API"
  "$CURL_DESC_JSON_API|$CURL_CMD_JSON_API"
  "$CURL_DESC_XSQL_LLM|$CURL_CMD_XSQL_LLM"
)

# =============================================================================
# SECTION: GLOBAL CONFIGURATION AND INITIALIZATION
# =============================================================================

DEFAULT_BASE_URL="http://localhost:8182"
TEST_RESULTS_DIR="./target/test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${TEST_RESULTS_DIR}/curl_tests_${TIMESTAMP}.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'
NC='\033[0m' # No Color

# 默认选项
PULSAR_BASE_URL="$DEFAULT_BASE_URL"
TIMEOUT_SECONDS=120
FAST_MODE=false
SKIP_SERVER_CHECK=false
VERBOSE_MODE=false
USER_NAME="platonai"

# 计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# 确保结果目录存在
mkdir -p "$TEST_RESULTS_DIR"

# =============================================================================
# SECTION: UTILITY FUNCTIONS
# =============================================================================

log() {
  local message="$1"
  local color="${2:-$WHITE}"
  local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  local log_message="[$timestamp] $message"
  echo -e "${color}${log_message}${NC}"
  echo "$log_message" >> "$LOG_FILE"
}

vlog() {
  local message="$1"
  if [ "$VERBOSE_MODE" = true ]; then
    log "[VERBOSE] $message" "$CYAN"
  fi
}

show_progress() {
  local current="$1"
  local total="$2"
  local percent=$((current * 100 / total))
  local filled=$((percent / 2))
  local empty=$((50 - filled))
  local fill_bar=""
  local empty_bar=""

  for ((i=0; i<filled; i++)); do
    fill_bar+="="
  done

  for ((i=0; i<empty; i++)); do
    empty_bar+="-"
  done

  echo -ne "\r${BLUE}[PROGRESS] [${fill_bar}${empty_bar}] ${percent}% ($current/$total)${NC}"
}

substitute_urls() {
  local command="$1"
  echo "$command" | sed "s|http://localhost:8182|$PULSAR_BASE_URL|g"
}

check_server() {
  log "[INFO] Checking PulsarRPA server at $PULSAR_BASE_URL..." "$BLUE"

  if curl -s "$PULSAR_BASE_URL/actuator/health" -m 5 > /dev/null; then
    log "[SUCCESS] PulsarRPA server is healthy and responding" "$GREEN"
    return 0
  fi

  if curl -s "$PULSAR_BASE_URL/" -m 5 > /dev/null; then
    log "[WARNING] Server responding but health check endpoint unavailable" "$YELLOW"
    return 0
  fi

  log "[ERROR] PulsarRPA server not accessible at $PULSAR_BASE_URL" "$RED"
  log "[HINT] Start PulsarRPA with:" "$CYAN"
  log "    java -DDEEPSEEK_API_KEY=\${DEEPSEEK_API_KEY} -jar PulsarRPA.jar" "$WHITE"
  return 1
}

# =============================================================================
# SECTION: TEST EXECUTION FUNCTIONS
# =============================================================================

run_curl_test() {
  local test_name="$1"
  local curl_command="$2"
  local test_number="$3"

  TOTAL_TESTS=$((TOTAL_TESTS + 1))
  log ""
  log "[TEST $test_number/${#CURL_COMMANDS[@]}] $test_name" "$PURPLE"

  # 显示命令预览
  if [ "$VERBOSE_MODE" = true ]; then
    log "[COMMAND]" "$CYAN"
    echo "$curl_command"
  else
    local short_cmd="${curl_command:0:80}..."
    short_cmd=$(echo "$short_cmd" | tr -d '\n' | tr -d '\r')
    log "[COMMAND] $short_cmd..." "$CYAN"
  fi

  # 替换URL
  local final_command=$(substitute_urls "$curl_command")
  local response_file=$(mktemp)
  local error_file=$(mktemp)
  local meta_file=$(mktemp)

  local full_command="$final_command --max-time $TIMEOUT_SECONDS -w '%{http_code}\n%{time_total}\n%{size_download}\n%{url_effective}' -o \"$response_file\" -s"
  vlog "Executing: ${full_command:0:150}..."

  # 执行命令
  local start_time=$(date +%s)
  local success=false

  eval $full_command > "$meta_file" 2> "$error_file"
  if [ $? -eq 0 ]; then
    success=true
  fi

  local end_time=$(date +%s)
  local duration=$((end_time - start_time))

  if [ "$success" = true ]; then
    local http_status="000"
    local time_total="0.000"
    local size_download="0"
    local url_effective="N/A"

    if [ -f "$meta_file" ]; then
      http_status=$(head -n 1 "$meta_file" 2>/dev/null || echo "000")
      time_total=$(sed -n '2p' "$meta_file" 2>/dev/null || echo "0.000")
      size_download=$(sed -n '3p' "$meta_file" 2>/dev/null || echo "0")
      url_effective=$(sed -n '4p' "$meta_file" 2>/dev/null || echo "N/A")
    fi

    log "[RESPONSE] Status: $http_status | Time: ${time_total}s | Size: ${size_download}B | Duration: ${duration}s" "$BLUE"

    # 检查成功
    if [[ "$http_status" =~ ^[23][0-9][0-9]$ ]]; then
      log "[PASS] ✅ Test completed successfully" "$GREEN"
      PASSED_TESTS=$((PASSED_TESTS + 1))
      cp "$response_file" "$TEST_RESULTS_DIR/test_${test_number}_success.json" 2>/dev/null

      if [ "$size_download" -gt 0 ] && [ "$size_download" -lt 3000 ]; then
        local preview=$(head -c 250 "$response_file" 2>/dev/null | tr -d '\n' | tr -d '\r' | sed 's/  */ /g')
        if [ -n "$preview" ] && [ "$preview" != " " ]; then
          log "[PREVIEW] $preview..." "$CYAN"
        fi
      elif [ "$size_download" -gt 3000 ]; then
        log "[INFO] Large response (${size_download}B) saved to results directory" "$CYAN"
      fi
    else
      log "[FAIL] ❌ HTTP Status: $http_status" "$RED"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      cp "$response_file" "$TEST_RESULTS_DIR/test_${test_number}_error_${http_status}.txt" 2>/dev/null

      if [ -f "$response_file" ]; then
        local error_content=$(head -c 200 "$response_file" 2>/dev/null | tr -d '\n')
        if [ -n "$error_content" ]; then
          log "[ERROR RESPONSE] $error_content" "$RED"
        fi
      fi

      if [ -f "$error_file" ]; then
        local curl_error=$(head -c 200 "$error_file" 2>/dev/null | tr -d '\n')
        if [ -n "$curl_error" ]; then
          log "[CURL ERROR] $curl_error" "$RED"
        fi
      fi
    fi
  else
    log "[FAIL] ❌ Command execution failed" "$RED"
    FAILED_TESTS=$((FAILED_TESTS + 1))

    cat > "$TEST_RESULTS_DIR/test_${test_number}_exec_error.txt" << EOF
Command: $final_command
Error output:
$(cat "$error_file" 2>/dev/null)
EOF

    if [ -f "$error_file" ]; then
      local exec_error=$(head -c 200 "$error_file" 2>/dev/null | tr -d '\n')
      if [ -n "$exec_error" ]; then
        log "[EXECUTION ERROR] $exec_error" "$RED"
      fi
    fi
  fi

  rm -f "$response_file" "$error_file" "$meta_file"
  if [ "$test_number" -lt "${#CURL_COMMANDS[@]}" ]; then
    show_progress $TOTAL_TESTS ${#CURL_COMMANDS[@]}
  fi
}

run_all_tests() {
  log "[INFO] Starting test execution..." "$BLUE"
  log "[INFO] Total commands to test: ${#CURL_COMMANDS[@]}" "$BLUE"

  local test_counter=0
  for command_entry in "${CURL_COMMANDS[@]}"; do
    test_counter=$((test_counter + 1))
    IFS='|' read -r test_name curl_cmd <<< "$command_entry"
    run_curl_test "$test_name" "$curl_cmd" "$test_counter"
    if [ "$FAST_MODE" = false ]; then
      sleep 1
    fi
  done

  echo ""
}

print_summary() {
  log ""
  log "=============================================="
  log "[FINAL SUMMARY] Test Results" "$BLUE"
  log "=============================================="
  log "Test Session: $(date '+%Y-%m-%d %H:%M:%S')" "$BLUE"
  log "User: $USER_NAME" "$BLUE"
  log "Server: $PULSAR_BASE_URL" "$BLUE"
  log "Total Commands: ${#CURL_COMMANDS[@]}" "$BLUE"
  log "Tests Executed: $TOTAL_TESTS" "$BLUE"
  log "Passed: $PASSED_TESTS" "$GREEN"
  log "Failed: $FAILED_TESTS" "$RED"
  log "Skipped: $SKIPPED_TESTS" "$YELLOW"

  if [ $TOTAL_TESTS -gt 0 ]; then
    local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    log "Success Rate: ${success_rate}%" "$BLUE"
  fi

  log "Log File: $LOG_FILE" "$BLUE"
  log "Results Directory: $TEST_RESULTS_DIR" "$BLUE"
  log "=============================================="

  if [ $TOTAL_TESTS -eq 0 ]; then
    log "[INFO] No tests were executed" "$YELLOW"
    exit 0
  elif [ $FAILED_TESTS -eq 0 ]; then
    log "[SUCCESS] All tests passed! 🎉" "$GREEN"
    exit 0
  else
    log "[PARTIAL SUCCESS] Some tests failed. Check logs for details." "$YELLOW"
    exit 1
  fi
}

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTIONS]

Test curl commands from README.md against PulsarRPA server.

OPTIONS:
-u, --url URL         PulsarRPA base URL (default: $DEFAULT_BASE_URL)
-f, --fast            Fast mode - minimal delays between tests
-s, --skip-server     Skip server connectivity check
-t, --timeout SEC     Request timeout in seconds (default: 120)
-v, --verbose         Enable verbose output
-h, --help            Show this help message

EXAMPLES:
$(basename "$0")                              # Run all tests with defaults
$(basename "$0") -u http://localhost:8080     # Use custom server URL
$(basename "$0") -f -t 60                     # Fast mode with 60s timeout
$(basename "$0") -s -v                        # Skip server check with verbose output

REQUIREMENTS:
- curl command available
- PulsarRPA server running (unless --skip-server)

UPDATING COMMANDS:
Edit the CURL_COMMANDS array to add/modify tests.
EOF
}

# =============================================================================
# SECTION: MAIN EXECUTION LOGIC
# =============================================================================

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -u|--url)
        PULSAR_BASE_URL="$2"
        shift 2
        ;;
      -f|--fast)
        FAST_MODE=true
        shift
        ;;
      -s|--skip-server)
        SKIP_SERVER_CHECK=true
        shift
        ;;
      -t|--timeout)
        TIMEOUT_SECONDS="$2"
        shift 2
        ;;
      -v|--verbose)
        VERBOSE_MODE=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "Unknown option: $1"
        usage
        exit 1
        ;;
    esac
  done
}

main() {
  log "[INFO] PulsarRPA Curl Command Test Suite" "$BLUE"
  log "[INFO] User: $USER_NAME" "$BLUE"
  log "[INFO] Timestamp: $(date '+%Y-%m-%d %H:%M:%S')" "$BLUE"
  log "[INFO] Server URL: $PULSAR_BASE_URL" "$BLUE"
  log "[INFO] Timeout: ${TIMEOUT_SECONDS}s" "$BLUE"
  log "[INFO] Fast Mode: $FAST_MODE" "$BLUE"
  log "[INFO] Verbose Mode: $VERBOSE_MODE" "$BLUE"

  if ! command -v curl &> /dev/null; then
    log "[ERROR] curl command not found. Please install curl." "$RED"
    exit 1
  fi

  if [ "$SKIP_SERVER_CHECK" = false ]; then
    if ! check_server; then
      log "[WARNING] Use --skip-server to bypass server check" "$YELLOW"
      exit 1
    fi
  fi

  run_all_tests
  print_summary
}

# 处理Ctrl+C中断
trap 'echo -e "\n${YELLOW}[INFO] Tests interrupted by user${NC}"; exit 130' INT

parse_args "$@"
main