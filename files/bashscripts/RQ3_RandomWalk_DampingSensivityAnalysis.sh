#!/bin/bash

# ============================================================
# MASTER RUNNER: RQ3 DAMPING SENSITIVITY ANALYSIS
# Description: Generates Random Walk tests with damping factors
#              {0.80, 0.85, 0.90} and runs fault detection on
#              3 representative SPLs (small, medium, large).
#
# Phase 1: Generate RW test suites for 3 damping factors
# Phase 2: Edge Omission fault detection per damping factor
# Phase 3: Event Omission fault detection per damping factor
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
echo "RQ3 DAMPING SENSITIVITY ANALYSIS"
echo "Detected OS: $OSTYPE"
echo "Logical Total Shards: $TARGET_SHARDS"
echo "Assigned Node Range: Shard $START_SHARD to $END_SHARD"
echo "--------------------------------------------------"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

# Only 3 representative SPLs: small, medium, large
CASES=(
  "Elevator El"
  "BankAccountv2 BAv2"
  "Tesla Te"
)

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"

wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local script_name=$3
  local error_detected=false

  echo "Monitoring logs in: $log_dir"

  while pgrep -f "java.*Damping.*$case_name" > /dev/null; do
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

dispatch_java() {
  local case_name=$1
  local short_name=$2
  local main_class=$3
  local log_prefix=$4
  local log_dir=$5

  if [[ "$OSTYPE" == "darwin"* ]]; then 
      XMS=512m; XMX=1200m 
  else 
      XMS=512m; XMX=1500m 
  fi

  JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

  for i in $(seq $START_SHARD $END_SHARD); do
    LOG="${log_dir}/${log_prefix}_shard$(printf "%02d" $i).log"

    N_SHARDS=$TARGET_SHARDS SHARD=$i \
    nohup java $JAVA_OPTS -cp "$CP" "$main_class" > "$LOG" 2>&1 &

    echo "   Shard $i dispatched -> $LOG"
    sleep 0.2
  done
}

echo "=== STARTING DAMPING SENSITIVITY ANALYSIS ==="

cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found"; exit 1; }

mvn clean package dependency:copy-dependencies -DskipTests > "${FILES_DIR}/logs/DampingSensitivity_build.log" 2>&1
export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"

for RUN_ID in $(seq 1 $TOTAL_RUNS); do

    echo ""
    echo "##################################################"
    echo "GLOBAL RUN: $RUN_ID / $TOTAL_RUNS"
    echo "##################################################"

    # ======================================================
    # PHASE 1: GENERATE TESTS FOR 3 DAMPING FACTORS
    # ======================================================
    echo ""
    echo "=================================================="
    echo "PHASE 1: TEST GENERATION (3 Damping Factors)"
    echo "=================================================="

    for entry in "${CASES[@]}"; do
        set -- $entry
        CASE_NAME=$1
        SHORT_NAME=$2

        LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ3/DampingSensitivity"
        mkdir -p "$LOG_DIR"

        MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ3_DampingSensitivity_TestGenerator_${SHORT_NAME}"

        echo "GENERATING: $CASE_NAME (Damping: 0.80, 0.85, 0.90)"
        dispatch_java "$CASE_NAME" "$SHORT_NAME" "$MAIN" "DampingSens_TestGen" "$LOG_DIR"
        wait_and_monitor "$CASE_NAME" "$LOG_DIR" "TestGenerator"

        # Verify damping folders
        DAMPING_BASE="${FILES_DIR}/Cases/${CASE_NAME}/testsequences/L0"
        DAMPING_COUNT=$(find "$DAMPING_BASE" -maxdepth 1 -type d -name "damping*" 2>/dev/null | wc -l)
        echo "   VERIFIED: $DAMPING_COUNT damping folders found"

        sleep 2
    done

    echo ""
    echo "PHASE 1 COMPLETE."

    # ======================================================
    # PHASE 2: EDGE OMISSION FAULT DETECTION
    # ======================================================
    echo ""
    echo "=================================================="
    echo "PHASE 2: EDGE OMISSION (3 Damping Factors)"
    echo "=================================================="

    for entry in "${CASES[@]}"; do
        set -- $entry
        CASE_NAME=$1
        SHORT_NAME=$2

        LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ3/DampingSensitivity"
        MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_${SHORT_NAME}"

        echo "EDGE OMISSION: $CASE_NAME"
        dispatch_java "$CASE_NAME" "$SHORT_NAME" "$MAIN" "DampingSens_EdgeOmit" "$LOG_DIR"
        wait_and_monitor "$CASE_NAME" "$LOG_DIR" "EdgeOmitter"

        sleep 2
    done

    # ======================================================
    # PHASE 3: EVENT OMISSION FAULT DETECTION
    # ======================================================
    echo ""
    echo "=================================================="
    echo "PHASE 3: EVENT OMISSION (3 Damping Factors)"
    echo "=================================================="

    for entry in "${CASES[@]}"; do
        set -- $entry
        CASE_NAME=$1
        SHORT_NAME=$2

        LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ3/DampingSensitivity"
        MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ3_DampingSensitivity_FaultDetection_EventOmitter_${SHORT_NAME}"

        echo "EVENT OMISSION: $CASE_NAME"
        dispatch_java "$CASE_NAME" "$SHORT_NAME" "$MAIN" "DampingSens_EventOmit" "$LOG_DIR"
        wait_and_monitor "$CASE_NAME" "$LOG_DIR" "EventOmitter"

        sleep 2
    done
done

echo ""
echo "=================================================="
echo "DAMPING SENSITIVITY ANALYSIS COMPLETED!"
echo ""
echo "Results per SPL:"
echo "  Test gen summary: .../faultdetection/sensitivity/*_DampingSensitivity_TestGen_*.csv"
echo "  Edge omission:    .../faultdetection/sensitivity/*_DampingSensitivity_EdgeOmission_*.csv"
echo "  Event omission:   .../faultdetection/sensitivity/*_DampingSensitivity_EventOmission_*.csv"
echo ""
echo "Each CSV has a DampingFactor column — pivot by this to build the sensitivity table."
echo "=================================================="