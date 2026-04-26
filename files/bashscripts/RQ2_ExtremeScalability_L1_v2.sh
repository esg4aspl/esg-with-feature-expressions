#!/bin/bash
# RQ2 Extreme Scalability: ESG-Fx L1 — v2 (TARGETED SHARD)
# USAGE: ./RQ2_ExtremeScalability_L1_v2.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]
#
# v2 default: single shard (START = END = SHARD_ID).
# The master runner_v2 dispatches one invocation per (shard, run) pair.

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "❌ ERROR: Missing required arguments."
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
  N=$([[ "$OSTYPE" == "darwin"* ]] && echo 4 || echo 80)
fi

if [ -n "$START_PARAM" ]; then
  S_NODE=$START_PARAM
else
  S_NODE=0
fi
if [ -n "$END_PARAM" ]; then
  E_NODE=$END_PARAM
else
  E_NODE=$S_NODE     # v2 default: single shard
fi

if [[ "$OSTYPE" == "darwin"* ]]; then
  XMS=512m; XMX=1200m
else
  XMS=512m; XMX=1500m
fi

if [[ "$CASE_NAME" == "HockertyShirts" ]] || [[ "$CASE_NAME" == "HS" ]]; then
    MY_TIMEOUT_HOURS=6
else
    MY_TIMEOUT_HOURS=0
fi

cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found"; exit 1; }

LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ2/ESGFx/L1"
mkdir -p "$LOG_DIR"

export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"

MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ2_ExtremeScalability_L1_${SHORT_NAME}"
JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== 🚀 EXTREME SCALABILITY L1 (v2 TARGETED): SHARDS $S_NODE to $E_NODE | RunID: $RUN_PARAM ==="

for i in $(seq $S_NODE $E_NODE); do
  LOG="${LOG_DIR}/run_RQ2_L1_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"

  N_SHARDS=$N SHARD=$i runID=$RUN_PARAM TIMEOUT_HOURS=$MY_TIMEOUT_HOURS \
  nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &

  echo "✅ Shard $i dispatched -> $LOG"
  if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
done
sleep 3

echo "=== L1 (v2) DISPATCH FINISHED ==="