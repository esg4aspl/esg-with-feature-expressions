#!/bin/bash

# ============================================================
# MASTER RUNNER: DISTRIBUTED SHARD ORCHESTRATOR (CUSTOM FLOW)
# Description: 1. Runs EdgeOmitter & Recorder ONLY for Elevator
#              2. Runs EventOmitter for ALL cases
# ============================================================

# --- 1. DYNAMIC SHARD CONFIGURATION ---
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
echo "--------------------------------------------------"

# --- 3. PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

# --- VERIFICATION FUNCTION ---
verify_results() {
    local case_name=$1
    local script_name=$2
    
    local target_dir=""
    if [[ "$script_name" == *"EdgeOmitter"* ]]; then target_dir="shards_mutantgenerator_edgeomitter"; fi
    if [[ "$script_name" == *"EventOmitter"* ]]; then target_dir="shards_mutantgenerator_eventomitter"; fi
    if [[ "$script_name" == *"EFG"* ]]; then target_dir="shards_efgfilewriter"; fi
    if [[ "$script_name" == *"TestSequence"* ]]; then target_dir="shards_testsequencegeneration"; fi
    
    local full_path="$PROJECT_ROOT/files/Cases/$case_name/$target_dir"
    
    if [ -d "$full_path" ]; then
        local count=$(find "$full_path" -type f \( -name "*.csv" -o -name "*.EFG" \) | wc -l)
        if [ "$count" -gt 0 ]; then
            echo "   ✅ VERIFIED: $count output files found in $target_dir"
        else
            echo "   ⚠️  WARNING: Folder exists but NO files found in $target_dir"
        fi
    else
        echo "   ❌ ERROR: Output folder NOT created: $target_dir"
    fi
}

# --- MONITORING FUNCTION ---
wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local script_name=$3
  local error_detected=false

  echo "⏳ Monitoring logs in: $log_dir"

  # Wait while the specific case java process is running
  while pgrep -f "java.*$case_name" > /dev/null; do
    if [ -d "$log_dir" ]; then
        # Check logs modified in the last 5 minutes for errors (Original Pattern Kept)
        if find "$log_dir" -name "run_${case_name}_s*.log" -mmin -5 -exec grep -E "$ERROR_KEYWORDS" {} + 2>/dev/null | tail -n 1 > error_snippet.tmp; then
            if [ -s error_snippet.tmp ] && [ "$error_detected" = false ]; then
                local msg=$(cat error_snippet.tmp)
                echo -e "\n❌ CRITICAL ERROR detected in: $case_name \n$msg"
                error_detected=true
            fi
        fi
    fi
    echo -ne "   ... Processing Shards $START_SHARD-$END_SHARD ... (Error Status: $error_detected)\r"
    sleep 10
  done
  
  rm -f error_snippet.tmp
  echo -e "\n✅ PROCESS FINISHED: $case_name ($script_name)"
  
  verify_results "$case_name" "$script_name"
}

# --- CORE EXECUTION HELPER ---
# Wrapper to run the script and monitor logs without changing logic
execute_task() {
    local SCRIPT_NAME=$1
    local CASE_ENTRY=$2

    set -- $CASE_ENTRY
    local CASE_NAME=$1
    local SHORT_NAME=$2
    local TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
    local LOG_DIR="${PROJECT_ROOT}/logs/${CASE_NAME}"

    # Check if script exists
    if [ ! -f "$TARGET_SCRIPT" ]; then
      echo "⚠️  Warning: Script not found: $TARGET_SCRIPT"
      return
    fi

    mkdir -p "$LOG_DIR"

    echo "=================================================="
    echo "🔷 PROCESSING CASE: $CASE_NAME"
    echo "▶️  EXECUTING: $SCRIPT_NAME"
    echo "=================================================="
    
    # Original Execution Command Preserved
    bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
    
    # Monitor using the exact same logic
    wait_and_monitor "$CASE_NAME" "$LOG_DIR" "$SCRIPT_NAME"
    sleep 2
}

echo "=== STARTING MASTER RUNNER (CUSTOM SEQUENCE) ==="

# ============================================================
# PHASE 1: ELEVATOR SPECIAL TASKS
# Run EdgeOmitter & Recorder ONLY for Elevator
# ============================================================
echo ""
echo "##################################################"
echo "### PHASE 1: ELEVATOR SPECIFIC TASKS           ###"
echo "##################################################"

ELEVATOR_CASE="Elevator El"

# 1.1 Run Edge Omitter for Elevator
execute_task "MutantGeneratorEdgeOmitter.sh" "$ELEVATOR_CASE"

# 1.2 Run Test Sequence Recorder for Elevator
execute_task "TestSequenceRecorder.sh" "$ELEVATOR_CASE"


# ============================================================
# PHASE 2: EVENT OMITTER FOR ALL CASES
# Run EventOmitter for the full list
# ============================================================
echo ""
echo "##################################################"
echo "### PHASE 2: EVENT OMITTER FOR ALL CASES       ###"
echo "##################################################"

EVENT_OMITTER_CASES=(
  "SodaVendingMachine SVM"
  "eMail eM"
  "Elevator El"
  "BankAccountv2 BAv2"
  "StudentAttendanceSystem SAS"
  "Tesla Te"
)

SCRIPT_EVENT_OMITTER="MutantGeneratorEventOmitter.sh"

for CASE_ENTRY in "${EVENT_OMITTER_CASES[@]}"; do
    execute_task "$SCRIPT_EVENT_OMITTER" "$CASE_ENTRY"
done

echo ""
echo "=================================================="
echo "🏁 ALL TASKS COMPLETED ON THIS NODE ($START_SHARD-$END_SHARD)!"
echo "=================================================="