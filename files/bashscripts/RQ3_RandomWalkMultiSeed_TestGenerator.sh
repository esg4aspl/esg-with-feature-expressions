#!/bin/bash
# USAGE: ./RQ3_RandomWalkMultiSeed_TestGenerator.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then 
    echo "Error: Missing arguments. Usage: ./RQ3_RandomWalkMultiSeed_TestGenerator.sh <Case> <Short> <RunID> [ShardCount] [StartNode] [EndNode]"
    exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
RUN_PARAM=$3
SHARD_PARAM=$4
START_PARAM=$5
END_PARAM=$6

if [ "$RUN_PARAM" -ne 1 ]; then
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

if [ -n "$SHARD_PARAM" ]; then N=$SHARD_PARAM; else N=$([[ "$OSTYPE" == "darwin"* ]] && echo 4 || echo 40); fi
if [ -n "$START_PARAM" ]; then S_NODE=$START_PARAM; else S_NODE=0; fi
if [ -n "$END_PARAM" ]; then E_NODE=$END_PARAM; else E_NODE=$((N-1)); fi

if [[ "$OSTYPE" == "darwin"* ]]; then 
    XMS=512m; XMX=1200m 
else 
    XMS=512m; XMX=1500m 
fi

cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found"; exit 1; }

LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ3/MultiSeedRW"
mkdir -p "$LOG_DIR"

export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"

MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ3_RandomWalkMultiSeed_TestGenerator_${SHORT_NAME}"
JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== MULTI-SEED RW TEST GENERATION STARTED: SHARDS $S_NODE to $E_NODE ==="

for i in $(seq $S_NODE $E_NODE); do
  LOG="${LOG_DIR}/run_RQ3_MultiSeedRW_TestGen_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"
  
  N_SHARDS=$N SHARD=$i \
  nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &
  
  echo "Shard $i dispatched -> $LOG"
  sleep 0.2
done

echo "MULTI-SEED RW TEST GENERATION DISPATCHED FOR $CASE_NAME"