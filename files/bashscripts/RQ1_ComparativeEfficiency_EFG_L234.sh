#!/bin/bash
# USAGE: ./RQ1_ComparativeEfficiency_EFG_L234.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]
# EXAMPLE: ./RQ1_ComparativeEfficiency_EFG_L234.sh SodaVendingMachine SVM 5 40 0 39

# === ARGUMENT VALIDATION ===
if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "ERROR: Missing required arguments."
  echo "Usage: ./RQ1_ComparativeEfficiency_EFG_L234.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]"
  echo "Example: ./RQ1_ComparativeEfficiency_EFG_L234.sh SodaVendingMachine SVM 1 4 0 3"
  exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
RUN_PARAM=$3
SHARD_PARAM=$4
START_PARAM=$5
END_PARAM=$6

# --- 1. DYNAMIC PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$FILES_DIR")"

# --- 2. CLUSTER SHARD LOGIC ---
if [ -n "$SHARD_PARAM" ]; then
  N=$SHARD_PARAM
else
  if [[ "$OSTYPE" == "darwin"* ]]; then N=4; else N=80; fi
fi

if [ -n "$START_PARAM" ]; then S_NODE=$START_PARAM; else S_NODE=0; fi
if [ -n "$END_PARAM" ]; then E_NODE=$END_PARAM; else E_NODE=$((N-1)); fi

# --- 3. MEMORY OPTIMIZATION ---
if [[ "$OSTYPE" == "darwin"* ]]; then
  XMS=512m; XMX=1200m
else
  XMS=512m; XMX=1500m
fi

# Navigate to project directory
cd "$PROJECT_DIR" || { echo "ERROR: Directory not found: $PROJECT_DIR"; exit 1; }

# --- 4. COMPILE & DEPENDENCIES ---
echo "Compiling project and gathering dependencies..."
LOG_DIR_BASE="${FILES_DIR}/logs/${CASE_NAME}/RQ1"
mkdir -p "$LOG_DIR_BASE"


#mvn clean package dependency:copy-dependencies -DskipTests > "$LOG_DIR_BASE/RQ1_EFG_build.log" 2>&1
#if [ $? -ne 0 ]; then
    #echo "COMPILATION ERROR! Check '$LOG_DIR_BASE/RQ1_EFG_build.log' for details."
    #exit 1
#fi

export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"
MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ1_ComparativeEfficiency_EFG_L234_${SHORT_NAME}"
JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

# =======================================================
echo "=== BENCHMARK START: SHARDS $S_NODE to $E_NODE (Total Context: $N) | RunID: $RUN_PARAM ==="

# --- 5. L_LEVEL EXECUTION LOOP (2, 3, 4) ---
for L_VAL in 2 3 4; do
    echo "--- Initiating Execution for L_LEVEL = $L_VAL ---"
    
    # Corrected folder name from ESGFx to EFG
    LOG_DIR="${LOG_DIR_BASE}/EFG_L${L_VAL}"
    mkdir -p "$LOG_DIR"

    for i in $(seq $S_NODE $E_NODE); do

      # Corrected log file prefix from ESGFx to EFG
      LOG="${LOG_DIR}/run_RQ1_EFG_L${L_VAL}_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"
      
      # Environmental variables injected here
      N_SHARDS=$N SHARD=$i runID=$RUN_PARAM L_LEVEL=$L_VAL \
      nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &
      
      echo "Shard $i dispatched -> $LOG"
      
      # Stagger start
      if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
    done
    
    # FIX: Wait for all shards of this L level to finish before starting next L
    echo "Waiting for all shards of L_LEVEL = $L_VAL to complete..."
    wait
    echo "L_LEVEL = $L_VAL completed."
    echo "--------------------------------------------------"
    
done
 
echo "PROCESS DISPATCHED FOR $CASE_NAME (EFG L2,3,4 | Range: $S_NODE-$E_NODE | RunID: $RUN_PARAM)"