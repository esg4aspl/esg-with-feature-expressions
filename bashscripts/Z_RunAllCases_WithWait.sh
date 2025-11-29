#!/bin/bash

# ============================================================
# MASTER RUNNER: DISTRIBUTED SHARD ORCHESTRATOR
# Description: Orchestrates the execution of mutation analysis 
#              and test generation scripts across distributed nodes.
# Location: esg-with-feature-expressions/bashscripts/
# ============================================================

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
echo "--------------------------------------------------"

# --- 3. PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 1. DEFINE CASES
CASES=(
  #"SodaVendingMachine SVM"
  #"eMail eM"
  "Elevator El"               # Açık
  #"BankAccountv2 BAv2"
  "StudentAttendanceSystem SAS" # Açık
  "syngovia Svia"             # Açık
  "Tesla Te"                  # Açık
)

# 2. DEFINE TASK SCRIPTS
SCRIPTS=(
  "MutantGeneratorEdgeOmitter.sh"
  #"MutantGeneratorEdgeRedirector.sh" # KAPALI
  "MutantGeneratorEventOmitter.sh"
  "ProductESGToEFGFileWriter.sh"
  "TestSequenceRecorder.sh"
)

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

# --- VERIFICATION FUNCTION ---
# Bu fonksiyon işlem bitince dosyaları sayar
verify_results() {
    local case_name=$1
    local script_name=$2
    
    # Script ismine göre çıktı klasörünü tahmin et
    local target_dir=""
    if [[ "$script_name" == *"EdgeOmitter"* ]]; then target_dir="shards_mutantgenerator_edgeomitter"; fi
    if [[ "$script_name" == *"EventOmitter"* ]]; then target_dir="shards_mutantgenerator_eventomitter"; fi
    if [[ "$script_name" == *"EFG"* ]]; then target_dir="shards_efgfilewriter"; fi
    if [[ "$script_name" == *"TestSequence"* ]]; then target_dir="shards_testsequencegeneration"; fi
    
    local full_path="$PROJECT_ROOT/files/Cases/$case_name/$target_dir"
    
    if [ -d "$full_path" ]; then
        # CSV veya EFG dosyası say
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

  while pgrep -f "java.*$case_name" > /dev/null; do
    if [ -d "$log_dir" ]; then
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
  
  # İşlem bitince hemen sonucu kontrol et
  verify_results "$case_name" "$script_name"
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
    
    # --- SAFETY SKIP: EdgeRedirector ---
    if [[ "$SCRIPT_NAME" == *"EdgeRedirector"* ]]; then
        echo "⏩ SKIPPING: $SCRIPT_NAME (Globally Disabled)"
        continue
    fi

    TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
    
    if [ ! -f "$TARGET_SCRIPT" ]; then
      echo "⚠️  Warning: Script not found: $TARGET_SCRIPT"
      continue
    fi

    echo "▶️  EXECUTING: $SCRIPT_NAME (Range: $START_SHARD - $END_SHARD)"
    
    bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
    
    # Script ismini de gönderiyoruz ki hangi klasöre bakacağını bilsin
    wait_and_monitor "$CASE_NAME" "$LOG_DIR" "$SCRIPT_NAME"
    sleep 2
  done 
done

echo ""
echo "=================================================="
echo "🏁 ALL TASKS COMPLETED ON THIS NODE ($START_SHARD-$END_SHARD)!"
echo "=================================================="