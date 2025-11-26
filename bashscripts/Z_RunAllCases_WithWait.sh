#!/bin/bash

# ============================================================
# MASTER RUNNER: DISTRIBUTED SHARD ORCHESTRATOR
# Description: Orchestrates the execution of mutation analysis 
#              and test generation scripts across distributed nodes.
# Location: esg-with-feature-expressions/bashscripts/
# ============================================================

# --- 1. DYNAMIC SHARD CONFIGURATION ---
# TARGET_SHARDS represents the total logical partitions of the workload.
# This must remain constant (e.g., 30) across all nodes to ensure 
# consistent mathematical distribution (modulus operations) in Java.
if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  # Local macOS development environment
else
  TARGET_SHARDS=30 # Production environment (Linux Cluster)
fi

# --- 2. EXECUTION RANGE (CLUSTER CONFIGURATION) ---
# Accepts command-line arguments to define the specific workload for this node.
# Usage: ./master_runner.sh [START_NODE] [END_NODE]
# Example: ./master_runner.sh 0 3 (Runs shards 0, 1, 2, and 3)

START_SHARD=${1:-0}                    # Default: Start from shard 0
END_SHARD=${2:-$((TARGET_SHARDS-1))}   # Default: Run until the last shard

echo "--------------------------------------------------"
echo "🖥️  Detected OS: $OSTYPE"
echo "🔢 Logical Total Shards: $TARGET_SHARDS"
echo "🚀 Assigned Node Range: Shard $START_SHARD to $END_SHARD"
echo "--------------------------------------------------"

# --- 3. PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PYTHON_SCRIPT_DIR="${PROJECT_ROOT}/scripts"

# 1. DEFINE CASES (Subject Software Product Lines)
CASES=(
  "SodaVendingMachine SVM"
  "eMail eM"
  "BankAccountv2 BAv2"
  "StudentAttendanceSystem SAS"
  "syngovia Svia"
  "Tesla Te"
  #"HockertyShirts HS"
)

# 2. DEFINE TASK SCRIPTS
SCRIPTS=(
 # "AutomaticProductConfigurationGeneration.sh"
 # "MutantGeneratorEdgeInserter.sh"
  "MutantGeneratorEdgeOmitter.sh"
  "MutantGeneratorEdgeRedirector.sh"
 # "MutantGeneratorEventInserter.sh"
  "MutantGeneratorEventOmitter.sh"
  "ProductESGToEFGFileWriter.sh"
  "TestSequenceRecorder.sh"
)

# Error keywords to monitor in logs
ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

# --- MONITORING FUNCTION ---
wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local error_detected=false

  echo "⏳ Monitoring logs in: $log_dir"

  # Monitor while the Java process for this case is running
  while pgrep -f "java.*$case_name" > /dev/null; do
    if [ -d "$log_dir" ]; then
        # Check for critical errors in recent log files (last 5 minutes)
        if find "$log_dir" -name "run_${case_name}_s*.log" -mmin -5 -exec grep -E "$ERROR_KEYWORDS" {} + 2>/dev/null | tail -n 1 > error_snippet.tmp; then
            if [ -s error_snippet.tmp ] && [ "$error_detected" = false ]; then
                local msg=$(cat error_snippet.tmp)
                echo -e "\n❌ CRITICAL ERROR detected in: $case_name \n$msg"
                error_detected=true
            fi
        fi
    fi
    # Refresh status line
    echo -ne "   ... Processing Shards $START_SHARD-$END_SHARD ... (Error Status: $error_detected)\r"
    sleep 10
  done
  
  rm -f error_snippet.tmp
  echo -e "\n✅ COMPLETED: $case_name"
}

echo "=== STARTING MASTER RUNNER ==="

# --- MAIN EXECUTION LOOP ---
for entry in "${CASES[@]}"; do
  set -- $entry
  CASE_NAME=$1
  SHORT_NAME=$2

  LOG_DIR="${PROJECT_ROOT}/logs/${CASE_NAME}"
  mkdir -p "$LOG_DIR"

  echo "🔷 PROCESSING CASE: $CASE_NAME"

  for SCRIPT_NAME in "${SCRIPTS[@]}"; do
    TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
    
    if [ ! -f "$TARGET_SCRIPT" ]; then
      echo "⚠️  Warning: Script not found: $TARGET_SCRIPT"
      continue
    fi

    echo "▶️  EXECUTING: $SCRIPT_NAME (Range: $START_SHARD - $END_SHARD)"
    
    # Pass the range arguments to the child script
    bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
    
    wait_and_monitor "$CASE_NAME" "$LOG_DIR"
    sleep 2
  done 
done

# --- COMPLETION ---
# Summary generation is skipped here as it requires aggregation of results from all nodes.
echo ""
echo "=================================================="
echo "🏁 ALL TASKS COMPLETED ON THIS NODE ($START_SHARD-$END_SHARD)!"
echo "=================================================="