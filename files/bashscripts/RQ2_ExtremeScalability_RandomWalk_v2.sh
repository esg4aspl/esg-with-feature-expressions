#!/bin/bash
# RQ2 Extreme Scalability: Random Walk Baseline — v2 (TARGETED SHARD)
# USAGE: ./RQ2_ExtremeScalability_RandomWalk_v2.sh <CaseName> <ShortName> <RunID> [TotalShards] [ShardID] [EndShard]
#
# NOTE: In v2, START_SHARD and END_SHARD are typically the SAME value —
# the master runner invokes this script once per (shard, run) pair it
# needs to rerun. Kept 6-arg signature for drop-in compatibility with
# the original master runner.

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

# Total shards defaults to 80 on Linux (cluster), 4 on macOS (local test)
if [ -n "$SHARD_PARAM" ]; then
  N=$SHARD_PARAM
else
  if [[ "$OSTYPE" == "darwin"* ]]; then N=4; else N=80; fi
fi

# In v2, default START = END (single shard). If both provided, respect them.
if [ -n "$START_PARAM" ]; then
  S_NODE=$START_PARAM
else
  S_NODE=0
fi
if [ -n "$END_PARAM" ]; then
  E_NODE=$END_PARAM
else
  E_NODE=$S_NODE     # v2 default: single shard, not full range
fi

if [[ "$OSTYPE" == "darwin"* ]]; then
  XMS=512m; XMX=1200m
else
  XMS=512m; XMX=2000m
fi

# ============================================================
# TIMEOUT CONFIGURATION (unchanged from v1)
# ============================================================
if [[ "$CASE_NAME" == "HockertyShirts" ]] || [[ "$SHORT_NAME" == "HS" ]]; then
    MY_TIMEOUT_HOURS=6
    echo "⏱️  HockertyShirts detected: 6-hour timeout"
elif [[ "$CASE_NAME" == "syngovia" ]] || [[ "$SHORT_NAME" == "Svia" ]]; then
    if [ "$RUN_PARAM" -eq 1 ]; then
        MY_TIMEOUT_HOURS=0
        echo "⏱️  syngo.via Run #1: NO TIMEOUT"
    else
        MY_TIMEOUT_HOURS=8
        echo "⏱️  syngo.via Run #$RUN_PARAM: 8-hour timeout"
    fi
else
    MY_TIMEOUT_HOURS=0
    echo "✓ No timeout (run to completion)"
fi

cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found"; exit 1; }

LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ2/RandomWalk"
mkdir -p "$LOG_DIR"

export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"

MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ2_ExtremeScalability_RandomWalk_${SHORT_NAME}"
JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== RANDOM WALK (v2 TARGETED): SHARDS $S_NODE to $E_NODE | RunID: $RUN_PARAM ==="

for i in $(seq $S_NODE $E_NODE); do
  LOG="${LOG_DIR}/run_RQ2_RandomWalk_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"

  N_SHARDS=$N SHARD=$i runID=$RUN_PARAM TIMEOUT_HOURS=$MY_TIMEOUT_HOURS \
  nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &

  echo "  Shard $i dispatched -> $LOG"
  if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
done
sleep 3

echo "=== RANDOM WALK (v2) DISPATCH FINISHED ==="