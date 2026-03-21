#!/bin/bash

# ============================================================
# MASTER RUNNER: RQ3 FAULT DETECTION MASTER
# Description: Orchestrates execution of Fault Detection 
#              pipelines (Event Omission, Edge Omission).
#              Runs ONLY 1 TIME as it is deterministic.
#
# Phase 1: Generate multi-seed Random Walk test suites (10 seeds)
# Phase 2: Run fault detection (Edge Omission, Event Omission)
#          — deterministic approaches + multi-seed RW
# ============================================================

if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  
else
  TARGET_SHARDS=40 
fi

START_SHARD=${1:-0}                    
END_SHARD=${2:-$((TARGET_SHARDS-1))}   

TOTAL_RUNS=1

echo "--------------------------------------------------"
echo "Detected OS: $OSTYPE"
echo "Logical Total Shards: $TARGET_SHARDS"
echo "Assigned Node Range: Shard $START_SHARD to $END_SHARD"
echo "Total Independent Runs: $TOTAL_RUNS"
echo "--------------------------------------------------"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

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

SCRIPTS_PHASE1=(
  "RQ3_RandomWalkMultiSeed_TestGenerator.sh"
)

SCRIPTS_PHASE2=(
  "RQ3_FaultDetection_EventOmitter.sh"
  "RQ3_FaultDetection_EdgeOmitter.sh"
)

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

MASTER_LOG_DIR="${FILES_DIR}/logs"
mkdir -p "$MASTER_LOG_DIR"

echo "=== COMPILING PROJECT ONCE ==="
cd "$PROJECT_ROOT" || exit 1
mvn clean package dependency:copy-dependencies -DskipTests > "$MASTER_LOG_DIR/RQ3_master_build.log" 2>&1
echo "=== COMPILATION FINISHED ==="


verify_results() {
    local case_name=$1
    local script_name=$2
    local run_id=$3
    
    local target_dir="${FILES_DIR}/Cases/$case_name/faultdetection"
    
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

verify_seed_folders() {
    local case_name=$1
    
    local seed_base="${FILES_DIR}/Cases/$case_name/testsequences/L0"
    local seed_count=$(find "$seed_base" -maxdepth 1 -type d -name "seed*" 2>/dev/null | wc -l)
    echo "   VERIFIED: $seed_count seed folders found under $seed_base"
}

wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local script_name=$3
  local error_detected=false

  # FIX: Use a unique tmp file per case to avoid concurrent overwrites across cases
  local error_tmp="${log_dir}/.error_snippet_${case_name}.tmp"

  echo "Monitoring logs in: $log_dir"

  # FIX: Initial sleep to allow nohup java processes to actually start before first pgrep check
  sleep 3

  while pgrep -f "java.*${case_name}.*RQ3" > /dev/null; do
    if [ -d "$log_dir" ]; then
        if find "$log_dir" -type f -name "*.log" -mmin -5 -exec grep -E "$ERROR_KEYWORDS" {} + 2>/dev/null | tail -n 1 > "$error_tmp"; then
            if [ -s "$error_tmp" ] && [ "$error_detected" = false ]; then
                local msg=$(cat "$error_tmp")
                echo -e "\nCRITICAL ERROR detected in: $case_name \n$msg"
                error_detected=true
            fi
        fi
    fi
    echo -ne "   ... Processing Shards $START_SHARD-$END_SHARD ... (Error Status: $error_detected)\r"
    sleep 10
  done
  
  rm -f "$error_tmp"
  echo -e "\nPROCESS FINISHED: $case_name ($script_name)"
}

echo "=== STARTING RQ3 MASTER RUNNER ==="

for RUN_ID in $(seq 1 $TOTAL_RUNS); do
    
    echo ""
    echo "##################################################"
    echo "INITIATING GLOBAL RUN: $RUN_ID / $TOTAL_RUNS"
    echo "##################################################"

    # ======================================================
    # PHASE 1: GENERATE MULTI-SEED RANDOM WALK TEST SUITES
    # ======================================================
    for SCRIPT_NAME in "${SCRIPTS_PHASE1[@]}"; do
        TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
        
        if [ ! -f "$TARGET_SCRIPT" ]; then
          echo "Warning: Script not found: $TARGET_SCRIPT"
          continue
        fi
        
        echo ""
        echo "=================================================="
        echo "PHASE 1: $SCRIPT_NAME (Run: $RUN_ID)"
        echo "=================================================="

        for entry in "${CASES[@]}"; do
            set -- $entry
            CASE_NAME=$1
            SHORT_NAME=$2

            LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ3/MultiSeedRW"
            mkdir -p "$LOG_DIR"

            echo "PROCESSING CASE: $CASE_NAME"
            echo "EXECUTING: $SCRIPT_NAME (Range: $START_SHARD - $END_SHARD)"
            
            bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$RUN_ID" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
            
            wait_and_monitor "$CASE_NAME" "$LOG_DIR" "$SCRIPT_NAME"
            verify_seed_folders "$CASE_NAME"
            
            sleep 2
        done
    done

    echo ""
    echo "PHASE 1 COMPLETE: All multi-seed RW test suites generated."

    # ======================================================
    # PHASE 2: FAULT DETECTION (EDGE + EVENT OMISSION)
    # ======================================================
    for SCRIPT_NAME in "${SCRIPTS_PHASE2[@]}"; do
        TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
        
        if [ ! -f "$TARGET_SCRIPT" ]; then
          echo "Warning: Script not found: $TARGET_SCRIPT"
          continue
        fi
        
        echo ""
        echo "=================================================="
        echo "PHASE 2: $SCRIPT_NAME (Run: $RUN_ID)"
        echo "=================================================="

        for entry in "${CASES[@]}"; do
            set -- $entry
            CASE_NAME=$1
            SHORT_NAME=$2

            if [[ "$SCRIPT_NAME" == *"EventOmitter"* ]]; then
                LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ3/EventOmitter"
            else
                LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ3/EdgeOmitter"
            fi
            
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
echo "ALL RQ3 RUNS COMPLETED SUCCESSFULLY ON THIS NODE ($START_SHARD-$END_SHARD)!"
echo "=================================================="