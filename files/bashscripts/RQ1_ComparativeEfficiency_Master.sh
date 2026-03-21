#!/bin/bash

# ============================================================
# MASTER RUNNER: RQ1 COMPARATIVE EFFICIENCY MASTER
# Description: Orchestrates the execution of Comparative Efficiency 
#              pipelines (ESG-Fx, EFG, RandomWalk) across 
#              distributed nodes for multiple runs.
# ============================================================

if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  
else
  TARGET_SHARDS=40 
fi

START_SHARD=${1:-0}                    
END_SHARD=${2:-$((TARGET_SHARDS-1))}   
TOTAL_RUNS=11

echo "--------------------------------------------------"
echo "Detected OS: $OSTYPE"
echo "Logical Total Shards: $TARGET_SHARDS"
echo "Assigned Node Range: Shard $START_SHARD to $END_SHARD"
echo "Total Independent Runs: $TOTAL_RUNS"
echo "--------------------------------------------------"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"

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

SCRIPTS=(
  "RQ1_ComparativeEfficiency_ESGFx_L1.sh"
  "RQ1_ComparativeEfficiency_ESGFx_L234.sh"
  "RQ1_ComparativeEfficiency_EFG_L234.sh"
  "RQ1_ComparativeEfficiency_RandomWalk.sh"
)

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

verify_results() {
    local case_name=$1
    local script_name=$2
    local run_id=$3
    
    local target_dir="${FILES_DIR}/Cases/$case_name/comparativeEfficiencyTestPipeline"
    
    if [ -d "$target_dir" ]; then
        local count=$(find "$target_dir" -type f -name "*.csv" | wc -l)
        if [ "$count" -gt 0 ]; then
            echo "   VERIFIED: Output CSV files found in $target_dir"
        else
            echo "   WARNING: Folder exists but NO CSV files found in $target_dir"
        fi
    else
        echo "   ERROR: Output folder NOT created: $target_dir"
    fi
}

wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local script_name=$3
  local error_detected=false

  echo "Monitoring logs in: $log_dir"

  while pgrep -f "java.*$case_name" > /dev/null; do
    if [ -d "$log_dir" ]; then
        if find "$log_dir" -type f -name "*.log" -mmin -5 -exec grep -E "$ERROR_KEYWORDS" {} + 2>/dev/null | tail -n 1 > error_snippet.tmp; then
            if [ -s error_snippet.tmp ] && [ "$error_detected" = false ]; then
                local msg=$(cat error_snippet.tmp)
                echo -e "\nCRITICAL ERROR detected in: $case_name \n$msg"
                error_detected=true
            fi
        fi
    fi
    echo -ne "   ... Processing Shards $START_SHARD-$END_SHARD ... (Error Status: $error_detected)\r"
    sleep 10
  done
  
  rm -f error_snippet.tmp
  echo -e "\nPROCESS FINISHED: $case_name ($script_name)"
}

echo "=== STARTING MASTER RUNNER ==="

for RUN_ID in $(seq 1 $TOTAL_RUNS); do
    
    echo ""
    echo "##################################################"
    echo "INITIATING GLOBAL RUN: $RUN_ID / $TOTAL_RUNS"
    echo "##################################################"

    for SCRIPT_NAME in "${SCRIPTS[@]}"; do
        TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
        
        if [ ! -f "$TARGET_SCRIPT" ]; then
          echo "Warning: Script not found: $TARGET_SCRIPT"
          continue
        fi
        
        echo "=================================================="
        echo "STARTING BATCH TASK: $SCRIPT_NAME (Run: $RUN_ID)"
        echo "=================================================="

        for entry in "${CASES[@]}"; do
            set -- $entry
            CASE_NAME=$1
            SHORT_NAME=$2

            LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}"
            mkdir -p "$LOG_DIR"

            echo "PROCESSING CASE: $CASE_NAME"
            echo "EXECUTING: $SCRIPT_NAME (Range: $START_SHARD - $END_SHARD)"
            
            bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$RUN_ID" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
            
            wait_and_monitor "$CASE_NAME" "$LOG_DIR" "$SCRIPT_NAME"
            verify_results "$CASE_NAME" "$SCRIPT_NAME" "$RUN_ID"
            
            sleep 2
        done
    done
done

echo ""
echo "=================================================="
echo "ALL 11 RUNS COMPLETED SUCCESSFULLY ON THIS NODE ($START_SHARD-$END_SHARD)!"
echo "=================================================="