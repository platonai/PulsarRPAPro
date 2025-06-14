#!/bin/bash

# Test script for curl commands from README.md
# All curl commands are explicitly listed below for easy maintenance
# Author: Auto-generated for platonai
# Date: 2025-06-14 08:00:37

# set -euo pipefail
IFS=$'\n\t'

# =========================
# CURL COMMANDS AND DESCRIPTIONS
# =========================

# System Health Checks (Quick tests first)
readonly CURL_DESC_HEALTH_CHECK="Health Check Endpoint"
readonly CURL_CMD_HEALTH_CHECK='curl -X GET "http://localhost:8182/actuator/health"'

readonly CURL_DESC_QUERY_PARAMS="Query Parameters Test"
readonly CURL_CMD_QUERY_PARAMS='curl -X GET "http://localhost:8182/actuator/health?details=true"'

readonly CURL_DESC_WEBUI="WebUI Command Interface"
readonly CURL_CMD_WEBUI='curl -X GET "http://localhost:8182/command.html"'

readonly CURL_DESC_CUSTOM_HEADERS="Custom Headers Test"
readonly CURL_CMD_CUSTOM_HEADERS='curl -X GET "http://localhost:8182/actuator/health" -H "Accept: application/json" -H "User-Agent: PulsarRPA-Test-Suite/1.0"'

# Simple Data Extraction Tests
readonly CURL_DESC_SIMPLE_LOAD="Simple Page Load Test"
readonly CURL_CMD_SIMPLE_LOAD='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_base_uri(dom) as url,
dom_first_text(dom, '\''title'\'') as page_title
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

readonly CURL_DESC_HTML_PARSE="HTML Parsing Test"
readonly CURL_CMD_HTML_PARSE='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, '\''h1'\'') as heading,
dom_all_texts(dom, '\''p'\'') as paragraphs
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

readonly CURL_DESC_COMPLEX_XSQL="Complex X-SQL Query"
readonly CURL_CMD_COMPLEX_XSQL='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, '\''title'\'') as page_title,
dom_first_text(dom, '\''h1,h2'\'') as main_heading,
dom_base_uri(dom) as base_url
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

readonly CURL_DESC_FORM_DATA="Form Data Test"
readonly CURL_CMD_FORM_DATA='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select dom_first_text(dom, '\''title'\'') as title from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

# Advanced API Tests (Longer running)
readonly CURL_DESC_PLAIN_API="Plain Text Command API - Amazon Product"
readonly CURL_CMD_PLAIN_API='curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

After browser launch: clear browser cookies.
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
"'

readonly CURL_DESC_JSON_API="JSON Command API - Amazon Product"
readonly CURL_CMD_JSON_API='curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '\''{"url": "https://www.amazon.com/dp/B0C1H26C46", "pageSummaryPrompt": "Provide a brief introduction of this product.", "dataExtractionRules": "product name, price, and ratings", "linkExtractionRules": "all links containing /dp/ on the page", "onPageReadyActions": ["click #title", "scroll to the middle"]}'\'''

readonly CURL_DESC_XSQL_LLM="X-SQL API - LLM Data Extraction"
readonly CURL_CMD_XSQL_LLM='curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
llm_extract(dom, '\''product name, price, ratings'\'') as llm_extracted_data,
dom_base_uri(dom) as url,
dom_first_text(dom, '\''#productTitle'\'') as title,
dom_first_slim_html(dom, '\''img:expr(width > 400)'\'') as img
from load_and_select('\''https://www.amazon.com/dp/B0C1H26C46'\'', '\''body'\'');
"'

readonly CURL_DESC_ASYNC_MODE="Async Command Mode Test"
readonly CURL_CMD_ASYNC_MODE='curl -X POST "http://localhost:8182/api/commands/plain?mode=async" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

Extract the page title and all text content.
"'

# Command array with pipe-separated format for easy parsing
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

readonly DEFAULT_BASE_URL="http://localhost:8182"
readonly TEST_RESULTS_DIR="./target/test-results"
readonly TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
readonly LOG_FILE="${TEST_RESULTS_DIR}/curl_tests_${TIMESTAMP}.log"

# Colors
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly BOLD='\033[1m'
readonly NC='\033[0m'

# Default options
PULSAR_BASE_URL="$DEFAULT_BASE_URL"
TIMEOUT_SECONDS=120
FAST_MODE=false
SKIP_SERVER_CHECK=false
VERBOSE_MODE=false
USER_NAME="${USER:-platonai}"

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Ensure results directory exists
mkdir -p "$TEST_RESULTS_DIR"

# =============================================================================
# SECTION: UTILITY FUNCTIONS
# =============================================================================

log() {
    local message="$1"
    local color="${2:-NC}"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local log_message="[$timestamp] $message"

    # Print with color if specified
    if [[ "$color" != "NC" ]]; then
        echo -e "${!color}$log_message${NC}" | tee -a "$LOG_FILE"
    else
        echo "$log_message" | tee -a "$LOG_FILE"
    fi
}

vlog() {
    [[ "$VERBOSE_MODE" == "true" ]] && log "[VERBOSE] $1" "CYAN"
}

show_progress() {
    local current=$1
    local total=$2
    local percent=$(( current * 100 / total ))
    local filled=$(( percent / 2 ))
    local empty=$(( 50 - filled ))

    printf "\r${BLUE}[PROGRESS]${NC} ["
    printf "%*s" $filled | tr ' ' '='
    printf "%*s" $empty | tr ' ' '-'
    printf "] %d%% (%d/%d)" $percent $current $total
}

substitute_urls() {
    local command="$1"
    echo "$command" | sed "s|http://localhost:8182|$PULSAR_BASE_URL|g"
}

check_server() {
    log "Checking PulsarRPA server at $PULSAR_BASE_URL..." "BLUE"

    # Try health check endpoint first
    if curl -s --connect-timeout 5 --max-time 10 "$PULSAR_BASE_URL/actuator/health" >/dev/null 2>&1; then
        log "PulsarRPA server is healthy and responding" "GREEN"
        return 0
    fi

    # Try root endpoint as fallback
    if curl -s --connect-timeout 5 --max-time 10 "$PULSAR_BASE_URL/" >/dev/null 2>&1; then
        log "Server responding but health check endpoint unavailable" "YELLOW"
        return 0
    fi

    log "PulsarRPA server not accessible at $PULSAR_BASE_URL" "RED"
    log "Start PulsarRPA with:" "CYAN"
    log "    java -DDEEPSEEK_API_KEY=\${DEEPSEEK_API_KEY} -jar PulsarRPA.jar" "NC"
    return 1
}

# =============================================================================
# SECTION: TEST EXECUTION FUNCTIONS
# =============================================================================

run_curl_test() {
    local test_name="$1"
    local curl_command="$2"
    local test_number="$3"

    ((TOTAL_TESTS++))
    log ""
    log "[TEST $test_number/${#CURL_COMMANDS[@]}] $test_name" "PURPLE"

    # Show command preview
    if [[ "$VERBOSE_MODE" == "true" ]]; then
        log "[COMMAND]" "CYAN"
        echo "$curl_command"
    else
        local short_cmd=$(echo "$curl_command" | head -c 80 | tr '\n' ' ')
        log "[COMMAND] $short_cmd..." "CYAN"
    fi

    # Substitute URLs in command
    local final_command
    final_command=$(substitute_urls "$curl_command")

    # Create temporary files
    local response_file error_file meta_file
    response_file=$(mktemp)
    error_file=$(mktemp)
    meta_file=$(mktemp)

    local full_command="$final_command --max-time $TIMEOUT_SECONDS -w '%{http_code}\\n%{time_total}\\n%{size_download}\\n%{url_effective}' -o '$response_file' -s"

    vlog "Executing: $(echo "$full_command" | head -c 150)..."

    # Execute the command
    local start_time end_time duration
    start_time=$(date +%s)

    if eval "$full_command" > "$meta_file" 2> "$error_file"; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))

        # Parse curl output
        local http_status="000"
        local time_total="0.000"
        local size_download="0"
        local url_effective="N/A"

        if [[ -f "$meta_file" ]]; then
            {
                read -r http_status
                read -r time_total
                read -r size_download
                read -r url_effective
            } < "$meta_file" 2>/dev/null || true
        fi

        log "[RESPONSE] Status: $http_status | Time: ${time_total}s | Size: ${size_download}B | Duration: ${duration}s" "BLUE"

        # Check success
        if [[ "$http_status" =~ ^[23][0-9][0-9]$ ]]; then
            log "[PASS] ✅ Test completed successfully" "GREEN"
            ((PASSED_TESTS++))
            cp "$response_file" "${TEST_RESULTS_DIR}/test_${test_number}_success.json" 2>/dev/null || true

            # Show response preview
            if [[ "$size_download" -gt 0 && "$size_download" -lt 3000 ]]; then
                local preview
                preview=$(head -c 250 "$response_file" 2>/dev/null | tr -d '\n\r' | sed 's/[[:space:]]\+/ /g')
                [[ -n "$preview" && "$preview" != " " ]] && log "[PREVIEW] $preview..." "CYAN"
            elif [[ "$size_download" -gt 3000 ]]; then
                log "[INFO] Large response (${size_download}B) saved to results directory" "CYAN"
            fi
        else
            log "[FAIL] ❌ HTTP Status: $http_status" "RED"
            ((FAILED_TESTS++))
            cp "$response_file" "${TEST_RESULTS_DIR}/test_${test_number}_error_${http_status}.txt" 2>/dev/null || true

            # Show error details
            if [[ -s "$response_file" ]]; then
                local error_preview
                error_preview=$(head -c 200 "$response_file" 2>/dev/null | tr -d '\n\r')
                log "[ERROR RESPONSE] $error_preview" "RED"
            fi

            if [[ -s "$error_file" ]]; then
                local curl_error
                curl_error=$(head -c 200 "$error_file" 2>/dev/null | tr -d '\n\r')
                log "[CURL ERROR] $curl_error" "RED"
            fi
        fi
    else
        log "[FAIL] ❌ Command execution failed" "RED"
        ((FAILED_TESTS++))

        # Save execution error
        {
            echo "Command: $final_command"
            echo "Error output:"
            cat "$error_file" 2>/dev/null || echo "No error output available"
        } > "${TEST_RESULTS_DIR}/test_${test_number}_exec_error.txt"

        if [[ -s "$error_file" ]]; then
            local exec_error
            exec_error=$(head -c 200 "$error_file" 2>/dev/null | tr -d '\n\r')
            log "[EXECUTION ERROR] $exec_error" "RED"
        fi
    fi

    # Cleanup
    rm -f "$response_file" "$error_file" "$meta_file"

    [[ $test_number -lt ${#CURL_COMMANDS[@]} ]] && show_progress $TOTAL_TESTS ${#CURL_COMMANDS[@]}
}

run_all_tests() {
    log "Starting test execution..." "BLUE"
    log "Total commands to test: ${#CURL_COMMANDS[@]}" "BLUE"

    local test_counter=0
    for command_entry in "${CURL_COMMANDS[@]}"; do
        ((test_counter++))

        # Skip commented commands
        [[ "$command_entry" =~ ^[[:space:]]*# ]] && {
            log "Skipping commented command $test_counter" "YELLOW"
            ((SKIPPED_TESTS++))
            continue
        }

        # Parse description and command
        local description=$(echo "$command_entry" | cut -d'|' -f1)
        local curl_command=$(echo "$command_entry" | cut -d'|' -f2-)

        run_curl_test "$description" "$curl_command" "$test_counter"

        [[ "$FAST_MODE" == "false" ]] && sleep 1
    done

    echo # New line after progress
}

print_summary() {
    local success_rate=0
    [[ $TOTAL_TESTS -gt 0 ]] && success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))

    log ""
    log "=============================================="
    log "[FINAL SUMMARY] Test Results" "BLUE"
    log "=============================================="
    log "Test Session: $(date '+%Y-%m-%d %H:%M:%S UTC')" "BLUE"
    log "User: $USER_NAME" "BLUE"
    log "Server: $PULSAR_BASE_URL" "BLUE"
    log "Total Commands: ${#CURL_COMMANDS[@]}" "BLUE"
    log "Tests Executed: $TOTAL_TESTS" "BLUE"
    log "Passed: $PASSED_TESTS" "GREEN"
    log "Failed: $FAILED_TESTS" "RED"
    log "Skipped: $SKIPPED_TESTS" "YELLOW"
    [[ $TOTAL_TESTS -gt 0 ]] && log "Success Rate: $success_rate%" "BLUE"
    log "Log File: $LOG_FILE" "BLUE"
    log "Results Directory: $TEST_RESULTS_DIR" "BLUE"
    log "=============================================="

    if [[ $TOTAL_TESTS -eq 0 ]]; then
        log "No tests were executed" "YELLOW"
        exit 0
    elif [[ $FAILED_TESTS -eq 0 ]]; then
        log "All tests passed! 🎉" "GREEN"
        exit 0
    else
        log "Some tests failed. Check logs for details." "YELLOW"

        if [[ $success_rate -lt 80 ]]; then
            log "Success rate below 80%. Exiting with failure." "RED"
            exit 1
        else
            exit 0
        fi
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
    $0 --fast --timeout 60          # Fast mode with 60s timeout
    $0 --skip-server --verbose      # Skip server check with verbose output

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
    log "PulsarRPA Curl Command Test Suite" "BLUE"
    log "User: $USER_NAME" "BLUE"
    log "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')" "BLUE"
    log "Server URL: $PULSAR_BASE_URL" "BLUE"
    log "Timeout: ${TIMEOUT_SECONDS}s" "BLUE"
    log "Fast Mode: $FAST_MODE" "BLUE"
    log "Verbose Mode: $VERBOSE_MODE" "BLUE"

    # Check for curl command
    if ! command -v curl &> /dev/null; then
        log "curl command not found. Please install curl." "RED"
        exit 1
    fi

    # Server check
    if [[ "$SKIP_SERVER_CHECK" != "true" ]]; then
        if ! check_server; then
            log "Use --skip-server to bypass server check" "YELLOW"
            exit 1
        fi
    fi

    # Run tests
    run_all_tests
    print_summary
}

# Signal handling
trap 'log "\nTests interrupted by user" "YELLOW"; exit 130' INT TERM

# Execute main flow
parse_args "$@"
main