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
#
# USAGE: bash RQ3_RandomWalk_DampingSensivityAnalysis.sh [START_SHARD] [END_SHARD]
# ============================================================

if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4  
else
  TARGET_SHARDS=80 
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

cd "$PROJECT_ROOT" || { echo "CRITICAL ERROR: Project root not found!"; exit 1; }

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

ERROR_KEYWORDS="Exception|Error|FAILURE|Java heap space|AccessDenied"
MASTER_LOG_DIR="${FILES_DIR}/logs"
mkdir -p "$MASTER_LOG_DIR"

echo "=== COMPILING PROJECT ONCE FOR THIS MASTER RUN ==="
cd "$PROJECT_ROOT" || { echo "CRITICAL ERROR: Project root not found!"; exit 1; }

mvn clean package dependency:copy-dependencies -DskipTests -Dfile.encoding=UTF-8 > "${FILES_DIR}/logs/RQ3_Damping_Master_Build_$$.log" 2>&1
if [ $? -ne 0 ]; then
    echo "CRITICAL ERROR: Maven build FAILED! Check logs/RQ3_Damping_Master_Build_$$.log"
    exit 1
fi
echo "=== COMPILATION FINISHED SUCCESSFULLY ==="

export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"

wait_and_monitor() {
  local case_name=$1
  local log_dir=$2
  local script_name=$3
  local error_detected=false

  local error_tmp="${log_dir}/.error_snippet_${case_name}_$$.tmp"

  echo "Monitoring logs in: $log_dir"

  while pgrep -f "java.*Damping.*$case_name" > /dev/null; do
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
    if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
  done
  sleep 3
}

verify_damping_folders() {
    local case_name=$1
    local damping_base="${FILES_DIR}/Cases/${case_name}/testsequences/L0"
    local damping_count=$(find "$damping_base" -maxdepth 1 -type d -name "damping*" 2>/dev/null | wc -l)
    echo "   VERIFIED: $damping_count damping folders found under $damping_base"
}

verify_sensitivity_results() {
    local case_name=$1
    local sensitivity_dir="${FILES_DIR}/Cases/$case_name/faultdetection/sensitivity"
    
    if [ -d "$sensitivity_dir" ]; then
        local csv_count=$(find "$sensitivity_dir" -type f -name "*DampingSensitivity*.csv" | wc -l)
        if [ "$csv_count" -gt 0 ]; then
            echo "   VERIFIED: $csv_count sensitivity CSV files found in $sensitivity_dir"
        else
            echo "   WARNING: Folder exists but NO sensitivity CSV files found"
        fi
    else
        echo "   ERROR: Sensitivity folder NOT created: $sensitivity_dir"
    fi
}

echo "=== STARTING DAMPING SENSITIVITY ANALYSIS ==="

for RUN_ID in $(seq 1 $TOTAL_RUNS); do

    echo ""
    echo "##################################################"
    echo "GLOBAL RUN: $RUN_ID / $TOTAL_RUNS"
    echo "##################################################"

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

        echo "PROCESSING CASE: $CASE_NAME"
        echo "GENERATING: Damping factors {0.80, 0.85, 0.90}"
        
        dispatch_java "$CASE_NAME" "$SHORT_NAME" "$MAIN" "DampingSens_TestGen" "$LOG_DIR"
        wait_and_monitor "$CASE_NAME" "$LOG_DIR" "TestGenerator"
        verify_damping_folders "$CASE_NAME"

        sleep 2
    done

    echo ""
    echo "PHASE 1 COMPLETE: All damping test suites generated."

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

        echo "PROCESSING CASE: $CASE_NAME"
        echo "EXECUTING: Edge Omission (3 damping factors)"
        
        dispatch_java "$CASE_NAME" "$SHORT_NAME" "$MAIN" "DampingSens_EdgeOmit" "$LOG_DIR"
        wait_and_monitor "$CASE_NAME" "$LOG_DIR" "EdgeOmitter"
        verify_sensitivity_results "$CASE_NAME"

        sleep 2
    done

    echo ""
    echo "PHASE 2 COMPLETE: Edge Omission finished."

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

        echo "PROCESSING CASE: $CASE_NAME"
        echo "EXECUTING: Event Omission (3 damping factors)"
        
        dispatch_java "$CASE_NAME" "$SHORT_NAME" "$MAIN" "DampingSens_EventOmit" "$LOG_DIR"
        wait_and_monitor "$CASE_NAME" "$LOG_DIR" "EventOmitter"
        verify_sensitivity_results "$CASE_NAME"

        sleep 2
    done
done
sleep 3

echo ""
echo "=================================================="
echo "DAMPING SENSITIVITY ANALYSIS COMPLETED!"
echo "=================================================="