#!/bin/bash
# ============================================================
# RQ2 EXTREME SCALABILITY MASTER RUNNER
# Runs: ESG-Fx (L1-4) + EFG (L2-4) + Random Walk (L0)
# ============================================================

if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  
else
  TARGET_SHARDS=80 
fi

START_SHARD=${1:-0}                    
END_SHARD=${2:-$((TARGET_SHARDS-1))}   

# Repetition counts
SMALL_MEDIUM_RUNS=11  # Small/Medium SPLs
LARGE_RUNS=3          # Large SPLs (Tesla, syngo.via, HockertyShirts)

echo "--------------------------------------------------"
echo "🖥️  RQ2 EXTREME SCALABILITY MASTER RUNNER"
echo "🖥️  OS: $OSTYPE"
echo "🔢 Total Shards: $TARGET_SHARDS"
echo "🚀 Node Range: Shard $START_SHARD to $END_SHARD"
echo "🔄 Runs: Small/Medium=$SMALL_MEDIUM_RUNS, Large=$LARGE_RUNS"
echo "--------------------------------------------------"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

cd "$PROJECT_ROOT" || { echo "CRITICAL ERROR: Project root not found!"; exit 1; }

# Case studies with short names
CASES=(
  "SodaVendingMachine SVM"
  "eMail eM"
  "Elevator El"
  "BankAccountv2 BAv2"
  "StudentAttendanceSystem SAS"
  #"Tesla Te"
  #"syngovia Svia"
  "HockertyShirts HS"
)

# RQ2 scripts - ALL THREE APPROACHES
SCRIPTS=(
  "RQ2_ExtremeScalability_RandomWalk.sh"   # Random Walk L0 (BASELINE)
  "RQ2_ExtremeScalability_L1.sh"           # ESG-Fx L1
  #"RQ2_ExtremeScalability_L234.sh"         # ESG-Fx L2,3,4
  #"RQ2_ExtremeScalability_EFG_L234.sh"     # EFG L2,3,4 (BASELINE)
)

echo "=== COMPILING PROJECT ONCE FOR THIS MASTER RUN ==="
cd "$PROJECT_ROOT" || { echo "CRITICAL ERROR: Project root not found!"; exit 1; }

mvn clean package dependency:copy-dependencies -DskipTests -Dfile.encoding=UTF-8 >  "${FILES_DIR}/logs/RQ2_Master_Build_$$.log" 2>&1
if [ $? -ne 0 ]; then
    echo "CRITICAL ERROR: Maven build FAILED! See logs/RQ2_Master_Build_$$.log"
    exit 1
fi
echo "=== COMPILATION FINISHED SUCCESSFULLY ==="

wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local script_name=$3
  local error_detected=false

  local tmp_file="error_snippet_${case_name}_$$.tmp"

  echo "Monitoring logs in: $log_dir"

  while pgrep -f "java.*$case_name" > /dev/null; do
    if [ -d "$log_dir" ]; then
        if find "$log_dir" -type f -name "*.log" -mmin -5 -exec grep -E "$ERROR_KEYWORDS" {} + 2>/dev/null | tail -n 1 > "$tmp_file"; then
            if [ -s "$tmp_file" ] && [ "$error_detected" = false ]; then
                local msg=$(cat "$tmp_file")
                echo -e "\nCRITICAL ERROR detected in: $case_name \n$msg"
                error_detected=true
            fi
        fi
    fi
    echo -ne "   ... Processing Shards $START_SHARD-$END_SHARD ... (Error Status: $error_detected)\r"
    sleep 10
  done
  
  rm -f "$tmp_file"
  echo -e "\nPROCESS FINISHED: $case_name ($script_name)"
}

verify_results() {
    local case_name=$1
    local script_name=$2
    local run_id=$3
    
    local target_dir="$PROJECT_ROOT/Cases/$case_name/extremeScalabilityTestPipeline"
    
    if [ -d "$target_dir" ]; then
        local count=$(find "$target_dir" -type f -name "*.csv" | wc -l)
        if [ "$count" -gt 0 ]; then
            echo "✅ VERIFIED: $count CSV files in $target_dir"
        else
            echo "⚠️  WARNING: No CSV files in $target_dir"
        fi
    else
        echo "❌ ERROR: Output folder NOT created: $target_dir"
    fi
}

echo "=== RQ2 EXTREME SCALABILITY STARTED ==="

for entry in "${CASES[@]}"; do
    set -- $entry
    CASE_NAME=$1
    SHORT_NAME=$2

    # Determine max runs for this case
    if [[ "$CASE_NAME" == "Tesla" || "$CASE_NAME" == "syngovia" || "$CASE_NAME" == "HockertyShirts" ]]; then
        MAX_RUNS=$LARGE_RUNS
    else
        MAX_RUNS=$SMALL_MEDIUM_RUNS
    fi

    echo ""
    echo "############################################################"
    echo "  CASE: $CASE_NAME ($MAX_RUNS runs)"
    echo "############################################################"

    for SCRIPT_NAME in "${SCRIPTS[@]}"; do
        TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
        
        if [ ! -f "$TARGET_SCRIPT" ]; then
          echo "⚠️  Warning: Script not found: $TARGET_SCRIPT"
          continue
        fi

        echo ""
        echo "  === $SCRIPT_NAME ==="

        for RUN_ID in $(seq 1 $MAX_RUNS); do

            LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ2"
            mkdir -p "$LOG_DIR"

            echo "    Run $RUN_ID/$MAX_RUNS"
            
            bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$RUN_ID" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
            
            wait_and_monitor "$CASE_NAME" "$LOG_DIR" "$SCRIPT_NAME"
            verify_results "$CASE_NAME" "$SCRIPT_NAME" "$RUN_ID"
            
            if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
        done
    done

    echo "  $CASE_NAME COMPLETE"
done

echo ""
echo "=================================================="
echo "🏆 RQ2 EXTREME SCALABILITY COMPLETE!"
echo "=================================================="
echo ""
echo "📊 Next steps:"
echo "   1. Run aggregation: python RQ2_aggregate_results.py <SPL_dir> <SPL_name>"
echo "   2. Example: python RQ2_aggregate_results.py Cases/Tesla/extremeScalabilityTestPipeline Tesla"
echo ""
sleep 3