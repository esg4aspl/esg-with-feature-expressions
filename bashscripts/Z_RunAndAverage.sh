#!/bin/bash

# ============================================================
# MASTER BENCHMARK RUNNER (DISTRIBUTED CLUSTER MODE)
# Description: Orchestrates the execution of time measurement
#              benchmarks across distributed nodes.
# Location: esg-with-feature-expressions/bashscripts/
# ============================================================

# Configuration: Repeat execution 10 times to ensure statistical significance
REPEAT_COUNT=10

# --- 1. CLUSTER & SHARD CONFIGURATION ---

# TARGET_SHARDS: Total logical partitions (Context). Must be 30 for consistency.
if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  # Local Debug
else
  TARGET_SHARDS=30 # Production Cluster
fi

# EXECUTION RANGE: Determines which shards run on THIS specific node.
# Usage: ./master_benchmark_runner.sh [START_NODE] [END_NODE]
START_SHARD=${1:-0}
END_SHARD=${2:-$((TARGET_SHARDS-1))}

echo "--------------------------------------------------"
echo "🖥️  OS Detected: $OSTYPE"
echo "🔢 Logical Total Shards: $TARGET_SHARDS"
echo "🚀 Assigned Node Range: Shard $START_SHARD to $END_SHARD"
echo "🔄 Repetitions per Level: $REPEAT_COUNT"
echo "--------------------------------------------------"

# --- 2. PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PYTHON_SCRIPT_DIR="${PROJECT_ROOT}/scripts"

# 3. DEFINE SUBJECT CASES
CASES=(
  "SodaVendingMachine SVM"
  "eMail eM"
  "Elevator El"
  "BankAccountv2 BAv2"
  "StudentAttendanceSystem SAS"
  "syngovia Svia"
  "Tesla Te"
  #"HockertyShirts HS"
)

# 4. DEFINE MEASUREMENT LEVELS
LEVELS=("L0" "L1" "L2" "L3" "L4")
ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

# --- MONITORING FUNCTION ---
wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local error_detected=false

  # Wait while any Java process related to this case is running
  while pgrep -f "java.*$case_name" > /dev/null; do
    if [ -d "$log_dir" ]; then
        # Check for critical errors in the last 5 minutes of logs
        if find "$log_dir" -name "*.log" -mmin -5 -exec grep -E "$ERROR_KEYWORDS" {} + 2>/dev/null | tail -n 1 > error_snippet.tmp; then
            if [ -s error_snippet.tmp ] && [ "$error_detected" = false ]; then
                local msg=$(cat error_snippet.tmp)
                echo -e "\n❌ CRITICAL ERROR detected: $case_name \n$msg"
                error_detected=true
            fi
        fi
    fi
    echo -ne "   ... Benchmarking Shards $START_SHARD-$END_SHARD ... (Error Status: $error_detected)\r"
    sleep 10
  done
  rm -f error_snippet.tmp
  echo -e "\n✅ DONE: Iteration completed for $case_name."
}

echo "=== STARTING DISTRIBUTED BENCHMARK ==="

for entry in "${CASES[@]}"; do
  set -- $entry
  CASE_NAME=$1
  SHORT_NAME=$2

  LOG_DIR="${PROJECT_ROOT}/logs/${CASE_NAME}"
  mkdir -p "$LOG_DIR"
  
  REPORT_FILE="${LOG_DIR}/BenchmarkResults_TotalTimeMeasurement_${CASE_NAME}_Node_${START_SHARD}_${END_SHARD}.txt"
  echo "BENCHMARK RESULTS (Node: $START_SHARD-$END_SHARD): $CASE_NAME" > "$REPORT_FILE"

  echo "📂 PROCESSING CASE: $CASE_NAME ($SHORT_NAME)"

  for LEVEL in "${LEVELS[@]}"; do
    SCRIPT_NAME="${SCRIPT_DIR}/TotalTimeMeasurement_${LEVEL}.sh"
    
    if [ ! -f "$SCRIPT_NAME" ]; then
      echo "⚠️  WARNING: Script $SCRIPT_NAME not found. Skipping."
      continue
    fi

    echo "   🔹 Benchmarking Level: $LEVEL..."
    
    # Clean up old CSVs/Logs to prevent data contamination
    # Note: On a cluster, this only cleans local files.
    find "$PROJECT_ROOT" -name "*${SHORT_NAME}*.csv" -delete 2>/dev/null
    rm -f "${LOG_DIR}/run_time${LEVEL}_s*.log"

    # --- REPETITION LOOP ---
    for (( i=1; i<=REPEAT_COUNT; i++ )); do
      echo "      ▶️  Run $i / $REPEAT_COUNT ..."
      
      # Execute Child Script with RANGE arguments
      bash "$SCRIPT_NAME" "$CASE_NAME" "$SHORT_NAME" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
      
      wait_and_monitor "$CASE_NAME" "$LOG_DIR"
      sleep 1
    done
    echo "" 

    # --- LOCAL AVERAGE CALCULATION ---
    # Note: This calculates the average ONLY for the shards running on this machine.
    # A global average requires merging CSVs from all 8 nodes after downloading.
    TOTAL_AVG=0
    FILE_FOUND=false
    for file in $(find "$PROJECT_ROOT" -name "*${SHORT_NAME}*.csv"); do
        if [ -s "$file" ]; then
            AVG=$(grep -v "SPL Name" "$file" | cut -d';' -f3 | tr ',' '.' | awk '{ sum += $1; n++ } END { if (n > 0) print sum / n; else print 0 }')
            TOTAL_AVG=$AVG
            FILE_FOUND=true
            break 
        fi
    done

    if [ "$FILE_FOUND" = true ]; then
        echo "      ✅ $LEVEL Local Average: $TOTAL_AVG ms"
        echo "$LEVEL | $TOTAL_AVG" >> "$REPORT_FILE"
    else
        echo "      ❌ Result file not found for $LEVEL (on this node)."
        echo "$LEVEL | ERROR" >> "$REPORT_FILE"
    fi
  done
done

echo ""
echo "=================================================="
echo "🏁 BENCHMARK COMPLETED ON THIS NODE ($START_SHARD-$END_SHARD)!"
echo "⚠️  REMINDER: Merge CSV files from all nodes for global results."
echo "=================================================="