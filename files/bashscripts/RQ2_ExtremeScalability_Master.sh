#!/bin/bash
# ============================================================
# RQ2 EXTREME SCALABILITY MASTER RUNNER
# Runs: ESG-Fx (L1-4) + EFG (L2-4) + Random Walk (L0)
# ============================================================

if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  
else
  TARGET_SHARDS=40 
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
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
echo "Project Root: $PROJECT_ROOT"

# Case studies with short names
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

# RQ2 scripts - ALL THREE APPROACHES
SCRIPTS=(
  "RQ2_ExtremeScalability_L1.sh"           # ESG-Fx L1
  "RQ2_ExtremeScalability_L234.sh"         # ESG-Fx L2,3,4
  "RQ2_ExtremeScalability_EFG_L234.sh"     # EFG L2,3,4 (BASELINE)
  "RQ2_ExtremeScalability_RandomWalk.sh"   # Random Walk L0 (BASELINE)
)

wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local script_name=$3

  echo "⏳ Monitoring $case_name ($script_name)..."

  while pgrep -f "java.*$case_name" > /dev/null; do
    echo -ne "   ... Processing shards $START_SHARD-$END_SHARD ...\r"
    sleep 30
  done
  
  echo -e "\n✅ FINISHED: $case_name ($script_name)"
}

verify_results() {
    local case_name=$1
    local script_name=$2
    local run_id=$3
    
    local target_dir="$PROJECT_ROOT/Cases/$case_name/extremeScalabilityTestPipeline"
    
    if [ -d "$target_dir" ]; then
        local count=$(find "$target_dir" -type f -name "*.csv" | wc -l)
        if [ "$count" -gt 0 ]; then
            echo "   ✅ VERIFIED: $count CSV files in $target_dir"
        else
            echo "   ⚠️  WARNING: No CSV files in $target_dir"
        fi
    else
        echo "   ❌ ERROR: Output folder NOT created: $target_dir"
    fi
}

echo "=== RQ2 EXTREME SCALABILITY STARTED ==="

for RUN_ID in $(seq 1 $SMALL_MEDIUM_RUNS); do
    
    echo ""
    echo "##################################################"
    echo "🏁 GLOBAL RUN: $RUN_ID / $SMALL_MEDIUM_RUNS"
    echo "##################################################"

    for SCRIPT_NAME in "${SCRIPTS[@]}"; do
        TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"
        
        if [ ! -f "$TARGET_SCRIPT" ]; then
          echo "⚠️  Warning: Script not found: $TARGET_SCRIPT"
          continue
        fi
        
        echo "=================================================="
        echo "🚀 BATCH: $SCRIPT_NAME (Run: $RUN_ID)"
        echo "=================================================="

        for entry in "${CASES[@]}"; do
            set -- $entry
            CASE_NAME=$1
            SHORT_NAME=$2

            # --- DYNAMIC RUN LIMITER ---
            # Large SPLs capped at 3 runs
            if [[ "$CASE_NAME" == "Tesla" || "$CASE_NAME" == "syngovia" || "$CASE_NAME" == "HockertyShirts" ]]; then
                if [ "$RUN_ID" -gt $LARGE_RUNS ]; then
                    echo "⏭️  SKIPPING: $CASE_NAME (Large SPL, cap=$LARGE_RUNS, current=$RUN_ID)"
                    continue
                fi
            fi

            LOG_DIR="${PROJECT_ROOT}/logs/ExtremeScalability/${CASE_NAME}"
            mkdir -p "$LOG_DIR"

            echo "🔷 CASE: $CASE_NAME | SCRIPT: $SCRIPT_NAME"
            
            bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME" "$RUN_ID" "$TARGET_SHARDS" "$START_SHARD" "$END_SHARD" > /dev/null 2>&1
            
            wait_and_monitor "$CASE_NAME" "$LOG_DIR" "$SCRIPT_NAME"
            verify_results "$CASE_NAME" "$SCRIPT_NAME" "$RUN_ID"
            
            sleep 2
        done
    done
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