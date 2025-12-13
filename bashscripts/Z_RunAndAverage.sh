#!/bin/bash

# ============================================================
# MASTER BENCHMARK RUNNER (DISTRIBUTED)
# Description: Runs time measurement benchmarks sequentially (5 runs).
# Location: esg-with-feature-expressions/bashscripts/
# ============================================================

# --- 1. CONFIGURATION ---
# Number of times to repeat the experiment (Cold Run)
REPETITIONS=5

if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  
else
  TARGET_SHARDS=40 
fi

# --- 2. EXECUTION RANGE ---
START_SHARD=${1:-0}
END_SHARD=${2:-$((TARGET_SHARDS-1))}

echo "--------------------------------------------------"
echo "🖥️  Detected OS: $OSTYPE"
echo "🔢 Logical Total Shards: $TARGET_SHARDS"
echo "🚀 Assigned Node Range: Shard $START_SHARD to $END_SHARD"
echo "🔄 Repetitions: $REPETITIONS (Sequential Cold Runs)"
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
  #"HockertyShirts HS" 
)

# 2. DEFINE LEVELS
LEVELS=("L0" "L1" "L2" "L3" "L4")

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

# --- VERIFICATION FUNCTION ---
verify_benchmark_results() {
    local case_name=$1
    local level=$2
    
    local target_dir="shards_timemeasurement"
    local full_path="$PROJECT_ROOT/files/Cases/$case_name/$target_dir"
    
    if [ -d "$full_path" ]; then
        # Check if CSV files exist
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
  local run_num=$4
  local error_detected=false

  echo "⏳ Monitoring logs in: $log_dir (Run $run_num)"

  # Wait while Java is running for this specific case
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
    # Just sleep, do not print repetitive lines to keep output clean
    sleep 10
  done
  
  rm -f error_snippet.tmp
  echo -e "\n✅ RUN $run_num FINISHED: $case_name ($level)"
  
  # Verify results after run finishes
  verify_benchmark_results "$case_name" "$level"
}

echo "=== STARTING DISTRIBUTED BENCHMARK ==="

for entry in "${CASES[@]}"; do
  set -- $entry
  CASE_NAME=$1
  SHORT_NAME=$2
  
  LOG_DIR="${PROJECT_ROOT}/logs/${CASE_NAME}"
  mkdir -p "$LOG_DIR"

  echo "🔷 PROCESSING CASE: $CASE_NAME"

  # Iterate through Levels (L0, L1, ...)
  for LEVEL in "${LEVELS[@]}"; do
      
      # Define target script name based on Level
      SCRIPT_NAME="TotalTimeMeasurement_${LEVEL}.sh"
      TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
      
      if [ ! -f "$TARGET_SCRIPT" ]; then
          echo "⏩ SKIPPING $LEVEL: Script not found ($SCRIPT_NAME)"
          continue
      fi

      echo "▶️  LEVEL: $LEVEL | Script: $SCRIPT_NAME"

      # --- REPETITION LOOP (COLD RUN) ---
      for (( run=1; run<=REPETITIONS; run++ )); do
          
          echo "   🔄 Starting Run #$run / $REPETITIONS"
          
          # 1. Execute the script (It starts Java in background via nohup)
          bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
          
          # 2. Wait for completion before starting the next run
          wait_and_monitor "$CASE_NAME" "$LOG_DIR" "$LEVEL" "$run"
          
          # Optional: Short cool down between runs
          sleep 2
      done
      
  done
  echo "--------------------------------------------------"
done

echo "🏁 ALL BENCHMARKS COMPLETED!"