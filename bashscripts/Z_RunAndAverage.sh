#!/bin/bash

# ============================================================
# MASTER BENCHMARK RUNNER (DISTRIBUTED & VERIFIED)
# Description: Runs time measurement benchmarks and verifies outputs.
# Location: esg-with-feature-expressions/bashscripts/
# ============================================================

# Configuration: 
# Since Java code handles WARMUP and MEASURE loops internally, 
# we only need to trigger it ONCE from Bash.
REPEAT_COUNT=1

# --- 1. DYNAMIC SHARD CONFIGURATION ---
if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  
else
  TARGET_SHARDS=30 
fi

# --- 2. EXECUTION RANGE ---
START_SHARD=${1:-0}
END_SHARD=${2:-$((TARGET_SHARDS-1))}

echo "--------------------------------------------------"
echo "🖥️  Detected OS: $OSTYPE"
echo "🔢 Logical Total Shards: $TARGET_SHARDS"
echo "🚀 Assigned Node Range: Shard $START_SHARD to $END_SHARD"
echo "🔄 Bash Repetitions: $REPEAT_COUNT (Java handles internal loops)"
echo "--------------------------------------------------"

# --- 3. PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 1. DEFINE CASES
CASES=(
  "SodaVendingMachine SVM"
  "eMail eM"
  "Elevator El"
  "BankAccountv2 BAv2"
  "StudentAttendanceSystem SAS"
  "Tesla Te"
  "syngovia Svia"
  "HockertyShirts HS" 
)

# 2. DEFINE LEVELS
LEVELS=("L0" "L1" "L2" "L3" "L4")

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

# --- VERIFICATION FUNCTION (Dosya Kontrolü) ---
verify_benchmark_results() {
    local case_name=$1
    local level=$2
    
    # Benchmark sonuçları genelde buraya yazılır:
    local target_dir="shards_timemeasurement"
    local full_path="$PROJECT_ROOT/files/Cases/$case_name/$target_dir"
    
    if [ -d "$full_path" ]; then
        # CSV dosyası var mı bak (Boyutu 0'dan büyük olanlar)
        local count=$(find "$full_path" -type f -name "*.csv" -size +0c | wc -l)
        
        if [ "$count" -gt 0 ]; then
            echo "   ✅ VERIFIED: Found $count CSV result files in $target_dir"
        else
            echo "   ⚠️  WARNING: Folder exists but NO valid CSV files found!"
        fi
    else
        echo "   ❌ ERROR: Output folder NOT created: $target_dir"
    fi
}

# --- MONITORING FUNCTION ---
wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local level=$3
  local error_detected=false

  echo "⏳ Monitoring logs in: $log_dir"

  while pgrep -f "java.*$case_name" > /dev/null; do
    if [ -d "$log_dir" ]; then
        if find "$log_dir" -name "*.log" -mmin -5 -exec grep -E "$ERROR_KEYWORDS" {} + 2>/dev/null | tail -n 1 > error_snippet.tmp; then
            if [ -s error_snippet.tmp ] && [ "$error_detected" = false ]; then
                local msg=$(cat error_snippet.tmp)
                echo -e "\n❌ CRITICAL ERROR detected: $case_name \n$msg"
                error_detected=true
            fi
        fi
    fi
    echo -ne "   ... Benchmarking ($level) Shards $START_SHARD-$END_SHARD ... (Error: $error_detected)\r"
    sleep 10
  done
  
  rm -f error_snippet.tmp
  echo -e "\n✅ PROCESS FINISHED: $case_name ($level)"
  
  # İşlem bitince sonucu doğrula
  verify_benchmark_results "$case_name" "$level"
}

echo "=== STARTING DISTRIBUTED BENCHMARK ==="

for entry in "${CASES[@]}"; do
  set -- $entry
  CASE_NAME=$1
  SHORT_NAME=$2