#!/bin/bash
# RQ2 Extreme Scalability: EFG Baseline (L=2,3,4)
# USAGE: ./RQ2_ExtremeScalability_EFG_L234.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "ERROR: Missing required arguments."
  echo "USAGE: $0 <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]"
  exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
RUN_PARAM=$3
SHARD_PARAM=$4
START_PARAM=$5
END_PARAM=$6

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

if [ -n "$SHARD_PARAM" ]; then
  N=$SHARD_PARAM
else
  if [[ "$OSTYPE" == "darwin"* ]]; then N=4; else N=80; fi
fi

if [ -n "$START_PARAM" ]; then S_NODE=$START_PARAM; else S_NODE=0; fi
if [ -n "$END_PARAM" ]; then E_NODE=$END_PARAM; else E_NODE=$((N-1)); fi

if [[ "$OSTYPE" == "darwin"* ]]; then
  XMS=512m; XMX=1200m
else
  XMS=512m; XMX=1500m
fi

# ============================================================
# TIMEOUT CONFIGURATION - CORRECTED
# ============================================================
# HockertyShirts: 6h timeout (billions of products)
# syngo.via: 6h timeout (EFG expected to fail due to coverage degradation)
# Tesla: NO timeout (expected to complete in ~3 hours)
# Others: NO timeout (small/medium SPLs complete quickly)

if [[ "$CASE_NAME" == "HockertyShirts" ]] || [[ "$SHORT_NAME" == "HS" ]]; then
    MY_TIMEOUT_HOURS=6
    echo "⏱️  HockertyShirts detected: Setting 6-hour timeout for throughput measurement"
elif [[ "$CASE_NAME" == "syngovia" ]] || [[ "$SHORT_NAME" == "Svia" ]]; then
    MY_TIMEOUT_HOURS=1
    echo "⏱️  syngo.via detected: Setting 1-hour timeout (EFG expected to fail)"
else
    MY_TIMEOUT_HOURS=0
    echo "✓ No timeout (run to completion)"
fi

cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found"; exit 1; }

LOG_DIR_BASE="${FILES_DIR}/logs/${CASE_NAME}/RQ2"
mkdir -p "$LOG_DIR_BASE"


#mvn clean package dependency:copy-dependencies -DskipTests > "$LOG_DIR_BASE/RQ2_EFG_L234_build.log" 2>&1
export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"

MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ2_ExtremeScalability_EFG_L234_${SHORT_NAME}"
JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== EFG EXTREME SCALABILITY START: SHARDS $S_NODE to $E_NODE | RunID: $RUN_PARAM ==="

for L_VAL in 2 3 4; do
    echo "--- Starting EFG L=$L_VAL ---"
    
    LOG_DIR="${LOG_DIR_BASE}/EFG_L${L_VAL}"
    mkdir -p "$LOG_DIR"

    for i in $(seq $S_NODE $E_NODE); do
      LOG="${LOG_DIR}/run_RQ2_EFG_L${L_VAL}_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"
      
      N_SHARDS=$N SHARD=$i runID=$RUN_PARAM TIMEOUT_HOURS=$MY_TIMEOUT_HOURS L_LEVEL=$L_VAL \
      nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &
      
      echo "  Shard $i dispatched -> $LOG"
      if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
    done
    
    # FIX: Wait for all shards of this L level to finish before starting next L
    echo "  Waiting for EFG L=$L_VAL to finish..."
    wait
    echo "✓ EFG L=$L_VAL completed"
done
 
echo "=== EFG EXTREME SCALABILITY FINISHED: $CASE_NAME ==="