#!/bin/bash

# ============================================================
# MASTER RUNNER: RQ0 PRODUCT GENERATION MASTER
# Description: Orchestrates the generation of all EFG and DOT 
#              files locally without any sharding.
#              Runs ONLY 1 TIME.
# ============================================================

TOTAL_RUNS=1

echo "--------------------------------------------------"
echo "Detected OS: $OSTYPE"
echo "Execution Mode: NO SHARDING (Full Local Generation)"
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

SCRIPT_NAME="RQ0_ProductGeneration.sh"
TARGET_SCRIPT="${SCRIPT_DIR}/${SCRIPT_NAME}"

if [ ! -f "$TARGET_SCRIPT" ]; then
    echo "ERROR: Script not found: $TARGET_SCRIPT"
    exit 1
fi

echo "=== COMPILING PROJECT ONCE FOR THIS MASTER RUN ==="
cd "$PROJECT_ROOT" || { echo "CRITICAL ERROR: Project root not found!"; exit 1; }

mvn clean package dependency:copy-dependencies -DskipTests > "${FILES_DIR}/logs/RQ0_Master_Build_$$.log" 2>&1
if [ $? -ne 0 ]; then
    echo "CRITICAL ERROR: Maven build FAILED! See logs/RQ0_Master_Build_$$.log"
    exit 1
fi
echo "=== COMPILATION FINISHED SUCCESSFULLY ==="

echo "=== STARTING RQ0 MASTER RUNNER ==="

for entry in "${CASES[@]}"; do
    set -- $entry
    CASE_NAME=$1
    SHORT_NAME=$2

    LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ0_Generation"
    mkdir -p "$LOG_DIR"

    echo "=================================================="
    echo "GENERATING PRODUCTS FOR CASE: $CASE_NAME"
    echo "=================================================="
    
    bash "$TARGET_SCRIPT" "$CASE_NAME" "$SHORT_NAME"
    
    echo "Waiting for product generation to complete for $CASE_NAME..."
    while pgrep -f "java.*ProductESGFxToEFGAndDOTFileWriter_${SHORT_NAME}" > /dev/null; do
        sleep 5
    done
    
    echo "GENERATION FINISHED FOR: $CASE_NAME"
    sleep 2
done

echo ""
echo "=================================================="
echo "ALL RQ0 GENERATION TASKS COMPLETED SUCCESSFULLY!"
echo "=================================================="
sleep 3